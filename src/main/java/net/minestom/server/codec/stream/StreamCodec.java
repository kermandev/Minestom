package net.minestom.server.codec.stream;

import net.kyori.adventure.key.Key;
import net.minestom.server.codec.Codec;
import net.minestom.server.utils.ArrayUtils;
import net.minestom.server.utils.Either;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface StreamCodec<T extends @UnknownNullability Object> extends StreamEncoder<T>, StreamDecoder<T> {
    StreamCodec<Boolean> BOOLEAN = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeBoolean, StreamReader::takeBoolean);
    StreamCodec<Byte> BYTE = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeByte, StreamReader::takeByte);
    StreamCodec<Short> UNSIGNED_BYTE = BYTE.transform(it -> (short) (it & 0xFF), it -> (byte) (it & 0xFF));
    StreamCodec<Short> SHORT = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeShort, StreamReader::takeShort);
    StreamCodec<Integer> UNSIGNED_SHORT = SHORT.transform(it -> (it & 0xFFFF), it -> (short) (it & 0xFFFF));
    StreamCodec<Integer> INT = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeInt, StreamReader::takeInt);
    StreamCodec<Long> UNSIGNED_INT = INT.transform(it -> (it & 0xFFFFFFFFL), it -> (int) (it & 0xFFFFFFFFL));
    StreamCodec<Long> LONG = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeLong, StreamReader::takeLong);
    StreamCodec<Float> FLOAT = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeFloat, StreamReader::takeFloat);
    StreamCodec<Double> DOUBLE = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeDouble, StreamReader::takeDouble);
    StreamCodec<Integer> VAR_INT = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeVarInt, StreamReader::takeVarInt);
    StreamCodec<@Nullable Integer> OPTIONAL_VAR_INT = VAR_INT.transform(it -> it == 0 ? null : it - 1, it -> it == null ? 0 : it + 1);
    StreamCodec<Long> VAR_LONG = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeVarLong, StreamReader::takeVarLong);
    StreamCodec<byte[]> RAW_BYTES = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeBytes, StreamReader::takeBytes);
    StreamCodec<String> STRING = new StreamCodecImpl.PrimitiveImpl<>(StreamWriter::writeString, StreamReader::takeString);
    StreamCodec<Key> KEY = STRING.transform(Key::key, Key::asString);

    // Ordering
    @Override
    T decode(StreamReader stream) throws RuntimeException;

    @Override
    void encode(StreamWriter stream, T value) throws RuntimeException;


    // Combinators

    /**
     * Creates an enum type from the enum class
     * <br>
     * Encoded as a {@link #VAR_INT} from the ordinal value, unless the enum has less than 128 values,
     * in which case it will be encoded as a {@link #BYTE}, which should be a single VarInt value.
     *
     * @param enumClass the enum class
     * @param <E>       the enum type
     * @return the new enum type
     */
    @Contract(pure = true, value = "_ -> new")
    static <E extends Enum<E>> StreamCodec<E> Enum(Class<E> enumClass) {
        Objects.requireNonNull(enumClass, "enumClass");
        final E[] values = enumClass.getEnumConstants();
        // Use byte transform for small enums (likely the case).
        if (values.length < 128)
            return new StreamCodec<>() {
                @Override
                public E decode(StreamReader stream) throws RuntimeException {
                    byte value = stream.takeByte();
                    assert value >= 0 : "unreachable";
                    return values[value];
                }

                @Override
                public void encode(StreamWriter stream, E value) throws RuntimeException {
                    byte ordinal = (byte) value.ordinal();
                    assert ordinal >= 0 : "unreachable";
                    stream.writeByte(ordinal);
                }
            };

        return new StreamCodec<>() {
            @Override
            public E decode(StreamReader stream) throws RuntimeException {
                int ordinal = stream.takeVarInt();
                return values[ordinal];
            }

            @Override
            public void encode(StreamWriter stream, E value) throws RuntimeException {
                stream.writeVarInt(value.ordinal());
            }
        };
    }

    /**
     * Creates an enum set type from the enum class
     *
     * @param enumClass the enum class
     * @param <E>       the enum type
     * @return the new enum set type
     */
    @Contract(pure = true, value = "_ -> new")
    static <E extends Enum<E>> StreamCodec<EnumSet<E>> EnumSet(Class<E> enumClass) {
        final E[] values = enumClass.getEnumConstants();
        final StreamCodec<BitSet> bitSetStreamCodec = FixedBitSet(values.length);
        return new StreamCodec<>() {
            @Override
            public EnumSet<E> decode(StreamReader stream) throws RuntimeException {
                final BitSet bitSet = bitSetStreamCodec.decode(stream);
                EnumSet<E> enumSet = EnumSet.noneOf(enumClass);
                for (int i = 0; i < values.length; ++i) {
                    if (bitSet.get(i)) {
                        enumSet.add(values[i]);
                    }
                }
                return enumSet;
            }

            @Override
            public void encode(StreamWriter stream, EnumSet<E> value) throws RuntimeException {
                BitSet bitSet = new BitSet(values.length);
                for (int i = 0; i < values.length; i++) {
                    bitSet.set(i, value.contains(values[i]));
                }
                bitSetStreamCodec.encode(stream, bitSet);
            }
        };
    }

    /**
     * Creates a fixed bit set type with the specified length.
     * <br>
     * Note: If there aren't enough bits set during writing, the value will be padded with 0's.
     *
     * @param length the length
     * @return the type
     * @throws IllegalArgumentException if {@code length} is less than zero
     */
    @Contract(pure = true, value = "_ -> new")
    static StreamCodec<BitSet> FixedBitSet(int length) {
        final int size = (length + 7) / Long.BYTES;
        return new StreamCodec<>() {
            @Override
            public BitSet decode(StreamReader stream) throws RuntimeException {
                final byte[] array = stream.takeBytes(size);
                return BitSet.valueOf(array);
            }

            @Override
            public void encode(StreamWriter stream, BitSet value) throws RuntimeException {
                int bitSetLength = value.length();
                Check.argCondition(bitSetLength > length, "BitSet is larger than expected size ({0} > {1}", bitSetLength, length);
                byte[] array = value.toByteArray();
                if (array.length != size) // Pad if the length isn't correct.
                    array = Arrays.copyOf(array, size);
                stream.writeBytes(array);
            }
        };
    }

    /**
     * Creates a type that reads/writes in {@code length} bytes.
     *
     * @param length the length
     * @return the new type
     * @throws IllegalArgumentException if {@code length} is less than zero
     */
    @Contract(pure = true, value = "_ -> new")
    static StreamCodec<byte[]> FixedRawBytes(int length) {
        Check.argCondition(length < 0, "Length is negative found {0}", length);
        return new StreamCodec<>() {
            @Override
            public byte[] decode(StreamReader stream) throws RuntimeException {
                return stream.takeBytes(length);
            }

            @Override
            public void encode(StreamWriter stream, byte[] value) throws RuntimeException {
                Check.argCondition(value.length != length, "Expected length {0} found {1}", length, value.length);
                stream.writeBytes(value);
            }
        };
    }

    /**
     * Lazily compute the Type required for serialization.
     * <br>
     * Note your implementation should be thread safe, and should normally be called once. This may be updated to become a stable value.
     *
     * @param supplier the supplier
     * @param <T>      the type
     * @return the new type
     */
    @Contract(pure = true, value = "_ -> new")
    static <T> StreamCodec<T> Lazy(Supplier<StreamCodec<T>> supplier) {
        return new StreamCodec<T>() {
            private @Nullable StreamCodec<T> delegate;

            @Override
            public T decode(StreamReader stream) throws RuntimeException {
                return delegate().decode(stream);
            }

            @Override
            public void encode(StreamWriter stream, T value) throws RuntimeException {
                delegate().encode(stream, value);
            }

            public StreamCodec<T> delegate() {
                if (delegate != null) return delegate;
                return delegate = Objects.requireNonNull(supplier.get(), "delegate");
            }
        };
    }

    /**
     * Pass the type required for serialization for an inner part. Useful to break initialization where you only need one layer deep.
     * <br>
     * Note your implementation should be thread safe, and should normally be called once. This may be updated to become a stable value.
     *
     * @param supplier the supplier
     * @param <T>      the type
     * @return the new type
     */
    @Contract(pure = true, value = "_ -> new")
    static <T> StreamCodec<T> Recursive(UnaryOperator<StreamCodec<T>> supplier) {
        return new StreamCodec<T>() {
            private final StreamCodec<T> delegate = supplier.apply(this);

            @Override
            public T decode(StreamReader stream) throws RuntimeException {
                return delegate.decode(stream);
            }

            @Override
            public void encode(StreamWriter stream, T value) throws RuntimeException {
                delegate.encode(stream, value);
            }
        }.delegate;
    }

    /**
     * Either type for {@link L} and {@link R}
     *
     * @param left  the left type
     * @param right the right type
     * @param <L>   left type
     * @param <R>   right type
     * @return the new type for Either
     */
    @Contract(pure = true, value = "_, _ -> new")
    static <L, R> StreamCodec<Either<L, R>> Either(StreamCodec<L> left, StreamCodec<R> right) {
        return new StreamCodec<>() {
            @Override
            public Either<L, R> decode(StreamReader stream) throws RuntimeException {
                if (stream.takeBoolean())
                    return Either.left(left.decode(stream));
                return Either.right(right.decode(stream));
            }

            @Override
            public void encode(StreamWriter stream, Either<L, R> value) throws RuntimeException {
                switch (value) {
                    case Either.Left(L leftValue) -> {
                        stream.writeBoolean(true);
                        left.encode(stream, leftValue);
                    }
                    case Either.Right(R rightValue) -> {
                        stream.writeBoolean(false);
                        right.encode(stream, rightValue);
                    }
                }
            }
        };
    }

    /**
     * Transform the current type {@link T} to {@link S} and {@link S} to {@link T}.
     *
     * @param to   the function to call when reading your value
     * @param from the function to call when writing your value
     * @param <S>  type to
     * @return the new type that transforms {@link T}
     */
    @Contract(pure = true, value = "_, _ -> new")
    default <S extends @UnknownNullability Object> StreamCodec<S> transform(Function<? super T, ? extends S> to, Function<? super S, ? extends T> from) {
        return new StreamCodec<>() {
            @Override
            public S decode(StreamReader stream) {
                return to.apply(StreamCodec.this.decode(stream));
            }

            @Override
            public void encode(StreamWriter stream, S value) {
                StreamCodec.this.encode(stream, from.apply(value));
            }
        };
    }

    default <S extends @UnknownNullability Object> StreamCodec<S> then(BiFunction<? super StreamReader, ? super T, ? extends S> reader,
                                                                       Function<? super S, ? extends T> getter, StreamEncoder<? super S> writer) {
        return new StreamCodec<>() {
            @Override
            public S decode(StreamReader stream) {
                return reader.apply(stream, StreamCodec.this.decode(stream));
            }

            @Override
            public void encode(StreamWriter stream, S value) {
                final T val = getter.apply(value);
                StreamCodec.this.encode(stream, val);
                writer.encode(stream, value);
            }
        };
    }

    /**
     * Creates a map type to map the value of {@link T} with {@link V} into an unmodifiable map.
     *
     * @param valueType the value type
     * @param maxSize   the max size before throwing
     * @param <V>       the value type
     * @return the type
     */
    @Contract(pure = true, value = "_, _ -> new")
    default <V> StreamCodec<@Unmodifiable Map<T, V>> mapValue(StreamCodec<V> valueType, int maxSize) {
        Check.argCondition(maxSize < 0, "maxSize must be greater than 0 found {0}", maxSize);
        return new StreamCodec<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Map<T, V> decode(StreamReader stream) {
                int size = stream.takeVarInt();
                Check.argCondition(size > maxSize, "Map size ({0}) exceeds max size {1}", size, maxSize);
                if (size == 0) return Map.of();
                T[] keys = (T[]) new Object[size];
                V[] values = (V[]) new Object[size];
                for (int i = 0; i < size; i++) {
                    keys[i] = StreamCodec.this.decode(stream);
                    values[i] = valueType.decode(stream);
                }
                return ArrayUtils.toMap(keys, values, size);
            }

            @Override
            public void encode(StreamWriter stream, Map<T, V> value) {
                stream.writeVarInt(value.size());
                for (var entry : value.entrySet()) {
                    StreamCodec.this.encode(stream, entry.getKey());
                    valueType.encode(stream, entry.getValue());
                }
            }
        };
    }

    /**
     * Creates a map type to map the value of {@link T} with {@link V} into an unmodifiable map.
     * <br>
     * Note the max length allowed is {@link Integer#MAX_VALUE}, if you have a strict upperbound use {@link #mapValue(StreamCodec, int)}
     *
     * @param valueType the value type
     * @param <V>       the value type
     * @return the type
     */
    @Contract(pure = true, value = "_ -> new")
    default <V> StreamCodec<@Unmodifiable Map<T, V>> mapValue(StreamCodec<V> valueType) {
        return mapValue(valueType, Integer.MAX_VALUE);
    }

    /**
     * Creates an unmodifiable list type for {@link T} with its max sized defined
     * <br>
     * Note the encoding for null lists is a 0 byte.
     *
     * @param maxSize the max size before throwing.
     * @return the list type for {@link T}
     */
    @Contract(pure = true, value = "_ -> new")
    default StreamCodec<@Unmodifiable @UnknownNullability List<T>> list(int maxSize) {
        Check.argCondition(maxSize < 0, "maxSize must be greater than 0 found {0}", maxSize);
        return new StreamCodec<>() {
            @Override
            public List<T> decode(StreamReader reader) {
                int size = reader.takeVarInt();
                Check.argCondition(size > maxSize, "List size ({0}) exceeds max size {1}", size, maxSize);
                if (size == 0) return List.of();
                @SuppressWarnings("unchecked")
                T[] list = (T[]) new Object[size];
                for (int i = 0; i < size; i++) {
                    list[i] = StreamCodec.this.decode(reader);
                }
                return List.of(list);
            }

            @Override
            public void encode(StreamWriter stream, @Nullable List<T> values) {
                if (values == null) {
                    stream.writeByte((byte) 0);
                    return;
                }
                stream.writeVarInt(values.size());
                for (T value : values) {
                    StreamCodec.this.encode(stream, value);
                }
            }
        };
    }

    /**
     * Creates an unmodifiable list type for {@link T} with no max size defined.
     * <br>
     * Note the max length allowed is {@link Integer#MAX_VALUE}, if you have a strict upperbound use {@link #list(int)}
     * <br>
     * Note the encoding for null lists is a 0 byte.
     *
     * @return the list type for {@link T}
     */
    @Contract(pure = true, value = "-> new")
    default StreamCodec<@Unmodifiable @UnknownNullability List<T>> list() {
        return list(Integer.MAX_VALUE);
    }

    /**
     * Creates an unmodifiable set type for {@link T} with no max size defined.
     * <br>
     * Note the max length allowed is {@link Integer#MAX_VALUE}, if you have a strict upperbound use {@link #list(int)}
     * <br>
     * Note the encoding for null lists is a 0 byte.
     *
     * @return the list type for {@link T}
     */
    @Contract(pure = true, value = "_ -> new")
    default StreamCodec<@Unmodifiable @UnknownNullability Set<T>> set(int maxSize) {
        Check.argCondition(maxSize < 0, "maxSize must be greater than 0 found {0}", maxSize);
        return new StreamCodec<>() {
            @Override
            public void encode(StreamWriter stream, Set<T> value) {
                stream.writeVarInt(value.size());
                for (T t : value) {
                    StreamCodec.this.encode(stream, t);
                }
            }

            @Override
            public Set<T> decode(StreamReader stream) {
                int size = stream.takeVarInt();
                Check.argCondition(size > maxSize, "List size ({0}) exceeds max size {1}", size, maxSize);
                if (size == 0) return Set.of();
                @SuppressWarnings("unchecked")
                T[] list = (T[]) new Object[size];
                for (int i = 0; i < size; i++) {
                    list[i] = StreamCodec.this.decode(stream);
                }
                return Set.of(list);
            }
        };
    }

    /**
     * Creates an unmodifiable set type for {@link T} with no max size defined.
     * <br>
     * Note the max length allowed is {@link Integer#MAX_VALUE}, if you have a strict upperbound use {@link #list(int)}
     * <br>
     * Note the encoding for null lists is a 0 byte.
     *
     * @return the list type for {@link T}
     */
    @Contract(pure = true, value = "-> new")
    default StreamCodec<@Unmodifiable @UnknownNullability Set<T>> set() {
        return set(Integer.MAX_VALUE);
    }

    /**
     * Creates an optional type for {@link T}, which allows it to have null values.
     * <br>
     * Note the encoding prefixes all {@link T} behind {@link #BOOLEAN} where its value if {@link T} is not null.
     * For example, a not null {@link T} would be true, and {@code null} would be false.
     *
     * @return the new optional type
     */
    @Contract(pure = true, value = "-> new")
    default StreamCodec<@Nullable T> optional() {
        return new StreamCodec<>() {
            @Override
            public T decode(StreamReader stream) {
                return stream.takeBoolean() ? StreamCodec.this.decode(stream) : null;
            }

            @Override
            public void encode(StreamWriter stream, T value) {
                if (value == null) {
                    stream.writeBoolean(false);
                    return;
                }
                stream.writeBoolean(true);
                StreamCodec.this.encode(stream, value);
            }
        };
    }


    /**
     * Creates an optional type for {@link T}, which allows it to have null values, but replaces it with T when encoding
     *
     * @param encodeValueIfNull the value to encode when {@code value} is null in {@link #encode(StreamWriter, Object)}
     * @return the new optional type
     */
    @Contract(pure = true, value = "_, -> new")
    default StreamCodec<@Nullable T> optional(T encodeValueIfNull) {
        return new StreamCodec<>() {
            @Override
            public T decode(StreamReader stream) {
                return StreamCodec.this.decode(stream);
            }

            @Override
            public void encode(StreamWriter stream, T value) {
                StreamCodec.this.encode(stream, Objects.requireNonNullElse(value, encodeValueIfNull));
            }
        };
    }

    /**
     * Creates a union type for {@link T}, this allows you to map subtypes of {@link T} useful for sealed interfaces.
     *
     * @param serializers the map of {@link T} to the serializer
     * @param keyFunc     the key to use from {@link R} into {@link T} into {@code serializers}
     * @param <R>         the union type
     * @return the new union type for {@link T} using {@link R}
     */
    @Contract(pure = true, value = "_, _ -> new")
    default <R> StreamCodec<R> unionType(Function<? super T, ? extends StreamCodec<? extends R>> serializers, Function<? super R, ? extends T> keyFunc) {
        return new StreamCodec<>() {
            @Override
            public R decode(StreamReader stream) {
                return serializers.apply(StreamCodec.this.decode(stream)).decode(stream);
            }

            @Override
            public void encode(StreamWriter stream, R value) {
                T key = keyFunc.apply(value);
                StreamCodec.this.encode(stream, key);
                @SuppressWarnings("unchecked") // Using the correct generics would make us drop ? extends R here, or using two seperate functions
                var encoder = ((StreamEncoder<R>) serializers.apply(key));
                encoder.encode(stream, value);
            }
        };
    }


    /**
     * Creates a union type for {@link T}, this allows you to map subtypes of {@link T} useful for sealed interfaces, you must supply the upperbound.
     *
     * @param serializers the map of {@link T} to the serializer
     * @param keyFunc     the key to use from {@link R} into {@link T} into {@code serializers}
     * @param <R>         the union type
     * @return the new union type for {@link T} using {@link R}
     */
    @Contract(pure = true, value = "_, _, _ -> new")
    default <R> StreamCodec<R> unionType(Function<? super T, ? extends StreamCodec<? extends R>> serializers, Function<? super R, ? extends T> keyFunc, Set<T> entries) {
        // This will be a more optimized call site in the future, Map.ofLazy
        return unionType(serializers, keyFunc);
    }
}
