package net.minestom.server.network.packet.server.play;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minestom.server.network.NetworkBuffer.STRING;

public record ResetScorePacket(@NotNull String owner, @Nullable String objective) implements ServerPacket.Play {
    public static final NetworkBuffer.Type<ResetScorePacket> SERIALIZER = NetworkBufferTemplate.template(
            STRING, ResetScorePacket::owner,
            STRING.optional(), ResetScorePacket::objective,
            ResetScorePacket::new);
}
