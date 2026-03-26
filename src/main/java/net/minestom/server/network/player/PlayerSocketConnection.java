package net.minestom.server.network.player;

import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.ListenerHandle;
import net.minestom.server.event.player.PlayerPacketOutEvent;
import net.minestom.server.extras.mojangAuth.MojangCrypt;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.*;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.common.ClientCookieResponsePacket;
import net.minestom.server.network.packet.client.common.ClientKeepAlivePacket;
import net.minestom.server.network.packet.client.common.ClientPingRequestPacket;
import net.minestom.server.network.packet.client.configuration.ClientFinishConfigurationPacket;
import net.minestom.server.network.packet.client.configuration.ClientSelectKnownPacksPacket;
import net.minestom.server.network.packet.client.handshake.ClientHandshakePacket;
import net.minestom.server.network.packet.client.login.ClientEncryptionResponsePacket;
import net.minestom.server.network.packet.client.login.ClientLoginAcknowledgedPacket;
import net.minestom.server.network.packet.client.login.ClientLoginPluginResponsePacket;
import net.minestom.server.network.packet.client.login.ClientLoginStartPacket;
import net.minestom.server.network.packet.client.status.ClientStatusRequestPacket;
import net.minestom.server.network.packet.server.*;
import net.minestom.server.network.packet.server.login.SetCompressionPacket;
import net.minestom.server.utils.collection.ConcurrentMessageQueues;
import net.minestom.server.utils.collection.ObjectPool;
import net.minestom.server.utils.validate.Check;
import org.jctools.queues.MessagePassingQueue;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.zip.DataFormatException;

/**
 * Represents a socket connection.
 * <p>
 * It is the implementation used for all network client.
 */
@ApiStatus.Internal
public final class PlayerSocketConnection extends PlayerConnection {
    private static final Set<Class<? extends ClientPacket>> IMMEDIATE_PROCESS_PACKETS = Set.of(
            ClientHandshakePacket.class, // First received packet
            ClientCookieResponsePacket.class,
            ClientStatusRequestPacket.class,
            ClientPingRequestPacket.class,
            ClientKeepAlivePacket.class, // Used to calculate latency
            ClientLoginStartPacket.class,
            ClientEncryptionResponsePacket.class, // Auth request
            ClientLoginPluginResponsePacket.class,
            ClientSelectKnownPacksPacket.class, // Immediate answer to server request on config
            ClientLoginAcknowledgedPacket.class, // Handle config state
            ClientFinishConfigurationPacket.class // Enter play state
    );

    private static final ListenerHandle<PlayerPacketOutEvent> OUTGOING_HANDLE = EventDispatcher.getHandle(PlayerPacketOutEvent.class);

    private final SocketChannel channel;
    private SocketAddress remoteAddress;

    //Could be null. Only used for Mojang Auth
    private volatile @Nullable EncryptionContext encryptionContext;
    private final byte[] nonce = new byte[4];

    // Data from client packets
    private @Nullable String loginUsername;
    private @Nullable GameProfile gameProfile;
    private @Nullable String serverAddress;
    private int serverPort;
    private int protocolVersion;

    private final NetworkBuffer readBuffer = NetworkBuffer.resizableBuffer(ServerFlag.POOLED_BUFFER_SIZE, MinecraftServer.process());
    private final MessagePassingQueue<SendablePacket> packetQueue = ConcurrentMessageQueues.mpscUnboundedArrayQueue(1024);
    private final Thread readThread, writeThread;

    private final AtomicLong sentPacketCounter = new AtomicLong();
    private @Nullable NetworkBuffer writeLeftover = null;
    // Index where compression starts, linked to `sentPacketCounter`
    // Used instead of a simple boolean so we can get proper timing for serialization
    private volatile long compressionStart = Long.MAX_VALUE;

    // Write lock as the default behavior of the writing thread is to park itself
    // Requires ServerFlag.FASTER_SOCKET_WRITES to be enabled
    private final AtomicBoolean writeSignaled = new AtomicBoolean(false);

    public PlayerSocketConnection(SocketChannel channel, SocketAddress remoteAddress, Thread readThread, Thread writeThread) {
        super();
        this.channel = channel;
        this.remoteAddress = remoteAddress;
        this.writeThread = writeThread;
        this.readThread = readThread;
    }

    @Blocking
    public void read(PacketReader<ClientPacket> packetReader) throws IOException {
        NetworkBuffer readBuffer = this.readBuffer;
        final long writeIndex = readBuffer.writeIndex();
        final int length = readBuffer.readChannel(channel);
        // Decrypt newly read data
        final EncryptionContext encryptionContext = this.encryptionContext;
        if (!ServerFlag.DISABLE_ENCRYPTION && encryptionContext != null) {
            readBuffer.cipher(encryptionContext.decrypt(), writeIndex, length);
        }
        // Process packets
        processPackets(readBuffer, packetReader);
    }

