package net.minestom.server.network.ir;

import java.lang.classfile.TypeKind;

public sealed interface LocalType {
    record Kind(TypeKind kind) implements LocalType {
    }

    record Reference(Class<?> type) implements LocalType {
    }
}
