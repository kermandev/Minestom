package net.minestom.server.network.packet;

import net.minestom.server.ServerFlag;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferPool;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import net.minestom.server.registry.Registries;

/**
 * Tools to write packets into a {@link NetworkBuffer} for network processing.
 * <p>
 * Fairly internal and performance sensitive.
 */
@ApiStatus.Internal
public final class PacketWriting {
    public static void writeFramedPacket(NetworkBufferPool pool,
                                         NetworkBuffer buffer,
                                         ConnectionState state,
                                         ClientPacket packet,
                                         int compressionThreshold) throws IndexOutOfBoundsException {
        writeFramedPacket(pool, buffer, PacketVanilla.CLIENT_PACKET_PARSER, state, packet, compressionThreshold);
    }

    public static void writeFramedPacket(NetworkBufferPool pool,
                                         NetworkBuffer buffer,
                                         ConnectionState state,
                                         ServerPacket packet,
                                         int compressionThreshold) throws IndexOutOfBoundsException {
        writeFramedPacket(pool, buffer, PacketVanilla.SERVER_PACKET_PARSER, state, packet, compressionThreshold);
    }

    public static <T> void writeFramedPacket(NetworkBufferPool pool,
                                             NetworkBuffer buffer,
                                             PacketParser<? super T> parser,
                                             ConnectionState state,
                                             T packet,
                                             int compressionThreshold) throws IndexOutOfBoundsException {
        @SuppressWarnings("unchecked") // We assume ConnectionState and PacketRegistry are in sync
        final PacketRegistry<? super T> registry = (PacketRegistry<? super T>) parser.stateRegistry(state);
        writeFramedPacket(pool, buffer, registry, packet, compressionThreshold);
    }

    public static <T> void writeFramedPacket(NetworkBufferPool pool,
                                             NetworkBuffer buffer,
                                             PacketRegistry<? super T> registry,
                                             T packet,
                                             int compressionThreshold) throws IndexOutOfBoundsException {
        final PacketRegistry.PacketInfo<? super T> packetInfo = registry.packetInfo(packet);
        writeFramedPacket(
                pool,
                buffer,
                packetInfo, packet,
                compressionThreshold
        );
    }

    public static <T> void writeFramedPacket(NetworkBufferPool pool,
                                             NetworkBuffer buffer,
                                             PacketRegistry.PacketInfo<? super T> packetInfo,
                                             T packet,
                                             int compressionThreshold) throws IndexOutOfBoundsException {
        final int id = packetInfo.id();
        final NetworkBuffer.Type<? super T> serializer = packetInfo.serializer();
        writeFramedPacket(
                pool,
                buffer, serializer,
                id, packet,
                compressionThreshold
        );
    }

    public static <T> void writeFramedPacket(NetworkBufferPool pool,
                                             NetworkBuffer buffer,
                                             NetworkBuffer.Type<? super T> type,
                                             int id, T packet,
                                             int compressionThreshold) throws IndexOutOfBoundsException {
        if (compressionThreshold <= 0) writeUncompressedFormat(buffer, type, id, packet);
        else writeCompressedFormat(pool, buffer, type, id, packet, compressionThreshold);
    }

    private static <T> void writeUncompressedFormat(NetworkBuffer buffer,
                                                    NetworkBuffer.Type<? super T> type,
                                                    int id, T packet) throws IndexOutOfBoundsException {
        // Uncompressed format https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Protocol#Without_compression
        final long lengthIndex = buffer.advanceWrite(3);
        buffer.write(NetworkBuffer.VAR_INT, id);
        buffer.write(type, packet);
        final long finalSize = buffer.writeIndex() - (lengthIndex + 3);
        buffer.writeAt(lengthIndex, NetworkBuffer.VAR_INT_3, (int) finalSize);
    }

