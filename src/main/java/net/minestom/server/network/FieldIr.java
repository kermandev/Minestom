package net.minestom.server.network;

import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Function;

public record FieldIr<R extends @UnknownNullability Object, P extends @UnknownNullability Object>(
        int index,
        String name,
        NetworkBuffer.Type<P> originalType,
        TypeIr<P> type,
        Function<? super R, ? extends P> getter
) {
}
