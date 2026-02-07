package net.minestom.server.network;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.ServerFlag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.stream.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.network.foreign.NetworkBufferSegmentAllocator;
import net.minestom.server.network.foreign.NetworkBufferSegmentProvider;
import net.minestom.server.registry.Registries;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.Either;
import net.minestom.server.utils.Unit;
import net.minestom.server.utils.crypto.KeyUtils;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.*;

import javax.crypto.Cipher;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.zip.DataFormatException;

import static net.minestom.server.codec.stream.StreamCodec.*;

/**
 * A mutable byte buffer for reading and writing network protocol data with type-safe operations.
 * <p>
 * Buffers maintain separate read and write indices for bidirectional operations.
 * They come in two flavors:
 * <ul>
 *   <li><b>Static buffers</b> - Fixed capacity, created via {@link #staticBuffer(long)}</li>
 *   <li><b>Resizable buffers</b> - Resizeable capacity, created via {@link #resizableBuffer()}</li>
 * </ul>
 *
 * <b>Basic Usage:</b>
 * <pre>{@code
 * NetworkBuffer buffer = NetworkBuffer.resizableBuffer();
 * buffer.write(NetworkBuffer.INT, 42);
 * buffer.write(NetworkBuffer.STRING, "Hello");
 *
 * int value = buffer.read(NetworkBuffer.INT);
 * String text = buffer.read(NetworkBuffer.STRING);
 * }</pre>
 *
 * <b>Custom Types with Templates:</b>
 * <pre>{@code
 *     record MyData(int id, String name) {
 *         static final NetworkBuffer.Type<MyData> SERIALIZER = NetworkBufferTemplate.template(
 *          NetworkBuffer.INT, MyData::id,
 *          NetworkBuffer.STRING, MyData::name,
 *          MyData::new
 *         );
 *     }
 *     ...
 *     MyData data = new MyData(1, "Test");
 *     byte[] bytes = NetworkBuffer.makeArray(MyData.SERIALIZER, data);
 *     System.out.println("Bytes: " + Arrays.toString(bytes));
 *     NetworkBuffer buffer = NetworkBuffer.wrap(bytes, 0, bytes.length);
 *     MyData value = buffer.read(MyData.SERIALIZER);
 *     System.out.println("Value: " + value); // Value: MyData[id=1, name="Test"]
 * }</pre>
 * <br>
 *
 * <b>Note:</b> These are not thread safe, because of their index tracking,
 * also buffers will attempt to use native allocation through {@link NetworkBufferSegmentAllocator} if available.
 *
 * @see Type for custom types
 * @see NetworkBufferTemplate for templating
 * @see NetworkBufferFactory to create custom allocators
 * @see IOView to interface with existing code
 */
public interface NetworkBuffer extends StreamTranscoder {
    Type<Unit> UNIT = NetworkBufferTemplate.template(Unit.INSTANCE);
    StreamCodec<@Nullable Integer> OPTIONAL_VAR_INT = VAR_INT.transform(it -> it == 0 ? null : it - 1, it -> it == null ? 0 : it + 1);
    Type<String> STRING_IO_UTF8 = new NetworkBufferTypeImpl.StringIOUTFType();
    Type<BinaryTag> NBT = NetworkBufferTypeImpl.NbtType.typed();
    Type<CompoundBinaryTag> NBT_COMPOUND = NetworkBufferTypeImpl.NbtType.typed();
    // TAG_END special encoding for nullables.
    Type<@Nullable BinaryTag> OPTIONAL_NBT = NetworkBufferTypeImpl.OptionalNBTType.typed();
    // TAG_END special encoding for nullables.
    Type<@Nullable CompoundBinaryTag> OPTIONAL_NBT_COMPOUND = NetworkBufferTypeImpl.OptionalNBTType.typed();
    Type<Point> BLOCK_POSITION = new NetworkBufferTypeImpl.BlockPositionType();
    Type<Component> COMPONENT = new ComponentNetworkBufferTypeImpl();
    Type<Component> JSON_COMPONENT = new NetworkBufferTypeImpl.JsonComponentType();
    Type<UUID> UUID = new NetworkBufferTypeImpl.UUIDType();
    Type<Pos> POS = new NetworkBufferTypeImpl.PosType();

    Type<byte[]> BYTE_ARRAY = new NetworkBufferTypeImpl.ByteArrayType();
    Type<long[]> LONG_ARRAY = new NetworkBufferTypeImpl.LongArrayType();
    Type<int[]> VAR_INT_ARRAY = new NetworkBufferTypeImpl.VarIntArrayType();
    Type<long[]> VAR_LONG_ARRAY = new NetworkBufferTypeImpl.VarLongArrayType();

    StreamCodec<BitSet> BITSET = LONG_ARRAY.transform(BitSet::valueOf, BitSet::toLongArray);
    StreamCodec<Instant> INSTANT_MS = LONG.transform(Instant::ofEpochMilli, Instant::toEpochMilli);
    StreamCodec<PublicKey> PUBLIC_KEY = BYTE_ARRAY.transform(KeyUtils::publicRSAKeyFrom, PublicKey::getEncoded);