    private boolean compression() {
        return compressionStart != Long.MAX_VALUE;
    }

    private void processPackets(NetworkBuffer readBuffer, PacketReader<ClientPacket> packetReader) {
        final ConnectionState startingState = getClientState();
        final PacketReader.Result<ClientPacket> result;
        try {
            result = packetReader.readPackets(
                    readBuffer,
                    startingState, PacketVanilla::nextClientState,
                    compression()
            );
        } catch (DataFormatException | RuntimeException e) {
            // Pass any errors to the exception manager
            // Except ones that were generated in the starting state of X (Likely garbage)
            // Should cutdown on random packets sent to the server from scanners causing stack traces.
            // Note: if the last packet is the one that errors in a different state, this can't account for that.
            if (startingState.ordinal() > ServerFlag.SUPPRESS_PACKET_ERROR_LEVEL)
                MinecraftServer.getExceptionManager().handleException(e);
            // If anything is thrown, all packets in the queue will be lost here.
            // So it's highly recommended to disconnect to avoid more invalid state.
            if (ServerFlag.REJECT_MALFORMED_PACKET) disconnect();
            return;
        }
        switch (result) {
            case PacketReader.Result.Success<ClientPacket> success -> {
                for (PacketReader.ParsedPacket<ClientPacket> parsedPacket : success.packets()) {
                    final ClientPacket packet = parsedPacket.packet();

                    try {
                        final boolean processImmediately = IMMEDIATE_PROCESS_PACKETS.contains(packet.getClass());
                        if (processImmediately) {
                            // Interpret the packet using the connection state we received it.
                            MinecraftServer.getPacketListenerManager().processClientPacket(packet, this);
                        } else {
                            // To be processed during the next player tick
                            final Player player = getPlayer();
                            assert player != null;
                            player.addPacketToQueue(packet);
                        }
                    } catch (Exception e) {
                        if (startingState.ordinal() > ServerFlag.SUPPRESS_PACKET_ERROR_LEVEL)
                            MinecraftServer.getExceptionManager().handleException(e);
                        // Note this does not affect packets in the queue.
                        if (ServerFlag.REJECT_MISUSED_PACKET) disconnect();
                    }
                }
                // Compact in case of incomplete read
                readBuffer.compact();
            }
            case PacketReader.Result.Empty<ClientPacket> _ -> {
                // Empty
            }
            case PacketReader.Result.Failure<ClientPacket> failure -> {
                // Resize for next read
                final long requiredCapacity = failure.requiredCapacity();
                assert requiredCapacity > readBuffer.capacity() :
                        "New capacity should be greater than the current one: " + requiredCapacity + " <= " + readBuffer.capacity();
                readBuffer.resize(requiredCapacity);
            }
        }
    }

    /**
     * Sets the encryption key and add the codecs to the pipeline.
     *
     * @param secretKey the secret key to use in the encryption
     * @throws IllegalStateException if encryption is already enabled for this connection
     */
    public void setEncryptionKey(SecretKey secretKey) {
        if (ServerFlag.DISABLE_ENCRYPTION) throw new UnsupportedOperationException("Encryption is disabled");
        Check.stateCondition(encryptionContext != null, "Encryption is already enabled!");
        this.encryptionContext = new EncryptionContext(MojangCrypt.getCipher(1, secretKey), MojangCrypt.getCipher(2, secretKey));
    }

    /**
     * Enables compression and add a new codec to the pipeline.
     *
     * @throws IllegalStateException if encryption is already enabled for this connection
     */
    public void startCompression() {
        Check.stateCondition(compression(), "Compression is already enabled!");
        this.compressionStart = sentPacketCounter.get();
        final int threshold = ServerFlag.COMPRESSION_THRESHOLD;
        Check.stateCondition(threshold == 0, "Compression cannot be enabled because the threshold is equal to 0");
        sendPacket(new SetCompressionPacket(threshold));
    }

    @Override
    public void sendPacket(SendablePacket packet) {
        this.packetQueue.relaxedOffer(packet);
        tryUnlockWriteThread();
    }

    @Override
    public void sendPackets(Collection<? extends SendablePacket> packets) {
        for (SendablePacket packet : packets) this.packetQueue.relaxedOffer(packet);
        tryUnlockWriteThread();
    }

    // Requires ServerFlag.FASTER_SOCKET_WRITES
    public void tryUnlockWriteThread() {
        if (!ServerFlag.FASTER_SOCKET_WRITES) return;
        if (this.writeSignaled.compareAndSet(false, true)) {
            unlockWriteThread();
        }
    }

