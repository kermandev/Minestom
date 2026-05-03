package net.minestom.server.network.ir;
import net.minestom.server.network.NetworkBuffer;

import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Function;

public sealed interface TypeIr<T extends @UnknownNullability Object>
        permits TypeIr.External, TypeIr.Template, TypeIr.Constant, TypeIr.Primitive, TypeIr.VarInt, TypeIr.VarLong,
        TypeIr.Optional, TypeIr.Either, TypeIr.Transform, TypeIr.StringUtf8, TypeIr.ByteArray, TypeIr.FixedBytes,
        TypeIr.ListType, TypeIr.MapType {
    record External<T extends @UnknownNullability Object>(NetworkBuffer.Type<T> type) implements TypeIr<T> {
    }

    record Template<T extends @UnknownNullability Object>(NetworkIr<T> ir) implements TypeIr<T> {
    }

    record Constant<T extends @UnknownNullability Object>(T value) implements TypeIr<T> {
    }

    record Primitive<T extends @UnknownNullability Object>(PrimitiveKind kind) implements TypeIr<T> {
    }

    record VarInt() implements TypeIr<Integer> {
    }

    record VarLong() implements TypeIr<Long> {
    }

    record Optional<T extends @UnknownNullability Object>(TypeIr<T> parent) implements TypeIr<T> {
    }

    record Either<L extends @UnknownNullability Object, R extends @UnknownNullability Object>(TypeIr<L> left, TypeIr<R> right) implements TypeIr<net.minestom.server.utils.Either<L, R>> {
    }

    record Transform<A extends @UnknownNullability Object, B extends @UnknownNullability Object>(
            TypeIr<A> parent,
            Function<? super A, ? extends B> to,
            Function<? super B, ? extends A> from
    ) implements TypeIr<B> {
    }

    record StringUtf8(int maxLength) implements TypeIr<String> {
    }

    record ByteArray(int maxLength) implements TypeIr<byte[]> {
    }

    record FixedBytes(int length) implements TypeIr<byte[]> {
    }

    record ListType<E extends @UnknownNullability Object, C extends @UnknownNullability Object>(
            NetworkBuffer.Type<C> originalType,
            TypeIr<E> element,
            int maxLength
    ) implements TypeIr<C> {
    }

    record MapType<K extends @UnknownNullability Object, V extends @UnknownNullability Object, M extends @UnknownNullability Object>(
            NetworkBuffer.Type<M> originalType,
            TypeIr<K> key,
            TypeIr<V> value,
            int maxLength
    ) implements TypeIr<M> {
    }
}
