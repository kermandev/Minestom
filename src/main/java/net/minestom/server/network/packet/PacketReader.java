package net.minestom.server.network.packet;

import net.minestom.server.ServerFlag;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.utils.collection.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.zip.DataFormatException;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;
import static net.minestom.server.network.packet.PacketReader.Result.Empty.empty;

public sealed interface PacketReader<T> {
    PacketLibrary<T> reader();

    ObjectPool<NetworkBuffer> bufferPool();

    Result<T> readPackets(NetworkBuffer buffer, ConnectionState state, boolean compressed) throws DataFormatException;

    default PacketReader.Result<T> readPackets(
            NetworkBuffer buffer,
            ConnectionState state,
            BiFunction<T, ConnectionState, ConnectionState> stateUpdater,
            boolean compressed
    ) throws DataFormatException {
        List<PacketReader.ParsedPacket<T>> packets = new ArrayList<>();
        readLoop:
        while (buffer.readableBytes() > 0) {
            final PacketReader.Result<T> result = readPacket(buffer, state, stateUpdater, compressed);
            if (buffer.readableBytes() == 0 && packets.isEmpty()) return result;
            switch (result) {
                case PacketReader.Result.Success<T> success -> {
                    assert success.packets().size() == 1;
                    final PacketReader.ParsedPacket<T> parsedPacket = success.packets().getFirst();
                    packets.add(parsedPacket);
                    state = parsedPacket.nextState();
                }
                case PacketReader.Result.Empty<T> _ -> {
                    break readLoop;
                }
                case PacketReader.Result.Failure<T> failure -> {
                    return packets.isEmpty() ? failure : new PacketReader.Result.Success<>(packets);
                }
            }
        }
        return !packets.isEmpty() ? new PacketReader.Result.Success<>(packets) : empty();
    }

    Result<T> readPacket(NetworkBuffer buffer, ConnectionState state, boolean compressed) throws DataFormatException;

    default PacketReader.Result<T> readPacket(
            NetworkBuffer buffer,
            ConnectionState state,
            BiFunction<T, ConnectionState, ConnectionState> stateUpdater,
            boolean compressed
    ) throws DataFormatException {
        final long beginMark = buffer.readIndex();
        // READ PACKET LENGTH
        final int packetLength;
        try {
            packetLength = buffer.read(VAR_INT);
        } catch (IndexOutOfBoundsException e) {
            // Couldn't read a single var-int
            buffer.readIndex(beginMark);
            return new PacketReader.Result.Failure<>(5);
        }
        final long readerStart = buffer.readIndex();
        if (readerStart > buffer.writeIndex()) {
            // Can't read the packet length, buffer has enough capacity
            buffer.readIndex(beginMark);
            return empty();
        }
        final int maxPacketSize = maxPacketSize(state);
        if (packetLength > maxPacketSize) {
            throw new DataFormatException("Packet too large: %d > %d:%s".formatted(packetLength, maxPacketSize, state.name()));
        }
        // READ PAYLOAD https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Protocol#Packet_format
        if (buffer.readableBytes() < packetLength) {
            // Can't read the full packet
            buffer.readIndex(beginMark);
            final long packetLengthVarIntSize = readerStart - beginMark;
            final long requiredCapacity = packetLengthVarIntSize + packetLength;
            // Must return a failure if the buffer is too small
            // Otherwise do nothing, and hope to read the packet remains next time
            if (requiredCapacity > buffer.capacity()) return new PacketReader.Result.Failure<>(requiredCapacity);
            else return empty();
        }
        final long readerEnd = readerStart + packetLength;
        // We create a slice here so capacity is enforced, we also set it to read only cause we dont want readers writing into this buffer.
        final NetworkBuffer slice = buffer.slice(readerStart, packetLength, 0, packetLength).readOnly();
        final T packet = readFramedPacket(slice, state, compressed);
        final ConnectionState nextState = stateUpdater.apply(packet, state);
        buffer.readIndex(readerEnd);
        return new PacketReader.Result.Success<>(new PacketReader.ParsedPacket<>(nextState, packet));
    }

    private T readFramedPacket(NetworkBuffer buffer,
                               ConnectionState state,
                               boolean compressed) throws DataFormatException {
        if (!compressed) {
            // No compression format
            return readPayload(buffer, state);
        }

        // READ COMPRESSION HEADER
        final int dataLength = buffer.read(VAR_INT);
        if (dataLength == 0) {
            // Uncompressed packet
            return readPayload(buffer, state);
        }

        // Decompress the packet into the pooled buffer
        // and read the uncompressed packet from it
        final ObjectPool<NetworkBuffer> bufferPool = bufferPool();
        NetworkBuffer decompressed = bufferPool.get();
        try {
            if (decompressed.capacity() < dataLength) decompressed.resize(dataLength);
            buffer.decompress(buffer.readIndex(), buffer.readableBytes(), decompressed);
            return readPayload(decompressed.readOnly(), state); // Payload should not write into the buffer
        } finally {
            bufferPool.add(decompressed);
        }
    }

