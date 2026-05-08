package net.minestom.server.network.ir;

import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.Nullable;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodTypeDesc;

import static net.minestom.server.network.ir.IrEmitter.*;
import static net.minestom.server.network.ir.IrLocalAccess.emitLoadLocal;

final class IrValueEmitter {
    private IrValueEmitter() {
    }

    static void emitValue(CodeBuilder codeBuilder, IrEmitter.EmitContext context, Value value) {
        switch (value) {
            case Value.LocalValue localValue -> emitLoadLocal(codeBuilder, context, localValue.local());
            case Value.Const constant -> emitConstant(codeBuilder, constant.value());
            case Value.Index index -> codeBuilder.lload(context.indexSlot());
            case Value.IsNull isNull -> {
                emitValue(codeBuilder, context, isNull.value());
                final Label isNullLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                codeBuilder.ifnull(isNullLabel);
                codeBuilder.iconst_0();
                codeBuilder.goto_(endLabel);
                codeBuilder.labelBinding(isNullLabel);
                codeBuilder.iconst_1();
                codeBuilder.labelBinding(endLabel);
            }
            case Value.IsNotNull isNotNull -> {
                emitValue(codeBuilder, context, isNotNull.value());
                final Label isNotNullLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                codeBuilder.ifnonnull(isNotNullLabel);
                codeBuilder.iconst_0();
                codeBuilder.goto_(endLabel);
                codeBuilder.labelBinding(isNotNullLabel);
                codeBuilder.iconst_1();
                codeBuilder.labelBinding(endLabel);
            }
            case Value.Not not -> {
                emitValue(codeBuilder, context, not.value());
                final Label isZero = codeBuilder.newLabel();
                final Label end = codeBuilder.newLabel();
                codeBuilder.ifeq(isZero);
                codeBuilder.iconst_0();
                codeBuilder.goto_(end);
                codeBuilder.labelBinding(isZero);
                codeBuilder.iconst_1();
                codeBuilder.labelBinding(end);
            }
            case Value.IsLeft isLeft -> {
                emitValue(codeBuilder, context, isLeft.value());
                codeBuilder.instanceOf(CD_EITHER_LEFT);
            }
            case Value.EitherLeft eitherLeft -> {
                emitValue(codeBuilder, context, eitherLeft.value());
                codeBuilder.checkcast(CD_EITHER_LEFT);
                codeBuilder.invokevirtual(CD_EITHER_LEFT, "value", MethodTypeDesc.of(CD_OBJECT));
            }
            case Value.EitherRight eitherRight -> {
                emitValue(codeBuilder, context, eitherRight.value());
                codeBuilder.checkcast(CD_EITHER_RIGHT);
                codeBuilder.invokevirtual(CD_EITHER_RIGHT, "value", MethodTypeDesc.of(CD_OBJECT));
            }
            case Value.BoolByte boolByte -> emitValue(codeBuilder, context, boolByte.booleanValue());
            case Value.UnsignedByte unsignedByte -> {
                emitValue(codeBuilder, context, unsignedByte.byteValue());
                codeBuilder.sipush(0xFF)
                        .iand();
            }
            case Value.LessThanOrEqual cmp -> {
                emitLongValue(codeBuilder, context, cmp.left());
                emitLongValue(codeBuilder, context, cmp.right());
                final Label trueLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                codeBuilder.lcmp()
                        .ifgt(trueLabel)
                        .iconst_1()
                        .goto_(endLabel)
                        .labelBinding(trueLabel)
                        .iconst_0()
                        .labelBinding(endLabel);
            }
            case Value.GreaterThan cmp -> {
                emitLongValue(codeBuilder, context, cmp.left());
                emitLongValue(codeBuilder, context, cmp.right());
                final Label trueLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                codeBuilder.lcmp()
                        .ifle(trueLabel)
                        .iconst_1()
                        .goto_(endLabel)
                        .labelBinding(trueLabel)
                        .iconst_0()
                        .labelBinding(endLabel);
            }
            case Value.ArrayLength arrayLength -> {
                emitValue(codeBuilder, context, arrayLength.array());
                codeBuilder.arraylength();
            }
            case Value.CollectionSize collectionSize -> {
                emitValue(codeBuilder, context, collectionSize.collection());
                codeBuilder.checkcast(CD_COLLECTION);
                codeBuilder.invokeinterface(CD_COLLECTION, "size", MethodTypeDesc.of(CD_INT));
            }
            case Value.MapSize mapSize -> {
                emitValue(codeBuilder, context, mapSize.map());
                codeBuilder.checkcast(CD_MAP);
                codeBuilder.invokeinterface(CD_MAP, "size", MethodTypeDesc.of(CD_INT));
            }
            case Value.StringUtf8Bytes stringUtf8Bytes -> {
                emitValue(codeBuilder, context, stringUtf8Bytes.string());
                codeBuilder.getstatic(CD_STANDARD_CHARSETS, "UTF_8", CD_CHARSET);
                codeBuilder.invokevirtual(CD_STRING, "getBytes", MethodTypeDesc.of(CD_BYTE_ARRAY, CD_CHARSET));
                codeBuilder.arraylength();
            }
            case Value.VarIntSize varIntSize -> {
                emitIntValue(codeBuilder, context, varIntSize.intValue());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varIntSize", MT_VAR_INT_SIZE, true);
            }
            case Value.VarLongSize varLongSize -> {
                emitLongValue(codeBuilder, context, varLongSize.longValue());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varLongSize", MT_VAR_LONG_SIZE, true);
            }
            case Value.Add add -> {
                emitLongValue(codeBuilder, context, add.left());
                emitLongValue(codeBuilder, context, add.right());
                codeBuilder.ladd();
            }
            case Value.Mul mul -> {
                emitLongValue(codeBuilder, context, mul.left());
                emitLongValue(codeBuilder, context, mul.right());
                codeBuilder.lmul();
            }
            case Value.ShiftLeft shift -> {
                emitLongValue(codeBuilder, context, shift.value());
                codeBuilder.loadConstant(shift.amount());
                codeBuilder.lshl();
            }
            case Value.ShiftRightUnsigned shift -> {
                emitLongValue(codeBuilder, context, shift.value());
                codeBuilder.loadConstant(shift.amount());
                codeBuilder.lushr();
            }
            case Value.And and -> {
                emitLongValue(codeBuilder, context, and.left());
                emitLongValue(codeBuilder, context, and.right());
                codeBuilder.land();
            }
            case Value.Or or -> {
                emitLongValue(codeBuilder, context, or.left());
                emitLongValue(codeBuilder, context, or.right());
                codeBuilder.lor();
            }
            default -> throw new UnsupportedOperationException("Unsupported IR value: " + value);
        }
    }

