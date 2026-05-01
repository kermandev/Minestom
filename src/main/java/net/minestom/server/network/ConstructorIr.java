package net.minestom.server.network;

import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

@FunctionalInterface
public interface ConstructorIr<T extends @UnknownNullability Object> {
    T construct(List<?> values);
}