    private T readPayload(NetworkBuffer buffer, ConnectionState state) {
        final int packetId = buffer.read(VAR_INT);
        final T packet = reader().parse(state, packetId, buffer);
        warnUnreadBytes(buffer, packet, packetId);
        return packet;
    }

    private void warnUnreadBytes(NetworkBuffer buffer, Object packet, int packetId) {
        if (!ServerFlag.WARN_UNREAD_BYTES_PACKET || buffer.readableBytes() == 0) return;
        // Only init the logger if it's used.
        class Logging {
            static final Logger LOGGER = LoggerFactory.getLogger(PacketReader.class);
        }
        Logging.LOGGER.warn("WARNING: Packet ({}) 0x{} not fully read ({})",
                packet.getClass().getSimpleName(), Integer.toHexString(packetId), buffer);
    }

    sealed interface Result<T> {

        /**
         * At least one packet was read.
         * The buffer may still contain half-read packets and should therefore be compacted for next read.
         */
        record Success<T>(List<PacketReader.ParsedPacket<T>> packets) implements Result<T> {
            public Success {
                if (packets.isEmpty()) {
                    throw new IllegalArgumentException("Empty packets");
                }
                packets = List.copyOf(packets);
            }

            public Success(PacketReader.ParsedPacket<T> packet) {
                this(List.of(packet));
            }
        }

        /**
         * Represents no packet to read. Can generally be ignored.
         * <p>
         * Happens when a packet length or payload couldn't be read, but the buffer has enough capacity.
         */
        record Empty<T>() implements Result<T> {
            private static final Empty<?> INSTANCE = new Empty<>();

            @SuppressWarnings("unchecked")
            public static <T> Empty<T> empty() {
                return (Empty<T>) INSTANCE;
            }
        }

        /**
         * Represents a failure to read a packet due to insufficient buffer capacity.
         * <p>
         * Buffer should be expanded to at least {@code requiredCapacity} bytes.
         * <p>
         * If the buffer does not allow to read the packet length, max var-int length is returned.
         */
        record Failure<T>(long requiredCapacity) implements Result<T> {
        }
    }

    record ParsedPacket<T>(ConnectionState nextState, T packet) {
    }

    record Server(PacketLibrary.Server reader,
                  ObjectPool<NetworkBuffer> bufferPool) implements PacketReader<ServerPacket> {
        public Server(ObjectPool<NetworkBuffer> bufferPool) {
            this(PacketVanilla.SERVER_PACKET_PARSER, bufferPool);
        }

        public Server {
            Objects.requireNonNull(reader, "reader");
            Objects.requireNonNull(bufferPool, "bufferPool");
        }

        @Override
        public Result<ServerPacket> readPackets(NetworkBuffer buffer, ConnectionState state, boolean compressed) throws DataFormatException {
            return readPackets(buffer, state, PacketVanilla::nextServerState, compressed);
        }

        @Override
        public Result<ServerPacket> readPacket(NetworkBuffer buffer, ConnectionState state, boolean compressed) throws DataFormatException {
            return readPacket(buffer, state, PacketVanilla::nextServerState, compressed);
        }
    }

    record Client(PacketLibrary.Client reader,
                  ObjectPool<NetworkBuffer> bufferPool) implements PacketReader<ClientPacket> {
        public Client(ObjectPool<NetworkBuffer> bufferPool) {
            this(PacketVanilla.CLIENT_PACKET_PARSER, bufferPool);
        }

        public Client {
            Objects.requireNonNull(reader, "reader");
            Objects.requireNonNull(bufferPool, "bufferPool");
        }

        @Override
        public Result<ClientPacket> readPackets(NetworkBuffer buffer, ConnectionState state, boolean compressed) throws DataFormatException {
            return readPackets(buffer, state, PacketVanilla::nextClientState, compressed);
        }

        @Override
        public Result<ClientPacket> readPacket(NetworkBuffer buffer, ConnectionState state, boolean compressed) throws DataFormatException {
            return readPacket(buffer, state, PacketVanilla::nextClientState, compressed);
        }
    }

    static int maxPacketSize(ConnectionState state) {
        return switch (state) {
            case HANDSHAKE, LOGIN -> ServerFlag.MAX_PACKET_SIZE_PRE_AUTH;
            default -> ServerFlag.MAX_PACKET_SIZE;
        };
    }
}
