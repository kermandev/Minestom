package net.minestom.server.network.ir;

import java.lang.classfile.TypeKind;
import java.util.Objects;

public sealed interface LocalType {
    record Kind(TypeKind kind) implements LocalType {
        public Kind {
            Objects.requireNonNull(kind, "kind");
            if (kind == TypeKind.REFERENCE) {
                throw new IllegalArgumentException("Use LocalType.Reference for reference locals");
            }
        }
    }

    record Reference(Class<?> type) implements LocalType {
        public Reference {
            Objects.requireNonNull(type, "type");
            if (type.isPrimitive()) {
                throw new IllegalArgumentException("Use LocalType.Kind for primitive locals");
            }
        }
    }
}