    Type<Point> VECTOR3 = new NetworkBufferTypeImpl.Vector3Type();
    Type<Point> VECTOR3D = new NetworkBufferTypeImpl.Vector3DType();
    Type<Point> VECTOR3I = new NetworkBufferTypeImpl.Vector3IType();
    Type<Point> VECTOR3B = new NetworkBufferTypeImpl.Vector3BType();
    Type<Vec> LP_VECTOR3 = new NetworkBufferTypeImpl.LpVector3Type();
    Type<float[]> QUATERNION = new NetworkBufferTypeImpl.QuaternionType();
    StreamCodec<Float> LP_ANGLE = BYTE.transform(to -> to * 360f / 256f, from -> (byte) (from * 256f / 360f));

    StreamCodec<@Nullable Component> OPT_CHAT = COMPONENT.optional();
    StreamCodec<@Nullable Point> OPT_BLOCK_POSITION = BLOCK_POSITION.optional();

    StreamCodec<Direction> DIRECTION = Enum(Direction.class);
    StreamCodec<EntityPose> POSE = Enum(EntityPose.class);

    // Combinators

    /**
     * Creates a typed NBT serializer using a {@link Codec}
     *
     * @param serializer the serializer
     * @param <T>        the codec type
     * @return the new type
     */
    @Contract(pure = true, value = "_ -> new")
    static <T> Type<T> TypedNBT(Codec<T> serializer) {
        return new NetworkBufferTypeImpl.TypedNbtType<>(serializer);
    }

    /**
     * Creates a new static buffer using {@link NetworkBufferFactory#staticFactory()}.
     *
     * @param size       the size to use for {@link NetworkBufferFactory#allocate(long)}
     * @param registries the registries to use
     * @return the new network buffer
     */
    @Contract("_, _ -> new")
    static NetworkBuffer staticBuffer(long size, Registries registries) {
        Objects.requireNonNull(registries, "registries");
        return NetworkBufferFactory.staticFactory().registry(registries).allocate(size);
    }

    /**
     * Creates a new static buffer using {@link NetworkBufferFactory#staticFactory()}.
     *
     * @param size the size to use for {@link NetworkBufferFactory#allocate(long)}
     * @return the new network buffer
     */
    @Contract("_ -> new")
    static NetworkBuffer staticBuffer(long size) {
        return NetworkBufferFactory.staticFactory().allocate(size);
    }

    /**
     * Creates a resizeable buffer using {@link NetworkBufferFactory#resizeableFactory()}
     *
     * @param initialSize the initial size to use for {@link NetworkBufferFactory#allocate(long)}
     * @param registries  the registries to use
     * @return the new buffer
     */
    @Contract("_, _ -> new")
    static NetworkBuffer resizableBuffer(long initialSize, Registries registries) {
        Objects.requireNonNull(registries, "registries");
        return NetworkBufferFactory.resizeableFactory()
                .registry(registries)
                .allocate(initialSize);
    }

    /**
     * Creates a resizeable buffer using {@link NetworkBufferFactory#resizeableFactory()}
     *
     * @param initialSize the initial size to use for {@link NetworkBufferFactory#allocate(long)}
     * @return the new buffer
     */
    @Contract("_ -> new")
    static NetworkBuffer resizableBuffer(int initialSize) {
        return NetworkBufferFactory.resizeableFactory().allocate(initialSize);
    }

    /**
     * Creates a resizeable buffer using {@link #resizableBuffer(long, Registries)}
     * with an initial size of 256, determined by {@link ServerFlag#DEFAULT_RESIZEABLE_SIZE}.
     *
     * @param registries the registries to use if required during encoding/decoding.
     * @return the new buffer
     */
    @Contract("_ -> new")
    static NetworkBuffer resizableBuffer(Registries registries) {
        Objects.requireNonNull(registries, "registries");
        return resizableBuffer(ServerFlag.DEFAULT_RESIZEABLE_SIZE, registries);
    }

    /**
     * Creates a resizeable buffer
     * with an initial size of 256, determined by {@link ServerFlag#DEFAULT_RESIZEABLE_SIZE}.
     *
     * @return the new buffer
     */
    @Contract("-> new")
    static NetworkBuffer resizableBuffer() {
        return resizableBuffer(ServerFlag.DEFAULT_RESIZEABLE_SIZE);
    }

    /**
     * Wrap the {@link MemorySegment} into a {@link NetworkBuffer} with the registries.
     * <br>
     * Useful when you already have a memory segment.
     *
     * @param segment    the segment
     * @param readIndex  the {@link #readIndex()}
     * @param writeIndex the {@link #writeIndex()}
     * @param registries the {@link #registries()}
     * @return the new {@link NetworkBuffer}
     */
    @Contract("_, _, _, _ -> new")
    @ApiStatus.Experimental
    static NetworkBuffer wrap(MemorySegment segment, long readIndex, long writeIndex, @Nullable Registries registries) {
        Objects.requireNonNull(segment, "segment");
        return NetworkBufferSegmentProvider.INSTANCE.wrap(segment, readIndex, writeIndex, registries);
    }

