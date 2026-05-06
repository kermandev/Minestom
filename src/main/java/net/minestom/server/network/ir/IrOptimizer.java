package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBufferTypeImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IrOptimizer {
    private IrOptimizer() {
    }

    public static List<RunIr> optimize(List<RunIr> runs) {
        boolean[] changed = new boolean[1];
        List<RunIr> current = runs;
        do {
            changed[0] = false;
            
            // Pass 1: Node-level Optimization (Value folding & DCE)
            List<RunIr> folded = foldRuns(current, changed);
            
            // Pass 2: Block Merging
            current = mergeRuns(folded, changed);
            
        } while (changed[0]);
        return current;
    }

    private static List<RunIr> foldRuns(List<RunIr> runs, boolean[] changed) {
        List<RunIr> result = new ArrayList<>();
        for (RunIr run : runs) {
            Value newSize = optimizeValue(run.size(), changed);
            List<RunItem> newItems = new ArrayList<>();
            List<RunIr> injectedRuns = null;

            for (RunItem item : run.items()) {
                RunItem optItem = optimizeItem(item, changed);
                if (optItem instanceof RunItem.If ifItem && ifItem.condition() instanceof Value.Const c) {
                    boolean cond = false;
                    if (c.value() instanceof Boolean b) cond = b;
                    
                    List<RunIr> branch = cond ? ifItem.thenRuns() : ifItem.elseRuns();
                    if (injectedRuns == null) injectedRuns = new ArrayList<>();
                    injectedRuns.addAll(branch);
                    changed[0] = true;
                    // The If item is dropped.
                } else {
                    newItems.add(optItem);
                }
            }

            if (newSize != run.size() || newItems.size() != run.items().size() || !newItems.equals(run.items())) {
                changed[0] = true;
                run = new RunIr(newSize, newItems);
            }

            // DCE: Prune empty runs
            if (isZero(run.size()) && run.items().isEmpty()) {
                changed[0] = true;
            } else {
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

    private static List<RunIr> mergeRuns(List<RunIr> runs, boolean[] changed) {
        List<RunIr> result = new ArrayList<>();
        for (int i = 0; i < runs.size(); i++) {
            RunIr current = runs.get(i);
            
            if (i + 1 < runs.size()) {
                RunIr next = runs.get(i + 1);
                
                if (isMergeable(current) && isMergeable(next)) {
                    Set<Local> modifiedLocals = new HashSet<>();
                    for (RunItem item : current.items()) {
                        collectModifiedLocals(item, modifiedLocals);
                    }
                    
                    Set<Local> usedInSize = new HashSet<>();
                    collectUsedLocals(next.size(), usedInSize);
                    
                    boolean hasConflict = false;
                    for (Local used : usedInSize) {
                        if (modifiedLocals.contains(used)) {
                            hasConflict = true;
                            break;
                        }
                    }
                    
                    if (!hasConflict) {
                        Value mergedSize = optimizeValue(new Value.Add(current.size(), next.size()), changed);
                        List<RunItem> mergedItems = new ArrayList<>(current.items());
                        for (RunItem item : next.items()) {
                            mergedItems.add(shiftOffset(item, current.size(), changed));
                        }
                        
                        RunIr merged = new RunIr(mergedSize, mergedItems);
                        runs.set(i + 1, merged); // prepare for next merge
                        changed[0] = true;
                        continue; // Skip adding current, since it was merged into i+1
                    }
                }
            }
            result.add(current);
        }
        return result;
    }

    private static boolean isMergeable(RunIr block) {
        for (RunItem item : block.items()) {
            if (item instanceof RunItem.GetVarInt ||
                item instanceof RunItem.GetVarLong ||
                item instanceof RunItem.WriteExternal ||
                item instanceof RunItem.ReadExternal ||
                item instanceof RunItem.If ||
                item instanceof RunItem.ForEach ||
                item instanceof RunItem.ForIndex ||
                item instanceof RunItem.Return) {
                return false;
            }
        }
        return true;
    }

    private static RunItem shiftOffset(RunItem item, Value shiftBy, boolean[] changed) {
        switch (item) {
            case RunItem.Put put -> {
                return new RunItem.Put(put.kind(), optimizeValue(new Value.Add(put.offset(), shiftBy), changed), put.value());
            }
            case RunItem.Get get -> {
                return new RunItem.Get(get.kind(), optimizeValue(new Value.Add(get.offset(), shiftBy), changed), get.out());
            }
            case RunItem.PutVarInt put -> {
                return new RunItem.PutVarInt(optimizeValue(new Value.Add(put.offset(), shiftBy), changed), put.value(), put.encodedSize());
            }
            case RunItem.PutVarLong put -> {
                return new RunItem.PutVarLong(optimizeValue(new Value.Add(put.offset(), shiftBy), changed), put.value(), put.encodedSize());
            }
            case RunItem.PutBytes put -> {
                return new RunItem.PutBytes(optimizeValue(new Value.Add(put.offset(), shiftBy), changed), put.byteArray(), put.length());
            }
            case RunItem.GetBytes get -> {
                return new RunItem.GetBytes(optimizeValue(new Value.Add(get.offset(), shiftBy), changed), get.byteArray(), get.length());
            }
            default -> {
                return item;
            }
        }
    }

    private static void collectModifiedLocals(RunItem item, Set<Local> out) {
        if (item instanceof RunItem.Get g) out.add(g.out());
        else if (item instanceof RunItem.GetVarInt g) out.add(g.out());
        else if (item instanceof RunItem.GetVarLong g) out.add(g.out());
        else if (item instanceof RunItem.GetBytes g) out.add(g.byteArray());
        else if (item instanceof RunItem.Apply a) out.add(a.out());
        else if (item instanceof RunItem.Cast c) out.add(c.out());
        else if (item instanceof RunItem.Unbox u) out.add(u.out());
        else if (item instanceof RunItem.Box b) out.add(b.out());
        else if (item instanceof RunItem.StringToBytes s) out.add(s.out());
        else if (item instanceof RunItem.BytesToString b) out.add(b.out());
        else if (item instanceof RunItem.EitherLeft e) out.add(e.out());
        else if (item instanceof RunItem.EitherRight e) out.add(e.out());
        else if (item instanceof RunItem.Store s) out.add(s.out());
        else if (item instanceof RunItem.ReadExternal r) out.add(r.out());
        else if (item instanceof RunItem.ElementAt e) out.add(e.out());
        else if (item instanceof RunItem.MapEntrySet m) out.add(m.out());
        else if (item instanceof RunItem.MapEntryKey m) out.add(m.out());
        else if (item instanceof RunItem.MapEntryValue m) out.add(m.out());
        else if (item instanceof RunItem.ArrayCreate a) out.add(a.out());
        else if (item instanceof RunItem.ListFinish l) out.add(l.out());
        else if (item instanceof RunItem.MapFinish m) out.add(m.out());
        else if (item instanceof RunItem.Construct c) out.add(c.out());
    }

    private static void collectUsedLocals(Value value, Set<Local> out) {
        if (value == null) return;
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
            case Value.Index index -> {}
            default -> {}
        }
    }

    private static RunItem optimizeItem(RunItem item, boolean[] changed) {
        switch (item) {
            case RunItem.Put p -> {
                Value v = optimizeValue(p.value(), changed);
                Value o = optimizeValue(p.offset(), changed);
                if (v != p.value() || o != p.offset()) {
                    changed[0] = true;
                    return new RunItem.Put(p.kind(), o, v);
                }
                return p;
            }
            case RunItem.Get g -> {
                Value o = optimizeValue(g.offset(), changed);
                if (o != g.offset()) {
                    changed[0] = true;
                    return new RunItem.Get(g.kind(), o, g.out());
                }
                return g;
            }
            case RunItem.PutVarInt p -> {
                Value o = optimizeValue(p.offset(), changed);
                Value v = optimizeValue(p.value(), changed);
                Value s = optimizeValue(p.encodedSize(), changed);
                if (o != p.offset() || v != p.value() || s != p.encodedSize()) {
                    changed[0] = true;
                    return new RunItem.PutVarInt(o, v, s);
                }
                return p;
            }
            case RunItem.PutVarLong p -> {
                Value o = optimizeValue(p.offset(), changed);
                Value v = optimizeValue(p.value(), changed);
                Value s = optimizeValue(p.encodedSize(), changed);
                if (o != p.offset() || v != p.value() || s != p.encodedSize()) {
                    changed[0] = true;
                    return new RunItem.PutVarLong(o, v, s);
                }
                return p;
            }
            case RunItem.PutBytes p -> {
                Value o = optimizeValue(p.offset(), changed);
                Value b = optimizeValue(p.byteArray(), changed);
                Value l = optimizeValue(p.length(), changed);
                if (o != p.offset() || b != p.byteArray() || l != p.length()) {
                    changed[0] = true;
                    return new RunItem.PutBytes(o, b, l);
                }
                return p;
            }
            case RunItem.GetBytes g -> {
                Value o = optimizeValue(g.offset(), changed);
                Value l = optimizeValue(g.length(), changed);
                if (o != g.offset() || l != g.length()) {
                    changed[0] = true;
                    return new RunItem.GetBytes(o, g.byteArray(), l);
                }
                return g;
            }
            case RunItem.Store s -> {
                Value v = optimizeValue(s.value(), changed);
                if (v != s.value()) {
                    changed[0] = true;
                    return new RunItem.Store(v, s.out());
                }
                return s;
            }
            case RunItem.Check c -> {
                Value cond = optimizeValue(c.condition(), changed);
                if (cond != c.condition()) {
                    changed[0] = true;
                    return new RunItem.Check(cond, c.message());
                }
                return c;
            }
            case RunItem.WriteExternal w -> {
                Value v = optimizeValue(w.value(), changed);
                if (v != w.value()) {
                    changed[0] = true;
                    return new RunItem.WriteExternal(w.type(), v);
                }
                return w;
            }
            case RunItem.ElementAt e -> {
                Value s = optimizeValue(e.source(), changed);
                Value i = optimizeValue(e.index(), changed);
                if (s != e.source() || i != e.index()) {
                    changed[0] = true;
                    return new RunItem.ElementAt(s, i, e.out());
                }
                return e;
            }
            case RunItem.MapEntrySet m -> {
                Value v = optimizeValue(m.map(), changed);
                if (v != m.map()) {
                    changed[0] = true;
                    return new RunItem.MapEntrySet(v, m.out());
                }
                return m;
            }
            case RunItem.ResultElementSet r -> {
                Value res = optimizeValue(r.result(), changed);
                Value i = optimizeValue(r.index(), changed);
                Value v = optimizeValue(r.value(), changed);
                if (res != r.result() || i != r.index() || v != r.value()) {
                    changed[0] = true;
                    return new RunItem.ResultElementSet(res, i, v);
                }
                return r;
            }
            case RunItem.ArrayCreate a -> {
                Value s = optimizeValue(a.size(), changed);
                if (s != a.size()) {
                    changed[0] = true;
                    return new RunItem.ArrayCreate(s, a.out());
                }
                return a;
            }
            case RunItem.ArraySet a -> {
                Value i = optimizeValue(a.index(), changed);
                Value v = optimizeValue(a.value(), changed);
                if (i != a.index() || v != a.value()) {
                    changed[0] = true;
                    return new RunItem.ArraySet(a.array(), i, v);
                }
                return a;
            }
            case RunItem.MapFinish m -> {
                Value s = optimizeValue(m.size(), changed);
                if (s != m.size()) {
                    changed[0] = true;
                    return new RunItem.MapFinish(m.keys(), m.values(), s, m.out());
                }
                return m;
            }
            case RunItem.Construct c -> {
                List<Value> args = new ArrayList<>(c.args().size());
                boolean argsChanged = false;
                for (Value arg : c.args()) {
                    Value opt = optimizeValue(arg, changed);
                    args.add(opt);
                    if (opt != arg) argsChanged = true;
                }
                if (argsChanged) {
                    changed[0] = true;
                    return new RunItem.Construct(c.factory(), args, c.out());
                }
                return c;
            }
            case RunItem.Return r -> {
                Value v = optimizeValue(r.value(), changed);
                if (v != r.value()) {
                    changed[0] = true;
                    return new RunItem.Return(v);
                }
                return r;
            }
            case RunItem.If ifOp -> {
                Value cond = optimizeValue(ifOp.condition(), changed);
                List<RunIr> thenOpt = optimize(ifOp.thenRuns());
                List<RunIr> elseOpt = optimize(ifOp.elseRuns());
                if (cond != ifOp.condition() || !thenOpt.equals(ifOp.thenRuns()) || !elseOpt.equals(ifOp.elseRuns())) {
                    changed[0] = true;
                    return new RunItem.If(cond, thenOpt, elseOpt);
                }
                return ifOp;
            }
            case RunItem.ForEach forEach -> {
                Value src = optimizeValue(forEach.source(), changed);
                List<RunIr> bodyOpt = optimize(forEach.body());
                if (src != forEach.source() || !bodyOpt.equals(forEach.body())) {
                    changed[0] = true;
                    return new RunItem.ForEach(src, forEach.element(), bodyOpt);
                }
                return forEach;
            }
            case RunItem.ForIndex forIndex -> {
                Value start = optimizeValue(forIndex.start(), changed);
                Value end = optimizeValue(forIndex.end(), changed);
                List<RunIr> bodyOpt = optimize(forIndex.body());
                if (start != forIndex.start() || end != forIndex.end() || !bodyOpt.equals(forIndex.body())) {
                    changed[0] = true;
                    return new RunItem.ForIndex(forIndex.index(), start, end, bodyOpt);
                }
                return forIndex;
            }
            default -> {
                return item;
            }
        }
    }

    private static Value optimizeValue(Value value, boolean[] changed) {
        if (value == null) return null;
        switch (value) {
            case Value.Add add -> {
                Value l = optimizeValue(add.left(), changed);
                Value r = optimizeValue(add.right(), changed);
                
                if (r instanceof Value.Const cR && cR.value() instanceof Number nR && nR.longValue() == 0) {
                    changed[0] = true;
                    return l;
                }
                if (l instanceof Value.Const cL && cL.value() instanceof Number nL && nL.longValue() == 0) {
                    changed[0] = true;
                    return r;
                }
                if (l instanceof Value.Add nestedAdd && nestedAdd.right() instanceof Value.Const cA && r instanceof Value.Const cB) {
                    if (cA.value() instanceof Number nA && cB.value() instanceof Number nB) {
                        changed[0] = true;
                        return new Value.Add(nestedAdd.left(), new Value.Const(nA.longValue() + nB.longValue()));
                    }
                }
                if (l instanceof Value.Const cL && r instanceof Value.Const cR) {
                    if (cL.value() instanceof Number nL && cR.value() instanceof Number nR) {
                        changed[0] = true;
                        return new Value.Const(nL.longValue() + nR.longValue());
                    }
                }
                if (l instanceof Value.Const && !(r instanceof Value.Const)) {
                    changed[0] = true;
                    return new Value.Add(r, l);
                }
                if (l != add.left() || r != add.right()) {
                    changed[0] = true;
                    return new Value.Add(l, r);
                }
                return add;
            }
            case Value.Mul mul -> {
                Value l = optimizeValue(mul.left(), changed);
                Value r = optimizeValue(mul.right(), changed);
                
                if (l instanceof Value.Const && !(r instanceof Value.Const)) {
                    changed[0] = true;
                    return new Value.Mul(r, l);
                }
                if (l instanceof Value.Const cL && r instanceof Value.Const cR) {
                    if (cL.value() instanceof Number nL && cR.value() instanceof Number nR) {
                        changed[0] = true;
                        return new Value.Const(nL.longValue() * nR.longValue());
                    }
                }
                if (r instanceof Value.Const cR && cR.value() instanceof Number nR && nR.longValue() == 1) {
                    changed[0] = true;
                    return l;
                }
                if (r instanceof Value.Const cR && cR.value() instanceof Number nR && nR.longValue() == 0) {
                    changed[0] = true;
                    return new Value.Const(0L);
                }
                if (l != mul.left() || r != mul.right()) {
                    changed[0] = true;
                    return new Value.Mul(l, r);
                }
                return mul;
            }
            case Value.VarIntSize vis -> {
                Value v = optimizeValue(vis.intValue(), changed);
                if (v instanceof Value.Const c && c.value() instanceof Number n) {
                    changed[0] = true;
                    return new Value.Const((long) NetworkBufferTypeImpl.varIntSize(n.intValue()));
                }
                if (v != vis.intValue()) {
                    changed[0] = true;
                    return new Value.VarIntSize(v);
                }
                return vis;
            }
            case Value.VarLongSize vls -> {
                Value v = optimizeValue(vls.longValue(), changed);
                if (v instanceof Value.Const c && c.value() instanceof Number n) {
                    changed[0] = true;
                    return new Value.Const((long) NetworkBufferTypeImpl.varLongSize(n.longValue()));
                }
                if (v != vls.longValue()) {
                    changed[0] = true;
                    return new Value.VarLongSize(v);
                }
                return vls;
            }
            case Value.Ternary t -> {
                Value cond = optimizeValue(t.condition(), changed);
                Value tv = optimizeValue(t.trueValue(), changed);
                Value fv = optimizeValue(t.falseValue(), changed);
                if (cond instanceof Value.Const c && c.value() instanceof Boolean b) {
                    changed[0] = true;
                    return b ? tv : fv;
                }
                if (cond != t.condition() || tv != t.trueValue() || fv != t.falseValue()) {
                    changed[0] = true;
                    return new Value.Ternary(cond, tv, fv);
                }
                return t;
            }
            case Value.Not not -> {
                Value v = optimizeValue(not.value(), changed);
                if (v instanceof Value.Const c && c.value() instanceof Boolean b) {
                    changed[0] = true;
                    return new Value.Const(!b);
                }
                if (v != not.value()) {
                    changed[0] = true;
                    return new Value.Not(v);
                }
                return not;
            }
            case Value.IsNull isNull -> {
                Value v = optimizeValue(isNull.value(), changed);
                if (v != isNull.value()) {
                    changed[0] = true;
                    return new Value.IsNull(v);
                }
                return isNull;
            }
            case Value.IsNotNull isNotNull -> {
                Value v = optimizeValue(isNotNull.value(), changed);
                if (v != isNotNull.value()) {
                    changed[0] = true;
                    return new Value.IsNotNull(v);
                }
                return isNotNull;
            }
            case Value.ShiftLeft sl -> {
                Value v = optimizeValue(sl.value(), changed);
                if (v instanceof Value.Const c && c.value() instanceof Number n) {
                    changed[0] = true;
                    return new Value.Const(n.longValue() << sl.amount());
                }
                if (v != sl.value()) {
                    changed[0] = true;
                    return new Value.ShiftLeft(v, sl.amount());
                }
                return sl;
            }
            case Value.ShiftRightUnsigned sr -> {
                Value v = optimizeValue(sr.value(), changed);
                if (v instanceof Value.Const c && c.value() instanceof Number n) {
                    changed[0] = true;
                    return new Value.Const(n.longValue() >>> sr.amount());
                }
                if (v != sr.value()) {
                    changed[0] = true;
                    return new Value.ShiftRightUnsigned(v, sr.amount());
                }
                return sr;
            }
            case Value.And and -> {
                Value l = optimizeValue(and.left(), changed);
                Value r = optimizeValue(and.right(), changed);
                if (l instanceof Value.Const cL && r instanceof Value.Const cR) {
                    if (cL.value() instanceof Number nL && cR.value() instanceof Number nR) {
                        changed[0] = true;
                        return new Value.Const(nL.longValue() & nR.longValue());
                    }
                }
                if (l != and.left() || r != and.right()) {
                    changed[0] = true;
                    return new Value.And(l, r);
                }
                return and;
            }
            case Value.Or or -> {
                Value l = optimizeValue(or.left(), changed);
                Value r = optimizeValue(or.right(), changed);
                if (l instanceof Value.Const cL && r instanceof Value.Const cR) {
                    if (cL.value() instanceof Number nL && cR.value() instanceof Number nR) {
                        changed[0] = true;
                        return new Value.Const(nL.longValue() | nR.longValue());
                    }
                }
                if (l != or.left() || r != or.right()) {
                    changed[0] = true;
                    return new Value.Or(l, r);
                }
                return or;
            }
            case Value.ArrayLength al -> {
                Value a = optimizeValue(al.array(), changed);
                if (a != al.array()) {
                    changed[0] = true;
                    return new Value.ArrayLength(a);
                }
                return al;
            }
            case Value.CollectionSize cs -> {
                Value c = optimizeValue(cs.collection(), changed);
                if (c != cs.collection()) {
                    changed[0] = true;
                    return new Value.CollectionSize(c);
                }
                return cs;
            }
            case Value.MapSize ms -> {
                Value m = optimizeValue(ms.map(), changed);
                if (m != ms.map()) {
                    changed[0] = true;
                    return new Value.MapSize(m);
                }
                return ms;
            }
            case Value.StringUtf8Bytes s -> {
                Value sv = optimizeValue(s.string(), changed);
                if (sv != s.string()) {
                    changed[0] = true;
                    return new Value.StringUtf8Bytes(sv);
                }
                return s;
            }
            case Value.IsLeft il -> {
                Value v = optimizeValue(il.value(), changed);
                if (v != il.value()) {
                    changed[0] = true;
                    return new Value.IsLeft(v);
                }
                return il;
            }
            case Value.EitherLeft el -> {
                Value v = optimizeValue(el.value(), changed);
                if (v != el.value()) {
                    changed[0] = true;
                    return new Value.EitherLeft(v);
                }
                return el;
            }
            case Value.EitherRight er -> {
                Value v = optimizeValue(er.value(), changed);
                if (v != er.value()) {
                    changed[0] = true;
                    return new Value.EitherRight(v);
                }
                return er;
            }
            case Value.BoolByte bb -> {
                Value v = optimizeValue(bb.booleanValue(), changed);
                if (v != bb.booleanValue()) {
                    changed[0] = true;
                    return new Value.BoolByte(v);
                }
                return bb;
            }
            case Value.UnsignedByte ub -> {
                Value v = optimizeValue(ub.byteValue(), changed);
                if (v != ub.byteValue()) {
                    changed[0] = true;
                    return new Value.UnsignedByte(v);
                }
                return ub;
            }
            case Value.LessThanOrEqual lte -> {
                Value l = optimizeValue(lte.left(), changed);
                Value r = optimizeValue(lte.right(), changed);
                if (l != lte.left() || r != lte.right()) {
                    changed[0] = true;
                    return new Value.LessThanOrEqual(l, r);
                }
                return lte;
            }
            case Value.GreaterThan gt -> {
                Value l = optimizeValue(gt.left(), changed);
                Value r = optimizeValue(gt.right(), changed);
                if (l != gt.left() || r != gt.right()) {
                    changed[0] = true;
                    return new Value.GreaterThan(l, r);
                }
                return gt;
            }
            default -> {
                return value;
            }
        }
    }
}
