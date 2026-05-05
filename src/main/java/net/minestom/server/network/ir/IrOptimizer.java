package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBufferTypeImpl;

import java.util.ArrayList;
import java.util.List;

public final class IrOptimizer {
    private IrOptimizer() {}

    public static ProgramIr optimize(ProgramIr program) {
        List<Op> current = program.ops();
        while (true) {
            List<Op> optimized = optimizeOps(current);
            if (optimized.equals(current)) break;
            current = optimized;
        }
        return new ProgramIr(current, program.initialSource());
    }

    private static List<Op> optimizeOps(List<Op> ops) {
        List<Op> result = new ArrayList<>();
        for (int i = 0; i < ops.size(); i++) {
            Op op = optimizeOp(ops.get(i));

            // Folding
            switch (op) {
                case Op.If branch when branch.condition() instanceof Value.Const(Object val) && val instanceof Boolean b -> {
                    result.addAll(b ? branch.thenOps() : branch.elseOps());
                    continue;
                }
                case Op.Check check when check.condition() instanceof Value.Const(Object val) && Boolean.TRUE.equals(val) -> {
                    continue;
                }
                case Op.ForIndex loop when loop.start() instanceof Value.Const(Object sVal) && sVal instanceof Number s &&
                        loop.end() instanceof Value.Const(Object eVal) && eVal instanceof Number e &&
                        s.longValue() >= e.longValue() -> {
                    continue;
                }
                default -> {}
            }

            // Try to merge into a write run
            if (isWriteRunCompatible(op)) {
                List<Op> runOps = new ArrayList<>();
                runOps.add(op);
                while (i + 1 < ops.size() && isWriteRunCompatible(ops.get(i + 1))) {
                    i++;
                    runOps.add(optimizeOp(ops.get(i)));
                }
                result.addAll(mergeWriteRunOps(runOps));
                continue;
            }

            // Try to merge into a read run
            if (isReadRunCompatible(op)) {
                List<Op> runOps = new ArrayList<>();
                runOps.add(op);
                while (i + 1 < ops.size() && isReadRunCompatible(ops.get(i + 1))) {
                    i++;
                    runOps.add(optimizeOp(ops.get(i)));
                }
                result.addAll(mergeReadRunOps(runOps));
                continue;
            }

            result.add(op);
        }
        return result;
    }