    static void emitIntValue(CodeBuilder codeBuilder, IrEmitter.EmitContext context, Value value) {
        switch (value) {
            case Value.LocalValue localValue -> {
                emitLoadLocal(codeBuilder, context, localValue.local());
                switch (IrStackType.ofLocal(localValue.local())) {
                    case INT -> {
                    }
                    case LONG -> codeBuilder.l2i();
                    default -> throw new IllegalArgumentException("Expected int-compatible local, got " + localValue.local());
                }
            }
            case Value.Const constant -> {
                expectLongLike(value);
                codeBuilder.loadConstant(((Number) constant.value()).intValue());
            }
            case Value.ArrayLength arrayLength -> {
                emitValue(codeBuilder, context, arrayLength.array());
                codeBuilder.arraylength();
            }
            case Value.VarIntSize varIntSize -> {
                emitIntValue(codeBuilder, context, varIntSize.intValue());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varIntSize", MT_VAR_INT_SIZE, true);
            }
            default -> {
                emitValue(codeBuilder, context, value);
                switch (IrStackType.ofValue(value)) {
                    case INT -> {
                    }
                    case LONG -> codeBuilder.l2i();
                    default -> throw new IllegalArgumentException("Expected int-compatible value, got " + value);
                }
            }
        }
    }

