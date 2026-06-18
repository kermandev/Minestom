package net.minestom.server.network;

import net.minestom.server.network.packet.PacketParser;
import net.minestom.server.network.packet.PacketRegistry;
import net.minestom.server.network.packet.PacketReading;
import net.minestom.server.network.packet.PacketVanilla;
import net.minestom.server.network.packet.PacketWriting;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.common.ClientPluginMessagePacket;
import net.minestom.server.registry.Registries;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.DataFormatException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SocketReadTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void complete(boolean compressed) throws DataFormatException {
        var packet = new ClientPluginMessagePacket("channel", new byte[2000]);
        var pool = NetworkBufferPool.pool(8 * 1024 * 1024);

        var buffer = NetworkBuffer.resizableBuffer(256);
        PacketWriting.writeFramedPacket(pool, buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);

        var readResult = PacketReading.readClients(pool, buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Success<ClientPacket>(
                List<PacketReading.ParsedPacket<ClientPacket>> packets1
        ))) {
            throw new AssertionError("Expected a success result, got " + readResult);
        }
        List<ClientPacket> packets = packets1.stream().map(PacketReading.ParsedPacket::packet).toList();
        assertEquals(List.of(packet), packets);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void completeTwo(boolean compressed) throws DataFormatException {
        var packet = new ClientPluginMessagePacket("channel", new byte[2000]);
        var pool = NetworkBufferPool.pool(8 * 1024 * 1024);

        var buffer = NetworkBuffer.resizableBuffer(256);
        PacketWriting.writeFramedPacket(pool, buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);
        PacketWriting.writeFramedPacket(pool, buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);

        var readResult = PacketReading.readClients(pool, buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Success<ClientPacket>(
                List<PacketReading.ParsedPacket<ClientPacket>> packets1
        ))) {
            throw new AssertionError("Expected a success result, got " + readResult);
        }
        List<ClientPacket> packets = packets1.stream().map(PacketReading.ParsedPacket::packet).toList();
        assertEquals(List.of(packet, packet), packets);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void insufficientLength(boolean compressed) throws DataFormatException {
        // Write a complete packet then the next packet length without any payload

        var packet = new ClientPluginMessagePacket("channel", new byte[2000]);
        var pool = NetworkBufferPool.pool(8 * 1024 * 1024);

        var buffer = NetworkBuffer.resizableBuffer(256);
        PacketWriting.writeFramedPacket(pool, buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);
        buffer.write(NetworkBuffer.VAR_INT, 200); // incomplete 200 bytes packet

        var readResult = PacketReading.readClients(pool, buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Success<ClientPacket>(
                List<PacketReading.ParsedPacket<ClientPacket>> packets1
        ))) {
            throw new AssertionError("Expected a success result, got " + readResult);
        }
        List<ClientPacket> packets = packets1.stream().map(PacketReading.ParsedPacket::packet).toList();
        assertEquals(List.of(packet), packets);

        readResult = PacketReading.readClients(pool, buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Empty<ClientPacket>)) {
            throw new AssertionError("Expected an empty result, got " + readResult);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void incomplete(boolean compressed) throws DataFormatException {
        // Write a complete packet and incomplete var-int length for the next packet

        var packet = new ClientPluginMessagePacket("channel", new byte[2000]);
        var pool = NetworkBufferPool.pool(8 * 1024 * 1024);

        var buffer = NetworkBuffer.resizableBuffer(256);
        PacketWriting.writeFramedPacket(pool, buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);
        buffer.write(NetworkBuffer.BYTE, (byte) -85); // incomplete var-int length

        var readResult = PacketReading.readClients(pool, buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Success<ClientPacket>(
                List<PacketReading.ParsedPacket<ClientPacket>> packets1
        ))) {
            throw new AssertionError("Expected a success result, got " + readResult);
        }
        List<ClientPacket> packets = packets1.stream().map(PacketReading.ParsedPacket::packet).toList();
        assertEquals(1, buffer.readableBytes());

        assertEquals(List.of(packet), packets);

        // Try to read the next packet
        readResult = PacketReading.readClients(pool, buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Empty<ClientPacket>)) {
            throw new AssertionError("Expected an empty result, got " + readResult);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void resize(boolean compressed) throws DataFormatException {
        // Write a complete packet that is larger than the buffer capacity

        var packet = new ClientPluginMessagePacket("channel", new byte[2000]);
        var pool = NetworkBufferPool.pool(8 * 1024 * 1024);

        var buffer = NetworkBuffer.resizableBuffer(256);
        PacketWriting.writeFramedPacket(pool, buffer, ConnectionState.PLAY, packet, compressed ? 256 : 0);
        final long packetLength = buffer.writeIndex();
        buffer = buffer.copy(0, packetLength / 2).index(0, packetLength / 2);

        var readResult = PacketReading.readClients(pool, buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Failure<ClientPacket>(long requiredCapacity))) {
            throw new AssertionError("Expected a failure result, got " + readResult);
        }
        assertEquals(packetLength, requiredCapacity);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void resizeHeader(boolean compressed) throws DataFormatException {
        // Write a buffer where you cannot read the packet length

        var buffer = NetworkBuffer.staticBuffer(1);
        buffer.write(NetworkBuffer.BYTE, (byte) -85); // incomplete var-int length

        var pool = NetworkBufferPool.pool(8 * 1024 * 1024);
        var readResult = PacketReading.readClients(pool, buffer, ConnectionState.PLAY, compressed);
        if (!(readResult instanceof PacketReading.Result.Failure<ClientPacket>(long requiredCapacity))) {
            throw new AssertionError("Expected a failure result, got " + readResult);
        }
        // 5 = max var-int size
        assertEquals(5, requiredCapacity);
    }

    @Test
    public void compressedReadInheritsSourceBufferRegistries() throws DataFormatException {
        // Encode a framed packet large enough to actually compress (threshold = 256), keeping
        // the pool untouched by the test setup so the read is the only relevant consumer.
        final var packet = new ClientPluginMessagePacket("ch", new byte[2000]);
        final var pool = NetworkBufferPool.pool(8 * 1024 * 1024);
        final var encoded = NetworkBuffer.resizableBuffer();
        PacketWriting.writeFramedPacket(pool, encoded, ConnectionState.PLAY, packet, 256);
        final int length = (int) encoded.writeIndex();
        final byte[] framed = new byte[length];
        encoded.copyTo(0, framed, 0, length);

        final Registries sourceRegistries = Registries.vanilla();
        final var source = NetworkBuffer.wrap(framed, 0, framed.length, sourceRegistries);

        final AtomicBoolean asserted = new AtomicBoolean(false);
        PacketParser.Client originalParser = PacketVanilla.CLIENT_PACKET_PARSER;
        PacketRegistry<ClientPacket.Play> originalPlay = originalParser.play();
        PacketRegistry<ClientPacket.Play> customPlay = new PacketRegistry<>() {
            @Override
            public ClientPacket.Play create(int packetId, NetworkBuffer buffer) {
                return originalPlay.create(packetId, buffer);
            }
            @Override
            public PacketInfo<ClientPacket.Play> packetInfo(Class<?> packetClass) {
                return originalPlay.packetInfo(packetClass);
            }
            @Override
            public PacketInfo<ClientPacket.Play> packetInfo(int packetId) {
                var originalInfo = originalPlay.packetInfo(packetId);
                var originalSerializer = originalInfo.serializer();
                NetworkBuffer.Type<ClientPacket.Play> customSerializer = new NetworkBuffer.Type<>() {
                    @Override
                    public ClientPacket.Play read(NetworkBuffer reader) {
                        assertSame(sourceRegistries, reader.registries());
                        asserted.set(true);
                        return originalSerializer.read(reader);
                    }
                    @Override
                    public void write(NetworkBuffer writer, ClientPacket.Play value) {
                        originalSerializer.write(writer, value);
                    }
                };
                return new PacketInfo<>(originalInfo.packetClass(), originalInfo.id(), customSerializer);
            }
            @Override
            public ConnectionState state() {
                return originalPlay.state();
            }
            @Override
            public PacketRegistry.ConnectionSide side() {
                return originalPlay.side();
            }
            @Override
            public List<PacketInfo<? extends ClientPacket.Play>> packets() {
                return originalPlay.packets();
            }
        };

        PacketParser.Client customParser = new PacketParser.Client(
                originalParser.handshake(),
                originalParser.status(),
                originalParser.login(),
                originalParser.configuration(),
                customPlay
        );

        PacketReading.readPackets(pool, source, customParser, ConnectionState.PLAY, PacketVanilla::nextClientState, true);
        assertTrue(asserted.get(), "Assertion must have run");
    }

    private static int getVarIntSize(int input) {
        return (input & 0xFFFFFF80) == 0
                ? 1 : (input & 0xFFFFC000) == 0
                ? 2 : (input & 0xFFE00000) == 0
                ? 3 : (input & 0xF0000000) == 0
                ? 4 : 5;
    }
}
