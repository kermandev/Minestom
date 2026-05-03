package net.minestom.server.network;

import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.RegistryTranscoder;
import net.minestom.server.utils.Either;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.Unit;
import net.minestom.server.utils.json.JsonUtil;
import net.minestom.server.utils.nbt.BinaryTagReader;
import net.minestom.server.utils.nbt.BinaryTagWriter;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minestom.server.network.NetworkBuffer.*;
import static net.minestom.server.network.NetworkBufferImpl.impl;

public interface NetworkBufferTypeImpl<T> extends NetworkBuffer.Type<T> {
    int SEGMENT_BITS = 0x7F;
    int CONTINUE_BIT = 0x80;

    public static int varIntSize(int value) {
        int size = 1;
        while ((value & ~SEGMENT_BITS) != 0) {
            size++;
            value >>>= 7;
        }
        return size;
    }

    public static int varLongSize(long value) {
        int size = 1;
        while ((value & ~((long) SEGMENT_BITS)) != 0) {
            size++;
            value >>>= 7;
        }
        return size;
    }

    public static long writeVarIntUnchecked(NetworkBufferImpl impl, long index, int value) {
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                impl._putByteUnchecked(index, (byte) value);
                return index + 1;
            }
            impl._putByteUnchecked(index++, (byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));
            value >>>= 7;
        }
    }

    public static long writeVarLongUnchecked(NetworkBufferImpl impl, long index, long value) {
        while (true) {
            if ((value & ~((long) SEGMENT_BITS)) == 0) {
                impl._putByteUnchecked(index, (byte) value);
                return index + 1;
            }
            impl._putByteUnchecked(index++, (byte) (value & SEGMENT_BITS | CONTINUE_BIT));
            value >>>= 7;
        }
    }

    public static int readVarInt(NetworkBuffer buffer) {
        long index = buffer.readIndex();
        int result = 0;
        for (int shift = 0; ; shift += 7) {
            byte b = impl(buffer)._getByte(index++);
            result |= (b & 0x7f) << shift;
            if (b >= 0) {
                buffer.advanceRead(index - buffer.readIndex());
                return result;
            }
        }
    }

    public static long readVarLong(NetworkBuffer buffer) {
        int length = 0;
        long value = 0;
        int position = 0;
        byte currentByte;
        while (true) {
            currentByte = impl(buffer)._getByte(buffer.readIndex() + length);
            length++;
            value |= (long) (currentByte & SEGMENT_BITS) << position;
            if ((currentByte & CONTINUE_BIT) == 0) break;
            position += 7;
            if (position >= 64) throw new RuntimeException("VarLong is too big");
        }
        buffer.advanceRead(length);
        return value;
    }

    static void writeFixedBytes(NetworkBuffer buffer, byte[] value, int expectedLength) {
        checkFixedBytesLength(value, expectedLength);
        if (expectedLength == 0) return;
        final NetworkBufferImpl impl = impl(buffer);
        impl._putBytesUnchecked(impl.reserveWrite(expectedLength), value, 0, expectedLength);
    }

    static byte[] checkFixedBytesLength(byte[] value, int expectedLength) {
        if (value.length != expectedLength) {
            throw new IllegalArgumentException("Invalid length: " + value.length + " != " + expectedLength);
        }
        return value;
    }

    static byte[] readFixedBytes(NetworkBuffer buffer, int length) {
        if (length == 0) return new byte[0];
        final byte[] bytes = new byte[length];
        final NetworkBufferImpl impl = impl(buffer);
        impl._getBytesUnchecked(impl.reserveRead(length), bytes, 0, length);
        return bytes;
    }

    static void writeByteArray(NetworkBuffer buffer, byte[] value, int maxLength) {
        final int length = value.length;
        Check.argCondition(length > maxLength, "Array length ({0}) is higher than the maximum allowed length ({1})", length, maxLength);
        final int prefixSize = varIntSize(length);
        final long totalSize = Math.addExact(prefixSize, length);
        final NetworkBufferImpl impl = impl(buffer);
        final long base = impl.reserveWrite(totalSize);
        writeVarIntUnchecked(impl, base, length);
        if (length > 0) impl._putBytesUnchecked(base + prefixSize, value, 0, length);
    }

    static byte[] readByteArray(NetworkBuffer buffer, int maxLength) {
        final int length = readVarInt(buffer);
        Check.argCondition(length < 0, "Array length cannot be negative: {0}", length);
        Check.argCondition(length > maxLength, "Array length ({0}) is higher than the maximum allowed length ({1})", length, maxLength);
        return readFixedBytes(buffer, length);
    }

    static void writeStringUtf8(NetworkBuffer buffer, String value, int maxLength) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeByteArray(buffer, bytes, maxLength);
    }

    static String readStringUtf8(NetworkBuffer buffer, int maxLength) {
        final byte[] bytes = readByteArray(buffer, maxLength);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public record UnitType() implements NetworkBufferTypeImpl<Unit> {
        @Override
        public void write(NetworkBuffer buffer, Unit value) {
        }

        @Override
        public Unit read(NetworkBuffer buffer) {
            return Unit.INSTANCE;
        }
    }

    public record BooleanType() implements NetworkBufferTypeImpl<Boolean> {
        @Override
        public void write(NetworkBuffer buffer, Boolean value) {
            final NetworkBufferImpl impl = impl(buffer);
            impl._putByteUnchecked(impl.reserveWrite(1), value ? (byte) 1 : (byte) 0);
        }

        @Override
        public Boolean read(NetworkBuffer buffer) {
            final NetworkBufferImpl impl = impl(buffer);
            final byte value = impl._getByteUnchecked(impl.reserveRead(1));
            return value != 0;
        }
    }

    public record ByteType() implements NetworkBufferTypeImpl<Byte> {
        @Override
        public void write(NetworkBuffer buffer, Byte value) {
            final NetworkBufferImpl impl = impl(buffer);
            impl._putByteUnchecked(impl.reserveWrite(1), value);
        }

        @Override
        public Byte read(NetworkBuffer buffer) {
            final NetworkBufferImpl impl = impl(buffer);
            final byte value = impl._getByteUnchecked(impl.reserveRead(1));
            return value;
        }
    }

    public record UnsignedByteType() implements NetworkBufferTypeImpl<Short> {
        @Override
        public void write(NetworkBuffer buffer, Short value) {
            final NetworkBufferImpl impl = impl(buffer);
            impl._putByteUnchecked(impl.reserveWrite(1), (byte) (value & 0xFF));
        }

        @Override
        public Short read(NetworkBuffer buffer) {
            final NetworkBufferImpl impl = impl(buffer);
            final byte value = impl._getByteUnchecked(impl.reserveRead(1));
            return (short) (value & 0xFF);
        }
    }

    public record ShortType() implements NetworkBufferTypeImpl<Short> {
        @Override
        public void write(NetworkBuffer buffer, Short value) {
            final NetworkBufferImpl impl = impl(buffer);
            impl._putShortUnchecked(impl.reserveWrite(2), value);
        }

        @Override
        public Short read(NetworkBuffer buffer) {
            final NetworkBufferImpl impl = impl(buffer);
            final short value = impl._getShortUnchecked(impl.reserveRead(2));
            return value;
        }
    }

    public record UnsignedShortType() implements NetworkBufferTypeImpl<Integer> {
        @Override
        public void write(NetworkBuffer buffer, Integer value) {
            final NetworkBufferImpl impl = impl(buffer);
            impl._putShortUnchecked(impl.reserveWrite(2), (short) (value & 0xFFFF));
        }

        @Override
        public Integer read(NetworkBuffer buffer) {
            final NetworkBufferImpl impl = impl(buffer);
            final short value = impl._getShortUnchecked(impl.reserveRead(2));
            return value & 0xFFFF;
        }
    }

    public record IntType() implements NetworkBufferTypeImpl<Integer> {
        @Override
        public void write(NetworkBuffer buffer, Integer value) {
            final NetworkBufferImpl impl = impl(buffer);
            impl._putIntUnchecked(impl.reserveWrite(4), value);
        }

        @Override
        public Integer read(NetworkBuffer buffer) {
            final NetworkBufferImpl impl = impl(buffer);
            final int value = impl._getIntUnchecked(impl.reserveRead(4));
            return value;
        }
    }

    public record UnsignedIntType() implements NetworkBufferTypeImpl<Long> {
        @Override
        public void write(NetworkBuffer buffer, Long value) {
            final NetworkBufferImpl impl = impl(buffer);
            impl._putIntUnchecked(impl.reserveWrite(4), (int) (value & 0xFFFFFFFFL));
        }

        @Override
        public Long read(NetworkBuffer buffer) {
            final NetworkBufferImpl impl = impl(buffer);
            final int value = impl._getIntUnchecked(impl.reserveRead(4));
            return value & 0xFFFFFFFFL;
        }
    }

    public record LongType() implements NetworkBufferTypeImpl<Long> {
        @Override
        public void write(NetworkBuffer buffer, Long value) {
            final NetworkBufferImpl impl = impl(buffer);
            impl._putLongUnchecked(impl.reserveWrite(8), value);
        }

        @Override
        public Long read(NetworkBuffer buffer) {
            final NetworkBufferImpl impl = impl(buffer);
            final long value = impl._getLongUnchecked(impl.reserveRead(8));
            return value;
        }
    }

    public record FloatType() implements NetworkBufferTypeImpl<Float> {
        @Override
        public void write(NetworkBuffer buffer, Float value) {
            final NetworkBufferImpl impl = impl(buffer);
            impl._putFloatUnchecked(impl.reserveWrite(4), value);
        }

        @Override
        public Float read(NetworkBuffer buffer) {
            final NetworkBufferImpl impl = impl(buffer);
            final float value = impl._getFloatUnchecked(impl.reserveRead(4));
            return value;
        }
    }

    public record DoubleType() implements NetworkBufferTypeImpl<Double> {
        @Override
        public void write(NetworkBuffer buffer, Double value) {
            final NetworkBufferImpl impl = impl(buffer);
            impl._putDoubleUnchecked(impl.reserveWrite(8), value);
        }

        @Override
        public Double read(NetworkBuffer buffer) {
            final NetworkBufferImpl impl = impl(buffer);
            final double value = impl._getDoubleUnchecked(impl.reserveRead(8));
            return value;
        }
    }

    public record VarIntType() implements NetworkBufferTypeImpl<Integer> {
        @Override
        public void write(NetworkBuffer buffer, Integer boxed) {
            int value = boxed;
            var nio = impl(buffer);
            writeVarIntUnchecked(nio, nio.reserveWrite(varIntSize(value)), value);
        }

        @Override
        public Integer read(NetworkBuffer buffer) {
            return readVarInt(buffer);
        }
    }

    public record OptionalVarIntType() implements NetworkBufferTypeImpl<@Nullable Integer> {
        @Override
        public void write(NetworkBuffer buffer, @Nullable Integer value) {
            buffer.write(VAR_INT, value == null ? 0 : value + 1);
        }

        @Override
        public @Nullable Integer read(NetworkBuffer buffer) {
            final int value = buffer.read(VAR_INT);
            return value == 0 ? null : value - 1;
        }
    }

    public record VarInt3Type() implements NetworkBufferTypeImpl<Integer> {
        @Override
        public void write(NetworkBuffer buffer, Integer boxed) {
            final int value = boxed;
            // Value must be between 0 and 2^21
            Check.argCondition(value < 0 || value >= (1 << 21), "VarInt3 out of bounds: {0}", value);
            var impl = impl(buffer);
            final long startIndex = impl.reserveWrite(3);
            impl._putByteUnchecked(startIndex, (byte) (value & 0x7F | 0x80));
            impl._putByteUnchecked(startIndex + 1, (byte) ((value >>> 7) & 0x7F | 0x80));
            impl._putByteUnchecked(startIndex + 2, (byte) (value >>> 14));
        }

        @Override
        public Integer read(NetworkBuffer buffer) {
            // Ensure that the buffer can read other var-int sizes
            // The optimization is mostly relevant for writing
            return buffer.read(VAR_INT);
        }
    }

    public record VarLongType() implements NetworkBufferTypeImpl<Long> {
        @Override
        public void write(NetworkBuffer buffer, Long value) {
            final NetworkBufferImpl impl = impl(buffer);
            writeVarLongUnchecked(impl, impl.reserveWrite(varLongSize(value)), value);
        }

        @Override
        public Long read(NetworkBuffer buffer) {
            return readVarLong(buffer);
        }
    }

    public record RawBytesType(int length) implements NetworkBufferTypeImpl<byte[]> {
        @Override
        public void write(NetworkBuffer buffer, byte[] value) {
            if (length != -1) {
                writeFixedBytes(buffer, value, length);
                return;
            }
            final int length = value.length;
            if (length == 0) return;
            final NetworkBufferImpl impl = impl(buffer);
            impl._putBytesUnchecked(impl.reserveWrite(length), value, 0, length);
        }

        @Override
        public byte[] read(NetworkBuffer buffer) {
            if (this.length != -1) return readFixedBytes(buffer, this.length);
            long length = buffer.readableBytes();
            if (length == 0) return new byte[0];
            assert length > 0 : "Invalid remaining: " + length;

            final int arrayLength = Math.toIntExact(length);
            final byte[] bytes = new byte[arrayLength];
            final NetworkBufferImpl impl = impl(buffer);
            impl._getBytesUnchecked(impl.reserveRead(arrayLength), bytes, 0, arrayLength);
            return bytes;
        }
    }

    public record StringType() implements NetworkBufferTypeImpl<String> {
        @Override
        public void write(NetworkBuffer buffer, String value) {
            writeStringUtf8(buffer, value, 1024 * 1024);
        }

        @Override
        public String read(NetworkBuffer buffer) {
            return readStringUtf8(buffer, 1024 * 1024);
        }
    }

    public record StringTerminatedType() implements NetworkBufferTypeImpl<String> {
        @Override
        public void write(NetworkBuffer buffer, String value) {
            final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            byte[] terminated = new byte[bytes.length + 1];
            System.arraycopy(bytes, 0, terminated, 0, bytes.length);
            terminated[terminated.length - 1] = 0;
            buffer.write(RAW_BYTES, terminated);
        }

        @Override
        public String read(NetworkBuffer buffer) {
            ByteArrayList bytes = new ByteArrayList();
            byte b;
            while ((b = buffer.read(BYTE)) != 0) {
                bytes.add(b);
            }
            return new String(bytes.elements(), StandardCharsets.UTF_8);
        }
    }

    public record NbtType() implements NetworkBufferTypeImpl<BinaryTag> {
        @Override
        public void write(NetworkBuffer buffer, BinaryTag value) {
            BinaryTagWriter nbtWriter = impl(buffer).nbtWriter();
            try {
                nbtWriter.writeNameless(value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public BinaryTag read(NetworkBuffer buffer) {
            BinaryTagReader nbtReader = impl(buffer).nbtReader();
            try {
                return nbtReader.readNameless();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public record BlockPositionType() implements NetworkBufferTypeImpl<Point> {
        @Override
        public void write(NetworkBuffer buffer, Point value) {
            final int blockX = value.blockX();
            final int blockY = value.blockY();
            final int blockZ = value.blockZ();
            final long longPos = (((long) blockX & 0x3FFFFFF) << 38) |
                    (((long) blockZ & 0x3FFFFFF) << 12) |
                    ((long) blockY & 0xFFF);
            buffer.write(LONG, longPos);
        }

        @Override
        public Point read(NetworkBuffer buffer) {
            final long value = buffer.read(LONG);
            final int x = (int) (value >> 38);
            final int y = (int) (value << 52 >> 52);
            final int z = (int) (value << 26 >> 38);
            return new Vec(x, y, z);
        }
    }

    public record JsonComponentType() implements NetworkBufferTypeImpl<Component> {
        @Override
        public void write(NetworkBuffer buffer, Component value) {
            final Transcoder<JsonElement> coder = buffer.registries() != null
                    ? new RegistryTranscoder<>(Transcoder.JSON, buffer.registries())
                    : Transcoder.JSON;
            final String json = JsonUtil.toJson(Codec.COMPONENT.encode(coder, value).orElseThrow());
            buffer.write(STRING, json);
        }

        @Override
        public Component read(NetworkBuffer buffer) {
            final Transcoder<JsonElement> coder = buffer.registries() != null
                    ? new RegistryTranscoder<>(Transcoder.JSON, buffer.registries())
                    : Transcoder.JSON;
            final JsonElement json = JsonUtil.fromJson(buffer.read(STRING));
            return Codec.COMPONENT.decode(coder, json).orElseThrow();
        }
    }

    public record UUIDType() implements NetworkBufferTypeImpl<UUID> {
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

    public record PosType() implements NetworkBufferTypeImpl<Pos> {
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

    public record ByteArrayType() implements NetworkBufferTypeImpl<byte[]> {
        @Override
        public void write(NetworkBuffer buffer, byte[] value) {
            writeByteArray(buffer, value, Integer.MAX_VALUE);
        }

        @Override
        public byte[] read(NetworkBuffer buffer) {
            return readByteArray(buffer, Integer.MAX_VALUE);
        }
    }

    public record LongArrayType() implements NetworkBufferTypeImpl<long[]> {
        @Override
        public void write(NetworkBuffer buffer, long[] value) {
            final int length = value.length;
            final int prefixSize = varIntSize(length);
            final long bodySize = Math.multiplyExact((long) length, Long.BYTES);
            final NetworkBufferImpl impl = impl(buffer);
            final long base = impl.reserveWrite(Math.addExact(prefixSize, bodySize));
            writeVarIntUnchecked(impl, base, length);
            for (int i = 0; i < length; i++) {
                impl._putLongUnchecked(base + prefixSize + (long) i * Long.BYTES, value[i]);
            }
        }

        @Override
        public long[] read(NetworkBuffer buffer) {
            final int length = buffer.read(VAR_INT);
            Check.argCondition(length < 0, "Array length cannot be negative: {0}", length);
            final long[] longs = new long[length];
            final long bodySize = Math.multiplyExact((long) length, Long.BYTES);
            final NetworkBufferImpl impl = impl(buffer);
            final long base = impl.reserveRead(bodySize);
            for (int i = 0; i < length; i++) {
                longs[i] = impl._getLongUnchecked(base + (long) i * Long.BYTES);
            }
            return longs;
        }
    }

    public record VarIntArrayType() implements NetworkBufferTypeImpl<int[]> {
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

    public record VarLongArrayType() implements NetworkBufferTypeImpl<long[]> {
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

    public record Vector3Type() implements NetworkBufferTypeImpl<Point> {
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

    public record Vector3DType() implements NetworkBufferTypeImpl<Point> {
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

    public record Vector3IType() implements NetworkBufferTypeImpl<Point> {
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
            return new Vec(x, y, z);
        }
    }

    public record Vector3BType() implements NetworkBufferTypeImpl<Point> {
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
            return new Vec(x, y, z);
        }
    }

    public record LpVector3Type() implements NetworkBufferTypeImpl<Vec> {
        private static final int DATA_BITS_MASK = 0b111111111111111;
        private static final double MAX_QUANTIZED_VALUE = 32766.0;
        private static final int SCALE_BITS_MASK = 0b11;
        private static final int CONTINUATION_FLAG = 4;
        private static final int X_OFFSET = 3;
        private static final int Y_OFFSET = 18;
        private static final int Z_OFFSET = 33;
        public static final double ABS_MAX_VALUE = 1.7179869183E10;
        public static final double ABS_MIN_VALUE = 3.051944088384301E-5;

        @Override
        public void write(NetworkBuffer buffer, Vec value) {
            double x = sanitize(value.x()), y = sanitize(value.y()), z = sanitize(value.z());
            double max = MathUtils.absMax(x, MathUtils.absMax(y, z));
            if (max < ABS_MIN_VALUE) {
                buffer.write(BYTE, (byte) 0);
            } else {
                long i = MathUtils.ceilLong(max);
                boolean hasContinuation = (i & SCALE_BITS_MASK) != i;
                long flags = hasContinuation ? i & SCALE_BITS_MASK | CONTINUATION_FLAG : i;
                long px = pack(x / i) << X_OFFSET;
                long py = pack(y / i) << Y_OFFSET;
                long pz = pack(z / i) << Z_OFFSET;
                long packed = flags | px | py | pz;
                buffer.write(BYTE, (byte) packed);
                buffer.write(BYTE, (byte) (packed >> 8));
                buffer.write(INT, (int) (packed >> 16));
                if (hasContinuation)
                    buffer.write(VAR_INT, (int) (i >> 2));
            }
        }

        @Override
        public Vec read(NetworkBuffer buffer) {
            int flags = buffer.read(UNSIGNED_BYTE);
            if (flags == 0) {
                return Vec.ZERO;
            } else {
                int p2 = buffer.read(UNSIGNED_BYTE);
                long p3 = buffer.read(UNSIGNED_INT);
                long value = p3 << 16 | p2 << 8 | flags;
                long scale = flags & SCALE_BITS_MASK;
                if ((flags & CONTINUATION_FLAG) == CONTINUATION_FLAG)
                    scale |= (buffer.read(VAR_INT) & 0xFFFFFFFFL) << 2;
                return new Vec(
                        unpack(value >> X_OFFSET) * scale,
                        unpack(value >> Y_OFFSET) * scale,
                        unpack(value >> Z_OFFSET) * scale
                );
            }
        }

        private static double sanitize(double value) {
            return Double.isNaN(value) ? 0.0 : Math.clamp(value, -ABS_MAX_VALUE, ABS_MAX_VALUE);
        }

        private static long pack(double value) {
            return Math.round((value * 0.5 + 0.5) * MAX_QUANTIZED_VALUE);
        }

        private static double unpack(long value) {
            return Math.min((double) (value & DATA_BITS_MASK), MAX_QUANTIZED_VALUE) * 2.0 / MAX_QUANTIZED_VALUE - 1.0;
        }
    }

    public record QuaternionType() implements NetworkBufferTypeImpl<float[]> {
        @Override
        public void write(NetworkBuffer buffer, float[] value) {
            buffer.write(FLOAT, value[0]);
            buffer.write(FLOAT, value[1]);
            buffer.write(FLOAT, value[2]);
            buffer.write(FLOAT, value[3]);
        }

        @Override
        public float[] read(NetworkBuffer buffer) {
            final float x = buffer.read(FLOAT);
            final float y = buffer.read(FLOAT);
            final float z = buffer.read(FLOAT);
            final float w = buffer.read(FLOAT);
            return new float[]{x, y, z, w};
        }
    }

    // Combinators

    public record EnumSetType<E extends Enum<E>>(Class<E> enumType,
                                          E[] values) implements NetworkBufferTypeImpl<EnumSet<E>> {
        @Override
        public void write(NetworkBuffer buffer, EnumSet<E> value) {
            BitSet bitSet = new BitSet(values.length);
            for (int i = 0; i < values.length; ++i) {
                bitSet.set(i, value.contains(values[i]));
            }
            final byte[] array = bitSet.toByteArray();
            buffer.write(RAW_BYTES, array);
        }

        @Override
        public EnumSet<E> read(NetworkBuffer buffer) {
            final byte[] array = buffer.read(FixedRawBytes((values.length + 7) / 8));
            BitSet bitSet = BitSet.valueOf(array);
            EnumSet<E> enumSet = EnumSet.noneOf(enumType);
            for (int i = 0; i < values.length; ++i) {
                if (bitSet.get(i)) {
                    enumSet.add(values[i]);
                }
            }
            return enumSet;
        }
    }

    public record FixedBitSetType(int length) implements NetworkBufferTypeImpl<BitSet> {
        @Override
        public void write(NetworkBuffer buffer, BitSet value) {
            final int setLength = value.length();
            if (setLength > length) {
                throw new IllegalArgumentException("BitSet is larger than expected size (" + setLength + ">" + length + ")");
            } else {
                final byte[] array = value.toByteArray();
                buffer.write(RAW_BYTES, array);
            }
        }

        @Override
        public BitSet read(NetworkBuffer buffer) {
            final byte[] array = buffer.read(FixedRawBytes((length + 7) / 8));
            return BitSet.valueOf(array);
        }
    }

    public record OptionalType<T>(Type<T> parent) implements NetworkBufferTypeImpl<@Nullable T> {
        @Override
        public void write(NetworkBuffer buffer, T value) {
            buffer.write(BOOLEAN, value != null);
            if (value != null) buffer.write(parent, value);
        }

        @Override
        public T read(NetworkBuffer buffer) {
            return buffer.read(BOOLEAN) ? buffer.read(parent) : null;
        }
    }

    public record LengthPrefixedType<T>(Type<T> parent, int maxLength) implements NetworkBufferTypeImpl<T> {
        @Override
        public void write(NetworkBuffer buffer, T value) {
            // Write to another buffer and copy (kinda inefficient, but currently unused serverside so its ok for now)
            final byte[] componentData = NetworkBuffer.makeArray(b -> parent.write(b, value), buffer.registries());
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

    final class LazyType<T> implements NetworkBufferTypeImpl<T> {
        private final Supplier<NetworkBuffer.Type<T>> supplier;
        private Type<T> type;

        public LazyType(Supplier<NetworkBuffer.Type<T>> supplier) {
            this.supplier = supplier;
        }

        @Override
        public void write(NetworkBuffer buffer, T value) {
            if (type == null) type = supplier.get();
            type.write(buffer, value);
        }

        @Override
        public T read(NetworkBuffer buffer) {
            if (type == null) type = supplier.get();
            return null;
        }
    }

    public record TypedNbtType<T>(Codec<T> nbtType) implements NetworkBufferTypeImpl<T> {
        @Override
        public void write(NetworkBuffer buffer, T value) {
            final Registries registries = impl(buffer).registries;
            Check.stateCondition(registries == null, "Buffer does not have registries");
            final Result<BinaryTag> result = nbtType.encode(new RegistryTranscoder<>(Transcoder.NBT, registries), value);
            switch (result) {
                case Result.Ok(BinaryTag tag) -> buffer.write(NBT, tag);
                case Result.Error(String message) -> throw new IllegalArgumentException("Invalid NBT tag: " + message);
            }
        }

        @Override
        public T read(NetworkBuffer buffer) {
            final Registries registries = impl(buffer).registries;
            Check.stateCondition(registries == null, "Buffer does not have registries");
            final Result<T> result = nbtType.decode(new RegistryTranscoder<>(Transcoder.NBT, registries), buffer.read(NBT));
            return switch (result) {
                case Result.Ok(T value) -> value;
                case Result.Error(String message) -> throw new IllegalArgumentException("Invalid NBT tag: " + message);
            };
        }
    }

    public record EitherType<L, R>(
            NetworkBuffer.Type<L> left,
            NetworkBuffer.Type<R> right
    ) implements NetworkBuffer.Type<Either<L, R>> {
        @Override
        public void write(NetworkBuffer buffer, Either<L, R> value) {
            switch (value) {
                case Either.Left(L leftValue) -> {
                    buffer.write(BOOLEAN, true);
                    buffer.write(left, leftValue);
                }
                case Either.Right(R rightValue) -> {
                    buffer.write(BOOLEAN, false);
                    buffer.write(right, rightValue);
                }
            }
        }

        @Override
        public Either<L, R> read(NetworkBuffer buffer) {
            if (buffer.read(BOOLEAN))
                return Either.left(buffer.read(left));
            return Either.right(buffer.read(right));
        }
    }

    public record TransformType<T, S>(Type<T> parent, Function<T, S> to,
                               Function<S, T> from) implements NetworkBufferTypeImpl<S> {
        @Override
        public void write(NetworkBuffer buffer, S value) {
            parent.write(buffer, from.apply(value));
        }

        @Override
        public S read(NetworkBuffer buffer) {
            return to.apply(parent.read(buffer));
        }
    }

    public record MapType<K, V>(Type<K> parent, NetworkBuffer.Type<V> valueType,
                         int maxSize) implements NetworkBufferTypeImpl<Map<K, V>> {
        @Override
        public void write(NetworkBuffer buffer, Map<K, V> map) {
            final int size = map.size();
            Check.argCondition(size > maxSize, "Map size ({0}) is higher than the maximum allowed size ({1})", size, maxSize);
            buffer.write(VAR_INT, size);
            for (Map.Entry<K, V> entry : map.entrySet()) {
                buffer.write(parent, entry.getKey());
                buffer.write(valueType, entry.getValue());
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<K, V> read(NetworkBuffer buffer) {
            final int size = buffer.read(VAR_INT);
            Check.argCondition(size < 0, "Map size cannot be negative: {0}", size);
            Check.argCondition(size > maxSize, "Map size ({0}) is higher than the maximum allowed size ({1})", size, maxSize);
            K[] keys = (K[]) new Object[size];
            V[] values = (V[]) new Object[size];
            for (int i = 0; i < size; i++) {
                keys[i] = buffer.read(parent);
                values[i] = buffer.read(valueType);
            }
            return Map.copyOf(new Object2ObjectArrayMap<>(keys, values, size));
        }
    }

    public record ListType<T>(Type<T> parent, int maxSize) implements NetworkBufferTypeImpl<List<T>> {
        @Override
        public void write(NetworkBuffer buffer, List<T> values) {
            if (values == null) {
                buffer.write(BYTE, (byte) 0);
                return;
            }
            final int size = values.size();
            Check.argCondition(size > maxSize, "Collection size ({0}) is higher than the maximum allowed size ({1})", size, maxSize);
            buffer.write(VAR_INT, size);
            for (T value : values) buffer.write(parent, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<T> read(NetworkBuffer buffer) {
            final int size = buffer.read(VAR_INT);
            Check.argCondition(size < 0, "Collection size cannot be negative: {0}", size);
            Check.argCondition(size > maxSize, "Collection size ({0}) is higher than the maximum allowed size ({1})", size, maxSize);
            T[] values = (T[]) new Object[size];
            for (int i = 0; i < size; i++) values[i] = buffer.read(parent);
            return List.of(values);
        }
    }

    public record SetType<T>(Type<T> parent, int maxSize) implements NetworkBufferTypeImpl<Set<T>> {
        @Override
        public void write(NetworkBuffer buffer, Set<T> values) {
            if (values == null) {
                buffer.write(BYTE, (byte) 0);
                return;
            }
            final int size = values.size();
            Check.argCondition(size > maxSize, "Collection size ({0}) is higher than the maximum allowed size ({1})", size, maxSize);
            buffer.write(VAR_INT, size);
            for (T value : values) buffer.write(parent, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Set<T> read(NetworkBuffer buffer) {
            final int size = buffer.read(VAR_INT);
            Check.argCondition(size < 0, "Collection size cannot be negative: {0}", size);
            Check.argCondition(size > maxSize, "Collection size ({0}) is higher than the maximum allowed size ({1})", size, maxSize);
            T[] values = (T[]) new Object[size];
            for (int i = 0; i < size; i++) values[i] = buffer.read(parent);
            return Set.of(values);
        }
    }

    public record UnionType<T, K, TR extends T>(
            Type<K> keyType, Function<T, ? extends K> keyFunc,
            Function<K, NetworkBuffer.Type<TR>> serializers
    ) implements NetworkBufferTypeImpl<T> {

        @SuppressWarnings("unchecked") // Much nicer than using the correct wildcard type for returns, pretty much ensuring T has subtypes already.
        @Override
        public void write(NetworkBuffer buffer, T value) {
            final K key = keyFunc.apply(value);
            buffer.write(keyType, key);
            var serializer = serializers.apply(key);
            if (serializer == null)
                throw new UnsupportedOperationException("Unrecognized type: " + key);
            serializer.write(buffer, (TR) value);
        }

        @Override
        public T read(NetworkBuffer buffer) {
            final K key = buffer.read(keyType);
            var serializer = serializers.apply(key);
            if (serializer == null) throw new UnsupportedOperationException("Unrecognized type: " + key);
            return serializer.read(buffer);
        }
    }

    /**
     * This is a very gross version of {@link java.io.DataOutputStream#writeUTF(String)} & ${@link DataInputStream#readUTF()}. We need the data in the java
     * modified utf-8 format for Component, and I couldnt find a method without creating a new buffer for it.
     */
    public record IOUTF8StringType() implements NetworkBufferTypeImpl<String> {
        @Override
        public void write(NetworkBuffer buffer, String value) {
            final int strlen = value.length();
            int utflen = strlen; // optimized for ASCII

            for (int i = 0; i < strlen; i++) {
                int c = value.charAt(i);
                if (c >= 0x80 || c == 0)
                    utflen += (c >= 0x800) ? 2 : 1;
            }

            if (utflen > 65535 || /* overflow */ utflen < strlen)
                throw new RuntimeException("UTF-8 string too long");

            buffer.write(SHORT, (short) utflen);
            var impl = (NetworkBufferImpl) buffer;
            long index = impl.reserveWrite(utflen);
            int i;
            for (i = 0; i < strlen; i++) { // optimized for initial run of ASCII
                int c = value.charAt(i);
                if (c >= 0x80 || c == 0) break;
                impl._putByteUnchecked(index++, (byte) c);
            }

            for (; i < strlen; i++) {
                int c = value.charAt(i);
                if (c < 0x80 && c != 0) {
                    impl._putByteUnchecked(index++, (byte) c);
                } else if (c >= 0x800) {
                    impl._putByteUnchecked(index++, (byte) (0xE0 | ((c >> 12) & 0x0F)));
                    impl._putByteUnchecked(index++, (byte) (0x80 | ((c >> 6) & 0x3F)));
                    impl._putByteUnchecked(index++, (byte) (0x80 | ((c >> 0) & 0x3F)));
                } else {
                    impl._putByteUnchecked(index++, (byte) (0xC0 | ((c >> 6) & 0x1F)));
                    impl._putByteUnchecked(index++, (byte) (0x80 | ((c >> 0) & 0x3F)));
                }
            }
        }

        @Override
        public String read(NetworkBuffer buffer) {
            int utflen = buffer.read(UNSIGNED_SHORT);
            if (buffer.readableBytes() < utflen) throw new IllegalArgumentException("Invalid String size.");
            byte[] bytearr = buffer.read(FixedRawBytes(utflen));
            final char[] chararr = new char[utflen];

            int c, char2, char3;
            int count = 0;
            int chararr_count = 0;

            while (count < utflen) {
                c = (int) bytearr[count] & 0xff;
                if (c > 127) break;
                count++;
                chararr[chararr_count++] = (char) c;
            }

            while (count < utflen) {
                c = (int) bytearr[count] & 0xff;
                try { // Surround in try catch to throw a runtime exception instead of a checked one
                    switch (c >> 4) {
                        case 0, 1, 2, 3, 4, 5, 6, 7 -> {
                            /* 0xxxxxxx*/
                            count++;
                            chararr[chararr_count++] = (char) c;
                        }
                        case 12, 13 -> {
                            /* 110x xxxx   10xx xxxx*/
                            count += 2;
                            if (count > utflen)
                                throw new UTFDataFormatException(
                                        "malformed input: partial character at end");
                            char2 = bytearr[count - 1];
                            if ((char2 & 0xC0) != 0x80)
                                throw new UTFDataFormatException(
                                        "malformed input around byte " + count);
                            chararr[chararr_count++] = (char) (((c & 0x1F) << 6) |
                                    (char2 & 0x3F));
                        }
                        case 14 -> {
                            /* 1110 xxxx  10xx xxxx  10xx xxxx */
                            count += 3;
                            if (count > utflen)
                                throw new UTFDataFormatException(
                                        "malformed input: partial character at end");
                            char2 = bytearr[count - 2];
                            char3 = bytearr[count - 1];
                            if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                                throw new UTFDataFormatException(
                                        "malformed input around byte " + (count - 1));
                            chararr[chararr_count++] = (char) (((c & 0x0F) << 12) |
                                    ((char2 & 0x3F) << 6) |
                                    ((char3 & 0x3F) << 0));
                        }
                        default ->
                            /* 10xx xxxx,  1111 xxxx */
                                throw new UTFDataFormatException(
                                        "malformed input around byte " + count);
                    }
                } catch (UTFDataFormatException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            // The number of chars produced may be less than utflen
            return new String(chararr, 0, chararr_count);
        }
    }

    static <T> long sizeOf(Type<T> type, T value, Registries registries) {
        NetworkBuffer buffer = NetworkBufferImpl.dummy(registries);
        type.write(buffer, value);
        return buffer.writeIndex();
    }
}
