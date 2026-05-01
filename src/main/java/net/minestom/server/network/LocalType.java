package net.minestom.server.network;

import java.lang.classfile.TypeKind;

public sealed interface LocalType permits LocalType.Primitive, LocalType.Reference {
    record Primitive(TypeKind kind) implements LocalType {
    }

    record Reference(Class<?> type) implements LocalType {
    }
}
