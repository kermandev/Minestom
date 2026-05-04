package net.minestom.server.network.ir;

import java.lang.classfile.TypeKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class IrOptimizer {
    private IrOptimizer() {}

    public static ProgramIr optimize(ProgramIr program) {
        List<Op> ops = program.ops();
        boolean changed;
        do {
            changed = false;

            Result r = hoist(ops);
            ops = r.ops();
            changed |= r.changed();

            r = mergeReservations(ops);
            ops = r.ops();
            changed |= r.changed();

            r = mergeRuns(ops);
            ops = r.ops();
            changed |= r.changed();

            r = simplifyValues(ops);
            ops = r.ops();
            changed |= r.changed();

        } while (changed);
        return new ProgramIr(ops, program.initialSource());
    }

    private record Result(List<Op> ops, boolean changed) {}

    private static Result hoist(List<Op> ops) {
        List<Op> result = new ArrayList<>();
        boolean changed = false;
        for (Op op : ops) {
            Op hoisted = switch (op) {
                case Op.If ifOp -> {
                    Result rThen = hoist(ifOp.thenOps());
                    Result rElse = hoist(ifOp.elseOps());
                    if (rThen.changed() || rElse.changed()) {
                        changed = true;
                        yield new Op.If(ifOp.condition(), rThen.ops(), rElse.ops());
                    }
                    yield ifOp;
                }
                case Op.ForEach forEach -> {
                    Result rBody = hoist(forEach.body());
                    if (rBody.changed()) {
                        changed = true;
                        yield new Op.ForEach(forEach.source(), forEach.element(), rBody.ops());
                    }
                    yield forEach;
                }
                case Op.ForIndex forIndex -> {
                    Result rBody = hoist(forIndex.body());
                    if (rBody.changed()) {
                        changed = true;
                        yield new Op.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), rBody.ops());
                    }
                    yield forIndex;
                }
                default -> op;
            };

            if (hoisted instanceof Op.WritePrimitive p && p.address() == null) {
                changed = true;
                Local address = new Local(new LocalType.Kind(TypeKind.LONG));
                result.add(new Op.ReserveWrite(new Value.Const((long) p.kind().storeKind().byteSize()), address));
                result.add(new Op.WritePrimitive(p.kind(), p.value(), new Value.LocalValue(address)));
            } else if (hoisted instanceof Op.WriteVarInt v && v.address() == null) {
                changed = true;
                Local address = new Local(new LocalType.Kind(TypeKind.LONG));
                Value size = new Value.VarIntSize(v.value());
                result.add(new Op.ReserveWrite(size, address));
                result.add(new Op.WriteVarInt(v.value(), new Value.LocalValue(address)));
            } else if (hoisted instanceof Op.WriteVarLong v && v.address() == null) {
                changed = true;
                Local address = new Local(new LocalType.Kind(TypeKind.LONG));
                Value size = new Value.VarLongSize(v.value());
                result.add(new Op.ReserveWrite(size, address));
                result.add(new Op.WriteVarLong(v.value(), new Value.LocalValue(address)));
            } else if (hoisted instanceof Op.WriteFixedBytes f && f.address() == null) {
                changed = true;
                Local address = new Local(new LocalType.Kind(TypeKind.LONG));
                Value size = new Value.ArrayLength(f.value());
                result.add(new Op.ReserveWrite(size, address));
                result.add(new Op.WriteFixedBytes(f.value(), new Value.LocalValue(address)));
            } else if (hoisted instanceof Op.ReadPrimitive p && p.address() == null) {
                changed = true;
                Local address = new Local(new LocalType.Kind(TypeKind.LONG));
                result.add(new Op.ReserveRead(new Value.Const((long) p.kind().storeKind().byteSize()), address));
                result.add(new Op.ReadPrimitive(p.kind(), new Value.LocalValue(address), p.out()));
            } else if (hoisted instanceof Op.ReadVarInt v && v.address() == null) {
                changed = true;
                Local address = new Local(new LocalType.Kind(TypeKind.LONG));
                result.add(new Op.ReserveRead(new Value.Const(5L), address));
                result.add(new Op.ReadVarInt(new Value.LocalValue(address), v.out()));
            } else if (hoisted instanceof Op.ReadVarLong v && v.address() == null) {
                changed = true;
                Local address = new Local(new LocalType.Kind(TypeKind.LONG));
                result.add(new Op.ReserveRead(new Value.Const(10L), address));
                result.add(new Op.ReadVarLong(new Value.LocalValue(address), v.out()));
            } else if (hoisted instanceof Op.ReadFixedBytes f && f.address() == null) {
                changed = true;
                Local address = new Local(new LocalType.Kind(TypeKind.LONG));
                result.add(new Op.ReserveRead(f.length(), address));
                result.add(new Op.ReadFixedBytes(f.length(), new Value.LocalValue(address), f.out()));
            } else if (hoisted instanceof Op.WriteRun r && r.address() == null) {
                changed = true;
                Local address = new Local(new LocalType.Kind(TypeKind.LONG));
                result.add(new Op.ReserveWrite(r.run().size(), address));
                result.add(new Op.WriteRun(new Value.LocalValue(address), r.run()));
            } else if (hoisted instanceof Op.ReadRun r && r.address() == null) {
                changed = true;
                Local address = new Local(new LocalType.Kind(TypeKind.LONG));
                result.add(new Op.ReserveRead(r.run().size(), address));
                result.add(new Op.ReadRun(new Value.LocalValue(address), r.run()));
            } else {
                result.add(hoisted);
            }
        }
        return new Result(result, changed);
    }

    private static Result mergeReservations(List<Op> ops) {
        List<Op> result = new ArrayList<>();
        boolean changed = false;
        Map<Local, Value> substitutions = new HashMap<>();

        for (int i = 0; i < ops.size(); i++) {
            Op op = ops.get(i);
            Op substituted = substitute(op, substitutions);
            if (substituted != op) changed = true;
            op = substituted;

            if (op instanceof Op.ReserveWrite r1 && i + 1 < ops.size()) {
                Op next = ops.get(i + 1);
                next = substitute(next, substitutions);
                if (next instanceof Op.ReserveWrite r2) {
                    changed = true;
                    Value newSize = addValues(r1.size(), r2.size());
                    result.add(new Op.ReserveWrite(newSize, r1.addressOut()));
                    substitutions.put(r2.addressOut(), addValues(new Value.LocalValue(r1.addressOut()), r1.size()));
                    continue;
                }
            }

            if (op instanceof Op.ReserveRead r1 && i + 1 < ops.size()) {
                Op next = ops.get(i + 1);
                next = substitute(next, substitutions);
                if (next instanceof Op.ReserveRead r2) {
                    changed = true;
                    Value newSize = addValues(r1.size(), r2.size());
                    result.add(new Op.ReserveRead(newSize, r1.addressOut()));
                    substitutions.put(r2.addressOut(), addValues(new Value.LocalValue(r1.addressOut()), r1.size()));
                    continue;
                }
            }

            if (op instanceof Op.If ifOp) {
                Result rThen = mergeReservations(ifOp.thenOps());
                Result rElse = mergeReservations(ifOp.elseOps());
                if (rThen.changed() || rElse.changed()) {
                    changed = true;
                    op = new Op.If(ifOp.condition(), rThen.ops(), rElse.ops());
                }
            } else if (op instanceof Op.ForEach forEach) {
                Result rBody = mergeReservations(forEach.body());
                if (rBody.changed()) {
                    changed = true;
                    op = new Op.ForEach(forEach.source(), forEach.element(), rBody.ops());
                }
            } else if (op instanceof Op.ForIndex forIndex) {
                Result rBody = mergeReservations(forIndex.body());
                if (rBody.changed()) {
                    changed = true;
                    op = new Op.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), rBody.ops());
                }
            }

            result.add(op);
        }

        return new Result(result, changed);
    }

    private static Result mergeRuns(List<Op> ops) {
        List<Op> merged = new ArrayList<>();
        boolean changed = false;
        for (int i = 0; i < ops.size(); i++) {
            Op op = ops.get(i);
            Op optimized = switch (op) {
                case Op.If ifOp -> {
                    Result rThen = mergeRuns(ifOp.thenOps());
                    Result rElse = mergeRuns(ifOp.elseOps());
                    if (rThen.changed() || rElse.changed()) {
                        changed = true;
                        yield new Op.If(ifOp.condition(), rThen.ops(), rElse.ops());
                    }
                    yield ifOp;
                }
                case Op.ForEach forEach -> {
                    Result rBody = mergeRuns(forEach.body());
                    if (rBody.changed()) {
                        changed = true;
                        yield new Op.ForEach(forEach.source(), forEach.element(), rBody.ops());
                    }
                    yield forEach;
                }
                case Op.ForIndex forIndex -> {
                    Result rBody = mergeRuns(forIndex.body());
                    if (rBody.changed()) {
                        changed = true;
                        yield new Op.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), rBody.ops());
                    }
                    yield forIndex;
                }
                default -> op;
            };

            // Try to merge into a write run
            if (isWriteRunCompatible(optimized)) {
                List<Op> runOps = new ArrayList<>();
                runOps.add(optimized);
                while (i + 1 < ops.size() && isWriteRunCompatible(ops.get(i + 1))) {
                    i++;
                    runOps.add(ops.get(i));
                }
                if (runOps.size() > 1 || runOps.get(0) instanceof Op.WriteRun) {
                    List<Op> resultOps = mergeWriteRunOps(runOps);
                    if (resultOps.size() < runOps.size() || (resultOps.size() == 1 && !(runOps.get(0) instanceof Op.WriteRun))) {
                        changed = true;
                    }
                    merged.addAll(resultOps);
                    continue;
                }
            }

            // Try to merge into a read run
            if (isReadRunCompatible(optimized)) {
                List<Op> runOps = new ArrayList<>();
                runOps.add(optimized);
                while (i + 1 < ops.size() && isReadRunCompatible(ops.get(i + 1))) {
                    i++;
                    runOps.add(ops.get(i));
                }
                if (runOps.size() > 1 || runOps.get(0) instanceof Op.ReadRun) {
                    List<Op> resultOps = mergeReadRunOps(runOps);
                    if (resultOps.size() < runOps.size() || (resultOps.size() == 1 && !(runOps.get(0) instanceof Op.ReadRun))) {
                        changed = true;
                    }
                    merged.addAll(resultOps);
                    continue;
                }
            }

            merged.add(optimized);
        }
        return new Result(merged, changed);
    }

    private static Result simplifyValues(List<Op> ops) {
        List<Op> result = new ArrayList<>();
        boolean changed = false;
        for (Op op : ops) {
            Op simplified = switch (op) {
                case Op.If ifOp -> {
                    Result rThen = simplifyValues(ifOp.thenOps());
                    Result rElse = simplifyValues(ifOp.elseOps());
                    if (rThen.changed() || rElse.changed()) {
                        changed = true;
                        yield new Op.If(ifOp.condition(), rThen.ops(), rElse.ops());
                    }
                    yield ifOp;
                }
                case Op.ForEach forEach -> {
                    Result rBody = simplifyValues(forEach.body());
                    if (rBody.changed()) {
                        changed = true;
                        yield new Op.ForEach(forEach.source(), forEach.element(), rBody.ops());
                    }
                    yield forEach;
                }
                case Op.ForIndex forIndex -> {
                    Result rBody = simplifyValues(forIndex.body());
                    if (rBody.changed()) {
                        changed = true;
                        yield new Op.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), rBody.ops());
                    }
                    yield forIndex;
                }
                case Op.WritePrimitive p -> {
                    Value simplifiedAddress = simplifyValue(p.address());
                    if (simplifiedAddress != p.address()) {
                        changed = true;
                        yield new Op.WritePrimitive(p.kind(), p.value(), simplifiedAddress);
                    }
                    yield p;
                }
                case Op.ReadPrimitive p -> {
                    Value simplifiedAddress = simplifyValue(p.address());
                    if (simplifiedAddress != p.address()) {
                        changed = true;
                        yield new Op.ReadPrimitive(p.kind(), simplifiedAddress, p.out());
                    }
                    yield p;
                }
                case Op.WriteVarInt v -> {
                    Value simplifiedAddress = simplifyValue(v.address());
                    if (simplifiedAddress != v.address()) {
                        changed = true;
                        yield new Op.WriteVarInt(v.value(), simplifiedAddress);
                    }
                    yield v;
                }
                case Op.ReadVarInt v -> {
                    Value simplifiedAddress = simplifyValue(v.address());
                    if (simplifiedAddress != v.address()) {
                        changed = true;
                        yield new Op.ReadVarInt(simplifiedAddress, v.out());
                    }
                    yield v;
                }
                case Op.WriteVarLong v -> {
                    Value simplifiedAddress = simplifyValue(v.address());
                    if (simplifiedAddress != v.address()) {
                        changed = true;
                        yield new Op.WriteVarLong(v.value(), simplifiedAddress);
                    }
                    yield v;
                }
                case Op.ReadVarLong v -> {
                    Value simplifiedAddress = simplifyValue(v.address());
                    if (simplifiedAddress != v.address()) {
                        changed = true;
                        yield new Op.ReadVarLong(simplifiedAddress, v.out());
                    }
                    yield v;
                }
                case Op.WriteFixedBytes f -> {
                    Value simplifiedAddress = simplifyValue(f.address());
                    if (simplifiedAddress != f.address()) {
                        changed = true;
                        yield new Op.WriteFixedBytes(f.value(), simplifiedAddress);
                    }
                    yield f;
                }
                case Op.ReadFixedBytes f -> {
                    Value simplifiedAddress = simplifyValue(f.address());
                    if (simplifiedAddress != f.address()) {
                        changed = true;
                        yield new Op.ReadFixedBytes(f.length(), simplifiedAddress, f.out());
                    }
                    yield f;
                }
                case Op.WriteRun r -> {
                    Value simplifiedAddress = simplifyValue(r.address());
                    if (simplifiedAddress != r.address()) {
                        changed = true;
                        yield new Op.WriteRun(simplifiedAddress, r.run());
                    }
                    yield r;
                }
                case Op.ReadRun r -> {
                    Value simplifiedAddress = simplifyValue(r.address());
                    if (simplifiedAddress != r.address()) {
                        changed = true;
                        yield new Op.ReadRun(simplifiedAddress, r.run());
                    }
                    yield r;
                }
                default -> op;
            };
            result.add(simplified);
        }
        return new Result(result, changed);
    }

    private static Value simplifyValue(Value value) {
        return switch (value) {
            case Value.Add add -> {
                Value left = simplifyValue(add.left());
                Value right = simplifyValue(add.right());
                if (left instanceof Value.Const(Object lv) && lv instanceof Number n && n.longValue() == 0) yield right;
                if (right instanceof Value.Const(Object rv) && rv instanceof Number n && n.longValue() == 0) yield left;
                if (left != add.left() || right != add.right()) yield addValues(left, right);
                yield add;
            }
            default -> value;
        };
    }

    private static Op substitute(Op op, Map<Local, Value> substitutions) {
        if (substitutions.isEmpty()) return op;
        return switch (op) {
            case Op.WritePrimitive p -> new Op.WritePrimitive(p.kind(), p.value(), substituteValue(p.address(), substitutions));
            case Op.ReadPrimitive p -> new Op.ReadPrimitive(p.kind(), substituteValue(p.address(), substitutions), p.out());
            case Op.WriteVarInt v -> new Op.WriteVarInt(v.value(), substituteValue(v.address(), substitutions));
            case Op.ReadVarInt v -> new Op.ReadVarInt(substituteValue(v.address(), substitutions), v.out());
            case Op.WriteVarLong v -> new Op.WriteVarLong(v.value(), substituteValue(v.address(), substitutions));
            case Op.ReadVarLong v -> new Op.ReadVarLong(substituteValue(v.address(), substitutions), v.out());
            case Op.WriteFixedBytes f -> new Op.WriteFixedBytes(f.value(), substituteValue(f.address(), substitutions));
            case Op.ReadFixedBytes f -> new Op.ReadFixedBytes(f.length(), substituteValue(f.address(), substitutions), f.out());
            case Op.WriteRun r -> new Op.WriteRun(substituteValue(r.address(), substitutions), r.run());
            case Op.ReadRun r -> new Op.ReadRun(substituteValue(r.address(), substitutions), r.run());
            default -> op;
        };
    }

    private static Value substituteValue(Value value, Map<Local, Value> substitutions) {
        return switch (value) {
            case Value.LocalValue lv -> substitutions.getOrDefault(lv.local(), lv);
            case Value.Add add -> addValues(substituteValue(add.left(), substitutions), substituteValue(add.right(), substitutions));
            default -> value;
        };
    }

    private static boolean isWriteRunCompatible(Op op) {
        return op instanceof Op.WritePrimitive || op instanceof Op.WriteVarInt || op instanceof Op.WriteVarLong || op instanceof Op.WriteFixedBytes || op instanceof Op.WriteRun;
    }

    private static boolean isReadRunCompatible(Op op) {
        return op instanceof Op.ReadPrimitive || (op instanceof Op.ReadFixedBytes r && r.length() instanceof Value.Const) || op instanceof Op.ReadRun;
    }

    private static List<Op> mergeWriteRunOps(List<Op> ops) {
        if (ops.isEmpty()) return ops;

        // Group by base address if possible
        Value baseAddress = getBaseAddress(ops.get(0));
        if (baseAddress == null) return ops;

        List<Op> result = new ArrayList<>();
        List<RunItem> items = new ArrayList<>();
        Value totalSize = new Value.Const(0L);

        for (Op op : ops) {
            Value currentBase = getBaseAddress(op);
            if (!baseAddress.equals(currentBase)) {
                if (!items.isEmpty()) {
                    result.add(new Op.WriteRun(baseAddress, new RunIr(totalSize, items)));
                    items = new ArrayList<>();
                    totalSize = new Value.Const(0L);
                }
                baseAddress = currentBase;
                if (baseAddress == null) {
                    result.add(op);
                    continue;
                }
            }

            Value relativeOffset = getRelativeOffset(op, baseAddress);
            if (relativeOffset == null) {
                 if (!items.isEmpty()) {
                    result.add(new Op.WriteRun(baseAddress, new RunIr(totalSize, items)));
                    items = new ArrayList<>();
                    totalSize = new Value.Const(0L);
                }
                result.add(op);
                baseAddress = null; // Can't merge with this
                continue;
            }

            if (op instanceof Op.WriteRun writeRun) {
                for (RunItem item : writeRun.run().items()) {
                    items.add(shiftItem(item, relativeOffset));
                }
                totalSize = maxValues(totalSize, addValues(relativeOffset, writeRun.run().size()));
                continue;
            }

            RunItem item = null;
            Value itemSize = null;
            if (op instanceof Op.WritePrimitive p) {
                item = new RunItem.Put(p.kind().storeKind(), relativeOffset, p.value());
                itemSize = new Value.Const((long) p.kind().storeKind().byteSize());
            } else if (op instanceof Op.WriteVarInt v) {
                Value size = new Value.VarIntSize(v.value());
                item = new RunItem.PutVarInt(relativeOffset, v.value(), size);
                itemSize = size;
            } else if (op instanceof Op.WriteVarLong v) {
                Value size = new Value.VarLongSize(v.value());
                item = new RunItem.PutVarLong(relativeOffset, v.value(), size);
                itemSize = size;
            } else if (op instanceof Op.WriteFixedBytes f) {
                item = new RunItem.PutBytes(relativeOffset, f.value(), new Value.ArrayLength(f.value()));
                itemSize = new Value.ArrayLength(f.value());
            }

            if (item != null) {
                items.add(item);
                totalSize = maxValues(totalSize, addValues(relativeOffset, itemSize));
            } else {
                if (!items.isEmpty()) {
                    result.add(new Op.WriteRun(baseAddress, new RunIr(totalSize, items)));
                    items = new ArrayList<>();
                    totalSize = new Value.Const(0L);
                }
                result.add(op);
            }
        }

        if (!items.isEmpty()) {
            result.add(new Op.WriteRun(baseAddress, new RunIr(totalSize, items)));
        }

        return result;
    }

    private static List<Op> mergeReadRunOps(List<Op> ops) {
        if (ops.isEmpty()) return ops;

        Value baseAddress = getBaseAddress(ops.get(0));
        if (baseAddress == null) return ops;

        List<Op> result = new ArrayList<>();
        List<RunItem> items = new ArrayList<>();
        Value totalSize = new Value.Const(0L);

        for (Op op : ops) {
            Value currentBase = getBaseAddress(op);
            if (!baseAddress.equals(currentBase)) {
                if (!items.isEmpty()) {
                    result.add(new Op.ReadRun(baseAddress, new RunIr(totalSize, items)));
                    items = new ArrayList<>();
                    totalSize = new Value.Const(0L);
                }
                baseAddress = currentBase;
                if (baseAddress == null) {
                    result.add(op);
                    continue;
                }
            }

            Value relativeOffset = getRelativeOffset(op, baseAddress);
            if (relativeOffset == null) {
                 if (!items.isEmpty()) {
                    result.add(new Op.ReadRun(baseAddress, new RunIr(totalSize, items)));
                    items = new ArrayList<>();
                    totalSize = new Value.Const(0L);
                }
                result.add(op);
                baseAddress = null;
                continue;
            }

            if (op instanceof Op.ReadRun readRun) {
                for (RunItem item : readRun.run().items()) {
                    items.add(shiftItem(item, relativeOffset));
                }
                totalSize = maxValues(totalSize, addValues(relativeOffset, readRun.run().size()));
                continue;
            }

            RunItem item = null;
            Value itemSize = null;
            if (op instanceof Op.ReadPrimitive p) {
                item = new RunItem.Get(p.kind().storeKind(), relativeOffset, p.out());
                itemSize = new Value.Const((long) p.kind().storeKind().byteSize());
            } else if (op instanceof Op.ReadFixedBytes f && f.length() instanceof Value.Const c) {
                item = new RunItem.GetBytes(relativeOffset, f.out(), f.length());
                itemSize = f.length();
            }

            if (item != null) {
                items.add(item);
                totalSize = maxValues(totalSize, addValues(relativeOffset, itemSize));
            } else {
                if (!items.isEmpty()) {
                    result.add(new Op.ReadRun(baseAddress, new RunIr(totalSize, items)));
                    items = new ArrayList<>();
                    totalSize = new Value.Const(0L);
                }
                result.add(op);
            }
        }

        if (!items.isEmpty()) {
            result.add(new Op.ReadRun(baseAddress, new RunIr(totalSize, items)));
        }

        return result;
    }

    private static Value getBaseAddress(Op op) {
        Value addr = switch (op) {
            case Op.WritePrimitive p -> p.address();
            case Op.ReadPrimitive p -> p.address();
            case Op.WriteVarInt v -> v.address();
            case Op.ReadVarInt v -> v.address();
            case Op.WriteVarLong v -> v.address();
            case Op.ReadVarLong v -> v.address();
            case Op.WriteFixedBytes f -> f.address();
            case Op.ReadFixedBytes f -> f.address();
            case Op.WriteRun r -> r.address();
            case Op.ReadRun r -> r.address();
            default -> null;
        };
        if (addr == null) return null;
        while (addr instanceof Value.Add add) {
            addr = add.left();
        }
        return addr;
    }

    private static Value getRelativeOffset(Op op, Value base) {
        Value addr = switch (op) {
            case Op.WritePrimitive p -> p.address();
            case Op.ReadPrimitive p -> p.address();
            case Op.WriteVarInt v -> v.address();
            case Op.ReadVarInt v -> v.address();
            case Op.WriteVarLong v -> v.address();
            case Op.ReadVarLong v -> v.address();
            case Op.WriteFixedBytes f -> f.address();
            case Op.ReadFixedBytes f -> f.address();
            case Op.WriteRun r -> r.address();
            case Op.ReadRun r -> r.address();
            default -> null;
        };
        if (addr == null) return null;
        if (addr.equals(base)) return new Value.Const(0L);
        if (addr instanceof Value.Add add) {
            if (add.left().equals(base)) return add.right();
            Value sub = getRelativeOffsetFromAdd(add.left(), base);
            if (sub != null) return addValues(sub, add.right());
        }
        return null;
    }

    private static Value getRelativeOffsetFromAdd(Value addr, Value base) {
        if (addr.equals(base)) return new Value.Const(0L);
        if (addr instanceof Value.Add add) {
            if (add.left().equals(base)) return add.right();
            Value sub = getRelativeOffsetFromAdd(add.left(), base);
            if (sub != null) return addValues(sub, add.right());
        }
        return null;
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

    private static Value maxValues(Value left, Value right) {
        if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
            if (lv instanceof Number l && rv instanceof Number r) return new Value.Const(Math.max(l.longValue(), r.longValue()));
        }
        // Simplified max: if one is 0, return other
        if (left instanceof Value.Const(Object lv) && lv instanceof Number n && n.longValue() == 0) return right;
        if (right instanceof Value.Const(Object rv) && rv instanceof Number n && n.longValue() == 0) return left;
        return new Value.Add(left, right); // Conservative
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
