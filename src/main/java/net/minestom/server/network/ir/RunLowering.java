package net.minestom.server.network.ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class RunLowering {
    public static List<RunIr> lower(List<Op> ops) {
        Context context = new Context();
        for (Op op : ops) {
            lowerOp(context, op);
        }
        context.flush();
        return context.runs;
    }

    private static void lowerOp(Context context, Op op) {
        switch (op) {
            case Op.WritePrimitive write -> {
                StoreKind kind = write.kind().storeKind();
                context.items.add(new RunItem.Put(kind, context.offset(), write.value()));
                context.size = new Value.Add(context.size, new Value.Const(write.kind().byteSize()));
            }
            case Op.ReadPrimitive read -> {
                StoreKind kind = read.kind().storeKind();
                context.items.add(new RunItem.Get(kind, context.offset(), read.out()));
                context.size = new Value.Add(context.size, new Value.Const(read.kind().byteSize()));
            }
            case Op.WriteVarInt write -> {
                ensureDependencies(context, write.value());
                context.items.add(new RunItem.PutVarInt(context.offset(), write.value(), new Value.VarIntSize(write.value())));
                context.size = new Value.Add(context.size, new Value.VarIntSize(write.value()));
            }
            case Op.ReadVarInt read -> {
                context.flush();
                context.items.add(new RunItem.GetVarInt(read.out()));
                context.defined.add(read.out());
                context.flush();
            }
            case Op.WriteVarLong write -> {
                ensureDependencies(context, write.value());
                context.items.add(new RunItem.PutVarLong(context.offset(), write.value(), new Value.VarLongSize(write.value())));
                context.size = new Value.Add(context.size, new Value.VarLongSize(write.value()));
            }
            case Op.ReadVarLong read -> {
                context.flush();
                context.items.add(new RunItem.GetVarLong(read.out()));
                context.defined.add(read.out());
                context.flush();
            }
            case Op.WriteExternal write -> {
                context.flush();
                context.items.add(new RunItem.WriteExternal(write.type(), write.value()));
                context.flush();
            }
            case Op.ReadExternal read -> {
                context.flush();
                context.items.add(new RunItem.ReadExternal(read.type(), read.out()));
                context.defined.add(read.out());
                context.flush();
            }
            case Op.WriteFixedBytes write -> {
                Value length = new Value.ArrayLength(write.value());
                ensureDependencies(context, length);
                context.items.add(new RunItem.PutBytes(context.offset(), write.value(), length));
                context.size = new Value.Add(context.size, length);
            }
            case Op.ReadFixedBytes read -> {
                ensureDependencies(context, read.length());
                context.items.add(new RunItem.GetBytes(context.offset(), read.out(), read.length()));
                context.defined.add(read.out());
                context.size = new Value.Add(context.size, read.length());
            }
            case Op.If ifOp -> {
                context.flush();
                List<RunIr> thenRuns = lower(ifOp.thenOps());
                List<RunIr> elseRuns = lower(ifOp.elseOps());
                context.items.add(new RunItem.If(ifOp.condition(), thenRuns, elseRuns));
                context.flush();
            }
            case Op.ForEach forEach -> {
                context.flush();
                context.items.add(new RunItem.ForEach(forEach.source(), forEach.element(), lower(forEach.body())));
                context.flush();
            }
            case Op.ForIndex forIndex -> {
                context.flush();
                context.items.add(new RunItem.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), lower(forIndex.body())));
                context.flush();
            }
            case Op.Store store -> {
                context.items.add(new RunItem.Store(store.value(), store.out()));
                context.defined.add(store.out());
            }
            case Op.Cast cast -> {
                context.items.add(new RunItem.Cast(cast.in(), cast.targetClass(), cast.out()));
                context.defined.add(cast.out());
            }
            case Op.Apply apply -> {
                context.items.add(new RunItem.Apply(apply.function(), apply.in(), apply.out()));
                context.defined.add(apply.out());
            }
            case Op.Construct construct -> {
                context.items.add(new RunItem.Construct(construct.factory(), construct.args(), construct.out()));
                context.defined.add(construct.out());
            }
            case Op.Return ret -> {
                context.items.add(new RunItem.Return(ret.value()));
                context.flush();
            }
            case Op.StringToBytes s2b -> {
                context.items.add(new RunItem.StringToBytes(s2b.in(), s2b.out()));
                context.defined.add(s2b.out());
            }
            case Op.BytesToString b2s -> {
                context.items.add(new RunItem.BytesToString(b2s.in(), b2s.out()));
                context.defined.add(b2s.out());
            }
            case Op.Check check -> {
                context.items.add(new RunItem.Check(check.condition(), check.message()));
            }
            case Op.ElementAt ea -> {
                context.items.add(new RunItem.ElementAt(ea.source(), ea.index(), ea.out()));
                context.defined.add(ea.out());
            }
            case Op.MapEntrySet mes -> {
                context.items.add(new RunItem.MapEntrySet(mes.map(), mes.out()));
                context.defined.add(mes.out());
            }
            case Op.MapEntryKey mek -> {
                context.items.add(new RunItem.MapEntryKey(mek.entry(), mek.out()));
                context.defined.add(mek.out());
            }
            case Op.MapEntryValue mev -> {
                context.items.add(new RunItem.MapEntryValue(mev.entry(), mev.out()));
                context.defined.add(mev.out());
            }
            case Op.ResultElementSet res -> {
                context.items.add(new RunItem.ResultElementSet(res.result(), res.index(), res.value()));
            }
            case Op.ArrayCreate ac -> {
                context.items.add(new RunItem.ArrayCreate(ac.size(), ac.out()));
                context.defined.add(ac.out());
            }
            case Op.ArraySet as -> {
                context.items.add(new RunItem.ArraySet(as.array(), as.index(), as.value()));
            }
            case Op.ListFinish lf -> {
                context.items.add(new RunItem.ListFinish(lf.array(), lf.out()));
                context.defined.add(lf.out());
            }
            case Op.MapFinish mf -> {
                context.items.add(new RunItem.MapFinish(mf.keys(), mf.values(), mf.size(), mf.out()));
                context.defined.add(mf.out());
            }
            case Op.Unbox unbox -> {
                context.items.add(new RunItem.Unbox(unbox.kind(), unbox.in(), unbox.out()));
                context.defined.add(unbox.out());
            }
            case Op.Box box -> {
                context.items.add(new RunItem.Box(box.kind(), box.in(), box.out()));
                context.defined.add(box.out());
            }
            case Op.EitherLeft el -> {
                context.items.add(new RunItem.EitherLeft(el.in(), el.out()));
                context.defined.add(el.out());
            }
            case Op.EitherRight er -> {
                context.items.add(new RunItem.EitherRight(er.in(), er.out()));
                context.defined.add(er.out());
            }
        }
    }

    private static void ensureDependencies(Context context, Value value) {
        Set<Local> used = new HashSet<>();
        collectLocals(value, used);
        for (Local local : used) {
            if (context.defined.contains(local)) {
                context.flush();
                break;
            }
        }
    }

    private static void collectLocals(Value value, Set<Local> used) {
        switch (value) {
            case Value.LocalValue lv -> used.add(lv.local());
            case Value.Add add -> {
                collectLocals(add.left(), used);
                collectLocals(add.right(), used);
            }
            case Value.ArrayLength arrayLength -> collectLocals(arrayLength.array(), used);
            case Value.CollectionSize collectionSize -> collectLocals(collectionSize.collection(), used);
            case Value.MapSize mapSize -> collectLocals(mapSize.map(), used);
            case Value.StringUtf8Bytes stringUtf8Bytes -> collectLocals(stringUtf8Bytes.string(), used);
            case Value.VarIntSize varIntSize -> collectLocals(varIntSize.intValue(), used);
            case Value.VarLongSize varLongSize -> collectLocals(varLongSize.longValue(), used);
            case Value.Ternary ternary -> {
                collectLocals(ternary.condition(), used);
                collectLocals(ternary.trueValue(), used);
                collectLocals(ternary.falseValue(), used);
            }
            case Value.IsNull isNull -> collectLocals(isNull.value(), used);
            case Value.IsNotNull isNotNull -> collectLocals(isNotNull.value(), used);
            case Value.Not not -> collectLocals(not.value(), used);
            case Value.IsLeft isLeft -> collectLocals(isLeft.value(), used);
            case Value.EitherLeft eitherLeft -> collectLocals(eitherLeft.value(), used);
            case Value.EitherRight eitherRight -> collectLocals(eitherRight.value(), used);
            case Value.BoolByte boolByte -> collectLocals(boolByte.booleanValue(), used);
            case Value.UnsignedByte unsignedByte -> collectLocals(unsignedByte.byteValue(), used);
            case Value.Mul mul -> {
                collectLocals(mul.left(), used);
                collectLocals(mul.right(), used);
            }
            case Value.ShiftLeft shiftLeft -> collectLocals(shiftLeft.value(), used);
            case Value.ShiftRightUnsigned shiftRightUnsigned -> collectLocals(shiftRightUnsigned.value(), used);
            case Value.And and -> {
                collectLocals(and.left(), used);
                collectLocals(and.right(), used);
            }
            case Value.Or or -> {
                collectLocals(or.left(), used);
                collectLocals(or.right(), used);
            }
            default -> {}
        }
    }

    private static final class Context {
        final List<RunIr> runs = new ArrayList<>();
        List<RunItem> items = new ArrayList<>();
        Value size = new Value.Const(0L);
        final Set<Local> defined = new HashSet<>();

        Value offset() {
            return new Value.Add(new Value.Index(), size);
        }

        void flush() {
            if (!items.isEmpty()) {
                runs.add(new RunIr(size, items));
                items = new ArrayList<>();
                size = new Value.Const(0L);
                defined.clear();
            }
        }
    }
}