    /**
     * Wrap the {@link MemorySegment} into a {@link NetworkBuffer} without registries.
     * Useful when you already have a memory segment.
     *
     * @param segment    the segment
     * @param readIndex  the {@link #readIndex()}
     * @param writeIndex the {@link #writeIndex()}
     * @return the new {@link NetworkBuffer}
     */
    @Contract("_, _, _ -> new")
    @ApiStatus.Experimental
    static NetworkBuffer wrap(MemorySegment segment, long readIndex, long writeIndex) {
        return wrap(segment, readIndex, writeIndex, null);
    }

    /**
     * Wrap the byte array into a {@link NetworkBuffer} with the registries.
     * Useful when you already have a {@code byte[]}.
     *
     * @param bytes      the bytes
     * @param readIndex  the {@link #readIndex()}
     * @param writeIndex the {@link #writeIndex()}
     * @param registries the {@link #registries()}
     * @return the new {@link NetworkBuffer}
     */
    @Contract("_, _, _, _ -> new")
    static NetworkBuffer wrap(byte[] bytes, int readIndex, int writeIndex, @Nullable Registries registries) {
        Objects.requireNonNull(bytes, "bytes");
        return NetworkBufferSegmentProvider.INSTANCE.wrap(bytes, readIndex, writeIndex, registries);
    }

    /**
     * Wrap the byte array into a {@link NetworkBuffer}.
     * Useful when you already have a {@code byte[]}.
     *
     * @param bytes      the bytes
     * @param readIndex  the {@link #readIndex()}
     * @param writeIndex the {@link #writeIndex()}
     * @return the new {@link NetworkBuffer}
     */
    @Contract("_, _, _ -> new")
    static NetworkBuffer wrap(byte[] bytes, int readIndex, int writeIndex) {
        return wrap(bytes, readIndex, writeIndex, null);
    }

    /**
     * Creates a byte array from the consumer and with registries.
     * <br>
     * Note: only the current thread can use the buffer.
     *
     * @param writing    consumer of the {@link NetworkBuffer}
     * @param registries the registries to use in serialization
     * @return the smallest byte array to represent the contents of {@link NetworkBuffer}
     */
    @Contract("_, _ -> new")
    static byte[] makeArray(Consumer<? super NetworkBuffer> writing, @Nullable Registries registries) {
        Objects.requireNonNull(writing, "writing");
        return NetworkBufferSegmentProvider.INSTANCE.makeArray(writing, registries);
    }

    /**
     * Creates a byte array from the consumer and without registries.
     * <br>
     * Note: only the current thread can use the buffer.
     * Similar to {@link NetworkBuffer#makeArray(Consumer, Registries)}
     *
     * @param writing consumer of the {@link NetworkBuffer}
     * @return the smallest byte array to represent the contents of {@link NetworkBuffer}
     */
    @Contract("_ -> new")
    static byte[] makeArray(Consumer<? super NetworkBuffer> writing) {
        return makeArray(writing, null);
    }

    /**
     * Creates a byte array from the type and value registries.
     * <br>
     * Note: only the current thread can use the buffer.
     * Similar to {@link NetworkBuffer#makeArray(Consumer, Registries)}
     *
     * @param type       the {@link Type} for {@link T}
     * @param value      the value
     * @param registries the registries to use in serialization
     * @param <T>        the type
     * @return the smallest byte array to represent {@link T}
     */
    @Contract("_ ,_, _ -> new")
    static <T extends @UnknownNullability Object> byte[] makeArray(StreamEncoder<T> type, T value, @Nullable Registries registries) {
        Objects.requireNonNull(type, "type");
        return NetworkBufferSegmentProvider.INSTANCE.makeArray(type, value, registries);
    }

    /**
     * Creates a byte array from the type and value without registries.
     * <br>
     * Note: only the current thread can use the buffer.
     * Similar to {@link NetworkBuffer#makeArray(Consumer, Registries)}
     *
     * @param type  the {@link Type} for {@link T}
     * @param value the value
     * @param <T>   the type
     * @return the smallest byte array to represent {@link T}
     */
    @Contract("_, _ -> new")
    static <T extends @UnknownNullability Object> byte[] makeArray(StreamEncoder<T> type, T value) {
        return makeArray(type, value, null);
    }

