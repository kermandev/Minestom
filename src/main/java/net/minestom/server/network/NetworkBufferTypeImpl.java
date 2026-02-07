package net.minestom.server.network;

import com.google.gson.JsonElement;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.codec.stream.StreamCodec;
import net.minestom.server.codec.stream.StreamReader;
import net.minestom.server.codec.stream.StreamWriter;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.RegistryTranscoder;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.json.JsonUtil;
import net.minestom.server.utils.nbt.BinaryTagReader;
import net.minestom.server.utils.nbt.BinaryTagWriter;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

final class NetworkBufferTypeImpl {
    static final int SEGMENT_BITS = 0x7F;
    static final int CONTINUE_BIT = 0x80;

    record NbtType() implements StreamCodec<BinaryTag> {
        static final NbtType TYPE = new NbtType();

        @SuppressWarnings("unchecked")
        public static <T extends BinaryTag> StreamCodec<T> typed() {
            return (StreamCodec<T>) TYPE;
        }

        @Override
        public BinaryTag decode(StreamReader stream) throws RuntimeException {
            if (!(stream instanceof NetworkBuffer buffer)) {
                throw new IllegalArgumentException("Stream must be a NetworkBuffer");
            }
            final BinaryTagReader nbtReader = new BinaryTagReader(buffer.ioView());
            try {
                return nbtReader.readNameless();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void encode(StreamWriter stream, BinaryTag value) throws RuntimeException {
            if (!(stream instanceof NetworkBuffer buffer)) {
                throw new IllegalArgumentException("Stream must be a NetworkBuffer");
            }
            final BinaryTagWriter nbtWriter = new BinaryTagWriter(buffer.ioView());
            try {
                nbtWriter.writeNameless(value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    record OptionalNBTType() implements StreamCodec<@Nullable BinaryTag> {
        static final OptionalNBTType INSTANCE = new OptionalNBTType();

        @SuppressWarnings("unchecked")
        static <T extends @Nullable BinaryTag> StreamCodec<T> typed() {
            return (StreamCodec<T>) INSTANCE;
        }

        @Override
        public @Nullable BinaryTag decode(StreamReader stream) throws RuntimeException {
            var type = NbtType.TYPE.decode(stream);
            // TAG_END == null
            if (type == EndBinaryTag.endBinaryTag()) return null;
            return type;
        }

        @Override
        public void encode(StreamWriter stream, @Nullable BinaryTag value) throws RuntimeException {
            if (value != null) {
                NbtType.TYPE.encode(stream, value);
            } else {
                // TAG_END
                stream.writeByte((byte) 0x00);
            }
        }
    }

    record BlockPositionType() implements StreamCodec<Point> {

        @Override
        public Point decode(StreamReader stream) throws RuntimeException {
            final long value = stream.takeLong();
            final int x = (int) (value >> 38);
            final int y = (int) (value << 52 >> 52);
            final int z = (int) (value << 26 >> 38);
            return new BlockVec(x, y, z);
        }

        @Override
        public void encode(StreamWriter stream, Point value) throws RuntimeException {
            final int blockX = value.blockX();
            final int blockY = value.blockY();
            final int blockZ = value.blockZ();
            final long longPos = (((long) blockX & 0x3FFFFFF) << 38) |
                    (((long) blockZ & 0x3FFFFFF) << 12) |
                    ((long) blockY & 0xFFF);
            stream.writeLong(longPos);
        }
    }

    record JsonComponentType() implements StreamCodec<Component> {
        @Override
        public void write(NetworkBuffer buffer, Component value) {
            final Registries registries = buffer.registries();
            final Transcoder<JsonElement> coder = registries != null
                    ? new RegistryTranscoder<>(Transcoder.JSON, registries)
                    : Transcoder.JSON;
            final String json = JsonUtil.toJson(Codec.COMPONENT.encode(coder, value).orElseThrow());
            buffer.write(STRING, json);
        }

        @Override
        public Component read(NetworkBuffer buffer) {
            final Registries registries = buffer.registries();
            final Transcoder<JsonElement> coder = registries != null
                    ? new RegistryTranscoder<>(Transcoder.JSON, registries)
                    : Transcoder.JSON;
            final JsonElement json = JsonUtil.fromJson(buffer.read(STRING));
            return Codec.COMPONENT.decode(coder, json).orElseThrow();
        }
    }

    record UUIDType() implements StreamCodec<UUID> {
        @Override
        public void write(NetworkBuffer buffer, java.util.UUID value) {
            buffer.write(LONG, value.getMostSignificantBits());
            buffer.write(LONG, value.getLeastSignificantBits());
        }

        @Override
        public java.util.UUID read(NetworkBuffer buffer) {
            final long mostSignificantBits = buffer.read(LONG);
            final long leastSignificantBits = buffer.read(LONG);
            return new UUID(mostSignificantBits, leastSignificantBits);
        }
    }

    record PosType() implements StreamCodec<Pos> {
        @Override
        public void write(NetworkBuffer buffer, Pos value) {
            buffer.write(DOUBLE, value.x());
            buffer.write(DOUBLE, value.y());
            buffer.write(DOUBLE, value.z());
            buffer.write(FLOAT, value.yaw());
            buffer.write(FLOAT, value.pitch());
        }

        @Override
        public Pos read(NetworkBuffer buffer) {
            final double x = buffer.read(DOUBLE);
            final double y = buffer.read(DOUBLE);
            final double z = buffer.read(DOUBLE);
            final float yaw = buffer.read(FLOAT);
            final float pitch = buffer.read(FLOAT);
            return new Pos(x, y, z, yaw, pitch);
        }
    }

    record ByteArrayType() implements StreamCodec<byte[]> {
        @Override
        public void write(NetworkBuffer buffer, byte[] value) {
            buffer.write(VAR_INT, value.length);
            buffer.write(RAW_BYTES, value);
        }

        @Override
        public byte[] read(NetworkBuffer buffer) {
            final int length = buffer.read(VAR_INT);
            if (length == 0) return new byte[0];
            final long remaining = buffer.readableBytes();
            Check.argCondition(length > remaining, "String is too long (length: {0}, readable: {1})", length, remaining);
            return buffer.read(FixedRawBytes(length));
        }
    }

    record LongArrayType() implements StreamCodec<long[]> {
        @Override
        public void write(NetworkBuffer buffer, long[] value) {
            buffer.write(VAR_INT, value.length);
            for (long l : value) buffer.write(LONG, l);
        }

        @Override
        public long[] read(NetworkBuffer buffer) {
            final int length = buffer.read(VAR_INT);
            final long[] longs = new long[length];
            for (int i = 0; i < length; i++) longs[i] = buffer.read(LONG);
            return longs;
        }
    }

    record VarIntArrayType() implements StreamCodec<int[]> {
        @Override
        public void write(NetworkBuffer buffer, int[] value) {
            buffer.write(VAR_INT, value.length);
            for (int i : value) buffer.write(VAR_INT, i);
        }

        @Override
        public int[] read(NetworkBuffer buffer) {
            final int length = buffer.read(VAR_INT);
            final int[] ints = new int[length];
            for (int i = 0; i < length; i++) ints[i] = buffer.read(VAR_INT);
            return ints;
        }
    }

    record VarLongArrayType() implements StreamCodec<long[]> {
        @Override
        public void write(NetworkBuffer buffer, long[] value) {
            buffer.write(VAR_INT, value.length);
            for (long l : value) buffer.write(VAR_LONG, l);
        }

        @Override
        public long[] read(NetworkBuffer buffer) {
            final int length = buffer.read(VAR_INT);
            final long[] longs = new long[length];
            for (int i = 0; i < length; i++) longs[i] = buffer.read(VAR_LONG);
            return longs;
        }
    }

    record Vector3Type() implements StreamCodec<Point> {
        @Override
        public void write(NetworkBuffer buffer, Point value) {
            buffer.write(FLOAT, (float) value.x());
            buffer.write(FLOAT, (float) value.y());
            buffer.write(FLOAT, (float) value.z());
        }

        @Override
        public Point read(NetworkBuffer buffer) {
            final float x = buffer.read(FLOAT);
            final float y = buffer.read(FLOAT);
            final float z = buffer.read(FLOAT);
            return new Vec(x, y, z);
        }
    }

    record Vector3DType() implements Type<Point> {
        @Override
        public void write(NetworkBuffer buffer, Point value) {
            buffer.write(DOUBLE, value.x());
            buffer.write(DOUBLE, value.y());
            buffer.write(DOUBLE, value.z());
        }

        @Override
        public Point read(NetworkBuffer buffer) {
            final double x = buffer.read(DOUBLE);
            final double y = buffer.read(DOUBLE);
            final double z = buffer.read(DOUBLE);
            return new Vec(x, y, z);
        }
    }

    record Vector3IType() implements Type<Point> {
        @Override
        public void write(NetworkBuffer buffer, Point value) {
            buffer.write(VAR_INT, (int) value.x());
            buffer.write(VAR_INT, (int) value.y());
            buffer.write(VAR_INT, (int) value.z());
        }

        @Override
        public Point read(NetworkBuffer buffer) {
            final int x = buffer.read(VAR_INT);
            final int y = buffer.read(VAR_INT);
            final int z = buffer.read(VAR_INT);
            return new BlockVec(x, y, z);
        }
    }

    record Vector3BType() implements Type<Point> {
        @Override
        public void write(NetworkBuffer buffer, Point value) {
            buffer.write(BYTE, (byte) value.x());
            buffer.write(BYTE, (byte) value.y());
            buffer.write(BYTE, (byte) value.z());
        }

        @Override
        public Point read(NetworkBuffer buffer) {
            final byte x = buffer.read(BYTE);
            final byte y = buffer.read(BYTE);
            final byte z = buffer.read(BYTE);
            return new BlockVec(x, y, z);
        }
    }

    record LpVector3Type() implements StreamCodec<Vec> {
        public static final double ABS_MAX_VALUE = 1.7179869183E10;
        public static final double ABS_MIN_VALUE = 3.051944088384301E-5;
        private static final int DATA_BITS_MASK = 0b111111111111111;
        private static final double MAX_QUANTIZED_VALUE = 32766.0;
        private static final int SCALE_BITS_MASK = 0b11;
        private static final int CONTINUATION_FLAG = 4;
        private static final int X_OFFSET = 3;
        private static final int Y_OFFSET = 18;
        private static final int Z_OFFSET = 33;

        private static double sanitize(double value) {
            return Double.isNaN(value) ? 0.0 : Math.clamp(value, -ABS_MAX_VALUE, ABS_MAX_VALUE);
        }

        private static long pack(double value) {
            return Math.round((value * 0.5 + 0.5) * MAX_QUANTIZED_VALUE);
        }

        private static double unpack(long value) {
            return Math.min((double) (value & DATA_BITS_MASK), MAX_QUANTIZED_VALUE) * 2.0 / MAX_QUANTIZED_VALUE - 1.0;
        }

        @Override
        public Vec decode(StreamReader stream) throws RuntimeException {
            int flags = Byte.toUnsignedInt(stream.takeByte());
            if (flags == 0) {
                return Vec.ZERO;
            } else {
                int p2 = Byte.toUnsignedInt(stream.takeByte());
                long p3 = Integer.toUnsignedLong(stream.takeInt());
                long value = p3 << 16 | p2 << 8 | flags;
                long scale = flags & SCALE_BITS_MASK;
                if ((flags & CONTINUATION_FLAG) == CONTINUATION_FLAG)
                    scale |= (stream.takeVarInt() & 0xFFFFFFFFL) << 2;
                return new Vec(
                        unpack(value >> X_OFFSET) * scale,
                        unpack(value >> Y_OFFSET) * scale,
                        unpack(value >> Z_OFFSET) * scale
                );
            }
        }

        @Override
        public void encode(StreamWriter stream, Vec value) throws RuntimeException {
            double x = sanitize(value.x()), y = sanitize(value.y()), z = sanitize(value.z());
            double max = MathUtils.absMax(x, MathUtils.absMax(y, z));
            if (max < ABS_MIN_VALUE) {
                stream.writeByte((byte) 0);
            } else {
                long i = MathUtils.ceilLong(max);
                boolean hasContinuation = (i & SCALE_BITS_MASK) != i;
                long flags = hasContinuation ? i & SCALE_BITS_MASK | CONTINUATION_FLAG : i;
                long px = pack(x / i) << X_OFFSET;
                long py = pack(y / i) << Y_OFFSET;
                long pz = pack(z / i) << Z_OFFSET;
                long packed = flags | px | py | pz;
                stream.writeByte((byte) packed);
                stream.writeByte((byte) (packed >> 8));
                stream.writeInt((int) (packed >> 16));
                if (hasContinuation)
                    stream.writeVarInt((int) (i >> 2));
            }
        }
    }

    record QuaternionType() implements StreamCodec<float[]> {
        @Override
        public float[] decode(StreamReader stream) throws RuntimeException {
            return stream.takeFloats(4);
        }

        @Override
        public void encode(StreamWriter stream, float[] value) throws RuntimeException {
            Check.argCondition(value.length != 4, "Invalid quaternion length: {0}", value.length);
            stream.writeFloats(value);
        }
    }

    // Combinators

    record LengthPrefixedType<T>(Type<T> parent, int maxLength) implements Type<T> {
        public LengthPrefixedType {
            Objects.requireNonNull(parent, "parent");
            Check.argCondition(maxLength < 0, "length is negative found {0}", maxLength);
        }

        @Override
        public void write(NetworkBuffer buffer, T value) {
            // Write to another buffer and copy (kinda inefficient, but currently unused serverside so its ok for now)
            final byte[] componentData = NetworkBuffer.makeArray(parent, value, buffer.registries());
            buffer.write(NetworkBuffer.BYTE_ARRAY, componentData);
        }

        @Override
        public T read(NetworkBuffer buffer) {
            final int length = buffer.read(VAR_INT);
            Check.argCondition(length > maxLength, "Value is too long (length: {0}, max: {1})", length, maxLength);

            final long availableBytes = buffer.readableBytes();
            Check.argCondition(length > availableBytes, "Value is too long (length: {0}, available: {1})", length, availableBytes);
            final T value = parent.read(buffer);
            Check.argCondition(buffer.readableBytes() != availableBytes - length, "Value is too short (length: {0}, available: {1})", length, availableBytes);

            return value;
        }
    }

    record TypedNbtType<T>(Codec<T> nbtType) implements Type<T> {
        public TypedNbtType {
            Objects.requireNonNull(nbtType, "nbtType");
        }

        @Override
        public void write(NetworkBuffer buffer, T value) {
            final Registries registries = buffer.registries();
            Check.stateCondition(registries == null, "Buffer does not have registries");
            final Result<BinaryTag> result = nbtType.encode(new RegistryTranscoder<>(Transcoder.NBT, registries), value);
            switch (result) {
                case Result.Ok(BinaryTag tag) -> buffer.write(NBT, tag);
                case Result.Error(String message) -> throw new IllegalArgumentException("Invalid NBT tag: " + message);
            }
        }

        @Override
        public T read(NetworkBuffer buffer) {
            final Registries registries = buffer.registries();
            Check.stateCondition(registries == null, "Buffer does not have registries");
            final Result<T> result = nbtType.decode(new RegistryTranscoder<>(Transcoder.NBT, registries), buffer.read(NBT));
            return switch (result) {
                case Result.Ok(T value) -> value;
                case Result.Error(String message) -> throw new IllegalArgumentException("Invalid NBT tag: " + message);
            };
        }
    }

    /**
     * Used to write Java's UTF format, used primarily for {@link NetworkBuffer.IOView#writeUTF(String)}
     * This is not a pretty gross implementation cause it closely follows {@link java.io.DataOutputStream}
     * which optimizes for ascii for both read and write. This is quite expensive to write regardless as it requires
     * a few iterations to write.
     */
    record StringIOUTFType() implements Type<String> {
        static final int MAX_BYTE_LEN = 65535;

        @SuppressWarnings("deprecation")
        // Follows java.io.DataOutputStream#writeUTF(DataOutput, String) for JDK 25, not public sadly.
        @Override
        public void write(NetworkBuffer buffer, String value) {
            final int strlen = value.length();
            int utflen = strlen; // optimized for ASCII
            int copyableBytes = 0;

            for (int i = 0; i < strlen; i++) {
                int c = value.charAt(i);
                if (c >= 0x80 || c == 0)
                    utflen += (c >= 0x800) ? 2 : 1;
                if (strlen == utflen)
                    copyableBytes++; // We have no access to JLA for this.
            }

            if (utflen > MAX_BYTE_LEN || /* overflow */ utflen < strlen)
                throw new RuntimeException("UTF-8 string too long");

            buffer.write(UNSIGNED_SHORT, utflen);
            buffer.ensureWritable(utflen); // throw early if possible
            var impl = buffer.direct();
            if (copyableBytes > 0) { // write if we have any copyableBytes
                byte[] ascii = new byte[copyableBytes];
                value.getBytes(0, copyableBytes, ascii, 0);
                impl.putBytes(offset, ascii);
            }

            for (int i = copyableBytes; i < strlen; i++) { // Excerpt from ModifiedUtf#putChar
                int c = value.charAt(i);
                if (c != 0 && c < 0x80) {
                    impl.putByte(offset++, (byte) c);
                } else if (c >= 0x800) {
                    impl.putByte(offset++, (byte) (0xE0 | c >> 12 & 0x0F));
                    impl.putByte(offset++, (byte) (0x80 | c >> 6 & 0x3F));
                    impl.putByte(offset++, (byte) (0x80 | c & 0x3F));
                } else {
                    impl.putByte(offset++, (byte) (0xC0 | c >> 6 & 0x1F));
                    impl.putByte(offset++, (byte) (0x80 | c & 0x3F));
                }
            }
            buffer.writeIndex(offset);
        }

        @Override
        public String read(NetworkBuffer buffer) {
            var ioView = buffer.ioView();
            try { // DataInputStream only has readUTF sadly.
                return DataInputStream.readUTF(ioView);
            } catch (IOException e) {
                throw new IllegalStateException("failed to read string", e);
            }
        }
    }
}
