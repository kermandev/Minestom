package net.minestom.server.network.packet.server.common;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static net.minestom.server.network.NetworkBuffer.LimitedString;
import static net.minestom.server.network.NetworkBuffer.STRING;

public record CustomReportDetailsPacket(
        @NotNull Map<String, String> details
) implements ServerPacket.Configuration, ServerPacket.Play {
    private static final int MAX_DETAILS = 32;

    public static final NetworkBuffer.Type<CustomReportDetailsPacket> SERIALIZER = NetworkBufferTemplate.template(
            LimitedString(128).mapValue(LimitedString(4096), MAX_DETAILS), CustomReportDetailsPacket::details,
            CustomReportDetailsPacket::new
    );

    public CustomReportDetailsPacket {
        details = Map.copyOf(details);
    }
}
