package net.minestom.server.network.packet.client.login;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record ClientLoginStartPacket(@NotNull String username,
                                     @NotNull UUID profileId) implements ClientPacket {
    public static final NetworkBuffer.Type<ClientLoginStartPacket> SERIALIZER = NetworkBufferTemplate.template(
            LimitedString(16), ClientLoginStartPacket::username,
            UUID, ClientLoginStartPacket::profileId,
            ClientLoginStartPacket::new);

    public ClientLoginStartPacket {
        if (username.length() > 16)
            throw new IllegalArgumentException("Username is not allowed to be longer than 16 characters");
    }
}
