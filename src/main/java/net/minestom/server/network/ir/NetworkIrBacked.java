package net.minestom.server.network.ir;
import net.minestom.server.network.NetworkBuffer;

import org.jetbrains.annotations.UnknownNullability;

public interface NetworkIrBacked<T extends @UnknownNullability Object> extends NetworkBuffer.Type<T> {
    NetworkIr<T> ir();
}