    /**
     * Copies the src {@link NetworkBuffer} into the destination {@link NetworkBuffer}
     * <br>
     *
     * @param srcBuffer the source
     * @param srcOffset the source offset
     * @param dstBuffer the destination
     * @param dstOffset the destination offset
     * @param length    the length to copy
     * @throws UnsupportedOperationException if {@code srcBuffer} is a dummy
     * @throws UnsupportedOperationException if {@code dstBuffer} is a dummy
     * @throws UnsupportedOperationException if {@code dstBuffer} is read-only
     */
    @Contract(mutates = "param3")
    static void copy(NetworkBuffer srcBuffer, long srcOffset,
                     NetworkBuffer dstBuffer, long dstOffset, long length) {
        Objects.requireNonNull(srcBuffer, "srcBuffer");
        Objects.requireNonNull(dstBuffer, "dstBuffer");
        srcBuffer.copyTo(srcOffset, dstBuffer, dstOffset, length);
    }

    /**
     * @param buffer1 the buffer
     * @param buffer2 the buffer
     * @return if they are equals
     * @deprecated Use NetworkBuffer#contentEquals instead.
     */
    @Deprecated(forRemoval = true)
    static boolean equals(NetworkBuffer buffer1, NetworkBuffer buffer2) {
        return contentEquals(buffer1, buffer2);
    }

    /**
     * Checks if the contents of one buffer in its entirety.
     * Buffers with the same address and capacity will always be true.
     * <br>
     * Note: Dummy buffers are never equal in content.
     *
     * @param buffer1 the left buffer
     * @param buffer2 the right buffer
     * @return true if the content is equal
     */
    @Contract(pure = true)
    static boolean contentEquals(NetworkBuffer buffer1, NetworkBuffer buffer2) {
        Objects.requireNonNull(buffer1, "buffer1");
        Objects.requireNonNull(buffer2, "buffer2");
        return buffer1.contentEquals(buffer2);
    }

    /**
     * Creates a dummy buffer, useful for size calculations
     * <br>
     * A dummy buffer is one that can always be written, modified, but never read from.
     * Therefore, has an observed blank state, which could be reused over and over, also the benefit of no native allocations.
     * <br>
     * Operations that require the dummy buffer to be read or passed into logic where it's required will throw an exception.
     *
     * @param registries the registries to use if applicable
     * @return the new dummy buffer
     * @throws UnsupportedOperationException during usage, if directly called to read.
     * @throws RuntimeException if used on another implementation, that requires more underlying access.
     */
    @Contract(pure = true, value = "_ -> new")
    static NetworkBuffer dummy(@Nullable Registries registries) {
        return new NetworkBufferDummy(0, registries);
    }

    /**
     * Writes the value of {@link T} at {@link #writeIndex()}
     * <br>
     * Writing may require resizing so any side effects of {@link #resize(long)} could happen.
     *
     * @param type  the type
     * @param value the value to write
     * @param <T>   the type
     * @throws IndexOutOfBoundsException if the write index is out of bounds.
     */
    @Contract(mutates = "this")
    default <T extends @UnknownNullability Object> void write(StreamEncoder<T> type, T value) throws IndexOutOfBoundsException {
        type.encode(this, value);
    }

    /**
     * Reads the value of {@link T} at {@link #readIndex()}
     *
     * @param type type
     * @param <T>  the type
     * @return the value
     * @throws IndexOutOfBoundsException if the read index is out of bounds.
     */
    @Contract(mutates = "this")
    default <T extends @UnknownNullability Object> T read(StreamDecoder<T> type) throws IndexOutOfBoundsException {
        return type.decode(this);
    }

    /**
     * Write the value of {@link T} using at {@code index}
     * <br>
     * Note: Temporarily sets the write index to {@code index} to be used then restored at the end.
     *
     * @param index the index to write at
     * @param type  the type
     * @param value the value of T
     * @param <T>   the type
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    @Contract(mutates = "this")
    default <T extends @UnknownNullability Object> void writeAt(long index, StreamEncoder<T> type, T value) throws IndexOutOfBoundsException {
        final long oldWriteIndex = writeIndex();
        writeIndex(index);
        try {
            write(type, value);
        } finally {
            writeIndex(oldWriteIndex);
        }
    }

    /**
     * Read the value of {@link T} using at {@code index}
     * <br>
     * Note: Temporarily sets the read index to {@code index} to be used then restored at the end.
     *
     * @param index the index to read at
     * @param type  the type
     * @param <T>   the type
     * @return the value {@link T}
     * @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    @Contract(mutates = "this", value = "_, _ -> new")
    default <T extends @UnknownNullability Object> T readAt(long index, StreamDecoder<T> type) throws IndexOutOfBoundsException {
        final long oldReadIndex = readIndex();
        readIndex(index);
        try {
            return read(type);
        } finally {
            readIndex(oldReadIndex);
        }
    }

    /**
     * @param srcOffset  the source offset
     * @param dest       the dest buffer
     * @param destOffset the destination offset
     * @param length     the length
     * @deprecated Use {@link #copyTo(long, byte[], int, int)} instead as the length and destination offsets are integers.
     */
    @Deprecated(forRemoval = true) // No longer long's
    default void copyTo(long srcOffset, byte[] dest, long destOffset, long length) {
        this.copyTo(srcOffset, dest, Math.toIntExact(destOffset), Math.toIntExact(length));
    }

