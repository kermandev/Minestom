package net.minestom.server.network;

public interface NetworkContext {
    static Empty empty() {
        return Empty.INSTANCE;
    }


    enum Empty implements NetworkContext {
        INSTANCE
    }
}
