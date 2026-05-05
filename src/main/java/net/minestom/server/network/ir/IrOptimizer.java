package net.minestom.server.network.ir;

import java.util.ArrayList;
import java.util.List;

public final class IrOptimizer {
    private IrOptimizer() {}

    public static ProgramIr optimize(ProgramIr program) {
        return new ProgramIr(optimizeOps(program.ops()), program.initialSource());
    }

    private static List<Op> optimizeOps(List<Op> ops) {
        List<Op> result = new ArrayList<>();
        for (int i = 0; i < ops.size(); i++) {
            Op op = ops.get(i);
            op = switch (op) {
                case Op.If ifOp -> new Op.If(ifOp.condition(), optimizeOps(ifOp.thenOps()), optimizeOps(ifOp.elseOps()));
                case Op.ForEach forEach -> new Op.ForEach(forEach.source(), forEach.element(), optimizeOps(forEach.body()));
                case Op.ForIndex forIndex -> new Op.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), optimizeOps(forIndex.body()));
                default -> op;
            };

            // Try to merge into a write run
            if (isWriteRunCompatible(op)) {
                List<Op> runOps = new ArrayList<>();
                runOps.add(op);
                while (i + 1 < ops.size() && isWriteRunCompatible(ops.get(i + 1))) {
                    i++;
                    runOps.add(ops.get(i));
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
                    runOps.add(ops.get(i));
                }
                result.addAll(mergeReadRunOps(runOps));
                continue;
            }

            result.add(op);
        }
        return result;
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
                case Op.WriteFixedBytes f -> {
                    item = new RunItem.PutBytes(totalSize, f.value(), new Value.ArrayLength(f.value()));
                    itemSize = new Value.ArrayLength(f.value());
                }
                case null, default -> {
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