    private static <T> void writeCompressedFormat(NetworkBufferPool pool,
                                                  NetworkBuffer buffer,
                                                  NetworkBuffer.Type<? super T> type,
                                                  int id, T packet,
                                                  int compressionThreshold) throws IndexOutOfBoundsException {
        // Compressed format https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Protocol#With_compression
        final long compressedIndex = buffer.advanceWrite(3);
        final long uncompressedIndex = buffer.advanceWrite(3);
        final long contentStart = buffer.writeIndex();
        buffer.write(NetworkBuffer.VAR_INT, id);
        buffer.write(type, packet);
        final long packetSize = buffer.writeIndex() - contentStart;
        final boolean compressed = packetSize >= compressionThreshold;
        if (compressed) {
            // Write the compressed content into the pooled buffer
            // and compress it into the current buffer
            NetworkBuffer input = pool.acquireStatic(Math.max(ServerFlag.POOLED_BUFFER_SIZE, packetSize));
            try {
                NetworkBuffer.copy(buffer, contentStart, input, 0, packetSize);
                buffer.writeIndex(contentStart);
                input.compress(0, packetSize, buffer);
            } finally {
                pool.release(input);
            }
        }
        // Packet header (Packet + Data Length)
        buffer.writeAt(compressedIndex, NetworkBuffer.VAR_INT_3, (int) (buffer.writeIndex() - uncompressedIndex));
        buffer.writeAt(uncompressedIndex, NetworkBuffer.VAR_INT_3, compressed ? (int) packetSize : 0);
    }

    public static NetworkBuffer allocateTrimmedPacket(NetworkBufferPool pool,
                                                      @Nullable Registries registries,
                                                      ConnectionState state,
                                                      ClientPacket packet,
                                                      int compressionThreshold) {
        return allocateTrimmedPacket(pool, registries, PacketVanilla.CLIENT_PACKET_PARSER, state, packet, compressionThreshold);
    }

    public static NetworkBuffer allocateTrimmedPacket(NetworkBufferPool pool,
                                                      @Nullable Registries registries,
                                                      ConnectionState state,
                                                      ServerPacket packet,
                                                      int compressionThreshold) {
        return allocateTrimmedPacket(pool, registries, PacketVanilla.SERVER_PACKET_PARSER, state, packet, compressionThreshold);
    }

    public static <T> NetworkBuffer allocateTrimmedPacket(
            NetworkBufferPool pool,
            @Nullable Registries registries,
            PacketParser<T> parser,
            ConnectionState state,
            T packet,
            int compressionThreshold) {
        NetworkBuffer buffer = pool.acquireResizeable(ServerFlag.POOLED_BUFFER_SIZE, PACKET_WRITE_RESIZE, registries);
        try {
            return allocateTrimmedPacket(pool, buffer, parser, state, packet, compressionThreshold);
        } finally {
            pool.release(buffer);
        }
    }

    public static <T> NetworkBuffer allocateTrimmedPacket(
            NetworkBufferPool pool,
            NetworkBuffer tmpBuffer,
            PacketParser<? super T> parser,
            ConnectionState state,
            T packet,
            int compressionThreshold) {
        @SuppressWarnings("unchecked") // We assume ConnectionState and PacketRegistry are in sync
        final PacketRegistry<? super T> registry = (PacketRegistry<? super T>) parser.stateRegistry(state);
        return allocateTrimmedPacket(pool, tmpBuffer, registry, packet, compressionThreshold);
    }

    public static <T> NetworkBuffer allocateTrimmedPacket(
            NetworkBufferPool pool,
            NetworkBuffer tmpBuffer,
            PacketRegistry<? super T> registry,
            T packet,
            int compressionThreshold) {
        final PacketRegistry.PacketInfo<? super T> packetInfo = registry.packetInfo(packet);
        return allocateTrimmedPacket(pool, tmpBuffer, packetInfo, packet, compressionThreshold);
    }

    public static <T> NetworkBuffer allocateTrimmedPacket(
            NetworkBufferPool pool,
            NetworkBuffer tmpBuffer,
            PacketRegistry.PacketInfo<? super T> packetInfo,
            T packet,
            int compressionThreshold) {
        final int id = packetInfo.id();
        final NetworkBuffer.Type<? super T> serializer = packetInfo.serializer();
        writeFramedPacket(pool, tmpBuffer, serializer, id, packet, compressionThreshold);
        return tmpBuffer.copy(0, tmpBuffer.writeIndex());
    }

    public static final NetworkBuffer.AutoResize PACKET_WRITE_RESIZE = (capacity, targetSize) -> {
        if (targetSize > ServerFlag.MAX_PACKET_SIZE) return capacity;
        long newSize = Math.max(capacity * 2, targetSize);
        return Math.min(newSize, ServerFlag.MAX_PACKET_SIZE);
    };
}
