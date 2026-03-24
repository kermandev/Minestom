package net.minestom.server.network;

import net.minestom.server.network.packet.PacketReader;
import net.minestom.server.network.packet.PacketVanilla;
import net.minestom.server.network.packet.PacketWriter;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.common.ClientPluginMessagePacket;
import net.minestom.server.utils.collection.ObjectPool;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.zip.DataFormatException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnvTest // PacketPool
public class SocketReadTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void complete(boolean compressed, Env env) throws DataFormatException {
        var packet = new ClientPluginMessagePacket("channel", new byte[2000]);
        final ObjectPool<NetworkBuffer> networkPool = ObjectPool.unpooled(() -> NetworkBuffer.resizableBuffer(env.process()));
        final PacketWriter.Client packetWriter = new PacketWriter.Client(networkPool);
        final PacketReader.Client packetReader = new PacketReader.Client(networkPool);

        var buffer = networkPool.get();
        packetWriter.writeFramedPacket(buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);

        var readResult = packetReader.readPacket(buffer, ConnectionState.PLAY, PacketVanilla::nextClientState, compressed);
        if (!(readResult instanceof PacketReader.Result.Success<ClientPacket> success)) {
            throw new AssertionError("Expected a success result, got " + readResult);
        }
        List<ClientPacket> packets = success.packets().stream().map(PacketReader.ParsedPacket::packet).toList();
        assertEquals(List.of(packet), packets);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void completeTwo(boolean compressed, Env ignored) throws DataFormatException {
        var packet = new ClientPluginMessagePacket("channel", new byte[2000]);

        var buffer = PacketVanilla.PACKET_POOL.get();
        PacketWriting.writeFramedPacket(buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);
        PacketWriting.writeFramedPacket(buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);

        var readResult = PacketReading.readClients(buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReader.Result.Success<ClientPacket> success)) {
            throw new AssertionError("Expected a success result, got " + readResult);
        }
        List<ClientPacket> packets = success.packets().stream().map(PacketReader.ParsedPacket::packet).toList();
        assertEquals(List.of(packet, packet), packets);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void insufficientLength(boolean compressed, Env ignored) throws DataFormatException {
        // Write a complete packet then the next packet length without any payload

        var packet = new ClientPluginMessagePacket("channel", new byte[2000]);

        var buffer = PacketVanilla.PACKET_POOL.get();
        PacketWriting.writeFramedPacket(buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);
        buffer.write(NetworkBuffer.VAR_INT, 200); // incomplete 200 bytes packet

        var readResult = PacketReader.readClients(buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReader.Result.Success<ClientPacket> success)) {
            throw new AssertionError("Expected a success result, got " + readResult);
        }
        List<ClientPacket> packets = success.packets().stream().map(PacketReader.ParsedPacket::packet).toList();
        assertEquals(List.of(packet), packets);

        readResult = PacketReader.readClients(buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReader.Result.Empty<ClientPacket>)) {
            throw new AssertionError("Expected an empty result, got " + readResult);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void incomplete(boolean compressed, Env ignored) throws DataFormatException {
        // Write a complete packet and incomplete var-int length for the next packet

        var packet = new ClientPluginMessagePacket("channel", new byte[2000]);

        var buffer = PacketVanilla.PACKET_POOL.get();
        PacketWriting.writeFramedPacket(buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);
        buffer.write(NetworkBuffer.BYTE, (byte) -85); // incomplete var-int length

        var readResult = PacketReading.readClients(buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Success<ClientPacket> success)) {
            throw new AssertionError("Expected a success result, got " + readResult);
        }
        List<ClientPacket> packets = success.packets().stream().map(PacketReading.ParsedPacket::packet).toList();
        assertEquals(1, buffer.readableBytes());

        assertEquals(List.of(packet), packets);

        // Try to read the next packet
        readResult = PacketReading.readClients(buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Failure<ClientPacket>)) {
            throw new AssertionError("Expected an failure result, got " + readResult);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void resize(boolean compressed, Env ignored) throws DataFormatException {
        // Write a complete packet that is larger than the buffer capacity

        var packet = new ClientPluginMessagePacket("channel", new byte[2000]);

        var buffer = PacketVanilla.PACKET_POOL.get();
        PacketWriting.writeFramedPacket(buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);
        final long packetLength = buffer.writeIndex();
        buffer = buffer.copy(0, packetLength / 2).index(0, packetLength / 2);

        var readResult = PacketReading.readClients(buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Failure<ClientPacket> failure)) {
            throw new AssertionError("Expected a failure result, got " + readResult);
        }
        assertEquals(packetLength, failure.requiredCapacity());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void resizeHeader(boolean compressed, Env ignored) throws DataFormatException {
        // Write a buffer where you cannot read the packet length

        var buffer = NetworkBuffer.staticBuffer(1);
        buffer.write(NetworkBuffer.BYTE, (byte) -85); // incomplete var-int length

        var readResult = PacketReading.readClients(buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Failure<ClientPacket> failure)) {
            throw new AssertionError("Expected a failure result, got " + readResult);
        }
        // 5 = max var-int size
        assertEquals(5, failure.requiredCapacity());
        assertEquals(0, buffer.readIndex(), "Buffer should reset on failure");
    }
}
