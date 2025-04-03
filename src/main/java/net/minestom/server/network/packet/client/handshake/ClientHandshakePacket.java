package net.minestom.server.network.packet.client.handshake;

import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.*;

public record ClientHandshakePacket(int protocolVersion, @NotNull String serverAddress,
                                    int serverPort, @NotNull Intent intent) implements ClientPacket {
    public ClientHandshakePacket {
        final int serverAddressLength = serverAddress.length();
        Check.argCondition(serverAddressLength > MAX_HANDSHAKE_LENGTH, "Server address too long: {0} > {1}", serverAddressLength, MAX_HANDSHAKE_LENGTH);
    }

    // BungeeGuard limits handshake length to 2500 characters, while vanilla limits it to 255
    // The configuration should be loaded before this packet is first used.
    public static final short MAX_HANDSHAKE_LENGTH = BungeeCordProxy.isEnabled()
            ? (BungeeCordProxy.isBungeeGuardEnabled() ? 2500 : Short.MAX_VALUE)
            : 255;

    public static final NetworkBuffer.Type<ClientHandshakePacket> SERIALIZER = NetworkBufferTemplate.template(
            VAR_INT, ClientHandshakePacket::protocolVersion,
            LimitedString(MAX_HANDSHAKE_LENGTH), ClientHandshakePacket::serverAddress, // Lazy because this could possibly be called before the configuration is complete.
            UNSIGNED_SHORT, ClientHandshakePacket::serverPort,
            VAR_INT.transform(Intent::fromId, Intent::id), ClientHandshakePacket::intent,
            ClientHandshakePacket::new);

    public enum Intent {
        STATUS,
        LOGIN,
        TRANSFER;

        public static @NotNull Intent fromId(int id) {
            return switch (id) {
                case 1 -> STATUS;
                case 2 -> LOGIN;
                case 3 -> TRANSFER;
                default -> throw new IllegalArgumentException("Unknown connection intent: " + id);
            };
        }

        public int id() {
            return ordinal() + 1;
        }
    }
}
