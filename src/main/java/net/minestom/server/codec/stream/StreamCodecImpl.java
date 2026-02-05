package net.minestom.server.codec.stream;

import net.minestom.server.utils.ArrayUtils;
import net.minestom.server.utils.Either;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

final class StreamCodecImpl {
    private StreamCodecImpl() {
    }

    record PrimitiveImpl<T>(StreamEncoder<T> encoder, StreamDecoder<T> decoder) implements StreamCodec<T> {
        @Override
        public T decode(StreamReader stream) {
            return decoder.decode(stream);
        }

        @Override
        public void encode(StreamWriter stream, T value) {
            encoder.encode(stream, value);
        }
    }

    record EnumImpl<T extends Enum<T>>(List<T> values, boolean small) implements StreamCodec<T> {
        EnumImpl(Class<T> enumClass) {
            Objects.requireNonNull(enumClass, "enumClass");
            final T[] constants = enumClass.getEnumConstants();
            this(List.of(constants), constants.length < 128);
        }

        EnumImpl {
            values = List.copyOf(values);
        }

        @Override
        public T decode(StreamReader stream) {
            if (small) {
                byte index = stream.takeByte();
                assert index >= 0 : "unreachable";
                return values.get(index);
            } else {
                int index = stream.takeVarInt();
                return values.get(index);
            }
        }

        @Override
        public void encode(StreamWriter stream, T value) {
            int ordinal = value.ordinal();
            if (small) {
                stream.writeByte((byte) ordinal);
            } else {
                stream.writeVarInt(ordinal);
            }
        }
    }


    record EnumSetStreamCodec<E extends Enum<E>>(Class<E> enumClass, List<E> values,
                                                 StreamCodec<BitSet> bitSetStreamCodec) implements StreamCodec<EnumSet<E>> {
        EnumSetStreamCodec(Class<E> enumClass) {
            final E[] values = enumClass.getEnumConstants();
            final StreamCodec<BitSet> bitSetStreamCodec = StreamCodec.FixedBitSet(values.length);
            this(enumClass, List.of(values), bitSetStreamCodec);
        }

        EnumSetStreamCodec {
            Objects.requireNonNull(enumClass, "enumClass");
            values = List.copyOf(values);
            Objects.requireNonNull(bitSetStreamCodec, "bitSetStreamCodec");
        }

        @Override
        public EnumSet<E> decode(StreamReader stream) {
            final BitSet bitSet = bitSetStreamCodec.decode(stream);
            EnumSet<E> enumSet = EnumSet.noneOf(enumClass);
            for (int i = 0; i < values.size(); ++i) {
                if (bitSet.get(i)) {
                    enumSet.add(values.get(i));
                }
            }
            return enumSet;
        }

        @Override
        public void encode(StreamWriter stream, EnumSet<E> value) {
            BitSet bitSet = new BitSet(values.size());
            for (int i = 0; i < values.size(); i++) {
                bitSet.set(i, value.contains(values.get(i)));
            }
            bitSetStreamCodec.encode(stream, bitSet);
        }
    }


    record BitSetStreamCodec(int byteSize, int length) implements StreamCodec<BitSet> {
        @Override
        public BitSet decode(StreamReader stream) {
            final byte[] array = stream.takeBytes(byteSize);
            return BitSet.valueOf(array);
        }

        @Override
        public void encode(StreamWriter stream, BitSet value) {
            int bitSetLength = value.length();
            Check.argCondition(bitSetLength > length, "BitSet is larger than expected size ({0} > {1}", bitSetLength, length);
            byte[] array = value.toByteArray();
            if (array.length != byteSize) // Pad if the length isn't correct.
                array = Arrays.copyOf(array, byteSize);
            stream.writeBytes(array);
        }
    }

    record ByteStreamCodec(int length) implements StreamCodec<byte[]> {
        @Override
        public byte[] decode(StreamReader stream) {
            return stream.takeBytes(length);
        }

        @Override
        public void encode(StreamWriter stream, byte[] value) {
            Check.argCondition(value.length != length, "Expected length {0} found {1}", length, value.length);
            stream.writeBytes(value);
        }
    }

    static final class LazyStreamCodec<T> implements StreamCodec<T> {
        private final Supplier<StreamCodec<T>> supplier;
        private @Nullable
        volatile StreamCodec<T> delegate;

        public LazyStreamCodec(Supplier<StreamCodec<T>> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T decode(StreamReader stream) {
            return delegate().decode(stream);
        }

        @Override
        public void encode(StreamWriter stream, T value) {
            delegate().encode(stream, value);
        }

        private StreamCodec<T> delegate() {
            StreamCodec<T> delegate = this.delegate;
            if (delegate != null) return delegate;
            synchronized (this) {
                delegate = this.delegate;
                if (delegate == null) {
                    this.delegate = delegate = Objects.requireNonNull(supplier.get(), "delegate");
                }
            }
            return delegate;
        }
    }

    @SuppressWarnings("ClassCanBeRecord") // It really cant without a maximum depth
    static final class RecursiveStreamCodec<T> implements StreamCodec<T> {
        private final StreamCodec<T> delegate;

        RecursiveStreamCodec(UnaryOperator<StreamCodec<T>> delegate) {
            this.delegate = Objects.requireNonNull(delegate.apply(this), "delegate");
        }

