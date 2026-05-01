package net.minestom.server.network;

public enum StoreKind {
    BYTE(1), BOOLEAN(1), SHORT(2), INT(4), LONG(8), FLOAT(4), DOUBLE(8);

    private final int byteSize;

    StoreKind(int byteSize) {
        this.byteSize = byteSize;
    }

    public int byteSize() {
        return byteSize;
    }
}
