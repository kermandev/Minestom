package net.minestom.server.network.ir;

import org.jetbrains.annotations.UnknownNullability;

public record NetworkIr<T extends @UnknownNullability Object>(
        ProgramIr write,
        ProgramIr read
) {
}
