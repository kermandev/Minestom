package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class IrMetadata {
    private IrMetadata() {}

    record IrClassData(NetworkIr<?> ir, String path, List<IrFieldData> fields, List<TransformFieldData> transforms,
                       Map<String, Integer> constructors, Map<String, ConstructorIr<?>> constructorIrs,
                       List<ExternalTypeFieldData> externalTypes) {
        public IrClassData {
            fields = List.copyOf(fields);
            transforms = List.copyOf(transforms);
            constructors = Map.copyOf(constructors);
            constructorIrs = Map.copyOf(constructorIrs);
            externalTypes = List.copyOf(externalTypes);
        }

        public ConstructorIr<?> constructorIr(String name) {
            return constructorIrs.get(name);
        }
    }

    record IrFieldData(FieldIr<?, ?> ir, String path, int typeDataIndex, int getterDataIndex) {
    }

    record IrCtorData(String name, int fieldCount, int dataIndex) {
    }

    record TransformFieldData(String name, Function<?, ?> function, int dataIndex) {
    }

    record ExternalTypeFieldData(String name, NetworkBuffer.Type<?> type, int dataIndex) {
    }
}
