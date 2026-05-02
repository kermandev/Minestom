package net.minestom.server.network;

import java.lang.classfile.TypeKind;

public sealed interface LocalType permits LocalType.Kind, LocalType.Reference {
    record Kind(TypeKind kind) implements LocalType {
    }

    record Reference(Class<?> type) implements LocalType {
    }
}