    /**
     * Copies the buffer from {@code sourceOffset} to the {@code length}.
     *
     * @param srcOffset  the source offset
     * @param dest       the dest buffer
     * @param destOffset the destination offset
     * @param length     the length
     */
    @Contract(mutates = "param2")
    void copyTo(long srcOffset, byte[] dest, int destOffset, int length);


    /**
     * Copies the src {@link NetworkBuffer} into the destination {@link NetworkBuffer}
     * <br>
     *
     * @param srcOffset  the source offset
     * @param destBuffer the destination
     * @param destOffset the destination offset
     * @param length     the length to copy
     * @throws UnsupportedOperationException if {@code srcBuffer} is a dummy
     * @throws UnsupportedOperationException if {@code dstBuffer} is a dummy
     * @throws UnsupportedOperationException if {@code dstBuffer} is read-only
     */
    @Contract(mutates = "param2")
    void copyTo(long srcOffset, NetworkBuffer destBuffer, long destOffset, long length);

    /**
     * Fill the buffer with the byte value specified.
     * <br>
     * Useful if you want to zero a buffer after use if required.
     *
     * @param srcOffset the buffer
     * @param length    the length
     * @param value     the value to fill
     * @throws UnsupportedOperationException if this buffer is a dummy
     * @throws UnsupportedOperationException if this buffer is a read-only
     */
    void fill(long srcOffset, long length, byte value);

    /**
     * @param extractor the consumer of the network buffer
     * @return the bytes extracted
     * @deprecated Use {@link #extractReadBytes(Consumer)}
     * Consume read bytes from the extractor. Using {@link #readIndex()}
     * <br>
     * If you require the written bytes use {@link #extractWrittenBytes(Consumer)}
     */
    @Contract("_ -> new")
    @Deprecated(forRemoval = true)
    default byte[] extractBytes(Consumer<NetworkBuffer> extractor) {
        return extractReadBytes(extractor);
    }

    /**
     * Consume read bytes from the extractor. Using {@link #readIndex()}
     * <br>
     * If you require the write index bytes use {@link #extractWrittenBytes(Consumer)}
     *
     * @param type the type to extract
     * @return the bytes extracted
     */
    @Contract(mutates = "this", value = "_ -> new")
    default byte[] extractReadBytes(StreamDecoder<?> type) {
        Objects.requireNonNull(type, "type");
        return extractReadBytes((Consumer<NetworkBuffer>) buffer -> buffer.read(type));
    }

    /**
     * Consume read bytes from the extractor. Using {@link #readIndex()}
     * <br>
     * If you require the write index bytes use {@link #extractWrittenBytes(Consumer)}
     *
     * @param extractor the consumer of the network buffer
     * @return the bytes extracted
     */
    @Contract(mutates = "this", value = "_ -> new")
    byte[] extractReadBytes(Consumer<? super NetworkBuffer> extractor);

    /**
     * Consume read bytes from the extractor. Using {@link #readIndex()}
     * <br>
     * If you require the write index bytes use {@link #extractWrittenBytes(Consumer)}
     *
     * @param type the type to extract
     * @return the bytes extracted
     */
    @Contract(mutates = "this", value = "_, _ -> new")
    default <T extends @UnknownNullability Object> byte[] extractWrittenBytes(StreamEncoder<T> type, T value) {
        Objects.requireNonNull(type, "type");
        return extractWrittenBytes(buffer -> buffer.write(type, value));
    }

    /**
     * Consume written bytes from the extractor. Using {@link #writeIndex()}
     * <br>
     * If you require the read index bytes use {@link #extractReadBytes(Consumer)}
     *
     * @param extractor the consumer of the network buffer
     * @return the bytes extracted
     */
    @Contract(mutates = "this", value = "_ -> new")
    byte[] extractWrittenBytes(Consumer<? super NetworkBuffer> extractor);

    /**
     * Clears the data tracked by this buffer by setting the {@link #index(long, long)} to 0.
     * <br>
     * Note: the implementation does not require zeroing of the previously stored data,
     * instead use {@link NetworkBuffer#fill(long, long, byte)} if you require this.
     *
     * @return this
     */
    @Contract("-> this")
    default NetworkBuffer clear() {
        return index(0, 0);
    }

    /**
     * Returns the write index tracked by this buffer
     *
     * @return the write index
     */
    long writeIndex();

    /**
     * Returns the read index tracked by this buffer
     *
     * @return the read index
     */
    long readIndex();

    /**
     * Sets the write index
     *
     * @param writeIndex the new write index
     * @return this
     */
    @Contract("_ -> this")
    NetworkBuffer writeIndex(long writeIndex);

    /**
     * Sets the read index
     *
     * @param readIndex the new read index
     * @return this
     */
    @Contract("_ -> this")
    NetworkBuffer readIndex(long readIndex);

