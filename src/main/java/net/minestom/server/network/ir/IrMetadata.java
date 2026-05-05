package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class IrMetadata {
    private IrMetadata() {}

    record IrClassData(ProgramIr write, ProgramIr read, String path, List<TransformFieldData> transforms,
                       Map<String, Integer> constructors, Map<String, IrCtorData> constructorIrs,
                       List<ExternalTypeFieldData> externalTypes) {
        public IrClassData {
            transforms = List.copyOf(transforms);
            constructors = Map.copyOf(constructors);
            constructorIrs = Map.copyOf(constructorIrs);
            externalTypes = List.copyOf(externalTypes);
        }

        public IrCtorData constructorIr(String name) {
            return constructorIrs.get(name);
        }
    }

    record IrCtorData(Object factory, String name, int fieldCount, int dataIndex) {
    }

    record TransformFieldData(String name, Function<?, ?> function, int dataIndex) {
    }

    record ExternalTypeFieldData(String name, NetworkBuffer.Type<?> type, int dataIndex) {
    }
}