        @Override
        public T decode(StreamReader stream) {
            return delegate.decode(stream);
        }

        @Override
        public void encode(StreamWriter stream, T value) {
            delegate.encode(stream, value);
        }

        public StreamCodec<T> delegate() {
            return delegate;
        }
    }

    record EitherStreamCodec<L, R>(StreamCodec<L> left, StreamCodec<R> right) implements StreamCodec<Either<L, R>> {
        @Override
        public Either<L, R> decode(StreamReader stream) {
            if (stream.takeBoolean())
                return Either.left(left.decode(stream));
            return Either.right(right.decode(stream));
        }

        @Override
        public void encode(StreamWriter stream, Either<L, R> value) {
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
    }

    record TransformStreamCodec<T extends @UnknownNullability Object, S extends @UnknownNullability Object>(
            StreamCodec<T> parent,
            Function<? super T, ? extends S> to, Function<? super S, ? extends T> from) implements StreamCodec<S> {

        @Override
        public S decode(StreamReader stream) {
            return to.apply(parent.decode(stream));
        }

        @Override
        public void encode(StreamWriter stream, S value) {
            parent.encode(stream, from.apply(value));
        }
    }

    record ThenStreamCodec<T extends @UnknownNullability Object, S extends @UnknownNullability Object>(
            StreamCodec<T> parent, BiFunction<? super StreamReader, ? super T, ? extends S> reader,
            Function<? super S, ? extends T> getter, StreamEncoder<? super S> writer) implements StreamCodec<S> {

        ThenStreamCodec {
            Objects.requireNonNull(parent, "parent");
            Objects.requireNonNull(reader, "reader");
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(writer, "writer");
        }

        @Override
        public S decode(StreamReader stream) {
            return reader.apply(stream, parent.decode(stream));
        }

        @Override
        public void encode(StreamWriter stream, S value) {
            final T val = getter.apply(value);
            parent.encode(stream, val);
            writer.encode(stream, value);
        }
    }

    record MapStreamCodec<K, V>(StreamCodec<K> keyType, StreamCodec<V> valueType,
                                int maxSize) implements StreamCodec<Map<K, V>> {

        @Override
        @SuppressWarnings("unchecked")
        public Map<K, V> decode(StreamReader stream) {
            int size = stream.takeVarInt();
            Check.argCondition(size > maxSize, "Map size ({0}) exceeds max size {1}", size, maxSize);
            if (size == 0) return Map.of();
            K[] keys = (K[]) new Object[size];
            V[] values = (V[]) new Object[size];
            for (int i = 0; i < size; i++) {
                keys[i] = keyType.decode(stream);
                values[i] = valueType.decode(stream);
            }
            return ArrayUtils.toMap(keys, values, size);
        }

        @Override
        public void encode(StreamWriter stream, Map<K, V> value) {
            stream.writeVarInt(value.size());
            for (var entry : value.entrySet()) {
                keyType.encode(stream, entry.getKey());
                valueType.encode(stream, entry.getValue());
            }
        }
    }

    record ListStreamCodec<T>(StreamCodec<T> parent, int maxSize) implements StreamCodec<List<T>> {

        @Override
        public List<T> decode(StreamReader reader) {
            int size = reader.takeVarInt();
            Check.argCondition(size > maxSize, "List size ({0}) exceeds max size {1}", size, maxSize);
            if (size == 0) return List.of();
            @SuppressWarnings("unchecked")
            T[] list = (T[]) new Object[size];
            for (int i = 0; i < size; i++) {
                list[i] = parent.decode(reader);
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
                parent.encode(stream, value);
            }
        }
    }

    record SetStreamCodec<T>(StreamCodec<T> parent, int maxSize) implements StreamCodec<Set<T>> {
        @Override
        public void encode(StreamWriter stream, Set<T> value) {
            stream.writeVarInt(value.size());
            for (T t : value) {
                parent.encode(stream, t);
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
                list[i] = parent.decode(stream);
            }
            return Set.of(list);
        }
    }

    record OptionalStreamCodec<T extends @UnknownNullability Object>(StreamCodec<T> parent)
            implements StreamCodec<@Nullable T> {

        @Override
        public @Nullable T decode(StreamReader stream) {
            return stream.takeBoolean() ? parent.decode(stream) : null;
        }

        @Override
        public void encode(StreamWriter stream, @Nullable T value) {
            if (value == null) {
                stream.writeBoolean(false);
                return;
            }
            stream.writeBoolean(true);
            parent.encode(stream, value);
        }
    }

    record UnionStreamCodec<T extends @UnknownNullability Object, R>(StreamCodec<T> parent,
                                                                     Function<? super T, ? extends StreamCodec<? extends R>> serializers,
                                                                     Function<? super R, ? extends T> keyFunc) implements StreamCodec<R> {

        @Override
        public R decode(StreamReader stream) {
            return serializers.apply(parent.decode(stream)).decode(stream);
        }

        @Override
        public void encode(StreamWriter stream, R value) {
            T key = keyFunc.apply(value);
            parent.encode(stream, key);
            @SuppressWarnings("unchecked") // Using the correct generics would make us drop ? extends R here, or using two seperate functions
            var encoder = ((StreamEncoder<R>) serializers.apply(key));
            encoder.encode(stream, value);
        }
    }
}
