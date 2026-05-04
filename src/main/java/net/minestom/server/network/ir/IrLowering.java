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

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T extends @UnknownNullability Object> NetworkIr<T> networkIr(String name, Object[] values, int fieldCount,
                                                                                 ConstructorIr<T> constructor) {
        final List<FieldIr<T, ?>> fields = new ArrayList<>(fieldCount);
        final FieldIr<T, ?>[] fieldArray = new FieldIr[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            final NetworkBuffer.Type<?> type = (NetworkBuffer.Type<?>) values[i * 2];
            final Function<? super T, ?> getter = (Function<? super T, ?>) values[i * 2 + 1];
            final FieldIr<T, ?> field = new FieldIr(i, "field" + i, type, getter);
            fields.add(field);
            fieldArray[i] = field;
        }
        final ProgramIr write = IrOptimizer.optimize(writeProgram(fieldArray));
        final ProgramIr read = IrOptimizer.optimize(readProgram(fieldArray, constructor));
        return new NetworkIr<>(name, fields, constructor, write, read);
    }

    static IrClassData collectIrClassData(List<Object> classData, NetworkIr<?> ir) {
        final List<IrFieldData> fields = new ArrayList<>();
        final List<TransformFieldData> transforms = new ArrayList<>();
        final List<ExternalTypeFieldData> externalTypes = new ArrayList<>();
        final Map<String, Integer> constructors = new LinkedHashMap<>();
        final Map<String, ConstructorIr<?>> constructorIrs = new HashMap<>();

        final Usage usage = new Usage();
        collectUsage(ir.write(), usage);
        collectUsage(ir.read(), usage);

        int fieldIndex = 0;
        for (FieldIr<?, ?> field : usage.getters) {
            fields.add(new IrFieldData(field, "field" + fieldIndex++,
                    usage.externalTypes.contains(field.type()) ? addClassData(classData, field.type()) : -1,
                    addClassData(classData, field.getter())));
        }

        int ctorIndex = 0;
        for (ConstructorIr<?> constructor : usage.constructors) {
            final String name = "ctor" + ctorIndex++;
            constructors.put(name, addClassData(classData, constructor.object()));
            constructorIrs.put(name, constructor);
        }

        int transformIndex = 0;
        for (Function<?, ?> function : usage.functions) {
            transforms.add(new TransformFieldData("fn" + transformIndex++, function, addClassData(classData, function)));
        }

        int extIndex = 0;
        for (NetworkBuffer.Type<?> type : usage.externalTypes) {
            boolean alreadyAdded = false;
            for (IrFieldData field : fields) {
                if (field.ir().type() == type && field.typeDataIndex() != -1) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                externalTypes.add(new ExternalTypeFieldData("ext" + extIndex++, type, addClassData(classData, type)));
            }
        }

        return new IrClassData(ir, "", fields, transforms, constructors, constructorIrs, externalTypes);
    }

    private static class Usage {
        final Set<FieldIr<?, ?>> getters = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Function<?, ?>> functions = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<NetworkBuffer.Type<?>> externalTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<ConstructorIr<?>> constructors = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static void collectUsage(ProgramIr program, Usage usage) {
        for (Op op : program.ops()) {
            collectUsage(op, usage);
        }
    }

    private static void collectUsage(Op op, Usage usage) {
        switch (op) {
            case Op.GetField getField -> usage.getters.add(getField.field());
            case Op.Apply apply -> usage.functions.add(apply.function());
            case Op.WriteExternal write -> usage.externalTypes.add(write.type());
            case Op.ReadExternal read -> usage.externalTypes.add(read.type());
            case Op.Construct construct -> usage.constructors.add(construct.constructor());
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
            case RunStep.GetField getField -> usage.getters.add(getField.field());
            case RunStep.Apply apply -> usage.functions.add(apply.function());
            case RunStep.Construct construct -> usage.constructors.add(construct.constructor());
            default -> {
            }
        }
    }

    private static ProgramIr writeProgram(FieldIr<?, ?>[] fields) {
        final Local initialSource = referenceLocal();
        final WriteBuilderImpl builder = new WriteBuilderImpl(initialSource);
        for (int i = 0; i < fields.length; i++) {
            final FieldIr<?, ?> field = fields[i];
            final Local nested = referenceLocal();
            builder.push(new Op.GetField(field, Integer.toString(i + 1), builder.source(), nested));
            builder.pushSource(nested);
            builder.lower(field.type(), new Value.LocalValue(nested));
            builder.popSource();
        }
        return new ProgramIr(builder.result(), initialSource);
    }

    private static ProgramIr readProgram(FieldIr<?, ?>[] fields, ConstructorIr<?> constructor) {
        final ReadBuilderImpl builder = new ReadBuilderImpl();
        final List<Value> args = new ArrayList<>(fields.length);
        for (FieldIr<?, ?> field : fields) {
            args.add(builder.lower(field.type()));
        }
        final Local result = referenceLocal();
        builder.push(new Op.Construct(constructor, "", args, result));
        builder.push(new Op.Return(new Value.LocalValue(result)));
        return new ProgramIr(builder.result());
    }

    private static Local ensureLocal(List<Op> ops, Value value, String path) {
        if (value instanceof Value.LocalValue localValue) return localValue.local();
        final Local local = referenceLocal();
        ops.add(new Op.Store(value, local));
        return local;
    }

    private static Local referenceLocal() {
        return new Local(new LocalType.Reference(Object.class));
    }

    private static int addClassData(List<Object> classData, Object value) {
        final int index = classData.size();
        classData.add(value);
        return index;
    }

    private static final class WriteBuilderImpl implements IrWriteBuilder {
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

    private static final class ReadBuilderImpl implements IrReadBuilder {
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
