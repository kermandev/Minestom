package net.minestom.server.network.ir;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class IrVerifier {
    private IrVerifier() {
    }

    static void verify(ProgramIr program) {
        final Set<Local> defined = new HashSet<>();
        if (program.initialSource() != null) {
            defined.add(program.initialSource());
        }
        verifyRuns(program.runs(), defined);
    }

    private static void verifyRuns(List<RunIr> runs, Set<Local> defined) {
        for (RunIr run : runs) {
            verifyValue(run.size(), defined);
            for (RunItem item : run.items()) {
                verifyItem(item, defined);
            }
        }
    }

    private static void verifyItem(RunItem item, Set<Local> defined) {
        switch (item) {
            case RunItem.Put put -> {
                verifyValue(put.offset(), defined);
                verifyValue(put.value(), defined);
            }
            case RunItem.Get get -> {
                verifyValue(get.offset(), defined);
                define(defined, get.out());
            }
            case RunItem.PutVarInt put -> {
                verifyValue(put.offset(), defined);
                verifyValue(put.value(), defined);
                verifyValue(put.encodedSize(), defined);
            }
            case RunItem.GetVarInt get -> define(defined, get.out());
            case RunItem.PutVarLong put -> {
                verifyValue(put.offset(), defined);
                verifyValue(put.value(), defined);
                verifyValue(put.encodedSize(), defined);
            }
            case RunItem.GetVarLong get -> define(defined, get.out());
            case RunItem.PutBytes put -> {
                verifyValue(put.offset(), defined);
                verifyValue(put.byteArray(), defined);
                verifyValue(put.length(), defined);
            }
            case RunItem.GetBytes get -> {
                verifyValue(get.offset(), defined);
                verifyValue(get.length(), defined);
                define(defined, get.byteArray());
            }
            case RunItem.If branch -> {
                verifyValue(branch.condition(), defined);
                final Set<Local> thenDefined = new HashSet<>(defined);
                final Set<Local> elseDefined = new HashSet<>(defined);
                verifyRuns(branch.thenRuns(), thenDefined);
                verifyRuns(branch.elseRuns(), elseDefined);
                thenDefined.retainAll(elseDefined);
                defined.addAll(thenDefined);
            }
            case RunItem.ForEach loop -> {
                verifyValue(loop.source(), defined);
                final Set<Local> bodyDefined = new HashSet<>(defined);
                define(bodyDefined, loop.element());
                verifyRuns(loop.body(), bodyDefined);
            }
            case RunItem.ForIndex loop -> {
                verifyValue(loop.start(), defined);
                verifyValue(loop.end(), defined);
                final Set<Local> bodyDefined = new HashSet<>(defined);
                define(bodyDefined, loop.index());
                verifyRuns(loop.body(), bodyDefined);
            }
            case RunItem.Apply apply -> {
                verifyLocal(apply.in(), defined);
                define(defined, apply.out());
            }
            case RunItem.Cast cast -> {
                verifyLocal(cast.in(), defined);
                define(defined, cast.out());
            }
            case RunItem.Unbox unbox -> {
                verifyLocal(unbox.in(), defined);
                define(defined, unbox.out());
            }
            case RunItem.Box box -> {
                verifyLocal(box.in(), defined);
                define(defined, box.out());
            }
            case RunItem.StringToBytes stringToBytes -> {
                verifyLocal(stringToBytes.in(), defined);
                define(defined, stringToBytes.out());
            }
            case RunItem.BytesToString bytesToString -> {
                verifyLocal(bytesToString.in(), defined);
                define(defined, bytesToString.out());
            }
            case RunItem.EitherLeft eitherLeft -> {
                verifyLocal(eitherLeft.in(), defined);
                define(defined, eitherLeft.out());
            }
            case RunItem.EitherRight eitherRight -> {
                verifyLocal(eitherRight.in(), defined);
                define(defined, eitherRight.out());
            }
            case RunItem.Store store -> {
                verifyValue(store.value(), defined);
                define(defined, store.out());
            }
            case RunItem.Check check -> verifyValue(check.condition(), defined);
            case RunItem.WriteExternal writeExternal -> verifyValue(writeExternal.value(), defined);
            case RunItem.ReadExternal readExternal -> define(defined, readExternal.out());
            case RunItem.ElementAt elementAt -> {
                verifyValue(elementAt.source(), defined);
                verifyValue(elementAt.index(), defined);
                define(defined, elementAt.out());
            }
            case RunItem.MapEntrySet entrySet -> {
                verifyValue(entrySet.map(), defined);
                define(defined, entrySet.out());
            }
            case RunItem.MapEntryKey entryKey -> {
                verifyLocal(entryKey.entry(), defined);
                define(defined, entryKey.out());
            }
            case RunItem.MapEntryValue entryValue -> {
                verifyLocal(entryValue.entry(), defined);
                define(defined, entryValue.out());
            }
            case RunItem.ResultElementSet elementSet -> {
                verifyValue(elementSet.result(), defined);
                verifyValue(elementSet.index(), defined);
                verifyValue(elementSet.value(), defined);
            }
            case RunItem.ArrayCreate arrayCreate -> {
                verifyValue(arrayCreate.size(), defined);
                define(defined, arrayCreate.out());
            }
            case RunItem.ArraySet arraySet -> {
                verifyLocal(arraySet.array(), defined);
                verifyValue(arraySet.index(), defined);
                verifyValue(arraySet.value(), defined);
            }
            case RunItem.ListFinish listFinish -> {
                verifyLocal(listFinish.array(), defined);
                define(defined, listFinish.out());
            }
            case RunItem.MapFinish mapFinish -> {
                verifyLocal(mapFinish.keys(), defined);
                verifyLocal(mapFinish.values(), defined);
                verifyValue(mapFinish.size(), defined);
                define(defined, mapFinish.out());
            }
            case RunItem.Construct construct -> {
                for (Value arg : construct.args()) {
                    verifyValue(arg, defined);
                }
                define(defined, construct.out());
            }
            case RunItem.Return ret -> verifyValue(ret.value(), defined);
        }
    }

    private static void verifyValue(Value value, Set<Local> defined) {
        switch (value) {
            case Value.LocalValue localValue -> verifyLocal(localValue.local(), defined);
            case Value.Const ignored -> {
            }
            case Value.IsNull isNull -> verifyValue(isNull.value(), defined);
            case Value.IsNotNull isNotNull -> verifyValue(isNotNull.value(), defined);
            case Value.Not not -> verifyValue(not.value(), defined);
            case Value.IsLeft isLeft -> verifyValue(isLeft.value(), defined);
            case Value.EitherLeft eitherLeft -> verifyValue(eitherLeft.value(), defined);
            case Value.EitherRight eitherRight -> verifyValue(eitherRight.value(), defined);
            case Value.Add add -> verifyValues(defined, add.left(), add.right());
            case Value.Mul mul -> verifyValues(defined, mul.left(), mul.right());
            case Value.And and -> verifyValues(defined, and.left(), and.right());
            case Value.Or or -> verifyValues(defined, or.left(), or.right());
            case Value.LessThanOrEqual lessThanOrEqual -> verifyValues(defined, lessThanOrEqual.left(), lessThanOrEqual.right());
            case Value.GreaterThan greaterThan -> verifyValues(defined, greaterThan.left(), greaterThan.right());
            case Value.ShiftLeft shiftLeft -> verifyValue(shiftLeft.value(), defined);
            case Value.ShiftRightUnsigned shiftRightUnsigned -> verifyValue(shiftRightUnsigned.value(), defined);
            case Value.BoolByte boolByte -> verifyValue(boolByte.booleanValue(), defined);
            case Value.UnsignedByte unsignedByte -> verifyValue(unsignedByte.byteValue(), defined);
            case Value.VarIntSize varIntSize -> verifyValue(varIntSize.intValue(), defined);
            case Value.VarLongSize varLongSize -> verifyValue(varLongSize.longValue(), defined);
            case Value.ArrayLength arrayLength -> verifyValue(arrayLength.array(), defined);
            case Value.CollectionSize collectionSize -> verifyValue(collectionSize.collection(), defined);
            case Value.MapSize mapSize -> verifyValue(mapSize.map(), defined);
            case Value.MapEntryValue mapEntryValue -> {
                verifyLocal(mapEntryValue.entry(), defined);
            }
            case Value.StringUtf8Bytes stringUtf8Bytes -> verifyValue(stringUtf8Bytes.string(), defined);
            case Value.Ternary ternary -> verifyValues(defined, ternary.condition(), ternary.trueValue(), ternary.falseValue());
            case Value.Index ignored -> {
            }
        }
    }

    private static void verifyValues(Set<Local> defined, Value... values) {
        for (Value value : values) {
            verifyValue(value, defined);
        }
    }

    private static void define(Set<Local> defined, Local local) {
        defined.add(local);
    }

    private static void verifyLocal(Local local, Set<Local> defined) {
        if (!defined.contains(local)) {
            throw new IllegalStateException("IR local used before definition: " + local);
        }
    }
}