    @ApiStatus.Internal
    public void unlockWriteThread() {
        if (!ServerFlag.FASTER_SOCKET_WRITES) return;
        LockSupport.unpark(writeThread);
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Changes the internal remote address field.
     * <p>
     * Mostly unsafe, used internally when interacting with a proxy.
     *
     * @param remoteAddress the new connection remote address
     */
    public void setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public @Nullable GameProfile gameProfile() {
        return gameProfile;
    }

    public void UNSAFE_setProfile(GameProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

    /**
     * Retrieves the username received from the client during connection.
     * <p>
     * This value has not been checked and could be anything.
     *
     * @return the username given by the client, unchecked
     */
    public @Nullable String getLoginUsername() {
        return loginUsername;
    }

    /**
     * Sets the internal login username field.
     *
     * @param loginUsername the new login username field
     */
    public void UNSAFE_setLoginUsername(String loginUsername) {
        this.loginUsername = loginUsername;
    }

    /**
     * Gets the server address that the client used to connect.
     * <p>
     * WARNING: it is given by the client, it is possible for it to be wrong.
     *
     * @return the server address used
     */
    @Override
    public @Nullable String getServerAddress() {
        return serverAddress;
    }

    /**
     * Gets the server port that the client used to connect.
     * <p>
     * WARNING: it is given by the client, it is possible for it to be wrong.
     *
     * @return the server port used
     */
    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public void disconnect() {
        super.disconnect();
        unlockWriteThread();
    }

    /**
     * Gets the protocol version of a client.
     *
     * @return protocol version of client.
     */
    @Override
    public int getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Used in {@link ClientHandshakePacket} to change the internal fields.
     *
     * @param serverAddress   the server address which the client used
     * @param serverPort      the server port which the client used
     * @param protocolVersion the protocol version which the client used
     */
    public void refreshServerInformation(@Nullable String serverAddress, int serverPort, int protocolVersion) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.protocolVersion = protocolVersion;
    }

    public byte[] getNonce() {
        if (ServerFlag.DISABLE_ENCRYPTION) throw new UnsupportedOperationException("Encryption is disabled");
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        if (ServerFlag.DISABLE_ENCRYPTION) throw new UnsupportedOperationException("Encryption is disabled");
        System.arraycopy(nonce, 0, this.nonce, 0, nonce.length);
    }

    private boolean writeSendable(NetworkBuffer buffer, SendablePacket sendable, boolean compressed, PacketWriter<ServerPacket> writer) {
        final long start = buffer.writeIndex();
        final boolean result = writePacketSync(buffer, sendable, compressed, writer);
        if (!result) return false;
        // Encrypt data
        final long length = buffer.writeIndex() - start;
        final EncryptionContext encryptionContext = this.encryptionContext;
        if (!ServerFlag.DISABLE_ENCRYPTION && encryptionContext != null && length > 0) { // Encryption support
            buffer.cipher(encryptionContext.encrypt(), start, length);
        }
        return true;
    }

    private boolean writePacketSync(NetworkBuffer buffer, SendablePacket packet, boolean compressed, PacketWriter<ServerPacket> writer) {
        final Player player = getPlayer();
        final ConnectionState state = getServerState();
        if (player != null) {
            // Outgoing event
            if (OUTGOING_HANDLE.hasListener()) {
                final ServerPacket serverPacket = SendablePacket.extractServerPacket(packet, state, writer);
                if (serverPacket != null) { // Events are not called for buffered packets
                    PlayerPacketOutEvent event = new PlayerPacketOutEvent(player, serverPacket);
                    OUTGOING_HANDLE.call(event);
                    if (event.isCancelled()) return true;
                }
            }
            // Translation
            if (ServerFlag.AUTOMATIC_COMPONENT_TRANSLATION && packet instanceof ServerPacket.ComponentHolding) {
                packet = ((ServerPacket.ComponentHolding) packet).copyWithOperator(component ->
                        MinestomAdventure.COMPONENT_TRANSLATOR.apply(component, Objects.requireNonNullElseGet(player.getLocale(), MinestomAdventure::getDefaultLocale)));
            }
        }
        // Write packet
        final long start = buffer.writeIndex();
        final int compressionThreshold = compressed ? ServerFlag.COMPRESSION_THRESHOLD : 0;
        try {
            return switch (packet) {
                case ServerPacket serverPacket -> {
                    var nextState = PacketVanilla.nextServerState(serverPacket, state);
                    if (nextState != state) setServerState(nextState);

                    writer.writeFramedPacket(buffer, state, serverPacket, compressionThreshold);
                    yield true;
                }
                case FramedPacket framedPacket -> {
                    final NetworkBuffer body = framedPacket.body();
                    yield writeBuffer(buffer, body, 0, body.capacity());
                }
                case CachedPacket cachedPacket -> {
                    final NetworkBuffer body = cachedPacket.body(state, writer);
                    if (body != null) {
                        yield writeBuffer(buffer, body, 0, body.capacity());
                    } else {
                        writer.writeFramedPacket(buffer, state, cachedPacket.packet(state, writer), compressionThreshold);
                        yield true;
                    }
                }
                case LazyPacket lazyPacket -> {
                    writer.writeFramedPacket(buffer, state, lazyPacket.packet(), compressionThreshold);
                    yield true;
                }
                case BufferedPacket bufferedPacket -> {
                    final NetworkBuffer rawBuffer = bufferedPacket.buffer();
                    final long index = bufferedPacket.index();
                    final long length = bufferedPacket.length();
                    yield writeBuffer(buffer, rawBuffer, index, length);
                }
            };
        } catch (IndexOutOfBoundsException exception) {
            buffer.writeIndex(start);
            return false;
        }
    }

    private boolean writeBuffer(NetworkBuffer buffer, NetworkBuffer body, long index, long length) {
        if (buffer.writableBytes() < length) {
            // Not enough space in the buffer
            return false;
        }
        NetworkBuffer.copy(body, index, buffer, buffer.writeIndex(), length);
        buffer.advanceWrite(length);
        return true;
    }

    @Blocking
    public void awaitFlush() throws InterruptedException {
        // Consume queued packets
        final var packetQueue = this.packetQueue;
        if (!packetQueue.isEmpty()) return;
        if (ServerFlag.FASTER_SOCKET_WRITES) {
            assert this.writeThread == Thread.currentThread() : "writeThread should be the current thread";
            this.writeSignaled.set(false);
            // We cant sleep forever if writeLeftover still exists, we fall back to a fixed parkNanos, which is also spirituous
            final NetworkBuffer writeLeftover = this.writeLeftover;
            if (writeLeftover != null) {
                LockSupport.parkNanos(writeLeftover, 1_000_000 / ServerFlag.SERVER_TICKS_PER_SECOND / 2);
            } else {
                LockSupport.park(this);
            }
        } else {
            Thread.sleep(1000 / ServerFlag.SERVER_TICKS_PER_SECOND / 2);
        }
    }

    @Blocking
    public void flushSync(PacketWriter<ServerPacket> writer) throws IOException {
        final var channel = this.channel;
        if (!channel.isConnected()) throw new EOFException("Channel is closed");
        // Write leftover if any
        final NetworkBuffer leftover = this.writeLeftover;
        if (leftover != null) {
            final boolean success = leftover.writeChannel(channel);
            if (success) {
                this.writeLeftover = null;
                writer.bufferPool().add(leftover);
            } else {
                // Failed to write the whole leftover, try again next flush
                return;
            }
        }
        final MessagePassingQueue<SendablePacket> packetQueue = this.packetQueue;
        if (packetQueue.isEmpty()) return; // Nothing to write, no need to access the pool
        final ObjectPool<NetworkBuffer> bufferPool = writer.bufferPool();
        final NetworkBuffer buffer = bufferPool.get();
        // Write to buffer
        final AtomicLong sentPacketCounter = this.sentPacketCounter;
        final long compressionStart = this.compressionStart;
        int written = 0;
        while (!packetQueue.isEmpty()) {
            SendablePacket packet = packetQueue.peek();
            final boolean compressed = sentPacketCounter.get() > compressionStart;
            final boolean success = writeSendable(buffer, packet, compressed, writer);
            if (success) {
                sentPacketCounter.getAndIncrement();
                packetQueue.poll();
                written++;
                assert buffer.writeIndex() > 0;
                continue;
            }
            if (written < 1) {
                // Need to try again with a bigger buffer
                long newSize = Math.min(buffer.capacity() * 2, ServerFlag.MAX_PACKET_SIZE);
                if (newSize == buffer.capacity()) break; // Reached max size
                buffer.resize(newSize);
            } else {
                // Already wrote enough packets, break
                break;
            }
        }
        // Write to channel
        final boolean success = buffer.writeChannel(channel);
        // Keep the buffer if not fully written
        if (success) bufferPool.add(buffer);
        else this.writeLeftover = buffer;
    }

    public Thread readThread() {
        return readThread;
    }

    public Thread writeThread() {
        return writeThread;
    }

    record EncryptionContext(Cipher encrypt, Cipher decrypt) {
    }
}
