package net.minestom.server.network;

import org.jetbrains.annotations.UnknownNullability;

public record ConstructorIr<T extends @UnknownNullability Object>(Object object, int fieldCount) {
}
