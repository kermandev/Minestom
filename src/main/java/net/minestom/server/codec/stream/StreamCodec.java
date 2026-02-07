package net.minestom.server.codec.stream;

import net.kyori.adventure.key.Key;
import net.minestom.server.utils.Either;
import net.minestom.server.utils.Unit;
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

    /**
     * Creates an enum type from the enum class
     * <br>
     * Encoded as a {@link StreamWriter#writeVarInt(int)} from the ordinal value, unless the enum has less than 128 values,
     * in which case it will be encoded as a {@link StreamWriter#writeByte(byte)}, which should be a single VarInt value.
     *
     * @param enumClass the enum class
     * @param <E>       the enum type
     * @return the new enum type
     */
    @Contract(pure = true, value = "_ -> new")
    static <E extends Enum<E>> StreamCodec<E> Enum(Class<E> enumClass) {
        return new StreamCodecImpl.EnumImpl<>(enumClass);
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
        return new StreamCodecImpl.EnumSetStreamCodec<>(enumClass);
    }


    // Combinators

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
        final int byteSize = (length + 7) / Long.BYTES;
        return new StreamCodecImpl.BitSetStreamCodec(byteSize, length);
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
        return new StreamCodecImpl.ByteStreamCodec(length);
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
        return new StreamCodecImpl.LazyStreamCodec<>(supplier);
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
        return new StreamCodecImpl.RecursiveStreamCodec<>(supplier).delegate();
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
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return new StreamCodecImpl.EitherStreamCodec<>(left, right);
    }

    // Ordering
    @Override
    T decode(StreamReader stream) throws RuntimeException;

    @Override
    void encode(StreamWriter stream, T value) throws RuntimeException;

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
        return new StreamCodecImpl.TransformStreamCodec<>(this, to, from);
    }

    default <S extends @UnknownNullability Object> StreamCodec<S> then(BiFunction<? super StreamReader, ? super T, ? extends S> reader,
                                                                       Function<? super S, ? extends T> getter, StreamEncoder<? super S> writer) {
        return new StreamCodecImpl.ThenStreamCodec<>(this, reader, getter, writer);
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
        return new StreamCodecImpl.MapStreamCodec<>(this, valueType, maxSize);
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
        return new StreamCodecImpl.ListStreamCodec<>(this, maxSize);
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
        return new StreamCodecImpl.SetStreamCodec<>(this, maxSize);
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
     * Note the encoding prefixes all {@link T} behind {@link StreamWriter#writeBoolean(boolean)} where its value if {@link T} is not null.
     * For example, a not null {@link T} would be true, and {@code null} would be false.
     *
     * @return the new optional type
     */
    @Contract(pure = true, value = "-> new")
    default StreamCodec<@Nullable T> optional() {
        return new StreamCodecImpl.OptionalStreamCodec<>(this);
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
        return new StreamCodecImpl.UnionStreamCodec<>(this, serializers, keyFunc);
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
