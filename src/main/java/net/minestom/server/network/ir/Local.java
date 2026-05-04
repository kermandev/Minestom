package net.minestom.server.network.ir;

public final class Local {
    private final LocalType type;

    public Local(LocalType type) {
        this.type = type;
    }

    public LocalType type() {
        return type;
    }
}
