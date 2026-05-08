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
                   List<ExternalTypeFieldData> externalTypes) {
    public IrClassData {
        transforms = List.copyOf(transforms);
        constructors = Map.copyOf(constructors);
        constructorIrs = Map.copyOf(constructorIrs);
        externalTypes = List.copyOf(externalTypes);
    }

    static IrClassData collect(List<Object> classData, ProgramIr write, ProgramIr read) {
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
            constructors.put(name, dataIndex);
            constructorIrs.put(name, new IrCtorData(factory, name, fieldCount, dataIndex));
        }

        int transformIndex = 0;
        for (Function<?, ?> function : usage.functions) {
            transforms.add(new TransformFieldData("fn" + transformIndex++, function, addClassData(classData, function)));
        }

        int extIndex = 0;
        for (NetworkBuffer.Type<?> type : usage.externalTypes) {
            externalTypes.add(new ExternalTypeFieldData("ext" + extIndex++, type, addClassData(classData, type)));
        }

        return new IrClassData(write, read, transforms, constructors, constructorIrs, externalTypes);
    }

    IrCtorData constructorIr(String name) {
        return constructorIrs.get(name);
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