    private static Op optimizeOp(Op op) {
        return switch (op) {
            case Op.If branch -> new Op.If(optimizeValue(branch.condition()), optimizeOps(branch.thenOps()), optimizeOps(branch.elseOps()));
            case Op.ForEach loop -> new Op.ForEach(optimizeValue(loop.source()), loop.element(), optimizeOps(loop.body()));
            case Op.ForIndex loop -> new Op.ForIndex(loop.index(), optimizeValue(loop.start()), optimizeValue(loop.end()), optimizeOps(loop.body()));
            case Op.Apply apply -> apply;
            case Op.Cast cast -> cast;
            case Op.Unbox unbox -> unbox;
            case Op.Box box -> box;
            case Op.StringToBytes s2b -> s2b;
            case Op.BytesToString b2s -> b2s;
            case Op.EitherLeft left -> left;
            case Op.EitherRight right -> right;
            case Op.Store store -> new Op.Store(optimizeValue(store.value()), store.out());
            case Op.Check check -> new Op.Check(optimizeValue(check.condition()), check.message());
            case Op.WriteExternal write -> new Op.WriteExternal(write.type(), optimizeValue(write.value()));
            case Op.ReadExternal read -> read;
            case Op.WritePrimitive write -> new Op.WritePrimitive(write.kind(), optimizeValue(write.value()), write.address());
            case Op.ReadPrimitive read -> new Op.ReadPrimitive(read.kind(), read.address(), read.out());
            case Op.WriteVarInt write -> new Op.WriteVarInt(optimizeValue(write.value()), write.address());
            case Op.ReadVarInt read -> read;
            case Op.WriteVarLong write -> new Op.WriteVarLong(optimizeValue(write.value()), write.address());
            case Op.ReadVarLong read -> read;
            case Op.WriteFixedBytes write -> new Op.WriteFixedBytes(optimizeValue(write.value()), write.address());
            case Op.ReadFixedBytes read -> new Op.ReadFixedBytes(optimizeValue(read.length()), read.address(), read.out());
            case Op.WriteRun(RunIr run) -> new Op.WriteRun(optimizeRun(run));
            case Op.ReadRun(RunIr run) -> new Op.ReadRun(optimizeRun(run));
            case Op.ElementAt elementAt -> new Op.ElementAt(optimizeValue(elementAt.source()), optimizeValue(elementAt.index()), elementAt.out());
            case Op.MapEntrySet mapEntrySet -> new Op.MapEntrySet(optimizeValue(mapEntrySet.map()), mapEntrySet.out());
            case Op.MapEntryKey mapEntryKey -> mapEntryKey;
            case Op.MapEntryValue mapEntryValue -> mapEntryValue;
            case Op.ResultElementSet resultElementSet -> new Op.ResultElementSet(optimizeValue(resultElementSet.result()), optimizeValue(resultElementSet.index()), optimizeValue(resultElementSet.value()));
            case Op.ArrayCreate arrayCreate -> new Op.ArrayCreate(optimizeValue(arrayCreate.size()), arrayCreate.out());
            case Op.ArraySet arraySet -> new Op.ArraySet(arraySet.array(), optimizeValue(arraySet.index()), optimizeValue(arraySet.value()));
            case Op.ListFinish listFinish -> listFinish;
            case Op.MapFinish mapFinish -> new Op.MapFinish(mapFinish.keys(), mapFinish.values(), optimizeValue(mapFinish.size()), mapFinish.out());
            case Op.Construct construct -> new Op.Construct(construct.factory(), construct.args().stream().map(IrOptimizer::optimizeValue).toList(), construct.out());
            case Op.Return ret -> new Op.Return(optimizeValue(ret.value()));
            case Op.ReserveWrite reserve -> new Op.ReserveWrite(optimizeValue(reserve.size()), reserve.addressOut());
            case Op.ReserveRead reserve -> new Op.ReserveRead(optimizeValue(reserve.size()), reserve.addressOut());
            case Op.AdvanceWriteIndex advance -> new Op.AdvanceWriteIndex(optimizeValue(advance.amount()));
            case Op.AdvanceReadIndex advance -> new Op.AdvanceReadIndex(optimizeValue(advance.amount()));
        };
    }

    private static RunIr optimizeRun(RunIr run) {
        return new RunIr(optimizeValue(run.size()), run.items().stream().map(IrOptimizer::optimizeRunItem).toList());
    }

    private static RunItem optimizeRunItem(RunItem item) {
        return switch (item) {
            case RunItem.Put put -> new RunItem.Put(put.kind(), optimizeValue(put.offset()), optimizeValue(put.value()));
            case RunItem.Get get -> new RunItem.Get(get.kind(), optimizeValue(get.offset()), get.out());
            case RunItem.PutVarInt put -> new RunItem.PutVarInt(optimizeValue(put.offset()), optimizeValue(put.value()), optimizeValue(put.encodedSize()));
            case RunItem.PutVarLong put -> new RunItem.PutVarLong(optimizeValue(put.offset()), optimizeValue(put.value()), optimizeValue(put.encodedSize()));
            case RunItem.PutBytes put -> new RunItem.PutBytes(optimizeValue(put.offset()), optimizeValue(put.byteArray()), optimizeValue(put.length()));
            case RunItem.GetBytes get -> new RunItem.GetBytes(optimizeValue(get.offset()), get.byteArray(), optimizeValue(get.length()));
            case RunItem.ForIndex loop -> new RunItem.ForIndex(loop.index(), optimizeValue(loop.start()), optimizeValue(loop.end()), loop.body().stream().map(IrOptimizer::optimizeRunStep).toList());
        };
    }

