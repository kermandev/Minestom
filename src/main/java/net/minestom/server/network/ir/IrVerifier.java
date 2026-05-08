package net.minestom.server.network.ir;

import java.lang.classfile.TypeKind;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class IrVerifier {
    private IrVerifier() {
    }

    static void verifyWrite(ProgramIr program) {
        verify(program, ReturnMode.WRITE);
    }

    static void verifyRead(ProgramIr program) {
        verify(program, ReturnMode.READ);
    }

    private static void verify(ProgramIr program, ReturnMode mode) {
        final Set<Local> defined = new HashSet<>();
        if (program.initialSource() != null) {
            defined.add(program.initialSource());
            expectReferenceLocal(program.initialSource());
        }
        verifyRuns(program.runs(), defined);

        final int returnCount = countReturns(program.runs());
        switch (mode) {
            case WRITE -> {
                if (returnCount != 0) {
                    throw new IllegalStateException("Write IR must not contain return items");
                }
            }
            case READ -> {
                if (returnCount != 1) {
                    throw new IllegalStateException("Read IR must contain exactly one return item, got " + returnCount);
                }
            }
        }
    }

    private static void verifyRuns(List<RunIr> runs, Set<Local> defined) {
        for (RunIr run : runs) {
            expectLongLike(run.size(), defined);
            if (run.reserve() && run.size() instanceof Value.Const(Object value) && value instanceof Number n && n.longValue() < 0) {
                throw new IllegalStateException("Run size must be non-negative for barriers, got " + n.longValue());
            }
            for (RunItem item : run.items()) {
                verifyItem(item, defined);
            }
        }
    }

    private static void verifyItem(RunItem item, Set<Local> defined) {
        switch (item) {
            case RunItem.Put put -> {
                expectLongLike(put.offset(), defined);
                expectStoreWriteValue(put.kind(), put.value(), defined);
            }
            case RunItem.Get get -> {
                expectLongLike(get.offset(), defined);
                expectStoreReadLocal(get.kind(), get.out());
                define(defined, get.out());
            }
            case RunItem.PutVarInt put -> {
                expectLongLike(put.offset(), defined);
                expectIntCoercible(put.value(), defined);
                expectLongLike(put.encodedSize(), defined);
            }
            case RunItem.GetVarInt get -> {
                expectIntLikeLocal(get.out());
                define(defined, get.out());
            }
            case RunItem.PutVarLong put -> {
                expectLongLike(put.offset(), defined);
                expectLongLike(put.value(), defined);
                expectLongLike(put.encodedSize(), defined);
            }
            case RunItem.GetVarLong get -> {
                expectLocalStack(get.out(), IrStackType.LONG);
                define(defined, get.out());
            }
            case RunItem.PutBytes put -> {
                expectLongLike(put.offset(), defined);
                expectReference(put.byteArray(), defined);
                expectIntCoercible(put.length(), defined);
            }
            case RunItem.GetBytes get -> {
                expectLongLike(get.offset(), defined);
                expectReferenceLocal(get.byteArray());
                expectIntCoercible(get.length(), defined);
                define(defined, get.byteArray());
            }
            case RunItem.If branch -> {
                expectIntLike(branch.condition(), defined);
                final Set<Local> thenDefined = new HashSet<>(defined);
                final Set<Local> elseDefined = new HashSet<>(defined);
                verifyRuns(branch.thenRuns(), thenDefined);
                verifyRuns(branch.elseRuns(), elseDefined);
                thenDefined.retainAll(elseDefined);
                defined.addAll(thenDefined);
            }
            case RunItem.ForEach loop -> {
                expectReference(loop.source(), defined);
                expectReferenceLocal(loop.element());
                final Set<Local> bodyDefined = new HashSet<>(defined);
                define(bodyDefined, loop.element());
                verifyRuns(loop.body(), bodyDefined);
            }
            case RunItem.ForIndex loop -> {
                expectIntLocal(loop.index());
                expectIntCoercible(loop.start(), defined);
                expectIntCoercible(loop.end(), defined);
                final Set<Local> bodyDefined = new HashSet<>(defined);
                define(bodyDefined, loop.index());
                verifyRuns(loop.body(), bodyDefined);
            }
            case RunItem.Apply apply -> {
                verifyLocal(apply.in(), defined);
                expectReferenceLocal(apply.in());
                expectReferenceLocal(apply.out());
                define(defined, apply.out());
            }
            case RunItem.Cast cast -> {
                verifyLocal(cast.in(), defined);
                expectReferenceLocal(cast.in());
                expectReferenceLocal(cast.out());
                define(defined, cast.out());
            }
            case RunItem.Unbox unbox -> {
                verifyLocal(unbox.in(), defined);
                expectReferenceLocal(unbox.in());
                expectLocalStack(unbox.out(), IrStackType.ofPrimitiveKind(unbox.kind()));
                define(defined, unbox.out());
            }
            case RunItem.Box box -> {
                verifyLocal(box.in(), defined);
                expectLocalStack(box.in(), IrStackType.ofPrimitiveKind(box.kind()));
                expectReferenceLocal(box.out());
                define(defined, box.out());
            }
            case RunItem.StringToBytes stringToBytes -> {
                verifyLocal(stringToBytes.in(), defined);
                expectReferenceLocal(stringToBytes.in());
                expectReferenceLocal(stringToBytes.out());
                define(defined, stringToBytes.out());
            }
            case RunItem.BytesToString bytesToString -> {
                verifyLocal(bytesToString.in(), defined);
                expectReferenceLocal(bytesToString.in());
                expectReferenceLocal(bytesToString.out());
                define(defined, bytesToString.out());
            }
            case RunItem.EitherLeft eitherLeft -> {
                verifyLocal(eitherLeft.in(), defined);
                expectReferenceLocal(eitherLeft.in());
                expectReferenceLocal(eitherLeft.out());
                define(defined, eitherLeft.out());
            }
            case RunItem.EitherRight eitherRight -> {
                verifyLocal(eitherRight.in(), defined);
                expectReferenceLocal(eitherRight.in());
                expectReferenceLocal(eitherRight.out());
                define(defined, eitherRight.out());
            }
            case RunItem.Store store -> {
                expectAssignable(store.value(), store.out(), defined);
                define(defined, store.out());
            }
            case RunItem.Check check -> expectIntLike(check.condition(), defined);
            case RunItem.WriteExternal writeExternal -> expectReference(writeExternal.value(), defined);
            case RunItem.ReadExternal readExternal -> {
                expectReferenceLocal(readExternal.out());
                define(defined, readExternal.out());
            }
            case RunItem.ElementAt elementAt -> {
                expectReference(elementAt.source(), defined);
                expectIntCoercible(elementAt.index(), defined);
                expectReferenceLocal(elementAt.out());
                define(defined, elementAt.out());
            }
            case RunItem.MapEntrySet entrySet -> {
                expectReference(entrySet.map(), defined);
                expectReferenceLocal(entrySet.out());
                define(defined, entrySet.out());
            }
            case RunItem.MapEntryKey entryKey -> {
                verifyLocal(entryKey.entry(), defined);
                expectReferenceLocal(entryKey.entry());
                expectReferenceLocal(entryKey.out());
                define(defined, entryKey.out());
            }
            case RunItem.MapEntryValue entryValue -> {
                verifyLocal(entryValue.entry(), defined);
                expectReferenceLocal(entryValue.entry());
                expectReferenceLocal(entryValue.out());
                define(defined, entryValue.out());
            }
            case RunItem.ResultElementSet elementSet -> {
                expectReference(elementSet.result(), defined);
                expectIntCoercible(elementSet.index(), defined);
                expectReference(elementSet.value(), defined);
            }
            case RunItem.ArrayCreate arrayCreate -> {
                expectIntCoercible(arrayCreate.size(), defined);
                expectReferenceLocal(arrayCreate.out());
                define(defined, arrayCreate.out());
            }
            case RunItem.ArraySet arraySet -> {
                verifyLocal(arraySet.array(), defined);
                expectReferenceLocal(arraySet.array());
                expectIntCoercible(arraySet.index(), defined);
                expectReference(arraySet.value(), defined);
            }
            case RunItem.ListFinish listFinish -> {
                verifyLocal(listFinish.array(), defined);
                expectReferenceLocal(listFinish.array());
                expectReferenceLocal(listFinish.out());
                define(defined, listFinish.out());
            }
            case RunItem.MapFinish mapFinish -> {
                verifyLocal(mapFinish.keys(), defined);
                verifyLocal(mapFinish.values(), defined);
                expectReferenceLocal(mapFinish.keys());
                expectReferenceLocal(mapFinish.values());
                expectIntCoercible(mapFinish.size(), defined);
                expectReferenceLocal(mapFinish.out());
                define(defined, mapFinish.out());
            }
            case RunItem.Construct construct -> {
                for (Value arg : construct.args()) {
                    expectReference(arg, defined);
                }
                expectReferenceLocal(construct.out());
                define(defined, construct.out());
            }
            case RunItem.Return ret -> expectReference(ret.value(), defined);
        }
    }

    private static IrStackType verifyValue(Value value, Set<Local> defined) {
        switch (value) {
            case Value.LocalValue localValue -> verifyLocal(localValue.local(), defined);
            case Value.Const ignored -> {
            }
            case Value.IsNull isNull -> expectReference(isNull.value(), defined);
            case Value.IsNotNull isNotNull -> expectReference(isNotNull.value(), defined);
            case Value.Not not -> expectIntLike(not.value(), defined);
            case Value.IsLeft isLeft -> expectReference(isLeft.value(), defined);
            case Value.EitherLeft eitherLeft -> expectReference(eitherLeft.value(), defined);
            case Value.EitherRight eitherRight -> expectReference(eitherRight.value(), defined);
            case Value.Add add -> expectLongLikeValues(defined, add.left(), add.right());
            case Value.Mul mul -> expectLongLikeValues(defined, mul.left(), mul.right());
            case Value.And and -> expectLongLikeValues(defined, and.left(), and.right());
            case Value.Or or -> expectLongLikeValues(defined, or.left(), or.right());
            case Value.LessThanOrEqual lessThanOrEqual -> expectLongLikeValues(defined, lessThanOrEqual.left(), lessThanOrEqual.right());
            case Value.GreaterThan greaterThan -> expectLongLikeValues(defined, greaterThan.left(), greaterThan.right());
            case Value.ShiftLeft shiftLeft -> expectLongLike(shiftLeft.value(), defined);
            case Value.ShiftRightUnsigned shiftRightUnsigned -> expectLongLike(shiftRightUnsigned.value(), defined);
            case Value.BoolByte boolByte -> expectIntLike(boolByte.booleanValue(), defined);
            case Value.UnsignedByte unsignedByte -> expectIntLike(unsignedByte.byteValue(), defined);
            case Value.VarIntSize varIntSize -> expectIntCoercible(varIntSize.intValue(), defined);
            case Value.VarLongSize varLongSize -> expectLongLike(varLongSize.longValue(), defined);
            case Value.ArrayLength arrayLength -> expectReference(arrayLength.array(), defined);
            case Value.CollectionSize collectionSize -> expectReference(collectionSize.collection(), defined);
            case Value.MapSize mapSize -> expectReference(mapSize.map(), defined);
            case Value.MapEntryValue mapEntryValue -> {
                verifyLocal(mapEntryValue.entry(), defined);
                expectReferenceLocal(mapEntryValue.entry());
            }
            case Value.StringUtf8Bytes stringUtf8Bytes -> expectReference(stringUtf8Bytes.string(), defined);
            case Value.Ternary ternary -> {
                expectIntLike(ternary.condition(), defined);
                final IrStackType trueType = verifyValue(ternary.trueValue(), defined);
                final IrStackType falseType = verifyValue(ternary.falseValue(), defined);
                if (trueType != falseType) {
                    throw new IllegalStateException("Ternary branch stack types differ: " + trueType + " and " + falseType);
                }
            }
            case Value.Index ignored -> {
            }
        }
        return IrStackType.ofValue(value);
    }

    private static void expectLongLikeValues(Set<Local> defined, Value... values) {
        for (Value value : values) {
            expectLongLike(value, defined);
        }
    }

    private static void expectAssignable(Value value, Local target, Set<Local> defined) {
        final IrStackType valueType = verifyValue(value, defined);
        final IrStackType targetType = IrStackType.ofLocal(target);
        if (valueType != targetType) {
            throw new IllegalStateException("Cannot store " + valueType + " value into " + targetType + " local: " + target);
        }
    }

    private static void expectReference(Value value, Set<Local> defined) {
        expectStack(value, defined, IrStackType.REFERENCE);
    }

    private static void expectIntLike(Value value, Set<Local> defined) {
        final IrStackType type = verifyValue(value, defined);
        if (!type.isIntLike()) {
            throw new IllegalStateException("Expected int-like value, got " + type + ": " + value);
        }
    }

    private static void expectIntCoercible(Value value, Set<Local> defined) {
        final IrStackType type = verifyValue(value, defined);
        if (!type.isLongLike()) {
            throw new IllegalStateException("Expected int-coercible value, got " + type + ": " + value);
        }
    }

    private static void expectLongLike(Value value, Set<Local> defined) {
        final IrStackType type = verifyValue(value, defined);
        if (!type.isLongLike()) {
            throw new IllegalStateException("Expected long-like value, got " + type + ": " + value);
        }
    }

    private static void expectStack(Value value, Set<Local> defined, IrStackType expected) {
        final IrStackType actual = verifyValue(value, defined);
        if (actual != expected) {
            throw new IllegalStateException("Expected " + expected + " value, got " + actual + ": " + value);
        }
    }

    private static void expectStoreWriteValue(StoreKind kind, Value value, Set<Local> defined) {
        final IrStackType actual = verifyValue(value, defined);
        final boolean valid = switch (kind) {
            case BOOLEAN, BYTE, SHORT, INT -> actual.isLongLike();
            case LONG -> actual == IrStackType.LONG;
            case FLOAT -> actual == IrStackType.FLOAT;
            case DOUBLE -> actual == IrStackType.DOUBLE;
        };
        if (!valid) {
            throw new IllegalStateException("Cannot write " + actual + " value as " + kind + ": " + value);
        }
    }

    private static void expectStoreReadLocal(StoreKind kind, Local out) {
        final IrStackType target = IrStackType.ofLocal(out);
        final boolean valid = switch (kind) {
            case BOOLEAN, BYTE, SHORT -> target == IrStackType.INT;
            case INT -> target == IrStackType.INT || target == IrStackType.LONG;
            case LONG -> target == IrStackType.LONG;
            case FLOAT -> target == IrStackType.FLOAT;
            case DOUBLE -> target == IrStackType.DOUBLE;
        };
        if (!valid) {
            throw new IllegalStateException("Cannot read " + kind + " into " + target + " local: " + out);
        }
    }

    private static void expectReferenceLocal(Local local) {
        expectLocalStack(local, IrStackType.REFERENCE);
    }

    private static void expectIntLikeLocal(Local local) {
        final IrStackType actual = IrStackType.ofLocal(local);
        if (!actual.isIntLike()) {
            throw new IllegalStateException("Expected int-like local, got " + actual + ": " + local);
        }
    }

    private static void expectIntLocal(Local local) {
        if (!(local.type() instanceof LocalType.Kind kind) || kind.kind() != TypeKind.INT) {
            throw new IllegalStateException("Expected int local, got " + local);
        }
    }

    private static void expectLocalStack(Local local, IrStackType expected) {
        final IrStackType actual = IrStackType.ofLocal(local);
        if (actual != expected) {
            throw new IllegalStateException("Expected " + expected + " local, got " + actual + ": " + local);
        }
    }

    private static int countReturns(List<RunIr> runs) {
        int count = 0;
        for (RunIr run : runs) {
            for (RunItem item : run.items()) {
                count += countReturns(item);
            }
        }
        return count;
    }

    private static int countReturns(RunItem item) {
        return switch (item) {
            case RunItem.Return _ -> 1;
            case RunItem.If branch -> countReturns(branch.thenRuns()) + countReturns(branch.elseRuns());
            case RunItem.ForEach loop -> countReturns(loop.body());
            case RunItem.ForIndex loop -> countReturns(loop.body());
            default -> 0;
        };
    }

    private static void define(Set<Local> defined, Local local) {
        defined.add(local);
    }

    private static void verifyLocal(Local local, Set<Local> defined) {
        if (!defined.contains(local)) {
            throw new IllegalStateException("IR local used before definition: " + local);
        }
    }

    private enum ReturnMode {
        WRITE,
        READ
    }
}
