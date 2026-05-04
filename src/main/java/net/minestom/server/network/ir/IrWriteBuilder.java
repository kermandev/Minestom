package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import java.util.List;

public interface IrWriteBuilder {
    void push(Op op);

    Local source();

    void pushSource(Local source);

    void popSource();

    void lower(NetworkBuffer.Type<?> type, Value value);

    List<Op> buildNested(Runnable action);
}