    private static RunStep optimizeRunStep(RunStep step) {
        return switch (step) {
            case RunStep.Put put -> new RunStep.Put(put.kind(), optimizeValue(put.offset()), optimizeValue(put.value()));
            case RunStep.Get get -> new RunStep.Get(get.kind(), optimizeValue(get.offset()), get.out());
            case RunStep.PutVarInt put -> new RunStep.PutVarInt(optimizeValue(put.offset()), optimizeValue(put.value()), optimizeValue(put.encodedSize()));
            case RunStep.PutVarLong put -> new RunStep.PutVarLong(optimizeValue(put.offset()), optimizeValue(put.value()), optimizeValue(put.encodedSize()));
            case RunStep.PutBytes put -> new RunStep.PutBytes(optimizeValue(put.offset()), optimizeValue(put.byteArray()), optimizeValue(put.length()));
            case RunStep.GetBytes get -> new RunStep.GetBytes(optimizeValue(get.offset()), get.byteArray(), optimizeValue(get.length()));
            case RunStep.ElementAt elementAt -> new RunStep.ElementAt(optimizeValue(elementAt.source()), optimizeValue(elementAt.index()), elementAt.out());
            case RunStep.Apply apply -> apply;
            case RunStep.Cast cast -> cast;
            case RunStep.Unbox unbox -> unbox;
            case RunStep.Box box -> box;
            case RunStep.ArraySet arraySet -> new RunStep.ArraySet(arraySet.array(), optimizeValue(arraySet.index()), optimizeValue(arraySet.value()));
            case RunStep.ResultElementSet resultElementSet -> new RunStep.ResultElementSet(optimizeValue(resultElementSet.result()), optimizeValue(resultElementSet.index()), optimizeValue(resultElementSet.value()));
            case RunStep.Construct construct -> new RunStep.Construct(construct.factory(), construct.args().stream().map(IrOptimizer::optimizeValue).toList(), construct.out());
        };
    }

