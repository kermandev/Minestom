package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.Unit;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.classfile.TypeKind;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import static net.minestom.server.network.ir.IrMetadata.*;

final class IrLowering {
    private IrLowering() {}

    static IrClassData collectIrClassData(List<Object> classData, NetworkIr<?> ir) {
        final List<TransformFieldData> transforms = new ArrayList<>();
        final List<ExternalTypeFieldData> externalTypes = new ArrayList<>();
        final Map<String, Integer> constructors = new LinkedHashMap<>();
        final Map<String, IrCtorData> constructorIrs = new HashMap<>();

        final Usage usage = new Usage();
        collectUsage(ir.write(), usage);
        collectUsage(ir.read(), usage);

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

        return new IrClassData(ir, "", transforms, constructors, constructorIrs, externalTypes);
    }

    private static class Usage {
        final Set<Function<?, ?>> functions = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<NetworkBuffer.Type<?>> externalTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Object, Integer> constructors = new IdentityHashMap<>();
    }

    private static void collectUsage(ProgramIr program, Usage usage) {
        for (Op op : program.ops()) {
            collectUsage(op, usage);
        }
    }

    private static void collectUsage(Op op, Usage usage) {
        switch (op) {
            case Op.Apply apply -> usage.functions.add(apply.function());
            case Op.WriteExternal write -> usage.externalTypes.add(write.type());
            case Op.ReadExternal read -> usage.externalTypes.add(read.type());
            case Op.Construct construct -> usage.constructors.put(construct.factory(), construct.args().size());
            case Op.If ifOp -> {
                collectUsage(new ProgramIr(ifOp.thenOps()), usage);
                collectUsage(new ProgramIr(ifOp.elseOps()), usage);
            }
            case Op.ForEach forEach -> {
                collectUsage(new ProgramIr(forEach.body()), usage);
            }
            case Op.ForIndex forIndex -> {
                collectUsage(new ProgramIr(forIndex.body()), usage);
            }
            case Op.WriteRun writeRun -> collectUsage(writeRun.run(), usage);
            case Op.ReadRun readRun -> collectUsage(readRun.run(), usage);
            default -> {
            }
        }
    }

    private static void collectUsage(RunIr run, Usage usage) {
        for (RunItem item : run.items()) {
            if (item instanceof RunItem.ForIndex loop) {
                for (RunStep step : loop.body()) {
                    collectUsage(step, usage);
                }
            }
        }
    }

    private static void collectUsage(RunStep step, Usage usage) {
        switch (step) {
            case RunStep.Apply apply -> usage.functions.add(apply.function());
            case RunStep.Construct construct -> usage.constructors.put(construct.factory(), construct.args().size());
            default -> {
            }
        }
    }

    private static Local ensureLocal(List<Op> ops, Value value, String path) {
        if (value instanceof Value.LocalValue localValue) return localValue.local();
        final Local local = referenceLocal();
        ops.add(new Op.Store(value, local));
        return local;
    }

    static Local referenceLocal() {
        return new Local(new LocalType.Reference(Object.class));
    }

    private static int addClassData(List<Object> classData, Object value) {
        final int index = classData.size();
        classData.add(value);
        return index;
    }

    static final class WriteBuilderImpl implements IrWriteBuilder {
        private final Deque<List<Op>> opStack = new ArrayDeque<>();
        private final Deque<Local> sources = new ArrayDeque<>();

        WriteBuilderImpl(Local initialSource) {
            opStack.push(new ArrayList<>());
            sources.push(initialSource);
        }

        @Override public void push(Op op) { opStack.peek().add(op); }
        @Override public Local source() { return sources.peek(); }
        @Override public void pushSource(Local source) { sources.push(source); }
        @Override public void popSource() { sources.pop(); }

        @Override
        public void lower(NetworkBuffer.Type<?> type, Value value) {
            if (value instanceof Value.LocalValue localValue) {
                pushSource(localValue.local());
                type.lowerWrite(this);
                popSource();
            } else {
                Local temp = referenceLocal();
                push(new Op.Store(value, temp));
                pushSource(temp);
                type.lowerWrite(this);
                popSource();
            }
        }

        @Override
        public List<Op> buildNested(Runnable action) {
            opStack.push(new ArrayList<>());
            action.run();
            return opStack.pop();
        }

        List<Op> result() { return opStack.peek(); }
    }

    static final class ReadBuilderImpl implements IrReadBuilder {
        private final Deque<List<Op>> opStack = new ArrayDeque<>();

        ReadBuilderImpl() { opStack.push(new ArrayList<>()); }

        @Override public void push(Op op) { opStack.peek().add(op); }

        @Override
        public Value lower(NetworkBuffer.Type<?> type) {
            return type.lowerRead(this);
        }

        @Override
        public List<Op> buildNested(Runnable action) {
            opStack.push(new ArrayList<>());
            action.run();
            return opStack.pop();
        }

        List<Op> result() { return opStack.peek(); }
    }
}
