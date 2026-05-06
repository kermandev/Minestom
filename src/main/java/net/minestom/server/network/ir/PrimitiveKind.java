package net.minestom.server.network.ir;

public enum PrimitiveKind {
    BOOLEAN(StoreKind.BOOLEAN),
    BYTE(StoreKind.BYTE),
    UNSIGNED_BYTE(StoreKind.BYTE),
    SHORT(StoreKind.SHORT),
    UNSIGNED_SHORT(StoreKind.SHORT),
    INT(StoreKind.INT),
    UNSIGNED_INT(StoreKind.INT),
    LONG(StoreKind.LONG),
    FLOAT(StoreKind.FLOAT),
    DOUBLE(StoreKind.DOUBLE);

    private final StoreKind storeKind;

    PrimitiveKind(StoreKind storeKind) {
        this.storeKind = storeKind;
    }

    public StoreKind storeKind() {
        return storeKind;
    }

    public long byteSize() {
        return storeKind.byteSize();
    }
}
