package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import java.util.List;

public interface IrReadBuilder {
    void push(Op op);

    Value lower(NetworkBuffer.Type<?> type);

    List<Op> buildNested(Runnable action);
}