    /**
     * Sets both {@link #writeIndex()} and {@link #writeIndex()} to the specified ones
     *
     * @param readIndex  the new read index
     * @param writeIndex the new write index
     * @return this
     */
    @Contract(value = "_, _ -> this", mutates = "this")
    default NetworkBuffer index(long readIndex, long writeIndex) {
        writeIndex(writeIndex);
        readIndex(readIndex);
        return this;
    }

    /**
     * Advances the write index and returns the previous index, while storing the new index into {@link #writeIndex()}
     *
     * @param length the length to advance
     * @return the previous write index
     * @throws IllegalArgumentException if {@code length < 0}
     */
    @Contract(mutates = "this")
    default long advanceWrite(@Range(from = 0, to = Long.MAX_VALUE) long length) {
        if (length < 0) throw new IllegalArgumentException("Length cannot be negative");
        final long oldWriteIndex = writeIndex();
        writeIndex(oldWriteIndex + length);
        return oldWriteIndex;
    }

    /**
     * Advances the read index and returns the previous index, while storing the new index into {@link #readIndex()}
     *
     * @param length the length to advance
     * @return the previous read index
     * @throws IllegalArgumentException if {@code length < 0}
     */
    @Contract(mutates = "this")
    default long advanceRead(@Range(from = 0, to = Long.MAX_VALUE) long length) {
        if (length < 0) throw new IllegalArgumentException("Length cannot be negative");
        final long oldReadIndex = readIndex();
        readIndex(oldReadIndex + length);
        return oldReadIndex;
    }

    /**
     * Readable bytes are the number of bytes that have been written to the {@link #writeIndex()}
     * The readable bytes can be calculated by {@link #writeIndex()} - {@link #readIndex()}.
     *
     * @return the readable bytes
     */
    @Contract(pure = true)
    default long readableBytes() {
        return writeIndex() - readIndex();
    }

    /**
     * Writeable bytes are the number of bytes that are left in the buffer from the {@link #writeIndex()}
     * The writeable bytes can be calculated by {@link #capacity()} - {@link #writeIndex()}.
     *
     * @return the writeable bytes
     */
    @Contract(pure = true)
    default long writableBytes() {
        return capacity() - writeIndex();
    }

    /**
     * Gets the capacity for the buffer or its length.
     *
     * @return the capacity/length
     */
    @Contract(pure = true)
    @Range(from = 0, to = Long.MAX_VALUE)
    long capacity();

    /**
     * Creates a read-only version of this buffer.
     * <br>
     * Note: While this can be a view, during resizing of the original buffer this may no longer be valid.
     *
     * @return new static buffer
     */
    @Contract(pure = true)
    NetworkBuffer readOnly();

    /**
     * Returns true if the buffer has previously been {@link #readOnly()}
     *
     * @return true if the buffer is read-only
     */
    @Contract(pure = true)
    boolean isReadOnly();

    /**
     * Returns true if the buffer is a resizable buffer.
     * <br>
     * If false, the buffer is static and cannot be resized and {@link #resize(long)} will always fail.
     *
     * @return true if the buffer is resizable
     */
    @Contract(pure = true)
    boolean isResizable();

    /**
     * Resize the buffer to {@code length} the new {@link #capacity()}.
     * <br>
     * Note: This throws away the existing arena so it can be freed.
     * You can set a fixed arena by {@link NetworkBufferFactory#arena(Arena)}
     *
     * @param length the new size
     * @throws IllegalArgumentException      if {@code length < 0}
     * @throws IllegalArgumentException      if the new size is less than or equal to the current {@link #capacity()}.
     * @throws UnsupportedOperationException if this buffer cannot be resized
     * @throws UnsupportedOperationException if this buffer is a dummy
     * @throws UnsupportedOperationException if this buffer is read-only
     */
    @Contract(mutates = "this")
    void resize(@Range(from = 0, to = Long.MAX_VALUE) long length);

    /**
     * Ensures that the buffer {@link #writableBytes()} is greater or equal to {@code length}.
     * Otherwise, the buffer will be resized using {@link #resize(long)} if {@link #isResizable()} is true.
     *
     * @param length the length to ensure
     * @throws IllegalArgumentException  if {@code length < 0}
     * @throws IndexOutOfBoundsException if the upsize does not permit the length to be written
     */
    @Contract(mutates = "this")
    default void ensureWritable(@Range(from = 0, to = Long.MAX_VALUE) long length) throws IndexOutOfBoundsException {
        if (length < 0) throw new IllegalArgumentException("Length cannot be negative found %d".formatted(length));
        if (writableBytes() < length && !requestCapacity(writeIndex() + length))
            throw new IndexOutOfBoundsException("%d is too long to be writeable: %d".formatted(length, writableBytes()));
    }

    /**
     * Attempts to resize the current buffer, to the targetSize or greater using {@link AutoResize} strategy,
     * then uses {@link #resize(long)}.
     *
     * @param targetSize the request size minimum we need
     * @return true if successful, so at least targetSize is the new capacity.
     */
    @Contract(mutates = "this")
    boolean requestCapacity(long targetSize);

