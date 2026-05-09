package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

record IrClassData(ProgramIr write, ProgramIr read, List<TransformFieldData> transforms,
                   Map<String, Integer> constructors, Map<String, IrCtorData> constructorIrs,
                   List<ExternalTypeFieldData> externalTypes,
                   int dataIndex) {
    public IrClassData {
        transforms = List.copyOf(transforms);
        constructors = Map.copyOf(constructors);
        constructorIrs = Map.copyOf(constructorIrs);
        externalTypes = List.copyOf(externalTypes);
    }

    static IrClassData collect(List<Object> classData, NetworkBuffer.Type<?> type, ProgramIr write, ProgramIr read) {
        final List<TransformFieldData> transforms = new ArrayList<>();
        final List<ExternalTypeFieldData> externalTypes = new ArrayList<>();
        final Map<String, Integer> constructors = new LinkedHashMap<>();
        final Map<String, IrCtorData> constructorIrs = new HashMap<>();

        final Usage usage = new Usage();
        collectUsage(write, usage);
        collectUsage(read, usage);

        int ctorIndex = 0;
        for (Map.Entry<Object, Integer> entry : usage.constructors.entrySet()) {
            final String name = "ctor" + ctorIndex++;
            final Object factory = entry.getKey();
            final int fieldCount = entry.getValue();
            final int dataIndex = addClassData(classData, factory);
            final IrCtorData data = new IrCtorData(factory, name, fieldCount, dataIndex);
            constructors.put(name, dataIndex);
            constructorIrs.put(name, data);
        }

        int transformIndex = 0;
        for (Function<?, ?> function : usage.functions) {
            final TransformFieldData data = new TransformFieldData("fn" + transformIndex++, function, addClassData(classData, function));
            transforms.add(data);
        }

        int extIndex = 0;
        for (NetworkBuffer.Type<?> extType : usage.externalTypes) {
            final ExternalTypeFieldData data = new ExternalTypeFieldData("ext" + extIndex++, extType, addClassData(classData, extType));
            externalTypes.add(data);
        }

        final int dataIndex = addClassData(classData, type);
        return new IrClassData(write, read, transforms, constructors, constructorIrs, externalTypes, dataIndex);
    }

    IrCtorData constructorIr(String name) {
        return constructorIrs.get(name);
    }

    String typeFieldName(NetworkBuffer.Type<?> type) {
        for (ExternalTypeFieldData externalType : externalTypes) {
            if (externalType.type().equals(type)) return externalType.name();
        }
        throw new IllegalStateException("Missing type field for " + type);
    }

    String transformFunctionName(Function<?, ?> function) {
        for (TransformFieldData transform : transforms) {
            if (transform.function().equals(function)) return transform.name();
        }
        throw new IllegalStateException("Missing transform function field");
    }

    String ctorFieldName(Object factory) {
        for (IrCtorData constructor : constructorIrs.values()) {
            if (constructor.factory().equals(factory)) return constructor.name();
        }
        throw new IllegalStateException("Missing constructor field");
    }

    private static void collectUsage(ProgramIr program, Usage usage) {
        for (RunIr run : program.runs()) {
            collectUsage(run, usage);
        }
    }

    private static void collectUsage(RunIr run, Usage usage) {
        for (RunItem item : run.items()) {
            collectUsage(item, usage);
        }
    }

    private static void collectUsage(RunItem item, Usage usage) {
        switch (item) {
            case RunItem.Apply apply -> usage.functions.add(apply.function());
            case RunItem.WriteExternal write -> usage.externalTypes.add(write.type());
            case RunItem.ReadExternal read -> usage.externalTypes.add(read.type());
            case RunItem.Construct construct -> usage.constructors.put(construct.factory(), construct.args().size());
            case RunItem.If ifOp -> {
                for (RunIr run : ifOp.thenRuns()) collectUsage(run, usage);
                for (RunIr run : ifOp.elseRuns()) collectUsage(run, usage);
            }
            case RunItem.ForEach forEach -> {
                for (RunIr run : forEach.body()) collectUsage(run, usage);
            }
            case RunItem.ForIndex forIndex -> {
                for (RunIr run : forIndex.body()) collectUsage(run, usage);
            }
            default -> {
            }
        }
    }

    private static int addClassData(List<Object> classData, Object value) {
        final int index = classData.size();
        classData.add(value);
        return index;
    }

    private static class Usage {
        final Set<Function<?, ?>> functions = new LinkedHashSet<>();
        final Set<NetworkBuffer.Type<?>> externalTypes = new LinkedHashSet<>();
        final Map<Object, Integer> constructors = new LinkedHashMap<>();
    }


    record IrCtorData(Object factory, String name, int fieldCount, int dataIndex) {
    }

    record TransformFieldData(String name, Function<?, ?> function, int dataIndex) {
    }

    record ExternalTypeFieldData(String name, NetworkBuffer.Type<?> type, int dataIndex) {
    }
}
