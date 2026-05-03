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
        final IdentityHashMap<Function<?, ?>, Boolean> usedTransforms = new IdentityHashMap<>();
        collectApplyFunctions(ir.write(), usedTransforms);
        collectApplyFunctions(ir.read(), usedTransforms);
        collectIrMetadata("", ir, classData, fields, transforms, constructors, constructorIrs, factories, usedTransforms);

        // Add standalone transforms that were not found in TypeIr.Transform
        int standaloneIndex = 0;
        for (Function<?, ?> function : usedTransforms.keySet()) {
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
        final IdentityHashMap<NetworkBuffer.Type<?>, Boolean> usedExternalTypes = new IdentityHashMap<>();
        collectExternalTypes(ir.write(), usedExternalTypes);
        collectExternalTypes(ir.read(), usedExternalTypes);
        int extIndex = 0;
        for (NetworkBuffer.Type<?> type : usedExternalTypes.keySet()) {
            boolean alreadyAdded = false;
            for (IrFieldData field : fields) {
                if (field.ir().originalType() == type) {
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

    private static void collectExternalTypes(ProgramIr program, IdentityHashMap<NetworkBuffer.Type<?>, Boolean> types) {
        for (Op op : program.ops()) {
            collectExternalTypes(op, types);
        }
    }

    private static void collectExternalTypes(Op op, IdentityHashMap<NetworkBuffer.Type<?>, Boolean> types) {
        switch (op) {
            case Op.WriteExternal write -> types.put(write.type(), Boolean.TRUE);
            case Op.ReadExternal read -> types.put(read.type(), Boolean.TRUE);
            case Op.If ifOp -> {
                collectExternalTypes(new ProgramIr(ifOp.thenOps()), types);
                collectExternalTypes(new ProgramIr(ifOp.elseOps()), types);
            }
            case Op.ForEach forEach -> collectExternalTypes(new ProgramIr(forEach.body()), types);
            case Op.ForIndex forIndex -> collectExternalTypes(new ProgramIr(forIndex.body()), types);
            default -> {
            }
        }
    }

    private static void collectIrMetadata(String path, NetworkIr<?> ir, List<Object> classData,
                                          List<IrFieldData> allFields, List<TransformFieldData> allTransforms,
                                          Map<String, Integer> allConstructors, Map<String, ConstructorIr<?>> allConstructorIrs,
                                          List<FactoryFieldData> allFactories,
                                          IdentityHashMap<Function<?, ?>, Boolean> usedTransforms) {
        final String ctorName = ctorName(path);
        allConstructors.put(ctorName, addClassData(classData, ir.constructor().object()));
        allConstructorIrs.put(ctorName, ir.constructor());

        final List<? extends FieldIr<?, ?>> irFields = ir.fields();
        for (int i = 0; i < irFields.size(); i++) {
            final FieldIr<?, ?> field = irFields.get(i);
            final String fieldPath = childPath(path, i);
            allFields.add(new IrFieldData(field, fieldPath,
                    addClassData(classData, field.originalType()),
                    addClassData(classData, field.getter())));
            collectTypeMetadata(fieldPath, field.type(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
        }
    }

    private static void collectTypeMetadata(String path, TypeIr<?> type,
                                            IdentityHashMap<Function<?, ?>, Boolean> usedTransforms,
                                            List<Object> classData, List<IrFieldData> allFields,
                                            List<TransformFieldData> allTransforms,
                                            Map<String, Integer> allConstructors, Map<String, ConstructorIr<?>> allConstructorIrs,
                                            List<FactoryFieldData> allFactories) {
        switch (type) {
            case TypeIr.Template<?> template ->
                    collectIrMetadata(path, template.ir(), classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories, usedTransforms);
            case TypeIr.Transform<?, ?> transform -> {
                if (usedTransforms.containsKey(transform.from())) {
                    allTransforms.add(new TransformFieldData(transformFromName(path, 0), transform.from(), addClassData(classData, transform.from())));
                }
                if (usedTransforms.containsKey(transform.to())) {
                    allTransforms.add(new TransformFieldData(transformToName(path, 0), transform.to(), addClassData(classData, transform.to())));
                }
                collectTypeMetadata(path + "X", transform.parent(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            }
            case TypeIr.Optional<?> optional ->
                    collectTypeMetadata(path + "Opt", optional.parent(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            case TypeIr.ListType<?, ?> list -> {
                allFactories.add(new FactoryFieldData(factoryName(path), list.factory(), addClassData(classData, list.factory())));
                collectTypeMetadata(path + "E", list.element(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            }
            case TypeIr.MapType<?, ?, ?> map -> {
                allFactories.add(new FactoryFieldData(factoryName(path), map.factory(), addClassData(classData, map.factory())));
                collectTypeMetadata(path + "K", map.key(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
                collectTypeMetadata(path + "V", map.value(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            }
            default -> {
            }
        }
    }

    private static void collectApplyFunctions(ProgramIr program, IdentityHashMap<Function<?, ?>, Boolean> functions) {
        for (Op op : program.ops()) {
            collectApplyFunctions(op, functions);
        }
    }

    private static void collectApplyFunctions(Op op, IdentityHashMap<Function<?, ?>, Boolean> functions) {
        switch (op) {
            case Op.Apply apply -> functions.put(apply.function(), Boolean.TRUE);
            case Op.If branch -> {
                branch.thenOps().forEach(child -> collectApplyFunctions(child, functions));
                branch.elseOps().forEach(child -> collectApplyFunctions(child, functions));
            }
            case Op.ForEach loop -> loop.body().forEach(child -> collectApplyFunctions(child, functions));
            case Op.ForIndex loop -> loop.body().forEach(child -> collectApplyFunctions(child, functions));
            case Op.WriteRun writeRun -> collectApplyFunctions(writeRun.run(), functions);
            case Op.ReadRun readRun -> collectApplyFunctions(readRun.run(), functions);
            default -> {
            }
        }
    }

    private static void collectApplyFunctions(RunIr run, IdentityHashMap<Function<?, ?>, Boolean> functions) {
        for (RunItem item : run.items()) {
            if (item instanceof RunItem.ForIndex loop) {
                loop.body().forEach(step -> collectApplyFunctions(step, functions));
            }
        }
    }

    private static void collectApplyFunctions(RunStep step, IdentityHashMap<Function<?, ?>, Boolean> functions) {
        switch (step) {
            case RunStep.Apply apply -> functions.put(apply.function(), Boolean.TRUE);
            case RunStep.Construct construct -> {
                // Constructor is not a Function<?, ?>, so skip.
            }
            default -> {
            }
        }
    }

    private static ProgramIr writeProgram(FieldIr<?, ?>[] fields) {
        final List<Op> writeOps = new ArrayList<>();
        final Local source = referenceLocal("value");
        for (int i = 0; i < fields.length; i++) {
            lowerWrite(writeOps, fields[i].type(), fields[i], source, Integer.toString(i + 1), i, 0);
        }
        return new ProgramIr(mergeWriteRuns(writeOps));
    }

    private static void lowerWrite(List<Op> ops, TypeIr<?> type, @Nullable FieldIr<?, ?> field, Local source, String path, int fieldIndex, int depth) {
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
                lowerWrite(ops, subField.type(), subField, nested, path + "_" + (i + 1), i, depth + 1);
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
            final Local parentValue = referenceLocal("path" + path + "From");
            ops.add(new Op.Apply(transform.from(), raw, parentValue));
            lowerWrite(ops, transform.parent(), null, parentValue, path + "X", -1, depth + 1);
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
            ops.add(new Op.WriteRun(new RunIr(new Value.Const(1L),
                    List.of(new RunItem.Put(StoreKind.BOOLEAN, new Value.Const(0L), new Value.BoolByte(present))))));

            final List<Op> thenOps = new ArrayList<>();
            lowerWrite(thenOps, optional.parent(), null, raw, path + "Opt", -1, depth + 1);
            ops.add(new Op.If(present, thenOps, List.of()));
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

            ops.add(new Op.Check(new Value.LessThanOrEqual(sizeVal, new Value.Const(listType.maxLength())), "Collection too large"));

            final Local encodedSizeLocal = new Local("path" + path + "EncSize", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.Store(new Value.VarIntSize(sizeVal), encodedSizeLocal));
            final Value encodedSizeVal = new Value.LocalValue(encodedSizeLocal);

            ops.add(new Op.WriteRun(new RunIr(encodedSizeVal,
                    List.of(new RunItem.PutVarInt(new Value.Const(0L), sizeVal, encodedSizeVal)))));

            final Local index = new Local("path" + path + "Idx", new LocalType.Kind(TypeKind.INT));
            final Local element = referenceLocal("path" + path + "Elem");
            final List<Op> body = new ArrayList<>();
            body.add(new Op.ElementAt(new Value.LocalValue(raw), new Value.LocalValue(index), element));
            lowerWrite(body, listType.element(), null, element, path + "E", -1, depth + 1);

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

            ops.add(new Op.Check(new Value.LessThanOrEqual(sizeVal, new Value.Const(mapType.maxLength())), "Map too large"));

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
            lowerWrite(body, mapType.key(), null, key, path + "K", -1, depth + 1);
            lowerWrite(body, mapType.value(), null, value, path + "V", -1, depth + 1);

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
            final Local normalized = normalizeWritePrimitive(ops, type, raw, fieldIndex, depth);
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
            final Local normalized = normalizeWriteVarInt(ops, type, raw, fieldIndex, depth);
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
            final Local normalized = normalizeWriteVarLong(ops, type, raw, fieldIndex, depth);
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
            final Local bytes = materializeWriteReference(ops, type, TypeIr.FixedBytes.class, raw, fieldIndex, depth, byte[].class);
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

            final Value length = new Value.ArrayLength(new Value.LocalValue(bytes));
            ops.add(new Op.Check(new Value.LessThanOrEqual(length, new Value.Const((long) maxSize)), "Array too long"));
            final Value encodedSize = new Value.VarIntSize(length);
            ops.add(new Op.WriteRun(new RunIr(new Value.Add(encodedSize, length), List.of(
                    new RunItem.PutVarInt(new Value.Const(0L), length, encodedSize),
                    new RunItem.PutBytes(encodedSize, new Value.LocalValue(bytes), length)
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
            lowerWrite(ops, new TypeIr.ByteArray(maxSize), null, bytes, path + "Str", -1, depth + 1);
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
        final List<Op> merged = new ArrayList<>();
        Op.WriteRun current = null;
        for (Op op : ops) {
            if (op instanceof Op.WriteRun next) {
                if (current == null) {
                    current = next;
                } else {
                    current = mergeWriteRun(current, next);
                }
            } else {
                if (current != null) {
                    merged.add(current);
                    current = null;
                }
                merged.add(op);
            }
        }
        if (current != null) merged.add(current);
        return merged;
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
            args.add(lowerRead(readOps, fields[i].type(), Integer.toString(i + 1), i, 0));
        }
        final Local result = referenceLocal("result");
        readOps.add(new Op.Construct(constructor, "", args, result));
        readOps.add(new Op.Return(new Value.LocalValue(result)));
        return new ProgramIr(mergeReadRuns(readOps));
    }

    private static Value lowerRead(List<Op> ops, TypeIr<?> type, String path, int fieldIndex, int depth) {
        if (type instanceof TypeIr.Template<?> template) {
            final List<Value> args = new ArrayList<>();
            for (int i = 0; i < template.ir().fields().size(); i++) {
                final FieldIr<?, ?> subField = template.ir().fields().get(i);
                args.add(lowerRead(ops, subField.type(), path + "_" + (i + 1), i, depth + 1));
            }
            final Local result = referenceLocal("path" + path + "Result");
            ops.add(new Op.Construct(template.ir().constructor(), path, args, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.Transform<?, ?> transform) {
            final Value parentValue = lowerRead(ops, transform.parent(), path + "X", -1, depth + 1);
            final Local parentLocal = ensureLocal(ops, parentValue, path + "XL");
            final Local result = referenceLocal("path" + path + "To");
            ops.add(new Op.Apply(transform.to(), parentLocal, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.Optional<?> optional) {
            final Local present = new Local("path" + path + "Present", new LocalType.Kind(TypeKind.BOOLEAN));
            ops.add(new Op.ReadRun(new RunIr(new Value.Const(1L),
                    List.of(new RunItem.Get(StoreKind.BOOLEAN, new Value.Const(0L), present)))));

            final Local result = referenceLocal("path" + path + "Result");
            final List<Op> thenOps = new ArrayList<>();
            final Value parentValue = lowerRead(thenOps, optional.parent(), path + "Opt", -1, depth + 1);
            thenOps.add(new Op.Store(parentValue, result));

            final List<Op> elseOps = new ArrayList<>();
            elseOps.add(new Op.Store(new Value.Const(null), result));

            ops.add(new Op.If(new Value.LocalValue(present), thenOps, elseOps));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.ListType<?, ?> listType) {
            final Local size = new Local("path" + path + "Size", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(size));
            ops.add(new Op.Check(new Value.LessThanOrEqual(new Value.LocalValue(size), new Value.Const((long) listType.maxLength())), "Collection too large"));

            final Local collection = referenceLocal("path" + path + "Coll");
            ops.add(new Op.CollectionCreate(listType.factory(), path, new Value.LocalValue(size), collection));

            final Local index = new Local("path" + path + "Idx", new LocalType.Kind(TypeKind.INT));
            final List<Op> body = new ArrayList<>();
            final Value element = lowerRead(body, listType.element(), path + "E", -1, depth + 1);
            body.add(new Op.CollectionAdd(listType.factory(), path, collection, element));

            ops.add(new Op.ForIndex(index, new Value.Const(0), new Value.LocalValue(size), body));

            final Local result = referenceLocal("path" + path + "Res");
            ops.add(new Op.CollectionFinish(listType.factory(), path, collection, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.MapType<?, ?, ?> mapType) {
            final Local size = new Local("path" + path + "Size", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(size));
            ops.add(new Op.Check(new Value.LessThanOrEqual(new Value.LocalValue(size), new Value.Const((long) mapType.maxLength())), "Map too large"));

            final Local map = referenceLocal("path" + path + "Map");
            ops.add(new Op.MapCreate(mapType.factory(), path, new Value.LocalValue(size), map));

            final Local index = new Local("path" + path + "Idx", new LocalType.Kind(TypeKind.INT));
            final List<Op> body = new ArrayList<>();
            final Value key = lowerRead(body, mapType.key(), path + "K", -1, depth + 1);
            final Value value = lowerRead(body, mapType.value(), path + "V", -1, depth + 1);
            body.add(new Op.MapPut(mapType.factory(), path, map, key, value));

            ops.add(new Op.ForIndex(index, new Value.Const(0), new Value.LocalValue(size), body));

            final Local result = referenceLocal("path" + path + "Res");
            ops.add(new Op.MapFinish(mapType.factory(), path, map, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.ByteArray(int maxSize)) {
            final Local length = new Local("path" + path + "Length", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(length));
            ops.add(new Op.Check(new Value.LessThanOrEqual(new Value.LocalValue(length), new Value.Const((long) maxSize)), "Array too long"));

            final Local bytes = new Local("path" + path + "Bytes", new LocalType.Reference(byte[].class));
            ops.add(new Op.ReadRun(new RunIr(new Value.LocalValue(length),
                    List.of(new RunItem.GetBytes(new Value.Const(0L), bytes, new Value.LocalValue(length))))));
            return new Value.LocalValue(bytes);
        }

        if (type instanceof TypeIr.StringUtf8(int maxSize)) {
            final Value bytes = lowerRead(ops, new TypeIr.ByteArray(maxSize), path + "Str", -1, depth + 1);
            final Local bytesLocal = ensureLocal(ops, bytes, path + "StrL");
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
            return new Value.LocalValue(materializeReadPrimitive(ops, type, normalized, fieldIndex, depth));
        }

        if (isVarInt(type)) {
            final Local normalized = new Local("path" + path + "Value", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(normalized));
            return new Value.LocalValue(materializeReadVarInt(ops, type, normalized, fieldIndex, depth));
        }

        if (isVarLong(type)) {
            final Local normalized = new Local("path" + path + "Value", new LocalType.Kind(TypeKind.LONG));
            ops.add(new Op.ReadVarLong(normalized));
            return new Value.LocalValue(materializeReadVarLong(ops, type, normalized, fieldIndex, depth));
        }

        if (fixedBytesLength(type) >= 0) {
            final int length = fixedBytesLength(type);
            final Local bytes = new Local("path" + path + "Bytes", new LocalType.Reference(byte[].class));
            ops.add(new Op.ReadRun(new RunIr(
                    new Value.Const((long) length),
                    List.of(new RunItem.GetBytes(new Value.Const(0L), bytes, new Value.Const(length)))
            )));
            return new Value.LocalValue(materializeReadReference(ops, type, TypeIr.FixedBytes.class, bytes, fieldIndex, depth));
        }

        final Local readLocal = referenceLocal("path" + path);
        ops.add(new Op.ReadExternal(((TypeIr.External<?>) type).type(), readLocal));
        return new Value.LocalValue(readLocal);
    }

    private static List<Op> mergeReadRuns(List<Op> ops) {
        final List<Op> merged = new ArrayList<>();
        Op.ReadRun current = null;
        for (Op op : ops) {
            if (op instanceof Op.ReadRun next) {
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
                merged.add(op);
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

    private static Local normalizeWritePrimitive(List<Op> ops, TypeIr<?> type, Local in, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWritePrimitive(ops, transform.parent(), out, fieldIndex, depth + 1);
            }
            case TypeIr.Primitive<?> primitive -> {
                final PrimitiveKind kind = primitive.kind();
                final Local cast = referenceLocal("field" + fieldIndex + "Cast" + depth);
                final Local normalized = new Local("field" + fieldIndex + "Value" + depth, new LocalType.Kind(kind.localKind()));
                ops.add(new Op.Cast(in, kind.wrapperClass(), cast));
                ops.add(new Op.Unbox(kind, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not primitive run-compatible: " + type);
        };
    }

    private static Local materializeReadPrimitive(List<Op> ops, TypeIr<?> type, Local normalized, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadPrimitive(ops, transform.parent(), normalized, fieldIndex, depth + 1);
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.Primitive<?> primitive -> {
                final PrimitiveKind kind = primitive.kind();
                final Local boxed = referenceLocal("field" + fieldIndex + "Boxed" + depth);
                ops.add(new Op.Box(kind, normalized, boxed));
                yield boxed;
            }
            default -> throw new IllegalArgumentException("Type is not primitive run-compatible: " + type);
        };
    }

    private static Local normalizeWriteVarInt(List<Op> ops, TypeIr<?> type, Local in, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWriteVarInt(ops, transform.parent(), out, fieldIndex, depth + 1);
            }
            case TypeIr.VarInt _ -> {
                final Local cast = referenceLocal("field" + fieldIndex + "Cast" + depth);
                final Local normalized = new Local("field" + fieldIndex + "Value" + depth, new LocalType.Kind(TypeKind.INT));
                ops.add(new Op.Cast(in, Integer.class, cast));
                ops.add(new Op.Unbox(PrimitiveKind.INT, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not VarInt-compatible: " + type);
        };
    }

    private static Local materializeReadVarInt(List<Op> ops, TypeIr<?> type, Local normalized, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadVarInt(ops, transform.parent(), normalized, fieldIndex, depth + 1);
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.VarInt _ -> {
                final Local boxed = referenceLocal("field" + fieldIndex + "Boxed" + depth);
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

    private static Local normalizeWriteVarLong(List<Op> ops, TypeIr<?> type, Local in, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWriteVarLong(ops, transform.parent(), out, fieldIndex, depth + 1);
            }
            case TypeIr.VarLong _ -> {
                final Local cast = referenceLocal("field" + fieldIndex + "Cast" + depth);
                final Local normalized = new Local("field" + fieldIndex + "Value" + depth, new LocalType.Kind(TypeKind.LONG));
                ops.add(new Op.Cast(in, Long.class, cast));
                ops.add(new Op.Unbox(PrimitiveKind.LONG, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not VarLong-compatible: " + type);
        };
    }

    private static Local materializeReadVarLong(List<Op> ops, TypeIr<?> type, Local normalized, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadVarLong(ops, transform.parent(), normalized, fieldIndex, depth + 1);
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.VarLong _ -> {
                final Local boxed = referenceLocal("field" + fieldIndex + "Boxed" + depth);
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
                                                   int fieldIndex, int depth, Class<?> targetClass) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield materializeWriteReference(ops, transform.parent(), targetType, out, fieldIndex, depth + 1, targetClass);
            }
            default -> {
                if (!targetType.isInstance(type)) {
                    throw new IllegalArgumentException("Type is not " + targetType.getSimpleName() + "-compatible: " + type);
                }
                final Local cast = new Local("field" + fieldIndex + "Cast" + depth, new LocalType.Reference(targetClass));
                ops.add(new Op.Cast(in, targetClass, cast));
                yield cast;
            }
        };
    }

    private static Local materializeReadReference(List<Op> ops, TypeIr<?> type, Class<?> targetType, Local in,
                                                  int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadReference(ops, transform.parent(), targetType, in, fieldIndex, depth + 1);
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
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
            default -> -1;
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
