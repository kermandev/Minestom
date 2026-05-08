package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

record IrClassData(ProgramIr write, ProgramIr read, List<TransformFieldData> transforms,
                   Map<String, Integer> constructors, Map<String, IrCtorData> constructorIrs,
                   Map<Object, IrCtorData> constructorIrsByFactory,
                   List<ExternalTypeFieldData> externalTypes,
                   Map<Function<?, ?>, TransformFieldData> transformsByFunction,
                   Map<NetworkBuffer.Type<?>, ExternalTypeFieldData> externalTypesByType,
                   int dataIndex) {
    public IrClassData {
        transforms = List.copyOf(transforms);
        constructors = Map.copyOf(constructors);
        constructorIrs = Map.copyOf(constructorIrs);
        constructorIrsByFactory = identityCopy(constructorIrsByFactory);
        externalTypes = List.copyOf(externalTypes);
        transformsByFunction = identityCopy(transformsByFunction);
        externalTypesByType = identityCopy(externalTypesByType);
    }

    static IrClassData collect(List<Object> classData, NetworkBuffer.Type<?> type, ProgramIr write, ProgramIr read) {
        final List<TransformFieldData> transforms = new ArrayList<>();
        final List<ExternalTypeFieldData> externalTypes = new ArrayList<>();
        final Map<String, Integer> constructors = new LinkedHashMap<>();
        final Map<String, IrCtorData> constructorIrs = new HashMap<>();
        final Map<Object, IrCtorData> constructorIrsByFactory = new IdentityHashMap<>();
        final Map<Function<?, ?>, TransformFieldData> transformsByFunction = new IdentityHashMap<>();
        final Map<NetworkBuffer.Type<?>, ExternalTypeFieldData> externalTypesByType = new IdentityHashMap<>();

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
            constructorIrsByFactory.put(factory, data);
        }

        int transformIndex = 0;
        for (Function<?, ?> function : usage.functions) {
            final TransformFieldData data = new TransformFieldData("fn" + transformIndex++, function, addClassData(classData, function));
            transforms.add(data);
            transformsByFunction.put(function, data);
        }

        int extIndex = 0;
        for (NetworkBuffer.Type<?> extType : usage.externalTypes) {
            final ExternalTypeFieldData data = new ExternalTypeFieldData("ext" + extIndex++, extType, addClassData(classData, extType));
            externalTypes.add(data);
            externalTypesByType.put(extType, data);
        }

        final int dataIndex = addClassData(classData, type);
        return new IrClassData(write, read, transforms, constructors, constructorIrs, constructorIrsByFactory,
                externalTypes, transformsByFunction, externalTypesByType, dataIndex);
    }

    IrCtorData constructorIr(String name) {
        return constructorIrs.get(name);
    }

    String typeFieldName(NetworkBuffer.Type<?> type) {
        final ExternalTypeFieldData field = externalTypesByType.get(type);
        if (field != null) return field.name();
        throw new IllegalStateException("Missing type field for " + type);
    }

    String transformFunctionName(Function<?, ?> function) {
        final TransformFieldData field = transformsByFunction.get(function);
        if (field != null) return field.name();
        throw new IllegalStateException("Missing transform function field");
    }

    String ctorFieldName(Object factory) {
        final IrCtorData field = constructorIrsByFactory.get(factory);
        if (field != null) return field.name();
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

    private static <K, V> Map<K, V> identityCopy(Map<K, V> values) {
        final IdentityHashMap<K, V> copy = new IdentityHashMap<>(values);
        return Collections.unmodifiableMap(copy);
    }

    private static class Usage {
        final Set<Function<?, ?>> functions = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<NetworkBuffer.Type<?>> externalTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Object, Integer> constructors = new IdentityHashMap<>();
    }


    record IrCtorData(Object factory, String name, int fieldCount, int dataIndex) {
    }

    record TransformFieldData(String name, Function<?, ?> function, int dataIndex) {
    }

    record ExternalTypeFieldData(String name, NetworkBuffer.Type<?> type, int dataIndex) {
    }
}
