package net.minestom.server.network.packet.server;

import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.PacketParser;
import net.minestom.server.network.packet.PacketWriter;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.utils.collection.ObjectPool;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a packet that can be sent to a {@link PlayerConnection}.
 */
public sealed interface SendablePacket
        permits BufferedPacket, CachedPacket, FramedPacket, LazyPacket, ServerPacket {

    static @Nullable ServerPacket extractServerPacket(SendablePacket packet, ConnectionState state) {
        return SendablePacket.extractServerPacket(packet, state, null);
    }

    // This method could allocate packets, be warned.
    static @Nullable ServerPacket extractServerPacket(SendablePacket packet, ConnectionState state, @Nullable PacketWriter<ServerPacket> writer) {
        return switch (packet) {
            case ServerPacket serverPacket -> serverPacket;
            case CachedPacket cachedPacket -> cachedPacket.packet(state, writer);
            case FramedPacket framedPacket -> framedPacket.packet();
            case LazyPacket lazyPacket -> lazyPacket.packet();
            case BufferedPacket _ -> null;
        };
    }
}