    /**
     * Ensures that the buffer {@link #readableBytes()} is greater or equal to {@code length}.
     *
     * @param length the length to ensure
     * @throws IllegalArgumentException  if {@code length < 0}
     * @throws IndexOutOfBoundsException if the buffer does not have enough data for this length.
     */
    @Contract(pure = true)
    default void ensureReadable(@Range(from = 0, to = Long.MAX_VALUE) long length) throws IndexOutOfBoundsException {
        if (length < 0) throw new IllegalArgumentException("Length cannot be negative found %d".formatted(length));
        if (readableBytes() < length)
            throw new IndexOutOfBoundsException("%d is too long to be readable: %s".formatted(length, readableBytes()));
    }

    /**
     * Compact (copies) all the data from the {@link #readIndex()} to the {@link #writeIndex()} to be zero aligned.
     * This does not change the buffer capacity, instead it's a simple copy.
     */
    @Contract(mutates = "this")
    default void compact() {
        if (readIndex() == 0) return;
        copyTo(readIndex(), this, 0, readableBytes());
        writeIndex(readableBytes());
        readIndex(0);
    }

    /**
     * Resizes this buffer to be trimmed and assigns it to this {@link NetworkBuffer}.
     * <br>
     * A trimmed buffer is one that's from its {@link #readIndex()} to its {@link #readableBytes()} is the occupied data.
     * <br>
     * Like {@link #compact()} the buffer will be zero aligned (by copy), but unlike compact the capacity may be shrunk.
     *
     * @throws UnsupportedOperationException if this buffer cannot be trimmed (resized)
     * @see #trimmed()
     */
    @Contract(mutates = "this")
    default void trim() {
        compact();
    }

    /**
     * Creates a copy of the buffer trimmed using the factory to {@link NetworkBufferFactory#staticFactory()}.
     * <br>
     * A trimmed buffer is one that's from its {@link #readIndex()} to its {@link #readableBytes()} is the only occupied data.
     *
     * @return the trimmed buffer
     * @see #trim()
     */
    @Contract("-> new")
    default NetworkBuffer trimmed() {
        return trimmed(NetworkBufferFactory.staticFactory());
    }

    /**
     * Creates a copy of the buffer trimmed using the factory to {@link NetworkBufferFactory#allocate(long)}.
     * <br>
     * A trimmed buffer is one that's from its {@link #readIndex()} to its {@link #readableBytes()} is the only occupied data.
     *
     * @param factory the factory to allocate from
     * @return the trimmed buffer
     * @see #trim()
     */
    @Contract("_, -> new")
    default NetworkBuffer trimmed(NetworkBufferFactory factory) {
        final long readableBytes = readableBytes();
        return copy(factory, readIndex(), readableBytes, 0, readableBytes);
    }

    /**
     * Copies the current buffer using the factory specified {@link NetworkBufferFactory#staticFactory()}
     * with the index to the length using {@link #readIndex()} and {@link #writeIndex()}.
     *
     * @param index  the starting index
     * @param length the length
     * @return the copy of the current buffer into a new buffer
     */
    @Contract("_, _ -> new")
    default NetworkBuffer copy(long index, long length) {
        return copy(index, length, readIndex(), writeIndex());
    }

    /**
     * Copies the current buffer using the {@link NetworkBufferFactory} with the index to the length with
     * the using {@link #readIndex()} and {@link #writeIndex()}.
     *
     * @param factory the {@link NetworkBufferFactory} which {@link NetworkBufferFactory#allocate(long)} will be used for the new buffer.
     * @param index   the index
     * @param length  the length
     * @return the copy of the current buffer into a new buffer
     */
    @Contract("_, _, _ -> new")
    default NetworkBuffer copy(NetworkBufferFactory factory, long index, long length) {
        return copy(factory, index, length, readIndex(), writeIndex());
    }

    /**
     * Copies the current buffer using the factory specified {@link NetworkBufferFactory#staticFactory()}
     * with the index to the length with the new specified read and write indexes.
     *
     * @param index      the starting index
     * @param length     the length
     * @param readIndex  the read index
     * @param writeIndex the write index
     * @return the copy of the current buffer into a new buffer
     */
    @Contract("_, _, _, _ -> new")
    default NetworkBuffer copy(long index, long length, long readIndex, long writeIndex) {
        return copy(NetworkBufferFactory.staticFactory(), index, length, readIndex, writeIndex);
    }

    /**
     * Copies the current buffer using the {@link NetworkBufferFactory} with the index to the length with the new specified read and write indexes.
     *
     * @param factory    the {@link NetworkBufferFactory} which {@link NetworkBufferFactory#allocate(long)} will be used for the new buffer.
     * @param index      the starting index
     * @param length     the length
     * @param readIndex  the new read index
     * @param writeIndex the new write index
     * @return the copy of the current buffer into a new buffer
     */
    @Contract("_, _, _, _, _ -> new")
    NetworkBuffer copy(NetworkBufferFactory factory, long index, long length, long readIndex, long writeIndex);

