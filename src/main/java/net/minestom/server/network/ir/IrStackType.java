package net.minestom.server.network.ir;

import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.Nullable;

import java.lang.classfile.TypeKind;
import java.lang.constant.ConstantDesc;

enum IrStackType {
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    REFERENCE;

    boolean isIntLike() {
        return this == INT;
    }

    boolean isLongLike() {
        return this == INT || this == LONG;
    }

    static IrStackType ofLocal(Local local) {
        return ofLocalType(local.type());
    }

    static IrStackType ofLocalType(LocalType type) {
        return switch (type) {
            case LocalType.Kind kind -> ofTypeKind(kind.kind());
            case LocalType.Reference _ -> REFERENCE;
        };
    }

    static IrStackType ofStoreKind(StoreKind kind) {
        return switch (kind) {
            case BOOLEAN, BYTE, SHORT, INT -> INT;
            case LONG -> LONG;
            case FLOAT -> FLOAT;
            case DOUBLE -> DOUBLE;
        };
    }

    static IrStackType ofPrimitiveKind(PrimitiveKind kind) {
        return ofStoreKind(kind.storeKind());
    }

    static IrStackType ofValue(Value value) {
        return switch (value) {
            case Value.LocalValue localValue -> ofLocal(localValue.local());
            case Value.Const constant -> ofConstant(constant.value());
            case Value.Index _ -> LONG;
            case Value.IsNull _, Value.IsNotNull _, Value.Not _, Value.IsLeft _, Value.BoolByte _,
                 Value.UnsignedByte _, Value.LessThanOrEqual _, Value.GreaterThan _, Value.ArrayLength _,
                 Value.CollectionSize _, Value.MapSize _, Value.StringUtf8Bytes _, Value.VarIntSize _,
                 Value.VarLongSize _ -> INT;
            case Value.EitherLeft _, Value.EitherRight _ -> REFERENCE;
            case Value.Add _, Value.Mul _, Value.ShiftLeft _, Value.ShiftRightUnsigned _, Value.And _, Value.Or _ ->
                    LONG;
            case Value.MapEntryValue _, Value.Ternary _ ->
                    throw new IllegalStateException("Unsupported emitted IR value: " + value);
        };
    }

    private static IrStackType ofTypeKind(TypeKind kind) {
        return switch (kind) {
            case BOOLEAN, BYTE, SHORT, CHAR, INT -> INT;
            case LONG -> LONG;
            case FLOAT -> FLOAT;
            case DOUBLE -> DOUBLE;
            case REFERENCE -> REFERENCE;
            default -> throw new IllegalArgumentException("Unsupported local type kind: " + kind);
        };
    }

    private static IrStackType ofConstant(@Nullable Object value) {
        return switch (value) {
            case Boolean _, Byte _, Short _, Integer _ -> INT;
            case Long _ -> LONG;
            case Float _ -> FLOAT;
            case Double _ -> DOUBLE;
            case null, default -> REFERENCE;
        };
    }
}
