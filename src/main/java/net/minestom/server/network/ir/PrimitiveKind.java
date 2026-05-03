package net.minestom.server.network.ir;

import java.lang.classfile.TypeKind;

public enum PrimitiveKind {
    BOOLEAN(TypeKind.BOOLEAN, StoreKind.BOOLEAN, Boolean.class),
    BYTE(TypeKind.BYTE, StoreKind.BYTE, Byte.class),
    UNSIGNED_BYTE(TypeKind.INT, StoreKind.BYTE, Short.class),
    SHORT(TypeKind.SHORT, StoreKind.SHORT, Short.class),
    UNSIGNED_SHORT(TypeKind.INT, StoreKind.SHORT, Integer.class),
    INT(TypeKind.INT, StoreKind.INT, Integer.class),
    UNSIGNED_INT(TypeKind.LONG, StoreKind.INT, Long.class),
    LONG(TypeKind.LONG, StoreKind.LONG, Long.class),
    FLOAT(TypeKind.FLOAT, StoreKind.FLOAT, Float.class),
    DOUBLE(TypeKind.DOUBLE, StoreKind.DOUBLE, Double.class);

    private final TypeKind localKind;
    private final StoreKind storeKind;
    private final Class<?> wrapperClass;

    PrimitiveKind(TypeKind localKind, StoreKind storeKind, Class<?> wrapperClass) {
        this.localKind = localKind;
        this.storeKind = storeKind;
        this.wrapperClass = wrapperClass;
    }

    public TypeKind localKind() {
        return localKind;
    }

    public StoreKind storeKind() {
        return storeKind;
    }

    public Class<?> wrapperClass() {
        return wrapperClass;
    }

    public String unboxMethod() {
        return switch (this) {
            case BOOLEAN -> "booleanValue";
            case BYTE -> "byteValue";
            case UNSIGNED_BYTE, SHORT -> "shortValue";
            case UNSIGNED_SHORT, INT -> "intValue";
            case UNSIGNED_INT, LONG -> "longValue";
            case FLOAT -> "floatValue";
            case DOUBLE -> "doubleValue";
        };
    }

    public TypeKind unboxReturnKind() {
        return switch (this) {
            case BOOLEAN -> TypeKind.BOOLEAN;
            case BYTE -> TypeKind.BYTE;
            case UNSIGNED_BYTE, SHORT -> TypeKind.SHORT;
            case UNSIGNED_SHORT, INT -> TypeKind.INT;
            case UNSIGNED_INT, LONG -> TypeKind.LONG;
            case FLOAT -> TypeKind.FLOAT;
            case DOUBLE -> TypeKind.DOUBLE;
        };
    }
}
