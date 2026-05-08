package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBufferTypeImpl;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IrOptimizer {
    private static final List<Pass> PASSES = List.of(
            IrOptimizer::foldRuns,
            IrOptimizer::mergeRuns,
            IrOptimizer::simplifyRuns
    );

    private IrOptimizer() {
    }

    public static List<RunIr> optimize(List<RunIr> runs) {
        List<RunIr> current = runs;
        boolean modified;
        do {
            modified = false;
            for (Pass pass : PASSES) {
                List<RunIr> result = pass.apply(current);
                modified |= !result.equals(current);
                current = result;
            }
        } while (modified);
        return current;
    }

    private static List<RunIr> foldRuns(List<RunIr> runs) {
        List<RunIr> result = new ArrayList<>();
        for (RunIr run : runs) {
            Value newSize = optimizeValue(run.size());
            List<RunItem> newItems = new ArrayList<>();
            List<RunIr> injectedRuns = null;

            for (RunItem item : run.items()) {
                RunItem optItem = optimizeItem(item);
                if (optItem instanceof RunItem.If(
                        Value condition, List<RunIr> thenRuns, List<RunIr> elseRuns
                ) && condition instanceof Value.Const(Object value)) {
                    boolean cond = false;
                    if (value instanceof Boolean b) {
                        cond = b;
                    }

                    List<RunIr> branch = cond ? thenRuns : elseRuns;
                    if (injectedRuns == null) {
                        injectedRuns = new ArrayList<>();
                    }
                    injectedRuns.addAll(branch);
                    // The If item is dropped.
                } else {
                    newItems.add(optItem);
                }
            }

            if (newSize != run.size() || newItems.size() != run.items().size() || !newItems.equals(run.items())) {
                run = new RunIr(newSize, newItems);
            }

            if (!isZero(run.size()) || !run.items().isEmpty()) {
                result.add(run);
            }

            if (injectedRuns != null) {
                result.addAll(injectedRuns);
            }
        }
        return result;
    }

    private static boolean isZero(Value v) {
        if (v instanceof Value.Const(Object value) && value instanceof Number n) {
            return n.longValue() == 0;
        }
        return false;
    }

    private static List<RunIr> mergeRuns(List<RunIr> runs) {
        if (runs.isEmpty()) {
            return runs;
        }

        List<RunIr> result = new ArrayList<>();
        RunIr current = runs.getFirst();
        for (int i = 1; i < runs.size(); i++) {
            RunIr next = runs.get(i);
            RunIr merged = tryMerge(current, next);
            if (merged != null) {
                current = merged;
            } else {
                result.add(current);
                current = next;
            }
        }
        result.add(current);
        return result;
    }

    private static @Nullable RunIr tryMerge(RunIr current, RunIr next) {
        if (!isMergeable(current) || !isMergeable(next)) {
            return null;
        }

        Set<Local> modifiedLocals = new HashSet<>();
        for (RunItem item : current.items()) {
            collectModifiedLocals(item, modifiedLocals);
        }

        Set<Local> usedInSize = new HashSet<>();
        collectUsedLocals(next.size(), usedInSize);
        for (Local used : usedInSize) {
            if (modifiedLocals.contains(used)) {
                return null;
            }
        }

        Value mergedSize = optimizeValue(new Value.Add(current.size(), next.size()));
        List<RunItem> mergedItems = new ArrayList<>(current.items());
        for (RunItem item : next.items()) {
            mergedItems.add(shiftOffset(item, current.size()));
        }
        return new RunIr(mergedSize, mergedItems);
    }

    private static boolean isMergeable(RunIr block) {
        for (RunItem item : block.items()) {
            if (item instanceof RunItem.GetVarInt || item instanceof RunItem.GetVarLong || item instanceof RunItem.WriteExternal
                    || item instanceof RunItem.ReadExternal || item instanceof RunItem.If || item instanceof RunItem.ForEach || item instanceof RunItem.ForIndex
                    || item instanceof RunItem.Return) {
                return false;
            }
        }
        return true;
    }

    private static RunItem shiftOffset(RunItem item, Value shiftBy) {
        switch (item) {
            case RunItem.Put put -> {
                return new RunItem.Put(put.kind(), optimizeValue(new Value.Add(put.offset(), shiftBy)), put.value());
            }
            case RunItem.Get get -> {
                return new RunItem.Get(get.kind(), optimizeValue(new Value.Add(get.offset(), shiftBy)), get.out());
            }
            case RunItem.PutVarInt put -> {
                return new RunItem.PutVarInt(optimizeValue(new Value.Add(put.offset(), shiftBy)), put.value(), put.encodedSize());
            }
            case RunItem.PutVarLong put -> {
                return new RunItem.PutVarLong(optimizeValue(new Value.Add(put.offset(), shiftBy)), put.value(), put.encodedSize());
            }
            case RunItem.PutBytes put -> {
                return new RunItem.PutBytes(optimizeValue(new Value.Add(put.offset(), shiftBy)), put.byteArray(), put.length());
            }
            case RunItem.GetBytes get -> {
                return new RunItem.GetBytes(optimizeValue(new Value.Add(get.offset(), shiftBy)), get.byteArray(), get.length());
            }
            default -> {
                return item;
            }
        }
    }

    private static void collectModifiedLocals(RunItem item, Set<Local> out) {
        if (item == null) return;
        switch (item) {
            case RunItem.Get g -> out.add(g.out());
            case RunItem.GetVarInt g -> out.add(g.out());
            case RunItem.GetVarLong g -> out.add(g.out());
            case RunItem.GetBytes g -> out.add(g.byteArray());
            case RunItem.Apply a -> out.add(a.out());
            case RunItem.Cast c -> out.add(c.out());
            case RunItem.Unbox u -> out.add(u.out());
            case RunItem.Box b -> out.add(b.out());
            case RunItem.StringToBytes s -> out.add(s.out());
            case RunItem.BytesToString b -> out.add(b.out());
            case RunItem.EitherLeft e -> out.add(e.out());
            case RunItem.EitherRight e -> out.add(e.out());
            case RunItem.Store s -> out.add(s.out());
            case RunItem.ReadExternal r -> out.add(r.out());
            case RunItem.ElementAt e -> out.add(e.out());
            case RunItem.MapEntrySet m -> out.add(m.out());
            case RunItem.MapEntryKey m -> out.add(m.out());
            case RunItem.MapEntryValue m -> out.add(m.out());
            case RunItem.ArrayCreate a -> out.add(a.out());
            case RunItem.ListFinish l -> out.add(l.out());
            case RunItem.MapFinish m -> out.add(m.out());
            case RunItem.Construct c -> out.add(c.out());
            default -> {
            }
        }
    }

    private static void collectUsedLocals(Value value, Set<Local> out) {
        if (value == null)
            return;
        switch (value) {
            case Value.LocalValue lv -> out.add(lv.local());
            case Value.Add add -> {
                collectUsedLocals(add.left(), out);
                collectUsedLocals(add.right(), out);
            }
            case Value.Mul mul -> {
                collectUsedLocals(mul.left(), out);
                collectUsedLocals(mul.right(), out);
            }
            case Value.And and -> {
                collectUsedLocals(and.left(), out);
                collectUsedLocals(and.right(), out);
            }
            case Value.Or or -> {
                collectUsedLocals(or.left(), out);
                collectUsedLocals(or.right(), out);
            }
            case Value.ShiftLeft sl -> collectUsedLocals(sl.value(), out);
            case Value.ShiftRightUnsigned sr -> collectUsedLocals(sr.value(), out);
            case Value.ArrayLength al -> collectUsedLocals(al.array(), out);
            case Value.CollectionSize cs -> collectUsedLocals(cs.collection(), out);
            case Value.MapSize ms -> collectUsedLocals(ms.map(), out);
            case Value.StringUtf8Bytes s -> collectUsedLocals(s.string(), out);
            case Value.VarIntSize vi -> collectUsedLocals(vi.intValue(), out);
            case Value.VarLongSize vl -> collectUsedLocals(vl.longValue(), out);
            case Value.Ternary t -> {
                collectUsedLocals(t.condition(), out);
                collectUsedLocals(t.trueValue(), out);
                collectUsedLocals(t.falseValue(), out);
            }
            case Value.IsNull isNull -> collectUsedLocals(isNull.value(), out);
            case Value.IsNotNull isNotNull -> collectUsedLocals(isNotNull.value(), out);
            case Value.Not not -> collectUsedLocals(not.value(), out);
            case Value.IsLeft isLeft -> collectUsedLocals(isLeft.value(), out);
            case Value.EitherLeft el -> collectUsedLocals(el.value(), out);
            case Value.EitherRight er -> collectUsedLocals(er.value(), out);
            case Value.BoolByte bb -> collectUsedLocals(bb.booleanValue(), out);
            case Value.UnsignedByte ub -> collectUsedLocals(ub.byteValue(), out);
            case Value.LessThanOrEqual lte -> {
                collectUsedLocals(lte.left(), out);
                collectUsedLocals(lte.right(), out);
            }
            case Value.GreaterThan gt -> {
                collectUsedLocals(gt.left(), out);
                collectUsedLocals(gt.right(), out);
            }
            case Value.Index _ -> {
            }
            default -> {
            }
        }
    }

    private static RunItem optimizeItem(RunItem item) {
        switch (item) {
            case RunItem.Put p -> {
                Value v = optimizeValue(p.value());
                Value o = optimizeValue(p.offset());
                if (v != p.value() || o != p.offset()) {
                    return new RunItem.Put(p.kind(), o, v);
                }
                return p;
            }
            case RunItem.Get g -> {
                Value o = optimizeValue(g.offset());
                if (o != g.offset()) {
                    return new RunItem.Get(g.kind(), o, g.out());
                }
                return g;
            }
            case RunItem.PutVarInt p -> {
                Value o = optimizeValue(p.offset());
                Value v = optimizeValue(p.value());
                Value s = optimizeValue(p.encodedSize());
                if (o != p.offset() || v != p.value() || s != p.encodedSize()) {
                    return new RunItem.PutVarInt(o, v, s);
                }
                return p;
            }
            case RunItem.PutVarLong p -> {
                Value o = optimizeValue(p.offset());
                Value v = optimizeValue(p.value());
                Value s = optimizeValue(p.encodedSize());
                if (o != p.offset() || v != p.value() || s != p.encodedSize()) {
                    return new RunItem.PutVarLong(o, v, s);
                }
                return p;
            }
            case RunItem.PutBytes p -> {
                Value o = optimizeValue(p.offset());
                Value b = optimizeValue(p.byteArray());
                Value l = optimizeValue(p.length());
                if (o != p.offset() || b != p.byteArray() || l != p.length()) {
                    return new RunItem.PutBytes(o, b, l);
                }
                return p;
            }
            case RunItem.GetBytes g -> {
                Value o = optimizeValue(g.offset());
                Value l = optimizeValue(g.length());
                if (o != g.offset() || l != g.length()) {
                    return new RunItem.GetBytes(o, g.byteArray(), l);
                }
                return g;
            }
            case RunItem.Store s -> {
                Value v = optimizeValue(s.value());
                if (v != s.value()) {
                    return new RunItem.Store(v, s.out());
                }
                return s;
            }
            case RunItem.Check c -> {
                Value cond = optimizeValue(c.condition());
                if (cond != c.condition()) {
                    return new RunItem.Check(cond, c.message());
                }
                return c;
            }
            case RunItem.WriteExternal w -> {
                Value v = optimizeValue(w.value());
                if (v != w.value()) {
                    return new RunItem.WriteExternal(w.type(), v);
                }
                return w;
            }
            case RunItem.ElementAt e -> {
                Value s = optimizeValue(e.source());
                Value i = optimizeValue(e.index());
                if (s != e.source() || i != e.index()) {
                    return new RunItem.ElementAt(s, i, e.out());
                }
                return e;
            }
            case RunItem.MapEntrySet m -> {
                Value v = optimizeValue(m.map());
                if (v != m.map()) {
                    return new RunItem.MapEntrySet(v, m.out());
                }
                return m;
            }
            case RunItem.ResultElementSet r -> {
                Value res = optimizeValue(r.result());
                Value i = optimizeValue(r.index());
                Value v = optimizeValue(r.value());
                if (res != r.result() || i != r.index() || v != r.value()) {
                    return new RunItem.ResultElementSet(res, i, v);
                }
                return r;
            }
            case RunItem.ArrayCreate a -> {
                Value s = optimizeValue(a.size());
                if (s != a.size()) {
                    return new RunItem.ArrayCreate(s, a.out());
                }
                return a;
            }
            case RunItem.ArraySet a -> {
                Value i = optimizeValue(a.index());
                Value v = optimizeValue(a.value());
                if (i != a.index() || v != a.value()) {
                    return new RunItem.ArraySet(a.array(), i, v);
                }
                return a;
            }
            case RunItem.MapFinish m -> {
                Value s = optimizeValue(m.size());
                if (s != m.size()) {
                    return new RunItem.MapFinish(m.keys(), m.values(), s, m.out());
                }
                return m;
            }
            case RunItem.Construct c -> {
                List<Value> args = new ArrayList<>(c.args().size());
                boolean argsChanged = false;
                for (Value arg : c.args()) {
                    Value opt = optimizeValue(arg);
                    args.add(opt);
                    if (opt != arg)
                        argsChanged = true;
                }
                if (argsChanged) {
                    return new RunItem.Construct(c.factory(), args, c.out());
                }
                return c;
            }
            case RunItem.Return r -> {
                Value v = optimizeValue(r.value());
                if (v != r.value()) {
                    return new RunItem.Return(v);
                }
                return r;
            }
            case RunItem.If ifOp -> {
                Value cond = optimizeValue(ifOp.condition());
                List<RunIr> thenOpt = optimize(ifOp.thenRuns());
                List<RunIr> elseOpt = optimize(ifOp.elseRuns());
                if (cond != ifOp.condition() || !thenOpt.equals(ifOp.thenRuns()) || !elseOpt.equals(ifOp.elseRuns())) {
                    return new RunItem.If(cond, thenOpt, elseOpt);
                }
                return ifOp;
            }
            case RunItem.ForEach forEach -> {
                Value src = optimizeValue(forEach.source());
                List<RunIr> bodyOpt = optimize(forEach.body());
                if (src != forEach.source() || !bodyOpt.equals(forEach.body())) {
                    return new RunItem.ForEach(src, forEach.element(), bodyOpt);
                }
                return forEach;
            }
            case RunItem.ForIndex forIndex -> {
                Value start = optimizeValue(forIndex.start());
                Value end = optimizeValue(forIndex.end());
                List<RunIr> bodyOpt = optimize(forIndex.body());
                if (start != forIndex.start() || end != forIndex.end() || !bodyOpt.equals(forIndex.body())) {
                    return new RunItem.ForIndex(forIndex.index(), start, end, bodyOpt);
                }
                return forIndex;
            }
            default -> {
                return item;
            }
        }
    }

    private static Value optimizeValue(Value value) {
        if (value == null)
            return null;
        switch (value) {
            case Value.Add add -> {
                Value l = optimizeValue(add.left());
                Value r = optimizeValue(add.right());

                if (r instanceof Value.Const(Object value1) && value1 instanceof Number nR && nR.longValue() == 0) {
                    return l;
                }
                if (l instanceof Value.Const(Object value1) && value1 instanceof Number nL && nL.longValue() == 0) {
                    return r;
                }
                if (l instanceof Value.Add(Value left, Value right) && right instanceof Value.Const(
                        Object value4
                ) && r instanceof Value.Const(
                        Object value3
                )) {
                    if (value4 instanceof Number nA && value3 instanceof Number nB) {
                        return new Value.Add(left, new Value.Const(nA.longValue() + nB.longValue()));
                    }
                }
                if (l instanceof Value.Const(Object value2) && r instanceof Value.Const(Object value1)) {
                    if (value2 instanceof Number nL && value1 instanceof Number nR) {
                        return new Value.Const(nL.longValue() + nR.longValue());
                    }
                }
                if (l instanceof Value.Const && !(r instanceof Value.Const)) {
                    return new Value.Add(r, l);
                }
                if (l != add.left() || r != add.right()) {
                    return new Value.Add(l, r);
                }
                return add;
            }
            case Value.Mul mul -> {
                Value l = optimizeValue(mul.left());
                Value r = optimizeValue(mul.right());

                if (l instanceof Value.Const && !(r instanceof Value.Const)) {
                    return new Value.Mul(r, l);
                }
                if (l instanceof Value.Const(Object value4) && r instanceof Value.Const(Object value3)) {
                    if (value4 instanceof Number nL && value3 instanceof Number nR) {
                        return new Value.Const(nL.longValue() * nR.longValue());
                    }
                }
                if (r instanceof Value.Const(Object value2) && value2 instanceof Number nR && nR.longValue() == 1) {
                    return l;
                }
                if (r instanceof Value.Const(Object value1) && value1 instanceof Number nR && nR.longValue() == 0) {
                    return new Value.Const(0L);
                }
                if (l != mul.left() || r != mul.right()) {
                    return new Value.Mul(l, r);
                }
                return mul;
            }
            case Value.VarIntSize vis -> {
                Value v = optimizeValue(vis.intValue());
                if (v instanceof Value.Const(Object value1) && value1 instanceof Number n) {
                    return new Value.Const((long) NetworkBufferTypeImpl.varIntSize(n.intValue()));
                }
                if (v != vis.intValue()) {
                    return new Value.VarIntSize(v);
                }
                return vis;
            }
            case Value.VarLongSize vls -> {
                Value v = optimizeValue(vls.longValue());
                if (v instanceof Value.Const(Object value1) && value1 instanceof Number n) {
                    return new Value.Const((long) NetworkBufferTypeImpl.varLongSize(n.longValue()));
                }
                if (v != vls.longValue()) {
                    return new Value.VarLongSize(v);
                }
                return vls;
            }
            case Value.Ternary t -> {
                Value cond = optimizeValue(t.condition());
                Value tv = optimizeValue(t.trueValue());
                Value fv = optimizeValue(t.falseValue());
                if (cond instanceof Value.Const(Object value1) && value1 instanceof Boolean b) {
                    return b ? tv : fv;
                }
                if (cond != t.condition() || tv != t.trueValue() || fv != t.falseValue()) {
                    return new Value.Ternary(cond, tv, fv);
                }
                return t;
            }
            case Value.Not not -> {
                Value v = optimizeValue(not.value());
                if (v instanceof Value.Const(Object value1) && value1 instanceof Boolean b) {
                    return new Value.Const(!b);
                }
                if (v != not.value()) {
                    return new Value.Not(v);
                }
                return not;
            }
            case Value.IsNull isNull -> {
                Value v = optimizeValue(isNull.value());
                if (v != isNull.value()) {
                    return new Value.IsNull(v);
                }
                return isNull;
            }
            case Value.IsNotNull isNotNull -> {
                Value v = optimizeValue(isNotNull.value());
                if (v != isNotNull.value()) {
                    return new Value.IsNotNull(v);
                }
                return isNotNull;
            }
            case Value.ShiftLeft sl -> {
                Value v = optimizeValue(sl.value());
                if (v instanceof Value.Const(Object value1) && value1 instanceof Number n) {
                    return new Value.Const(n.longValue() << sl.amount());
                }
                if (v != sl.value()) {
                    return new Value.ShiftLeft(v, sl.amount());
                }
                return sl;
            }
            case Value.ShiftRightUnsigned sr -> {
                Value v = optimizeValue(sr.value());
                if (v instanceof Value.Const(Object value1) && value1 instanceof Number n) {
                    return new Value.Const(n.longValue() >>> sr.amount());
                }
                if (v != sr.value()) {
                    return new Value.ShiftRightUnsigned(v, sr.amount());
                }
                return sr;
            }
            case Value.And and -> {
                Value l = optimizeValue(and.left());
                Value r = optimizeValue(and.right());
                if (l instanceof Value.Const(Object value2) && r instanceof Value.Const(Object value1)) {
                    if (value2 instanceof Number nL && value1 instanceof Number nR) {
                        return new Value.Const(nL.longValue() & nR.longValue());
                    }
                }
                if (l != and.left() || r != and.right()) {
                    return new Value.And(l, r);
                }
                return and;
            }
            case Value.Or or -> {
                Value l = optimizeValue(or.left());
                Value r = optimizeValue(or.right());
                if (l instanceof Value.Const(Object value2) && r instanceof Value.Const(Object value1)) {
                    if (value2 instanceof Number nL && value1 instanceof Number nR) {
                        return new Value.Const(nL.longValue() | nR.longValue());
                    }
                }
                if (l != or.left() || r != or.right()) {
                    return new Value.Or(l, r);
                }
                return or;
            }
            case Value.ArrayLength al -> {
                Value a = optimizeValue(al.array());
                if (a != al.array()) {
                    return new Value.ArrayLength(a);
                }
                return al;
            }
            case Value.CollectionSize cs -> {
                Value c = optimizeValue(cs.collection());
                if (c != cs.collection()) {
                    return new Value.CollectionSize(c);
                }
                return cs;
            }
            case Value.MapSize ms -> {
                Value m = optimizeValue(ms.map());
                if (m != ms.map()) {
                    return new Value.MapSize(m);
                }
                return ms;
            }
            case Value.StringUtf8Bytes s -> {
                Value sv = optimizeValue(s.string());
                if (sv != s.string()) {
                    return new Value.StringUtf8Bytes(sv);
                }
                return s;
            }
            case Value.IsLeft il -> {
                Value v = optimizeValue(il.value());
                if (v != il.value()) {
                    return new Value.IsLeft(v);
                }
                return il;
            }
            case Value.EitherLeft el -> {
                Value v = optimizeValue(el.value());
                if (v != el.value()) {
                    return new Value.EitherLeft(v);
                }
                return el;
            }
            case Value.EitherRight er -> {
                Value v = optimizeValue(er.value());
                if (v != er.value()) {
                    return new Value.EitherRight(v);
                }
                return er;
            }
            case Value.BoolByte bb -> {
                Value v = optimizeValue(bb.booleanValue());
                if (v != bb.booleanValue()) {
                    return new Value.BoolByte(v);
                }
                return bb;
            }
            case Value.UnsignedByte ub -> {
                Value v = optimizeValue(ub.byteValue());
                if (v != ub.byteValue()) {
                    return new Value.UnsignedByte(v);
                }
                return ub;
            }
            case Value.LessThanOrEqual lte -> {
                Value l = optimizeValue(lte.left());
                Value r = optimizeValue(lte.right());
                if (l != lte.left() || r != lte.right()) {
                    return new Value.LessThanOrEqual(l, r);
                }
                return lte;
            }
            case Value.GreaterThan gt -> {
                Value l = optimizeValue(gt.left());
                Value r = optimizeValue(gt.right());
                if (l != gt.left() || r != gt.right()) {
                    return new Value.GreaterThan(l, r);
                }
                return gt;
            }
            default -> {
                return value;
            }
        }
    }

    private static List<RunIr> simplifyRuns(List<RunIr> runs) {
        List<RunIr> result = new ArrayList<>();
        for (RunIr run : runs) {
            boolean isZero = run.size() instanceof Value.Const(Object value) && value instanceof Number n && n.longValue() == 0;
            if (isZero && run.reserve() && !usesIndex(run)) {
                run = new RunIr(run.size(), run.items(), false);
            }
            // Recurse into items
            List<RunItem> newItems = new ArrayList<>();
            for (RunItem item : run.items()) {
                newItems.add(switch (item) {
                    case RunItem.If i -> new RunItem.If(i.condition(), optimize(i.thenRuns()), optimize(i.elseRuns()));
                    case RunItem.ForEach f -> new RunItem.ForEach(f.source(), f.element(), optimize(f.body()));
                    case RunItem.ForIndex f -> new RunItem.ForIndex(f.index(), f.start(), f.end(), optimize(f.body()));
                    default -> item;
                });
            }
            if (!newItems.equals(run.items())) {
                run = new RunIr(run.size(), newItems, run.reserve());
            }
            result.add(run);
        }
        return result;
    }

    private static boolean usesIndex(RunIr run) {
        for (RunItem item : run.items()) {
            if (usesIndex(item)) return true;
        }
        return false;
    }

    private static boolean usesIndex(RunItem item) {
        switch (item) {
            case RunItem.Put _:
            case RunItem.Get _:
            case RunItem.PutVarInt _:
            case RunItem.PutVarLong _:
            case RunItem.PutBytes _:
            case RunItem.GetBytes _:
                return true;
            case RunItem.If i:
                for (RunIr r : i.thenRuns()) if (usesIndex(r)) return true;
                for (RunIr r : i.elseRuns()) if (usesIndex(r)) return true;
                return false;
            case RunItem.ForEach f:
                for (RunIr r : f.body()) if (usesIndex(r)) return true;
                return false;
            case RunItem.ForIndex f:
                for (RunIr r : f.body()) if (usesIndex(r)) return true;
                return false;
            default:
                return false;
        }
    }

    @FunctionalInterface
    private interface Pass {
        List<RunIr> apply(List<RunIr> runs);
    }




}