    static void emitLongValue(CodeBuilder codeBuilder, IrEmitter.EmitContext context, Value value) {
        switch (value) {
            case Value.LocalValue localValue -> {
                emitLoadLocal(codeBuilder, context, localValue.local());
                switch (IrStackType.ofLocal(localValue.local())) {
                    case INT -> codeBuilder.i2l();
                    case LONG -> {
                    }
                    default -> throw new IllegalArgumentException("Expected long-compatible local, got " + localValue.local());
                }
            }
            case Value.Const constant -> {
                expectLongLike(value);
                codeBuilder.loadConstant(((Number) constant.value()).longValue());
            }
            default -> {
                emitValue(codeBuilder, context, value);
                switch (IrStackType.ofValue(value)) {
                    case INT -> codeBuilder.i2l();
                    case LONG -> {
                    }
                    default -> throw new IllegalArgumentException("Expected long-compatible value, got " + value);
                }
            }
        }
    }

    static boolean emitsLong(Value value) {
        return IrStackType.ofValue(value) == IrStackType.LONG;
    }

    private static void expectLongLike(Value value) {
        if (!IrStackType.ofValue(value).isLongLike()) {
            throw new IllegalArgumentException("Expected integer-compatible value, got " + value);
        }
    }

    private static void emitConstant(CodeBuilder codeBuilder, @Nullable Object value) {
        if (value == null) {
            codeBuilder.aconst_null();
        } else if (value instanceof Boolean booleanValue) {
            if (booleanValue) codeBuilder.iconst_1();
            else codeBuilder.iconst_0();
        } else if (value == Unit.INSTANCE) {
            codeBuilder.getstatic(CD_UNIT, "INSTANCE", CD_UNIT);
        } else if (value instanceof ConstantDesc constantDesc) {
            codeBuilder.loadConstant(constantDesc);
        }
    }

    static void emitBoxedTypeIrValue(CodeBuilder codeBuilder, PrimitiveKind kind) {
        switch (kind) {
            case BOOLEAN -> codeBuilder.invokestatic(CD_BOOLEAN_WRAPPER, "valueOf", MT_BOX_BOOLEAN);
            case BYTE -> codeBuilder.i2b()
                    .invokestatic(CD_BYTE_WRAPPER, "valueOf", MT_BOX_BYTE);
            case UNSIGNED_BYTE, SHORT -> codeBuilder.i2s()
                    .invokestatic(CD_SHORT_WRAPPER, "valueOf", MT_BOX_SHORT);
            case UNSIGNED_SHORT, INT -> codeBuilder.invokestatic(CD_INTEGER_WRAPPER, "valueOf", MT_BOX_INT);
            case UNSIGNED_INT, LONG -> codeBuilder.invokestatic(CD_LONG_WRAPPER, "valueOf", MT_BOX_LONG);
            case FLOAT -> codeBuilder.invokestatic(CD_FLOAT_WRAPPER, "valueOf", MT_BOX_FLOAT);
            case DOUBLE -> codeBuilder.invokestatic(CD_DOUBLE_WRAPPER, "valueOf", MT_BOX_DOUBLE);
        }
    }

    static void emitUnboxedTypeIrValue(CodeBuilder codeBuilder, PrimitiveKind kind) {
        switch (kind) {
            case BOOLEAN -> codeBuilder.invokevirtual(CD_BOOLEAN_WRAPPER, "booleanValue", MT_BOOLEAN_VALUE);
            case BYTE -> codeBuilder.invokevirtual(CD_BYTE_WRAPPER, "byteValue", MT_BYTE_VALUE);
            case UNSIGNED_BYTE -> codeBuilder.invokevirtual(CD_SHORT_WRAPPER, "shortValue", MT_SHORT_VALUE)
                    .sipush(0xFF)
                    .iand();
            case SHORT -> codeBuilder.invokevirtual(CD_SHORT_WRAPPER, "shortValue", MT_SHORT_VALUE);
            case UNSIGNED_SHORT -> codeBuilder.invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE)
                    .loadConstant(0xFFFF)
                    .iand();
            case INT -> codeBuilder.invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE);
            case UNSIGNED_INT -> codeBuilder.invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE)
                    .loadConstant(0xFFFFFFFFL)
                    .land();
            case LONG -> codeBuilder.invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE);
            case FLOAT -> codeBuilder.invokevirtual(CD_FLOAT_WRAPPER, "floatValue", MT_FLOAT_VALUE);
            case DOUBLE -> codeBuilder.invokevirtual(CD_DOUBLE_WRAPPER, "doubleValue", MT_DOUBLE_VALUE);
        }
    }
}