    /**
     * Creates a slice from the starting index to the length passing the read index and write index supplied
     * backed by the current {@link NetworkBuffer}
     * <br>
     * Note: if the buffer is resizable, this cannot be guaranteed to be a view.
     *
     * @param index      the starting index
     * @param length     the length
     * @param readIndex  the new read index
     * @param writeIndex the new write index
     * @return the network buffer slice
     */
    @Contract(pure = true, value = "_, _, _, _ -> new")
    NetworkBuffer slice(long index, long length, long readIndex, long writeIndex);

    /**
     * Creates a slice from the starting index to the length
     * backed by the current {@link NetworkBuffer}
     *
     * @param index  the starting index
     * @param length the length
     * @return a slice defined in {@link #slice(long, long, long, long)}
     */
    @Contract(pure = true, value = "_, _ -> new")
    default NetworkBuffer slice(long index, long length) {
        return slice(index, length, readIndex(), writeIndex());
    }

    /**
     * Reads the current buffer with the {@link ReadableByteChannel}
     * <br>
     * Uses the {@link #writableBytes()} starting from {@link #writeIndex()}
     *
     * @param channel the channel to write to
     * @return the amount of bytes read
     * @throws IOException if -1 bytes were read.
     */
    int readChannel(ReadableByteChannel channel) throws IOException;

    /**
     * Write the current buffer into the {@link WritableByteChannel}
     * <br>
     * Uses the {@link #readableBytes()} starting from {@link #readIndex()}
     *
     * @param channel the channel to write to
     * @return true if fully written, false otherwise
     * @throws IOException if -1 bytes were written.
     */
    boolean writeChannel(WritableByteChannel channel) throws IOException;

    /**
     * Encrypt/Decrypt this network buffer using a {@link Cipher}
     *
     * @param cipher the cipher to use
     * @param start  the start index
     * @param length the length
     */
    void cipher(Cipher cipher, long start, long length);

    /**
     * Compress this buffer into the output using {@link java.util.zip.Deflater}
     *
     * @param start  the start index
     * @param length the length
     * @param output the output buffer
     * @return the number of bytes that were compressed
     */
    long compress(long start, long length, NetworkBuffer output);

    /**
     * Decompress this buffer into the output using {@link java.util.zip.Inflater}
     *
     * @param start  the start index
     * @param length the length
     * @param output the output buffer
     * @return the number of bytes that were decompressed
     * @throws DataFormatException if the data is invalid
     */
    long decompress(long start, long length, NetworkBuffer output) throws DataFormatException;

    /**
     * The registries used when creating with {@link NetworkBufferFactory#registry(Registries)}
     *
     * @return the registries
     */
    @Nullable Registries registries();

    /**
     * Creates a new {@link IOView} of this buffer.
     * <br>
     * Useful to interface with API's that support {@link DataInput} or {@link DataOutput}.
     *
     * @return the io view.
     */
    @Contract(pure = true, value = "-> new")
    default DataIOStreamTranscoder ioView() {
        return () -> NetworkBuffer.this;
    }

    /**
     * Checks if the contents of one buffer in its entirety.
     * Buffers with the same address and capacity will always be true.
     * <br>
     * Note: Dummy buffers are never equal in content.
     *
     * @param buffer the right buffer
     * @return true if the content is equal
     */
    @Contract(pure = true)
    boolean contentEquals(NetworkBuffer buffer);

    /**
     * Tests to see if the current buffer equals in identity to the other buffer.
     * <br>
     * Note: This relies on {@code this == obj}.
     *
     * @param obj the reference object with which to compare.
     * @return true if equal in identity
     */
    @Override
    boolean equals(@Nullable Object obj);

    /**
     * The unique hashcode conforming to {@link Object#hashCode()}.
     * <br>
     * Note: This relies on identity using {@link System#identityHashCode(Object)}.
     *
     * @return the hash code.
     * @see #equals(Object)
     */
    @Override
    int hashCode();

        /**
         * Creates a type where it prefixes the length
         *
         * @param maxLength the max length before throwing
         * @return the new length prefixed type
         */
        @Contract(pure = true, value = "_ -> new")
        static <T> StreamCodec<T> LengthPrefixed(StreamCodec<T> type, int maxLength) {
            return new NetworkBufferTypeImpl.LengthPrefixedType<>(type, maxLength);
        }


    /**
     * Resize strategy for a {@link NetworkBuffer}.
     */
    @FunctionalInterface
    interface AutoResize {
        AutoResize DOUBLE = (capacity, targetSize) -> Math.max(capacity * 2, targetSize);

        /**
         * Provide the buffer a new size, guaranteeing that the new size is greater than its original.
         *
         * @param capacity   the current capacity of the buffer
         * @param targetSize the target size of the buffer
         * @return the new capacity of the buffer
         */
        @Contract(pure = true)
        long resize(long capacity, long targetSize);
    }
}
