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
        final ProgramIr write = writeProgram(fieldArray);
        final ProgramIr read = readProgram(fieldArray, constructor);
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

        collectIrMetadata("", ir, classData, fields, transforms, constructors, constructorIrs, usage);

        // Add standalone transforms
        int standaloneIndex = 0;
        for (Function<?, ?> function : usage.functions) {
            boolean alreadyAdded = false;
            for (TransformFieldData t : transforms) {
                if (t.function() == function) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                transforms.add(new TransformFieldData("fn" + standaloneIndex++, function, addClassData(classData, function)));
            }
        }

        // Add standalone types used in WriteExternal/ReadExternal
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

    private static void collectIrMetadata(String path, NetworkIr<?> ir, List<Object> classData,
                                          List<IrFieldData> allFields, List<TransformFieldData> allTransforms,
                                          Map<String, Integer> allConstructors, Map<String, ConstructorIr<?>> allConstructorIrs,
                                          Usage usage) {
        final String ctorName = ctorName(path);
        if (usage.constructors.contains(ir.constructor())) {
            allConstructors.put(ctorName, addClassData(classData, ir.constructor().object()));
            allConstructorIrs.put(ctorName, ir.constructor());
        }

        final List<? extends FieldIr<?, ?>> irFields = ir.fields();
        for (int i = 0; i < irFields.size(); i++) {
            final FieldIr<?, ?> field = irFields.get(i);
            final String fieldPath = childPath(path, i);
            final boolean getterUsed = usage.getters.contains(field);
            final boolean typeUsed = usage.externalTypes.contains(field.type());
            if (getterUsed || typeUsed) {
                allFields.add(new IrFieldData(field, fieldPath,
                        typeUsed ? addClassData(classData, field.type()) : -1,
                        getterUsed ? addClassData(classData, field.getter()) : -1));
            }
            collectTypeMetadata(fieldPath, field.type(), usage, classData, allFields, allTransforms, allConstructors, allConstructorIrs);
        }
    }

    private static void collectTypeMetadata(String path, NetworkBuffer.Type<?> type, Usage usage,
                                            List<Object> classData, List<IrFieldData> allFields,
                                            List<TransformFieldData> allTransforms,
                                            Map<String, Integer> allConstructors, Map<String, ConstructorIr<?>> allConstructorIrs) {
        if (type instanceof NetworkIrBacked<?> backed) {
            collectIrMetadata(path, backed.ir(), classData, allFields, allTransforms, allConstructors, allConstructorIrs, usage);
        } else if (type instanceof NetworkIrIntrinsic intrinsic) {
            intrinsic.collectMetadata(new NetworkIrIntrinsic.MetadataContext() {
                @Override
                public void child(String suffix, NetworkBuffer.Type<?> child) {
                    collectTypeMetadata(path + suffix, child, usage, classData, allFields, allTransforms, allConstructors, allConstructorIrs);
                }

                @Override
                public void transform(String suffix, Function<?, ?> function) {
                    final String name = suffix.equals("From") ? transformFromName(path, 0) : transformToName(path, 0);
                    if (usage.functions.contains(function)) {
                        allTransforms.add(new TransformFieldData(name, function, addClassData(classData, function)));
                    }
                }
            });
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
        return new ProgramIr(mergeWriteRuns(builder.result()), initialSource);
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
        return new ProgramIr(mergeReadRuns(builder.result()));
    }

    private static List<Op> mergeWriteRuns(List<Op> ops) {
        final List<Op> result = new ArrayList<>();
        Op.WriteRun pendingRun = null;
        final List<Op> pureOps = new ArrayList<>();

        for (Op op : ops) {
            Op processedOp = switch (op) {
                case Op.If ifOp -> new Op.If(ifOp.condition(), mergeWriteRuns(ifOp.thenOps()), mergeWriteRuns(ifOp.elseOps()));
                case Op.ForEach forEach -> new Op.ForEach(forEach.source(), forEach.element(), mergeWriteRuns(forEach.body()));
                case Op.ForIndex forIndex -> new Op.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), mergeWriteRuns(forIndex.body()));
                default -> op;
            };

            if (processedOp instanceof Op.WriteRun next) {
                if (pendingRun == null) {
                    pendingRun = next;
                } else {
                    pendingRun = mergeWriteRun(pendingRun, next);
                }
            } else if (isPure(processedOp)) {
                pureOps.add(processedOp);
            } else {
                if (pendingRun != null) {
                    result.addAll(pureOps);
                    result.add(pendingRun);
                    pendingRun = null;
                } else {
                    result.addAll(pureOps);
                }
                pureOps.clear();
                result.add(processedOp);
            }
        }
        result.addAll(pureOps);
        if (pendingRun != null) result.add(pendingRun);
        return result;
    }

    private static Op.WriteRun mergeWriteRun(Op.WriteRun left, Op.WriteRun right) {
        final Value newSize = addValues(left.run().size(), right.run().size());
        final List<RunItem> newItems = new ArrayList<>(left.run().items());
        for (RunItem item : right.run().items()) {
            newItems.add(shiftItem(item, left.run().size()));
        }
        return new Op.WriteRun(new RunIr(newSize, newItems));
    }

    private static List<Op> mergeReadRuns(List<Op> ops) {
        final List<Op> result = new ArrayList<>();
        Op.ReadRun pendingRun = null;
        final List<Op> pureOps = new ArrayList<>();

        for (Op op : ops) {
            Op processedOp = switch (op) {
                case Op.If ifOp -> new Op.If(ifOp.condition(), mergeReadRuns(ifOp.thenOps()), mergeReadRuns(ifOp.elseOps()));
                case Op.ForEach forEach -> new Op.ForEach(forEach.source(), forEach.element(), mergeReadRuns(forEach.body()));
                case Op.ForIndex forIndex -> new Op.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), mergeReadRuns(forIndex.body()));
                default -> op;
            };

            if (processedOp instanceof Op.ReadRun next) {
                if (pendingRun == null) {
                    pendingRun = next;
                } else {
                    pendingRun = mergeReadRun(pendingRun, next);
                }
            } else if (isPure(processedOp)) {
                pureOps.add(processedOp);
            } else {
                if (pendingRun != null) {
                    result.add(pendingRun);
                    result.addAll(pureOps);
                    pendingRun = null;
                } else {
                    result.addAll(pureOps);
                }
                pureOps.clear();
                result.add(processedOp);
            }
        }
        if (pendingRun != null) result.add(pendingRun);
        result.addAll(pureOps);
        return result;
    }

    private static Op.ReadRun mergeReadRun(Op.ReadRun left, Op.ReadRun right) {
        final Value newSize = addValues(left.run().size(), right.run().size());
        final List<RunItem> newItems = new ArrayList<>(left.run().items());
        for (RunItem item : right.run().items()) {
            newItems.add(shiftItem(item, left.run().size()));
        }
        return new Op.ReadRun(new RunIr(newSize, newItems));
    }

    private static boolean isPure(Op op) {
        return switch (op) {
            case Op.GetField _, Op.Apply _, Op.Cast _, Op.Unbox _, Op.Box _, Op.Store _, Op.Check _,
                 Op.Construct _, Op.MapEntrySet _, Op.MapEntryKey _, Op.MapEntryValue _, Op.ElementAt _,
                 Op.ArrayCreate _, Op.ArraySet _, Op.ListFinish _, Op.MapFinish _,
                 Op.StringToBytes _, Op.BytesToString _, Op.EitherLeft _, Op.EitherRight _ -> true;
            default -> false;
        };
    }

    private static Local ensureLocal(List<Op> ops, Value value, String path) {
        if (value instanceof Value.LocalValue localValue) return localValue.local();
        final Local local = referenceLocal();
        ops.add(new Op.Store(value, local));
        return local;
    }

    public static Value addValues(Value left, Value right) {
        if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
            if (lv instanceof Number l && rv instanceof Number r) return new Value.Const(l.longValue() + r.longValue());
        }
        if (left instanceof Value.Const(Object lv) && lv instanceof Number n && n.longValue() == 0) return right;
        if (right instanceof Value.Const(Object rv) && rv instanceof Number n && n.longValue() == 0) return left;

        // Normalize: ensure left-leaning tree
        if (right instanceof Value.Add rightAdd) {
            return addValues(addValues(left, rightAdd.left()), rightAdd.right());
        }
        return new Value.Add(left, right);
    }

    private static RunItem shiftItem(RunItem item, Value shift) {
        return switch (item) {
            case RunItem.Put put -> new RunItem.Put(put.kind(), addValues(shift, put.offset()), put.value());
            case RunItem.PutVarInt putVarInt ->
                    new RunItem.PutVarInt(addValues(shift, putVarInt.offset()), putVarInt.value(), putVarInt.encodedSize());
            case RunItem.PutBytes putBytes ->
                    new RunItem.PutBytes(addValues(shift, putBytes.offset()), putBytes.byteArray(), putBytes.length());
            case RunItem.Get get -> new RunItem.Get(get.kind(), addValues(shift, get.offset()), get.out());
            case RunItem.GetBytes getBytes ->
                    new RunItem.GetBytes(addValues(shift, getBytes.offset()), getBytes.byteArray(), getBytes.length());
            case RunItem.ForIndex forIndex -> {
                final List<RunStep> body = new ArrayList<>();
                for (RunStep step : forIndex.body()) {
                    body.add(shiftStep(step, shift));
                }
                yield new RunItem.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), body);
            }
        };
    }

    private static RunStep shiftStep(RunStep step, Value shift) {
        return switch (step) {
            case RunStep.Put put -> new RunStep.Put(put.kind(), addValues(shift, put.offset()), put.value());
            case RunStep.Get get -> new RunStep.Get(get.kind(), addValues(shift, get.offset()), get.out());
            case RunStep.PutBytes putBytes ->
                    new RunStep.PutBytes(addValues(shift, putBytes.offset()), putBytes.byteArray(), putBytes.length());
            case RunStep.GetBytes getBytes ->
                    new RunStep.GetBytes(addValues(shift, getBytes.offset()), getBytes.byteArray(), getBytes.length());
            default -> step;
        };
    }


    private static Local referenceLocal() {
        return new Local(new LocalType.Reference(Object.class));
    }

    private static int addClassData(List<Object> classData, Object value) {
        final int index = classData.size();
        classData.add(value);
        return index;
    }

    private static String childPath(String parent, int index) {
        final String value = Integer.toString(index + 1);
        return parent.isEmpty() ? value : parent + "_" + value;
    }

    private static String ctorName(String path) {
        return path.isEmpty() ? TemplateCompiler.CTOR_NAME : TemplateCompiler.CTOR_NAME + path;
    }

    private static String transformToName(String path, int level) {
        return TemplateCompiler.TRANSFORM_TO_PREFIX + path + "_" + (level + 1);
    }

    private static String transformFromName(String path, int level) {
        return TemplateCompiler.TRANSFORM_FROM_PREFIX + path + "_" + (level + 1);
    }

    private static String factoryName(String path) {
        return TemplateCompiler.FACTORY_PREFIX + path;
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
            if (type instanceof NetworkIrIntrinsic intrinsic) {
                if (value instanceof Value.LocalValue localValue) {
                    pushSource(localValue.local());
                    intrinsic.lowerWrite(this);
                    popSource();
                } else {
                    Local temp = referenceLocal();
                    push(new Op.Store(value, temp));
                    pushSource(temp);
                    intrinsic.lowerWrite(this);
                    popSource();
                }
            } else {
                push(new Op.WriteExternal(type, value));
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
            if (type instanceof NetworkIrIntrinsic intrinsic) {
                return intrinsic.lowerRead(this);
            } else {
                Local out = referenceLocal();
                push(new Op.ReadExternal(type, out));
                return new Value.LocalValue(out);
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
}
