package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTypeImpl;
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
            final NetworkBuffer.Type<?> originalType = (NetworkBuffer.Type<?>) values[i * 2];
            final Function<? super T, ?> getter = (Function<? super T, ?>) values[i * 2 + 1];
            final FieldIr<T, ?> field = new FieldIr(i, "field" + i, originalType, typeIr(originalType), getter);
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
        final List<FactoryFieldData> factories = new ArrayList<>();
        final List<ExternalTypeFieldData> externalTypes = new ArrayList<>();
        final Map<String, Integer> constructors = new LinkedHashMap<>();
        final Map<String, ConstructorIr<?>> constructorIrs = new HashMap<>();

        final Usage usage = new Usage();
        collectUsage(ir.write(), usage);
        collectUsage(ir.read(), usage);

        collectIrMetadata("", ir, classData, fields, transforms, constructors, constructorIrs, factories, usage);

        // Add standalone transforms that were not found in TypeIr.Transform
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
                if (field.ir().originalType() == type && field.typeDataIndex() != -1) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                externalTypes.add(new ExternalTypeFieldData("ext" + extIndex++, type, addClassData(classData, type)));
            }
        }

        return new IrClassData(ir, "", fields, transforms, constructors, constructorIrs, factories, externalTypes);
    }

    private static class Usage {
        final Set<FieldIr<?, ?>> getters = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Function<?, ?>> functions = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<NetworkBuffer.Type<?>> externalTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Object> factories = Collections.newSetFromMap(new IdentityHashMap<>());
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
            case Op.CollectionCreate create -> usage.factories.add(create.factory());
            case Op.CollectionAdd add -> usage.factories.add(add.factory());
            case Op.CollectionFinish finish -> usage.factories.add(finish.factory());
            case Op.MapCreate create -> usage.factories.add(create.factory());
            case Op.MapPut put -> usage.factories.add(put.factory());
            case Op.MapFinish finish -> usage.factories.add(finish.factory());
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
            case RunStep.CollectionAdd add -> usage.factories.add(add.factory());
            case RunStep.MapPut put -> usage.factories.add(put.factory());
            case RunStep.Construct construct -> usage.constructors.add(construct.constructor());
            default -> {
            }
        }
    }

    private static void collectIrMetadata(String path, NetworkIr<?> ir, List<Object> classData,
                                          List<IrFieldData> allFields, List<TransformFieldData> allTransforms,
                                          Map<String, Integer> allConstructors, Map<String, ConstructorIr<?>> allConstructorIrs,
                                          List<FactoryFieldData> allFactories, Usage usage) {
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
            final boolean typeUsed = usage.externalTypes.contains(field.originalType());
            if (getterUsed || typeUsed) {
                allFields.add(new IrFieldData(field, fieldPath,
                        typeUsed ? addClassData(classData, field.originalType()) : -1,
                        getterUsed ? addClassData(classData, field.getter()) : -1));
            }
            collectTypeMetadata(fieldPath, field.type(), usage, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
        }
    }

    private static void collectTypeMetadata(String path, TypeIr<?> type, Usage usage,
                                            List<Object> classData, List<IrFieldData> allFields,
                                            List<TransformFieldData> allTransforms,
                                            Map<String, Integer> allConstructors, Map<String, ConstructorIr<?>> allConstructorIrs,
                                            List<FactoryFieldData> allFactories) {
        switch (type) {
            case TypeIr.Template<?> template ->
                    collectIrMetadata(path, template.ir(), classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories, usage);
            case TypeIr.Transform<?, ?> transform -> {
                if (usage.functions.contains(transform.from())) {
                    allTransforms.add(new TransformFieldData(transformFromName(path, 0), transform.from(), addClassData(classData, transform.from())));
                }
                if (usage.functions.contains(transform.to())) {
                    allTransforms.add(new TransformFieldData(transformToName(path, 0), transform.to(), addClassData(classData, transform.to())));
                }
                collectTypeMetadata(path + "X", transform.parent(), usage, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            }
            case TypeIr.Optional<?> optional ->
                    collectTypeMetadata(path + "Opt", optional.parent(), usage, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            case TypeIr.Either<?, ?> either -> {
                collectTypeMetadata(path + "L", either.left(), usage, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
                collectTypeMetadata(path + "R", either.right(), usage, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            }
            case TypeIr.ListType<?, ?> list -> {
                if (usage.factories.contains(list.factory())) {
                    allFactories.add(new FactoryFieldData(factoryName(path), list.factory(), addClassData(classData, list.factory())));
                }
                collectTypeMetadata(path + "E", list.element(), usage, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            }
            case TypeIr.MapType<?, ?, ?> map -> {
                if (usage.factories.contains(map.factory())) {
                    allFactories.add(new FactoryFieldData(factoryName(path), map.factory(), addClassData(classData, map.factory())));
                }
                collectTypeMetadata(path + "K", map.key(), usage, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
                collectTypeMetadata(path + "V", map.value(), usage, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            }
            default -> {
            }
        }
    }

    private static ProgramIr writeProgram(FieldIr<?, ?>[] fields) {
        final List<Op> writeOps = new ArrayList<>();
        final Local source = referenceLocal("value");
        for (int i = 0; i < fields.length; i++) {
            lowerWrite(writeOps, fields[i].type(), fields[i], source, Integer.toString(i + 1), 0);
        }
        return new ProgramIr(mergeWriteRuns(writeOps));
    }

    private static void lowerWrite(List<Op> ops, TypeIr<?> type, @Nullable FieldIr<?, ?> field, Local source, String path, int depth) {
        if (type instanceof TypeIr.Template<?> template) {
            final Local nested;
            if (field != null) {
                nested = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, nested));
            } else {
                nested = source;
            }
            for (int i = 0; i < template.ir().fields().size(); i++) {
                final FieldIr<?, ?> subField = template.ir().fields().get(i);
                lowerWrite(ops, subField.type(), subField, nested, path + "_" + (i + 1), depth + 1);
            }
            return;
        }

        if (type instanceof TypeIr.Transform<?, ?> transform) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local parentValue = referenceLocal("path" + path + "_" + depth + "F");
            ops.add(new Op.Apply(transform.from(), raw, parentValue));
            lowerWrite(ops, transform.parent(), null, parentValue, path + "X", depth + 1);
            return;
        }

        if (type instanceof TypeIr.Optional<?> optional) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }

            final Value present = new Value.IsNotNull(new Value.LocalValue(raw));
            final long fixedSize = fixedByteSize(optional.parent());

            if (fixedSize >= 0) {
                final List<Op> thenOps = new ArrayList<>();
                lowerWrite(thenOps, optional.parent(), null, raw, path + "Opt", depth + 1);

                final List<Op> optimizedThenOps = new ArrayList<>();
                for (Op op : thenOps) {
                    if (op instanceof Op.WriteRun parentRun) {
                        final List<RunItem> mergedItems = new ArrayList<>();
                        mergedItems.add(new RunItem.Put(StoreKind.BOOLEAN, new Value.Const(0L), new Value.Const(true)));
                        for (RunItem item : parentRun.run().items()) {
                            mergedItems.add(shiftItem(item, new Value.Const(1L)));
                        }
                        optimizedThenOps.add(new Op.WriteRun(new RunIr(new Value.Const(1L + fixedSize), mergedItems)));
                    } else {
                        optimizedThenOps.add(op);
                    }
                }

                final List<Op> elseOps = new ArrayList<>();
                elseOps.add(new Op.WriteRun(new RunIr(new Value.Const(1L),
                        List.of(new RunItem.Put(StoreKind.BOOLEAN, new Value.Const(0L), new Value.Const(false))))));

                ops.add(new Op.If(present, optimizedThenOps, elseOps));
            } else {
                ops.add(new Op.WriteRun(new RunIr(new Value.Const(1L),
                        List.of(new RunItem.Put(StoreKind.BOOLEAN, new Value.Const(0L), new Value.BoolByte(present))))));

                final List<Op> thenOps = new ArrayList<>();
                lowerWrite(thenOps, optional.parent(), null, raw, path + "Opt", depth + 1);
                ops.add(new Op.If(present, thenOps, List.of()));
            }
            return;
        }

        if (type instanceof TypeIr.Either<?, ?> either) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }

            final Value isLeft = new Value.IsLeft(new Value.LocalValue(raw));
            final long leftSize = fixedByteSize(either.left());
            final long rightSize = fixedByteSize(either.right());

            if (leftSize >= 0 && rightSize >= 0 && leftSize == rightSize) {
                final List<Op> thenOps = new ArrayList<>();
                final Local leftRaw = referenceLocal("path" + path + "L");
                thenOps.add(new Op.Store(new Value.EitherLeft(new Value.LocalValue(raw)), leftRaw));
                lowerWrite(thenOps, either.left(), null, leftRaw, path + "L", depth + 1);

                final List<Op> optimizedThenOps = new ArrayList<>();
                for (Op op : thenOps) {
                    if (op instanceof Op.WriteRun leftRun) {
                        final List<RunItem> mergedItems = new ArrayList<>();
                        mergedItems.add(new RunItem.Put(StoreKind.BOOLEAN, new Value.Const(0L), new Value.Const(true)));
                        for (RunItem item : leftRun.run().items()) {
                            mergedItems.add(shiftItem(item, new Value.Const(1L)));
                        }
                        optimizedThenOps.add(new Op.WriteRun(new RunIr(new Value.Const(1L + leftSize), mergedItems)));
                    } else {
                        optimizedThenOps.add(op);
                    }
                }

                final List<Op> elseOps = new ArrayList<>();
                final Local rightRaw = referenceLocal("path" + path + "R");
                elseOps.add(new Op.Store(new Value.EitherRight(new Value.LocalValue(raw)), rightRaw));
                lowerWrite(elseOps, either.right(), null, rightRaw, path + "R", depth + 1);

                final List<Op> optimizedElseOps = new ArrayList<>();
                for (Op op : elseOps) {
                    if (op instanceof Op.WriteRun rightRun) {
                        final List<RunItem> mergedItems = new ArrayList<>();
                        mergedItems.add(new RunItem.Put(StoreKind.BOOLEAN, new Value.Const(0L), new Value.Const(false)));
                        for (RunItem item : rightRun.run().items()) {
                            mergedItems.add(shiftItem(item, new Value.Const(1L)));
                        }
                        optimizedElseOps.add(new Op.WriteRun(new RunIr(new Value.Const(1L + rightSize), mergedItems)));
                    } else {
                        optimizedElseOps.add(op);
                    }
                }

                ops.add(new Op.If(isLeft, optimizedThenOps, optimizedElseOps));
            } else {
                ops.add(new Op.WriteRun(new RunIr(new Value.Const(1L),
                        List.of(new RunItem.Put(StoreKind.BOOLEAN, new Value.Const(0L), new Value.BoolByte(isLeft))))));

                final List<Op> thenOps = new ArrayList<>();
                final Local leftRaw = referenceLocal("path" + path + "L");
                thenOps.add(new Op.Store(new Value.EitherLeft(new Value.LocalValue(raw)), leftRaw));
                lowerWrite(thenOps, either.left(), null, leftRaw, path + "L", depth + 1);

                final List<Op> elseOps = new ArrayList<>();
                final Local rightRaw = referenceLocal("path" + path + "R");
                elseOps.add(new Op.Store(new Value.EitherRight(new Value.LocalValue(raw)), rightRaw));
                lowerWrite(elseOps, either.right(), null, rightRaw, path + "R", depth + 1);

                ops.add(new Op.If(isLeft, thenOps, elseOps));
            }
            return;
        }

        if (type instanceof TypeIr.ListType<?, ?> listType) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local sizeLocal = new Local("path" + path + "Size", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.Store(new Value.CollectionSize(new Value.LocalValue(raw)), sizeLocal));
            final Value sizeVal = new Value.LocalValue(sizeLocal);

            if (listType.maxLength() != Integer.MAX_VALUE) {
                ops.add(new Op.Check(new Value.LessThanOrEqual(sizeVal, new Value.Const(listType.maxLength())), "Collection too large"));
            }

            final Local encodedSizeLocal = new Local("path" + path + "EncSize", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.Store(new Value.VarIntSize(sizeVal), encodedSizeLocal));
            final Value encodedSizeVal = new Value.LocalValue(encodedSizeLocal);

            ops.add(new Op.WriteRun(new RunIr(encodedSizeVal,
                    List.of(new RunItem.PutVarInt(new Value.Const(0L), sizeVal, encodedSizeVal)))));

            final Local index = new Local("path" + path + "Idx", new LocalType.Kind(TypeKind.INT));
            final Local element = referenceLocal("path" + path + "Elem");
            final List<Op> body = new ArrayList<>();
            body.add(new Op.ElementAt(new Value.LocalValue(raw), new Value.LocalValue(index), element));
            lowerWrite(body, listType.element(), null, element, path + "E", depth + 1);

            ops.add(new Op.ForIndex(index, new Value.Const(0), sizeVal, body));
            return;
        }

        if (type instanceof TypeIr.MapType<?, ?, ?> mapType) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local sizeLocal = new Local("path" + path + "Size", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.Store(new Value.MapSize(new Value.LocalValue(raw)), sizeLocal));
            final Value sizeVal = new Value.LocalValue(sizeLocal);

            if (mapType.maxLength() != Integer.MAX_VALUE) {
                ops.add(new Op.Check(new Value.LessThanOrEqual(sizeVal, new Value.Const(mapType.maxLength())), "Map too large"));
            }

            final Local encodedSizeLocal = new Local("path" + path + "EncSize", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.Store(new Value.VarIntSize(sizeVal), encodedSizeLocal));
            final Value encodedSizeVal = new Value.LocalValue(encodedSizeLocal);

            ops.add(new Op.WriteRun(new RunIr(encodedSizeVal,
                    List.of(new RunItem.PutVarInt(new Value.Const(0L), sizeVal, encodedSizeVal)))));

            final Local entrySet = referenceLocal("path" + path + "Entries");
            ops.add(new Op.MapEntrySet(new Value.LocalValue(raw), entrySet));

            final Local entry = referenceLocal("path" + path + "Entry");
            final Local key = referenceLocal("path" + path + "Key");
            final Local value = referenceLocal("path" + path + "Value");
            final List<Op> body = new ArrayList<>();
            body.add(new Op.MapEntryKey(entry, key));
            body.add(new Op.MapEntryValue(entry, value));
            lowerWrite(body, mapType.key(), null, key, path + "K", depth + 1);
            lowerWrite(body, mapType.value(), null, value, path + "V", depth + 1);

            ops.add(new Op.ForEach(new Value.LocalValue(entrySet), entry, body));
            return;
        }

        if (type instanceof TypeIr.Constant<?> constant) {
            final Object value = constant.value();
            if (value == net.minestom.server.utils.Unit.INSTANCE) return;
            final PrimitiveKind primitive = constantPrimitive(value);
            if (primitive != null) {
                ops.add(new Op.WriteRun(new RunIr(new Value.Const((long) primitive.storeKind().byteSize()),
                        List.of(new RunItem.Put(primitive.storeKind(), new Value.Const(0L), new Value.Const(value))))));
            } else {
                ops.add(new Op.WriteExternal(field != null ? field.originalType() : ((TypeIr.External<?>) type).type(), new Value.Const(value)));
            }
            return;
        }

        if (type instanceof TypeIr.Constant _) return; // Should be unreachable now

        final PrimitiveKind primitive = runPrimitive(type);
        if (primitive != null) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local normalized = normalizeWritePrimitive(ops, type, raw, path, depth);
            ops.add(new Op.WriteRun(new RunIr(new Value.Const((long) primitive.storeKind().byteSize()),
                    List.of(new RunItem.Put(primitive.storeKind(), new Value.Const(0L), new Value.LocalValue(normalized))))));
            return;
        }

        if (isVarInt(type)) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local normalized = normalizeWriteVarInt(ops, type, raw, path, depth);
            final Value value = new Value.LocalValue(normalized);
            final Value encodedSize = new Value.VarIntSize(value);
            ops.add(new Op.WriteRun(new RunIr(encodedSize, List.of(new RunItem.PutVarInt(new Value.Const(0L), value, encodedSize)))));
            return;
        }

        if (isVarLong(type)) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local normalized = normalizeWriteVarLong(ops, type, raw, path, depth);
            ops.add(new Op.WriteVarLong(new Value.LocalValue(normalized)));
            return;
        }

        if (fixedBytesLength(type) >= 0) {
            final int length = fixedBytesLength(type);
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local bytes = materializeWriteReference(ops, type, TypeIr.FixedBytes.class, raw, path, depth, byte[].class);
            ops.add(new Op.WriteRun(new RunIr(
                    new Value.Const((long) length),
                    List.of(new RunItem.PutBytes(new Value.Const(0L), new Value.LocalValue(bytes), new Value.Const(length)))
            )));
            return;
        }

        if (type instanceof TypeIr.ByteArray(int maxSize)) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local bytes = new Local("path" + path + "Cast", new LocalType.Reference(byte[].class));
            ops.add(new Op.Cast(raw, byte[].class, bytes));

            final Local lengthLocal = new Local("path" + path + "Len", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.Store(new Value.ArrayLength(new Value.LocalValue(bytes)), lengthLocal));
            final Value lengthVal = new Value.LocalValue(lengthLocal);

            if (maxSize != Integer.MAX_VALUE) {
                ops.add(new Op.Check(new Value.LessThanOrEqual(lengthVal, new Value.Const(maxSize)), "Array too long"));
            }

            final Local encodedSizeLocal = new Local("path" + path + "EncSize", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.Store(new Value.VarIntSize(lengthVal), encodedSizeLocal));
            final Value encodedSizeVal = new Value.LocalValue(encodedSizeLocal);

            ops.add(new Op.WriteRun(new RunIr(addValues(encodedSizeVal, lengthVal), List.of(
                    new RunItem.PutVarInt(new Value.Const(0L), lengthVal, encodedSizeVal),
                    new RunItem.PutBytes(encodedSizeVal, new Value.LocalValue(bytes), lengthVal)
            ))));
            return;
        }

        if (type instanceof TypeIr.StringUtf8(int maxSize)) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local str = new Local("path" + path + "Cast", new LocalType.Reference(String.class));
            ops.add(new Op.Cast(raw, String.class, str));

            final Local bytes = referenceLocal("path" + path + "Bytes");
            ops.add(new Op.Apply(TemplateCompiler.STRING_TO_BYTES, str, bytes));
            lowerWrite(ops, new TypeIr.ByteArray(maxSize), null, bytes, path + "Str", depth + 1);
            return;
        }

        final Local raw;
        if (field != null) {
            raw = referenceLocal("path" + path);
            ops.add(new Op.GetField(field, path, source, raw));
        } else {
            raw = source;
        }
        ops.add(new Op.WriteExternal(field != null ? field.originalType() : ((TypeIr.External<?>) type).type(), new Value.LocalValue(raw)));
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

    private static ProgramIr readProgram(FieldIr<?, ?>[] fields, ConstructorIr<?> constructor) {
        final List<Op> readOps = new ArrayList<>();
        final List<Value> args = new ArrayList<>(fields.length);
        for (int i = 0; i < fields.length; i++) {
            args.add(lowerRead(readOps, fields[i].type(), Integer.toString(i + 1), 0));
        }
        final Local result = referenceLocal("result");
        readOps.add(new Op.Construct(constructor, "", args, result));
        readOps.add(new Op.Return(new Value.LocalValue(result)));
        return new ProgramIr(mergeReadRuns(readOps));
    }

    private static Value lowerRead(List<Op> ops, TypeIr<?> type, String path, int depth) {
        if (type instanceof TypeIr.Template<?> template) {
            final List<Value> args = new ArrayList<>();
            for (int i = 0; i < template.ir().fields().size(); i++) {
                final FieldIr<?, ?> subField = template.ir().fields().get(i);
                args.add(lowerRead(ops, subField.type(), path + "_" + (i + 1), depth + 1));
            }
            final Local result = referenceLocal("path" + path + "Result");
            ops.add(new Op.Construct(template.ir().constructor(), path, args, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.Transform<?, ?> transform) {
            final Value parentValue = lowerRead(ops, transform.parent(), path + "X", depth + 1);
            final Local parentLocal = ensureLocal(ops, parentValue, path + "_" + depth + "XL");
            final Local result = referenceLocal("path" + path + "_" + depth + "T");
            ops.add(new Op.Apply(transform.to(), parentLocal, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.Optional<?> optional) {
            final Local present = new Local("path" + path + "Present", new LocalType.Kind(TypeKind.BOOLEAN));
            ops.add(new Op.ReadRun(new RunIr(new Value.Const(1L),
                    List.of(new RunItem.Get(StoreKind.BOOLEAN, new Value.Const(0L), present)))));

            final Local result = referenceLocal("path" + path + "Result");
            final List<Op> thenOps = new ArrayList<>();
            final Value parentValue = lowerRead(thenOps, optional.parent(), path + "Opt", depth + 1);
            thenOps.add(new Op.Store(parentValue, result));

            final List<Op> elseOps = new ArrayList<>();
            elseOps.add(new Op.Store(new Value.Const(null), result));

            ops.add(new Op.If(new Value.LocalValue(present), thenOps, elseOps));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.Either<?, ?> either) {
            final Local isLeft = new Local("path" + path + "IsLeft", new LocalType.Kind(TypeKind.BOOLEAN));
            ops.add(new Op.ReadRun(new RunIr(new Value.Const(1L),
                    List.of(new RunItem.Get(StoreKind.BOOLEAN, new Value.Const(0L), isLeft)))));

            final Local result = referenceLocal("path" + path + "Result");
            
            final List<Op> thenOps = new ArrayList<>();
            final Value leftValue = lowerRead(thenOps, either.left(), path + "L", depth + 1);
            final Local leftLocal = ensureLocal(thenOps, leftValue, path + "_" + depth + "LL");
            final Local eitherLeft = referenceLocal("path" + path + "_" + depth + "EL");
            thenOps.add(new Op.Apply(TemplateCompiler.EITHER_LEFT, leftLocal, eitherLeft));
            thenOps.add(new Op.Store(new Value.LocalValue(eitherLeft), result));

            final List<Op> elseOps = new ArrayList<>();
            final Value rightValue = lowerRead(elseOps, either.right(), path + "R", depth + 1);
            final Local rightLocal = ensureLocal(elseOps, rightValue, path + "_" + depth + "RL");
            final Local eitherRight = referenceLocal("path" + path + "_" + depth + "ER");
            elseOps.add(new Op.Apply(TemplateCompiler.EITHER_RIGHT, rightLocal, eitherRight));
            elseOps.add(new Op.Store(new Value.LocalValue(eitherRight), result));

            ops.add(new Op.If(new Value.LocalValue(isLeft), thenOps, elseOps));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.ListType<?, ?> listType) {
            final Local size = new Local("path" + path + "Size", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(size));
            if (listType.maxLength() != Integer.MAX_VALUE) {
                ops.add(new Op.Check(new Value.LessThanOrEqual(new Value.LocalValue(size), new Value.Const(listType.maxLength())), "Collection too large"));
            }

            final Local collection = referenceLocal("path" + path + "Coll");
            ops.add(new Op.CollectionCreate(listType.factory(), path, new Value.LocalValue(size), collection));

            final Local index = new Local("path" + path + "Idx", new LocalType.Kind(TypeKind.INT));
            final List<Op> body = new ArrayList<>();
            final Value element = lowerRead(body, listType.element(), path + "E", depth + 1);
            body.add(new Op.CollectionAdd(listType.factory(), path, collection, element));

            ops.add(new Op.ForIndex(index, new Value.Const(0), new Value.LocalValue(size), body));

            final Local result = referenceLocal("path" + path + "Res");
            ops.add(new Op.CollectionFinish(listType.factory(), path, collection, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.MapType<?, ?, ?> mapType) {
            final Local size = new Local("path" + path + "Size", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(size));
            if (mapType.maxLength() != Integer.MAX_VALUE) {
                ops.add(new Op.Check(new Value.LessThanOrEqual(new Value.LocalValue(size), new Value.Const(mapType.maxLength())), "Map too large"));
            }

            final Local map = referenceLocal("path" + path + "Map");
            ops.add(new Op.MapCreate(mapType.factory(), path, new Value.LocalValue(size), map));

            final Local index = new Local("path" + path + "Idx", new LocalType.Kind(TypeKind.INT));
            final List<Op> body = new ArrayList<>();
            final Value key = lowerRead(body, mapType.key(), path + "K", depth + 1);
            final Value value = lowerRead(body, mapType.value(), path + "V", depth + 1);
            body.add(new Op.MapPut(mapType.factory(), path, map, key, value));

            ops.add(new Op.ForIndex(index, new Value.Const(0), new Value.LocalValue(size), body));

            final Local result = referenceLocal("path" + path + "Res");
            ops.add(new Op.MapFinish(mapType.factory(), path, map, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.ByteArray(int maxSize)) {
            final Local length = new Local("path" + path + "Length", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(length));
            if (maxSize != Integer.MAX_VALUE) {
                ops.add(new Op.Check(new Value.LessThanOrEqual(new Value.LocalValue(length), new Value.Const(maxSize)), "Array too long"));
            }

            final Local bytes = new Local("path" + path + "Bytes", new LocalType.Reference(byte[].class));
            ops.add(new Op.ReadRun(new RunIr(new Value.LocalValue(length),
                    List.of(new RunItem.GetBytes(new Value.Const(0L), bytes, new Value.LocalValue(length))))));
            return new Value.LocalValue(bytes);
        }

        if (type instanceof TypeIr.StringUtf8(int maxSize)) {
            final Value bytes = lowerRead(ops, new TypeIr.ByteArray(maxSize), path + "Str", depth + 1);
            final Local bytesLocal = ensureLocal(ops, bytes, path + "_" + depth + "StrL");
            final Local result = referenceLocal("path" + path + "Result");
            ops.add(new Op.Apply(TemplateCompiler.BYTES_TO_STRING, bytesLocal, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.Constant(Object value)) return new Value.Const(value);

        final PrimitiveKind primitive = runPrimitive(type);
        if (primitive != null) {
            final Local normalized = new Local("path" + path + "Value", new LocalType.Kind(primitive.localKind()));
            ops.add(new Op.ReadRun(new RunIr(new Value.Const((long) primitive.storeKind().byteSize()),
                    List.of(new RunItem.Get(primitive.storeKind(), new Value.Const(0L), normalized)))));
            return new Value.LocalValue(materializeReadPrimitive(ops, type, normalized, path, depth));
        }

        if (isVarInt(type)) {
            final Local normalized = new Local("path" + path + "Value", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(normalized));
            return new Value.LocalValue(materializeReadVarInt(ops, type, normalized, path, depth));
        }

        if (isVarLong(type)) {
            final Local normalized = new Local("path" + path + "Value", new LocalType.Kind(TypeKind.LONG));
            ops.add(new Op.ReadVarLong(normalized));
            return new Value.LocalValue(materializeReadVarLong(ops, type, normalized, path, depth));
        }

        if (fixedBytesLength(type) >= 0) {
            final int length = fixedBytesLength(type);
            final Local bytes = new Local("path" + path + "Bytes", new LocalType.Reference(byte[].class));
            ops.add(new Op.ReadRun(new RunIr(
                    new Value.Const((long) length),
                    List.of(new RunItem.GetBytes(new Value.Const(0L), bytes, new Value.Const(length)))
            )));
            return new Value.LocalValue(materializeReadReference(ops, type, TypeIr.FixedBytes.class, bytes, path, depth));
        }

        final Local readLocal = referenceLocal("path" + path);
        ops.add(new Op.ReadExternal(((TypeIr.External<?>) type).type(), readLocal));
        return new Value.LocalValue(readLocal);
    }

    private static List<Op> mergeReadRuns(List<Op> ops) {
        final List<Op> merged = new ArrayList<>();
        Op.ReadRun current = null;
        for (Op op : ops) {
            Op processedOp = switch (op) {
                case Op.If ifOp -> new Op.If(ifOp.condition(), mergeReadRuns(ifOp.thenOps()), mergeReadRuns(ifOp.elseOps()));
                case Op.ForEach forEach -> new Op.ForEach(forEach.source(), forEach.element(), mergeReadRuns(forEach.body()));
                case Op.ForIndex forIndex -> new Op.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), mergeReadRuns(forIndex.body()));
                default -> op;
            };

            if (processedOp instanceof Op.ReadRun next) {
                if (current == null) {
                    current = next;
                } else {
                    current = mergeReadRun(current, next);
                }
            } else {
                if (current != null) {
                    merged.add(current);
                    current = null;
                }
                merged.add(processedOp);
            }
        }
        if (current != null) merged.add(current);
        return merged;
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
                 Op.CollectionCreate _, Op.CollectionFinish _, Op.MapCreate _, Op.MapFinish _ -> true;
            default -> false;
        };
    }

    private static Local ensureLocal(List<Op> ops, Value value, String path) {
        if (value instanceof Value.LocalValue localValue) return localValue.local();
        final Local local = referenceLocal("path" + path);
        ops.add(new Op.Store(value, local));
        return local;
    }

    public static Value addValues(Value left, Value right) {
        if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
            if (lv instanceof Long l && rv instanceof Long r) return new Value.Const(l + r);
            if (lv instanceof Integer l && rv instanceof Integer r) return new Value.Const((long) l + r);
            if (lv instanceof Long l && rv instanceof Integer r) return new Value.Const(l + r);
            if (lv instanceof Integer l && rv instanceof Long r) return new Value.Const(l + r);
        }
        if (left instanceof Value.Const(Object lv)) {
            if (lv instanceof Long l && l == 0) return right;
            if (lv instanceof Integer l && l == 0) return right;
        }
        if (right instanceof Value.Const(Object rv)) {
            if (rv instanceof Long r && r == 0) return left;
            if (rv instanceof Integer r && r == 0) return left;
        }
        return new Value.Add(left, right);
    }

    private static Local normalizeWritePrimitive(List<Op> ops, TypeIr<?> type, Local in, String path, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("path" + path + "F" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWritePrimitive(ops, transform.parent(), out, path, depth + 1);
            }
            case TypeIr.Primitive<?> primitive -> {
                final PrimitiveKind kind = primitive.kind();
                final Local cast = referenceLocal("path" + path + "C" + depth);
                final Local normalized = new Local("path" + path + "V" + depth, new LocalType.Kind(kind.localKind()));
                ops.add(new Op.Cast(in, kind.wrapperClass(), cast));
                ops.add(new Op.Unbox(kind, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not primitive run-compatible: " + type);
        };
    }

    private static Local materializeReadPrimitive(List<Op> ops, TypeIr<?> type, Local normalized, String path, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadPrimitive(ops, transform.parent(), normalized, path, depth + 1);
                final Local out = referenceLocal("path" + path + "T" + depth);
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.Primitive<?> primitive -> {
                final PrimitiveKind kind = primitive.kind();
                final Local boxed = referenceLocal("path" + path + "B" + depth);
                ops.add(new Op.Box(kind, normalized, boxed));
                yield boxed;
            }
            default -> throw new IllegalArgumentException("Type is not primitive run-compatible: " + type);
        };
    }

    private static Local normalizeWriteVarInt(List<Op> ops, TypeIr<?> type, Local in, String path, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("path" + path + "F" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWriteVarInt(ops, transform.parent(), out, path, depth + 1);
            }
            case TypeIr.VarInt _ -> {
                final Local cast = referenceLocal("path" + path + "C" + depth);
                final Local normalized = new Local("path" + path + "V" + depth, new LocalType.Kind(TypeKind.INT));
                ops.add(new Op.Cast(in, Integer.class, cast));
                ops.add(new Op.Unbox(PrimitiveKind.INT, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not VarInt-compatible: " + type);
        };
    }

    private static Local materializeReadVarInt(List<Op> ops, TypeIr<?> type, Local normalized, String path, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadVarInt(ops, transform.parent(), normalized, path, depth + 1);
                final Local out = referenceLocal("path" + path + "T" + depth);
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.VarInt _ -> {
                final Local boxed = referenceLocal("path" + path + "B" + depth);
                ops.add(new Op.Box(PrimitiveKind.INT, normalized, boxed));
                yield boxed;
            }
            default -> throw new IllegalArgumentException("Type is not VarInt-compatible: " + type);
        };
    }

    private static boolean isVarInt(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.VarInt _ -> true;
            case TypeIr.Transform<?, ?> transform -> isVarInt(transform.parent());
            case TypeIr.Optional<?> optional -> isVarInt(optional.parent());
            default -> false;
        };
    }

    private static Local normalizeWriteVarLong(List<Op> ops, TypeIr<?> type, Local in, String path, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("path" + path + "F" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWriteVarLong(ops, transform.parent(), out, path, depth + 1);
            }
            case TypeIr.VarLong _ -> {
                final Local cast = referenceLocal("path" + path + "C" + depth);
                final Local normalized = new Local("path" + path + "V" + depth, new LocalType.Kind(TypeKind.LONG));
                ops.add(new Op.Cast(in, Long.class, cast));
                ops.add(new Op.Unbox(PrimitiveKind.LONG, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not VarLong-compatible: " + type);
        };
    }

    private static Local materializeReadVarLong(List<Op> ops, TypeIr<?> type, Local normalized, String path, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadVarLong(ops, transform.parent(), normalized, path, depth + 1);
                final Local out = referenceLocal("path" + path + "T" + depth);
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.VarLong _ -> {
                final Local boxed = referenceLocal("path" + path + "B" + depth);
                ops.add(new Op.Box(PrimitiveKind.LONG, normalized, boxed));
                yield boxed;
            }
            default -> throw new IllegalArgumentException("Type is not VarLong-compatible: " + type);
        };
    }

    private static boolean isVarLong(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.VarLong _ -> true;
            case TypeIr.Transform<?, ?> transform -> isVarLong(transform.parent());
            case TypeIr.Optional<?> optional -> isVarLong(optional.parent());
            default -> false;
        };
    }

    private static Local materializeWriteReference(List<Op> ops, TypeIr<?> type, Class<?> targetType, Local in,
                                                   String path, int depth, Class<?> targetClass) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("path" + path + "F" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield materializeWriteReference(ops, transform.parent(), targetType, out, path, depth + 1, targetClass);
            }
            default -> {
                if (!targetType.isInstance(type)) {
                    throw new IllegalArgumentException("Type is not " + targetType.getSimpleName() + "-compatible: " + type);
                }
                final Local cast = new Local("path" + path + "C" + depth, new LocalType.Reference(targetClass));
                ops.add(new Op.Cast(in, targetClass, cast));
                yield cast;
            }
        };
    }

    private static Local materializeReadReference(List<Op> ops, TypeIr<?> type, Class<?> targetType, Local in,
                                                  String path, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadReference(ops, transform.parent(), targetType, in, path, depth + 1);
                final Local out = referenceLocal("path" + path + "T" + depth);
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            default -> {
                if (!targetType.isInstance(type)) {
                    throw new IllegalArgumentException("Type is not " + targetType.getSimpleName() + "-compatible: " + type);
                }
                yield in;
            }
        };
    }

    private static int fixedBytesLength(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.FixedBytes fixedBytes -> fixedBytes.length();
            case TypeIr.Transform<?, ?> transform -> fixedBytesLength(transform.parent());
            case TypeIr.Optional<?> optional -> fixedBytesLength(optional.parent());
            case TypeIr.Either<?, ?> either -> {
                int left = fixedBytesLength(either.left());
                int right = fixedBytesLength(either.right());
                yield (left == right && left >= 0) ? left : -1;
            }
            default -> -1;
        };
    }

    private static long fixedByteSize(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.Constant<?> _ -> 0;
            case TypeIr.Primitive<?> primitive -> primitive.kind().storeKind().byteSize();
            case TypeIr.FixedBytes fixedBytes -> fixedBytes.length();
            case TypeIr.Transform<?, ?> transform -> fixedByteSize(transform.parent());
            case TypeIr.Either<?, ?> either -> {
                long left = fixedByteSize(either.left());
                long right = fixedByteSize(either.right());
                yield (left == right && left >= 0) ? left : -1L;
            }
            case TypeIr.Template<?> template -> {
                long total = 0;
                for (FieldIr<?, ?> field : template.ir().fields()) {
                    long size = fixedByteSize(field.type());
                    if (size < 0) yield -1L;
                    total += size;
                }
                yield total;
            }
            default -> -1L;
        };
    }

    private static @Nullable PrimitiveKind constantPrimitive(Object value) {
        if (value instanceof Boolean) return PrimitiveKind.BOOLEAN;
        if (value instanceof Byte) return PrimitiveKind.BYTE;
        if (value instanceof Short) return PrimitiveKind.SHORT;
        if (value instanceof Integer) return PrimitiveKind.INT;
        if (value instanceof Long) return PrimitiveKind.LONG;
        if (value instanceof Float) return PrimitiveKind.FLOAT;
        if (value instanceof Double) return PrimitiveKind.DOUBLE;
        return null;
    }

    private static @Nullable PrimitiveKind runPrimitive(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.Primitive<?> primitive -> primitive.kind();
            case TypeIr.Transform<?, ?> transform -> runPrimitive(transform.parent());
            default -> null;
        };
    }

    private static TypeIr<?> typeIr(NetworkBuffer.Type<?> type) {
        return typeIr(type, new IdentityHashMap<>());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TypeIr<?> typeIr(NetworkBuffer.Type<?> type, IdentityHashMap<NetworkBuffer.Type<?>, Boolean> visiting) {
        if (visiting.put(type, Boolean.TRUE) != null) {
            return new TypeIr.External(type);
        }

        final TypeIr<?> result;
        try {
            result = switch (type) {
                case NetworkIrBacked<?> backed -> new TypeIr.Template(backed.ir());
                case NetworkBufferTypeImpl.UnitType _ -> new TypeIr.Constant<>(Unit.INSTANCE);
                case NetworkBufferTypeImpl.BooleanType _ -> new TypeIr.Primitive<>(PrimitiveKind.BOOLEAN);
                case NetworkBufferTypeImpl.ByteType _ -> new TypeIr.Primitive<>(PrimitiveKind.BYTE);
                case NetworkBufferTypeImpl.UnsignedByteType _ -> new TypeIr.Primitive<>(PrimitiveKind.UNSIGNED_BYTE);
                case NetworkBufferTypeImpl.ShortType _ -> new TypeIr.Primitive<>(PrimitiveKind.SHORT);
                case NetworkBufferTypeImpl.UnsignedShortType _ -> new TypeIr.Primitive<>(PrimitiveKind.UNSIGNED_SHORT);
                case NetworkBufferTypeImpl.IntType _ -> new TypeIr.Primitive<>(PrimitiveKind.INT);
                case NetworkBufferTypeImpl.UnsignedIntType _ -> new TypeIr.Primitive<>(PrimitiveKind.UNSIGNED_INT);
                case NetworkBufferTypeImpl.LongType _ -> new TypeIr.Primitive<>(PrimitiveKind.LONG);
                case NetworkBufferTypeImpl.FloatType _ -> new TypeIr.Primitive<>(PrimitiveKind.FLOAT);
                case NetworkBufferTypeImpl.DoubleType _ -> new TypeIr.Primitive<>(PrimitiveKind.DOUBLE);
                case NetworkBufferTypeImpl.VarIntType _ -> new TypeIr.VarInt();
                case NetworkBufferTypeImpl.VarLongType _ -> new TypeIr.VarLong();
                case NetworkBufferTypeImpl.StringType _ -> new TypeIr.StringUtf8(Integer.MAX_VALUE);
                case NetworkBufferTypeImpl.ByteArrayType _ -> new TypeIr.ByteArray(Integer.MAX_VALUE);
                case NetworkBufferTypeImpl.RawBytesType raw -> raw.length() >= 0 ? new TypeIr.FixedBytes(raw.length()) : new TypeIr.External(type);
                case NetworkBufferTypeImpl.OptionalType<?> optional -> new TypeIr.Optional(typeIr(optional.parent(), visiting));
                case NetworkBufferTypeImpl.EitherType<?, ?> either -> new TypeIr.Either(typeIr(either.left(), visiting), typeIr(either.right(), visiting));
                case NetworkBufferTypeImpl.TransformType<?, ?> transform ->
                        new TypeIr.Transform(typeIr(transform.parent(), visiting), transform.to(), transform.from());
                case NetworkBufferTypeImpl.ListType<?> list ->
                        new TypeIr.ListType(list, typeIr(list.parent(), visiting), list.maxSize(), LIST_FACTORY);
                case NetworkBufferTypeImpl.MapType<?, ?> map ->
                        new TypeIr.MapType(map, typeIr(map.parent(), visiting), typeIr(map.valueType(), visiting), map.maxSize(), MAP_FACTORY);
                default -> new TypeIr.External(type);
            };
        } finally {
            visiting.remove(type);
        }
        return result;
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

    public static final CollectionFactory<Object, List<Object>> LIST_FACTORY = new CollectionFactory<>() {
        @Override
        public Object create(int size) {
            return new ArrayList<>(size);
        }

        @Override
        public void add(Object collection, Object value) {
            ((List<Object>) collection).add(value);
        }

        @Override
        public List<Object> finish(Object collection) {
            return List.copyOf((List<Object>) collection);
        }
    };

    public static final MapFactory<Object, Object, Map<Object, Object>> MAP_FACTORY = new MapFactory<>() {
        @Override
        public Object create(int size) {
            return new LinkedHashMap<>(size);
        }

        @Override
        public void put(Object map, Object key, Object value) {
            ((Map<Object, Object>) map).put(key, value);
        }

        @Override
        public Map<Object, Object> finish(Object map) {
            return Map.copyOf((Map<Object, Object>) map);
        }
    };

    private static Local referenceLocal(String name) {
        return new Local(name, new LocalType.Reference(Object.class));
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
}
