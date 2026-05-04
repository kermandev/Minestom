package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import java.util.function.Function;

public interface NetworkIrIntrinsic {
    void lowerWrite(IrWriteBuilder builder);

    Value lowerRead(IrReadBuilder builder);

    default void collectMetadata(MetadataContext context) {
    }

    interface MetadataContext {
        void child(String suffix, NetworkBuffer.Type<?> type);

        void transform(String name, Function<?, ?> function);
    }
}
