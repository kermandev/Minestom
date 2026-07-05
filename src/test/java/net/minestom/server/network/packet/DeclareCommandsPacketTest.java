package net.minestom.server.network.packet;

import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeclareCommandsPacketTest {

    @Test
    void testWriteGameProfileArg() {
        var root = new DeclareCommandsPacket.Node(
                (byte) DeclareCommandsPacket.Node.IS_ARGUMENT,
                new int[0],
                0,
                "profile",
                ArgumentParserType.GAME_PROFILE,
                null,
                null
        );
        var packet = new DeclareCommandsPacket(List.of(root), 0);

        var array = NetworkBuffer.makeArray(DeclareCommandsPacket.SERIALIZER, packet);
        var readPacket = NetworkBuffer.wrap(array, 0, array.length).read(DeclareCommandsPacket.SERIALIZER);
        assertEquals(ArgumentParserType.GAME_PROFILE, readPacket.nodes().getFirst().parser());
    }
}
