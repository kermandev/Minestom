package net.minestom.server.network;

import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

public record NetworkIr<T extends @UnknownNullability Object>(
        String name,
        List<FieldIr<T, ?>> fields,
        ConstructorIr<T> constructor,
        ProgramIr write,
        ProgramIr read
) {
    public NetworkIr {
        fields = List.copyOf(fields);
    }
}
