package net.minestom.server.network.packet;

import net.minestom.server.ServerFlag;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.utils.collection.ObjectPool;

import java.util.Objects;

public sealed interface PacketWriter<T> {
    PacketParser<T> writer();

    ObjectPool<NetworkBuffer> bufferPool();

    default <P extends T> void writeFramedPacket(NetworkBuffer buffer,
                                                 ConnectionState state,
                                                 P packet,
                                                 int compressionThreshold) throws IndexOutOfBoundsException {
        @SuppressWarnings("unchecked") // We assume that T's registry is tied to the state.
        final PacketRegistry<? super P> registry = (PacketRegistry<? super P>) writer().stateRegistry(state);
        writeFramedPacket(buffer, registry, packet, compressionThreshold);
    }

    default <P extends T> void writeFramedPacket(NetworkBuffer buffer,
                                                 PacketRegistry<? super P> registry,
                                                 P packet,
                                                 int compressionThreshold) throws IndexOutOfBoundsException {
        final PacketRegistry.PacketInfo<P> packetInfo = registry.packetInfo(packet);
        writeFramedPacket(
                buffer,
                packetInfo, packet,
                compressionThreshold
        );
    }

    default <P extends T> void writeFramedPacket(NetworkBuffer buffer,
                                                 PacketRegistry.PacketInfo<P> packetInfo,
                                                 P packet,
                                                 int compressionThreshold) throws IndexOutOfBoundsException {
        final int id = packetInfo.id();
        final NetworkBuffer.Type<P> serializer = packetInfo.serializer();
        writeFramedPacket(
                buffer, serializer,
                id, packet,
                compressionThreshold
        );
    }

    default <P extends T> void writeFramedPacket(NetworkBuffer buffer,
                                                 NetworkBuffer.Type<P> type,
                                                 int id, P packet,
                                                 int compressionThreshold) throws IndexOutOfBoundsException {
        if (compressionThreshold <= 0) writeUncompressedFormat(buffer, type, id, packet);
        else writeCompressedFormat(buffer, type, id, packet, compressionThreshold);
    }

    default <P extends T> NetworkBuffer allocateTrimmedPacket(
            ConnectionState state,
            P packet,
            int compressionThreshold) {
        @SuppressWarnings("unchecked") // We assume that T's registry is tied to the state.
        final PacketRegistry<? super P> registry = (PacketRegistry<? super P>) writer().stateRegistry(state);
        return allocateTrimmedPacket(registry, packet, compressionThreshold);
    }

    default <P extends T> NetworkBuffer allocateTrimmedPacket(
            PacketRegistry<? super P> registry,
            P packet,
            int compressionThreshold) {
        ObjectPool<NetworkBuffer> bufferPool = bufferPool();
        NetworkBuffer buffer = bufferPool.get();
        try {
            return allocateTrimmedPacket(buffer, registry, packet, compressionThreshold);
        } finally {
            bufferPool.add(buffer);
        }
    }

    default <P extends T> NetworkBuffer allocateTrimmedPacket(
            NetworkBuffer tmpBuffer,
            PacketRegistry<? super P> registry,
            P packet,
            int compressionThreshold) {
        final PacketRegistry.PacketInfo<P> packetInfo = registry.packetInfo(packet);
        final int id = packetInfo.id();
        final NetworkBuffer.Type<P> serializer = packetInfo.serializer();
        try {
            writeFramedPacket(tmpBuffer, serializer, id, packet, compressionThreshold);
            return tmpBuffer.trimmed();
        } catch (IndexOutOfBoundsException e) {
            final long sizeOf = serializer.sizeOf(packet, tmpBuffer.registries());
            if (sizeOf > ServerFlag.MAX_PACKET_SIZE) {
                throw new IllegalStateException("Packet too large: " + sizeOf);
            }
            // Add 15 bytes to account for the 3 potential varints in the packet header
            // Packet Length - Data Length - Packet ID
            tmpBuffer.resize(sizeOf + 15);
            tmpBuffer.writeIndex(0);
            writeFramedPacket(tmpBuffer, serializer, id, packet, compressionThreshold);
            return tmpBuffer.trimmed();
        }
    }

    private <P extends T> void writeUncompressedFormat(NetworkBuffer buffer,
                                                       NetworkBuffer.Type<P> type,
                                                       int id, P packet) throws IndexOutOfBoundsException {
        // Uncompressed format https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Protocol#Without_compression
        final long lengthIndex = buffer.advanceWrite(3);
        buffer.write(NetworkBuffer.VAR_INT, id);
        buffer.write(type, packet);
        final long finalSize = buffer.writeIndex() - (lengthIndex + 3);
        buffer.writeAt(lengthIndex, NetworkBuffer.VAR_INT_3, (int) finalSize);
    }

    private <P extends T> void writeCompressedFormat(NetworkBuffer buffer,
                                                     NetworkBuffer.Type<P> type,
                                                     int id, P packet,
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
            ObjectPool<NetworkBuffer> bufferPool = bufferPool();
            NetworkBuffer input = bufferPool.get();
            try {
                if (input.capacity() < packetSize) input.resize(packetSize);
                NetworkBuffer.copy(buffer, contentStart, input, 0, packetSize);
                buffer.writeIndex(contentStart);
                input.compress(0, packetSize, buffer);
            } finally {
                bufferPool.add(input);
            }
        }
        // Packet header (Packet + Data Length)
        buffer.writeAt(compressedIndex, NetworkBuffer.VAR_INT_3, (int) (buffer.writeIndex() - uncompressedIndex));
        buffer.writeAt(uncompressedIndex, NetworkBuffer.VAR_INT_3, compressed ? (int) packetSize : 0);
    }

    record Server(PacketParser.Server writer,
                  ObjectPool<NetworkBuffer> bufferPool) implements PacketWriter<ServerPacket> {
        public Server(ObjectPool<NetworkBuffer> bufferPool) {
            this(PacketVanilla.SERVER_PACKET_PARSER, bufferPool);
        }

        public Server {
            Objects.requireNonNull(writer, "writer");
            Objects.requireNonNull(bufferPool, "bufferPool");
        }
    }

    record Client(PacketParser.Client writer,
                  ObjectPool<NetworkBuffer> bufferPool) implements PacketWriter<ClientPacket> {
        public Client(ObjectPool<NetworkBuffer> bufferPool) {
            this(PacketVanilla.CLIENT_PACKET_PARSER, bufferPool);
        }

        public Client {
            Objects.requireNonNull(writer, "writer");
            Objects.requireNonNull(bufferPool, "bufferPool");
        }
    }
}