    private static Value optimizeValue(Value value) {
        return switch (value) {
            case Value.IsNull isNull -> {
                Value optimized = optimizeValue(isNull.value());
                if (optimized instanceof Value.Const(Object value1)) yield new Value.Const(value1 == null);
                yield new Value.IsNull(optimized);
            }
            case Value.IsNotNull isNotNull -> {
                Value optimized = optimizeValue(isNotNull.value());
                if (optimized instanceof Value.Const(Object value1)) yield new Value.Const(value1 != null);
                yield new Value.IsNotNull(optimized);
            }
            case Value.Not not -> {
                Value optimized = optimizeValue(not.value());
                if (optimized instanceof Value.Const(Object value1) && value1 instanceof Boolean b) yield new Value.Const(!b);
                yield new Value.Not(optimized);
            }
            case Value.IsLeft isLeft -> new Value.IsLeft(optimizeValue(isLeft.value()));
            case Value.EitherLeft left -> new Value.EitherLeft(optimizeValue(left.value()));
            case Value.EitherRight right -> new Value.EitherRight(optimizeValue(right.value()));
            case Value.Add add -> addValues(optimizeValue(add.left()), optimizeValue(add.right()));
            case Value.Mul mul -> mulValues(optimizeValue(mul.left()), optimizeValue(mul.right()));
            case Value.And and -> {
                Value left = optimizeValue(and.left());
                Value right = optimizeValue(and.right());
                if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
                    if (lv instanceof Boolean lb && rv instanceof Boolean rb) yield new Value.Const(lb && rb);
                }
                yield new Value.And(left, right);
            }
            case Value.Or or -> {
                Value left = optimizeValue(or.left());
                Value right = optimizeValue(or.right());
                if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
                    if (lv instanceof Boolean lb && rv instanceof Boolean rb) yield new Value.Const(lb || rb);
                }
                yield new Value.Or(left, right);
            }
            case Value.LessThanOrEqual cmp -> {
                Value left = optimizeValue(cmp.left());
                Value right = optimizeValue(cmp.right());
                if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
                    if (lv instanceof Number l && rv instanceof Number r) yield new Value.Const(l.longValue() <= r.longValue());
                }
                yield new Value.LessThanOrEqual(left, right);
            }
            case Value.GreaterThan cmp -> {
                Value left = optimizeValue(cmp.left());
                Value right = optimizeValue(cmp.right());
                if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
                    if (lv instanceof Number l && rv instanceof Number r) yield new Value.Const(l.longValue() > r.longValue());
                }
                yield new Value.GreaterThan(left, right);
            }
            case Value.ShiftLeft shift -> {
                Value optimized = optimizeValue(shift.value());
                if (optimized instanceof Value.Const(Object c) && c instanceof Number n) {
                    yield new Value.Const(n.longValue() << shift.amount());
                }
                yield new Value.ShiftLeft(optimized, shift.amount());
            }
            case Value.ShiftRightUnsigned shift -> {
                Value optimized = optimizeValue(shift.value());
                if (optimized instanceof Value.Const(Object c) && c instanceof Number n) {
                    yield new Value.Const(n.longValue() >>> shift.amount());
                }
                yield new Value.ShiftRightUnsigned(optimized, shift.amount());
            }
            case Value.BoolByte b -> new Value.BoolByte(optimizeValue(b.booleanValue()));
            case Value.UnsignedByte b -> new Value.UnsignedByte(optimizeValue(b.byteValue()));
            case Value.VarIntSize v -> {
                Value optimized = optimizeValue(v.intValue());
                if (optimized instanceof Value.Const(Object c) && c instanceof Number n) {
                    yield new Value.Const(NetworkBufferTypeImpl.varIntSize(n.intValue()));
                }
                yield new Value.VarIntSize(optimized);
            }
            case Value.VarLongSize v -> {
                Value optimized = optimizeValue(v.longValue());
                if (optimized instanceof Value.Const(Object c) && c instanceof Number n) {
                    yield new Value.Const(NetworkBufferTypeImpl.varLongSize(n.longValue()));
                }
                yield new Value.VarLongSize(optimized);
            }
            case Value.ArrayLength a -> new Value.ArrayLength(optimizeValue(a.array()));
            case Value.CollectionSize s -> new Value.CollectionSize(optimizeValue(s.collection()));
            case Value.MapSize s -> new Value.MapSize(optimizeValue(s.map()));
            case Value.StringUtf8Bytes s -> new Value.StringUtf8Bytes(optimizeValue(s.string()));
            default -> value;
        };
    }

    private static boolean isWriteRunCompatible(Op op) {
        return op instanceof Op.WritePrimitive || op instanceof Op.WriteVarInt || op instanceof Op.WriteVarLong || op instanceof Op.WriteFixedBytes || op instanceof Op.WriteRun;
    }

    private static boolean isReadRunCompatible(Op op) {
        // ReadVarInt/ReadVarLong are NOT compatible with ReadRun because they are barriers (unknown size)
        return op instanceof Op.ReadPrimitive || (op instanceof Op.ReadFixedBytes r && r.length() instanceof Value.Const) || op instanceof Op.ReadRun;
    }

    private static List<Op> mergeWriteRunOps(List<Op> ops) {
        List<Op> result = new ArrayList<>();
        List<RunItem> items = new ArrayList<>();
        Value totalSize = new Value.Const(0L);

        for (Op op : ops) {
            if (op instanceof Op.WriteRun(RunIr run)) {
                for (RunItem item : run.items()) {
                    items.add(shiftItem(item, totalSize));
                }
                totalSize = addValues(totalSize, run.size());
                continue;
            }

            RunItem item = null;
            Value itemSize = null;
            switch (op) {
                case Op.WritePrimitive p -> {
                    item = new RunItem.Put(p.kind().storeKind(), totalSize, p.value());
                    itemSize = new Value.Const((long) p.kind().storeKind().byteSize());
                }
                case Op.WriteVarInt v -> {
                    Value size = new Value.VarIntSize(v.value());
                    item = new RunItem.PutVarInt(totalSize, v.value(), size);
                    itemSize = size;
                }
                case Op.WriteVarLong v -> {
                    Value size = new Value.VarLongSize(v.value());
                    item = new RunItem.PutVarLong(totalSize, v.value(), size);
                    itemSize = size;
                }
                case Op.WriteFixedBytes f -> {
                    item = new RunItem.PutBytes(totalSize, f.value(), new Value.ArrayLength(f.value()));
                    itemSize = new Value.ArrayLength(f.value());
                }
                default -> {
                }
            }

            if (item != null) {
                items.add(item);
                totalSize = addValues(totalSize, itemSize);
            } else {
                if (!items.isEmpty()) {
                    result.add(new Op.WriteRun(new RunIr(totalSize, items)));
                    items = new ArrayList<>();
                    totalSize = new Value.Const(0L);
                }
                result.add(op);
            }
        }

        if (!items.isEmpty()) {
            result.add(new Op.WriteRun(new RunIr(totalSize, items)));
        }

        return result;
    }

    private static List<Op> mergeReadRunOps(List<Op> ops) {
        List<Op> result = new ArrayList<>();
        List<RunItem> items = new ArrayList<>();
        Value totalSize = new Value.Const(0L);

        for (Op op : ops) {
            if (op instanceof Op.ReadRun(RunIr run)) {
                for (RunItem item : run.items()) {
                    items.add(shiftItem(item, totalSize));
                }
                totalSize = addValues(totalSize, run.size());
                continue;
            }

            RunItem item = null;
            Value itemSize = null;
            if (op instanceof Op.ReadPrimitive p) {
                item = new RunItem.Get(p.kind().storeKind(), totalSize, p.out());
                itemSize = new Value.Const((long) p.kind().storeKind().byteSize());
            } else if (op instanceof Op.ReadFixedBytes f && f.length() instanceof Value.Const c) {
                item = new RunItem.GetBytes(totalSize, f.out(), f.length());
                itemSize = f.length();
            }

            if (item != null) {
                items.add(item);
                totalSize = addValues(totalSize, itemSize);
            } else {
                if (!items.isEmpty()) {
                    result.add(new Op.ReadRun(new RunIr(totalSize, items)));
                    items = new ArrayList<>();
                    totalSize = new Value.Const(0L);
                }
                result.add(op);
            }
        }

        if (!items.isEmpty()) {
            result.add(new Op.ReadRun(new RunIr(totalSize, items)));
        }

        return result;
    }

    private static Value addValues(Value left, Value right) {
        if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
            if (lv instanceof Number l && rv instanceof Number r) return new Value.Const(l.longValue() + r.longValue());
        }
        if (left instanceof Value.Const(Object lv) && lv instanceof Number n && n.longValue() == 0) return right;
        if (right instanceof Value.Const(Object rv) && rv instanceof Number n && n.longValue() == 0) return left;

        // Normalize: ensure left-leaning tree
        if (right instanceof Value.Add(Value left1, Value right1)) {
            return addValues(addValues(left, left1), right1);
        }
        return new Value.Add(left, right);
    }

    private static Value mulValues(Value left, Value right) {
        if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
            if (lv instanceof Number l && rv instanceof Number r) return new Value.Const(l.longValue() * r.longValue());
        }
        if (left instanceof Value.Const(Object lv) && lv instanceof Number n && n.longValue() == 1) return right;
        if (right instanceof Value.Const(Object rv) && rv instanceof Number n && n.longValue() == 1) return left;
        if (left instanceof Value.Const(Object lv) && lv instanceof Number n && n.longValue() == 0) return left;
        if (right instanceof Value.Const(Object rv) && rv instanceof Number n && n.longValue() == 0) return right;

        // Normalize: ensure left-leaning tree
        if (right instanceof Value.Mul(Value left1, Value right1)) {
            return mulValues(mulValues(left, left1), right1);
        }
        return new Value.Mul(left, right);
    }

    private static RunItem shiftItem(RunItem item, Value shift) {
        return switch (item) {
            case RunItem.Put put -> new RunItem.Put(put.kind(), addValues(shift, put.offset()), put.value());
            case RunItem.PutVarInt putVarInt ->
                    new RunItem.PutVarInt(addValues(shift, putVarInt.offset()), putVarInt.value(), putVarInt.encodedSize());
            case RunItem.PutVarLong putVarLong ->
                    new RunItem.PutVarLong(addValues(shift, putVarLong.offset()), putVarLong.value(), putVarLong.encodedSize());
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
            case RunStep.PutVarInt putVarInt ->
                    new RunStep.PutVarInt(addValues(shift, putVarInt.offset()), putVarInt.value(), putVarInt.encodedSize());
            case RunStep.PutVarLong putVarLong ->
                    new RunStep.PutVarLong(addValues(shift, putVarLong.offset()), putVarLong.value(), putVarLong.encodedSize());
            case RunStep.PutBytes putBytes ->
                    new RunStep.PutBytes(addValues(shift, putBytes.offset()), putBytes.byteArray(), putBytes.length());
            case RunStep.GetBytes getBytes ->
                    new RunStep.GetBytes(addValues(shift, getBytes.offset()), getBytes.byteArray(), getBytes.length());
            default -> step;
        };
    }
}
