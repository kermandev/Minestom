package net.minestom.server.network.packet.client.play;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.minestom.server.network.NetworkBuffer.*;

public record ClientUpdateSignPacket(
        @NotNull Point blockPosition,
        boolean isFrontText,
        @NotNull List<String> lines
) implements ClientPacket {
    public ClientUpdateSignPacket {
        lines = List.copyOf(lines);
        if (lines.size() != 4) {
            throw new IllegalArgumentException("Signs must have 4 lines!");
        }
        for (String line : lines) {
            if (line.length() > 384) {
                throw new IllegalArgumentException("Signs must have a maximum of 384 characters per line!");
            }
        }
    }

    // TODO fixed list type
    private static final NetworkBuffer.Type<List<String>> LINES_SERIALIZER = new NetworkBuffer.Type<>() {
        private static final Type<String> SIGN_LINE = LimitedString(384);

        @Override
        public void write(@NotNull NetworkBuffer buffer, List<String> value) {
            buffer.write(SIGN_LINE, value.get(0));
            buffer.write(SIGN_LINE, value.get(1));
            buffer.write(SIGN_LINE, value.get(2));
            buffer.write(SIGN_LINE, value.get(3));
        }

        @Override
        public List<String> read(@NotNull NetworkBuffer buffer) {
            return List.of(buffer.read(SIGN_LINE), buffer.read(SIGN_LINE),
                    buffer.read(SIGN_LINE), buffer.read(SIGN_LINE));
        }
    };

    public static final NetworkBuffer.Type<ClientUpdateSignPacket> SERIALIZER = NetworkBufferTemplate.template(
            BLOCK_POSITION, ClientUpdateSignPacket::blockPosition,
            BOOLEAN, ClientUpdateSignPacket::isFrontText,
            LINES_SERIALIZER, ClientUpdateSignPacket::lines,
            ClientUpdateSignPacket::new
    );
}
