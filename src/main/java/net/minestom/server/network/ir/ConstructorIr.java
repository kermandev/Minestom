package net.minestom.server.network.ir;

import org.jetbrains.annotations.UnknownNullability;

public record ConstructorIr<T extends @UnknownNullability Object>(Object object, int fieldCount) {
}
