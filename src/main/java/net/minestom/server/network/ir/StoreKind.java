package net.minestom.server.network.ir;

public enum StoreKind {
    BYTE(Byte.BYTES),
    BOOLEAN(Byte.BYTES),
    SHORT(Short.BYTES),
    INT(Integer.BYTES),
    LONG(Long.BYTES),
    FLOAT(Float.BYTES),
    DOUBLE(Double.BYTES);

    private final int byteSize;

    StoreKind(int byteSize) {
        this.byteSize = byteSize;
    }

    public int byteSize() {
        return byteSize;
    }
}
