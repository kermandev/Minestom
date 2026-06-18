package net.minestom.server.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.Viewable;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferPool;
import net.minestom.server.network.packet.PacketWriting;
import net.minestom.server.network.packet.server.BufferedPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApiStatus.Internal
public final class PacketViewableUtils {
    // Viewable packets
    private static final @Nullable NetworkBufferPool VIEWABLE_POOL =
            !ServerFlag.INSIDE_TEST ? NetworkBufferPool.pool(ServerFlag.PACKET_POOL_SIZE) : null;

    private static NetworkBufferPool pool() {
        return VIEWABLE_POOL == null ? MinecraftServer.process().pool() : VIEWABLE_POOL;
    }

    private static volatile ConcurrentMap<Viewable, ViewableStorage> storageMap = new ConcurrentHashMap<>();

    public static void prepareViewablePacket(Viewable viewable, ServerPacket serverPacket,
                                             @Nullable Entity entity) {
        if (entity != null && !entity.hasPredictableViewers()) {
            // Operation cannot be optimized
            entity.sendPacketToViewers(serverPacket);
            return;
        }
        if (!ServerFlag.VIEWABLE_PACKET) {
            PacketSendingUtils.sendGroupedPacket(viewable.getViewers(), serverPacket, value -> !Objects.equals(value, entity));
            return;
        }
        final Player exception = entity instanceof Player ? (Player) entity : null;
        while (true) {
            // If the storage has been processed (retired) concurrently during a flush,
            // retry to obtain the newly swapped storage instance.
            ViewableStorage storage = storageMap.computeIfAbsent(viewable, _ -> new ViewableStorage());
            if (storage.append(serverPacket, exception)) {
                break;
            }
        }
    }

    public static void flush() {
        if (!ServerFlag.VIEWABLE_PACKET) return;
        ConcurrentMap<Viewable, ViewableStorage> map = storageMap;
        if (map.isEmpty()) return;
        storageMap = new ConcurrentHashMap<>();
        map.entrySet().parallelStream().forEach(entry ->
                entry.getValue().process(entry.getKey()));
    }

    public static void prepareViewablePacket(Viewable viewable, ServerPacket serverPacket) {
        prepareViewablePacket(viewable, serverPacket, null);
    }

    private static final class ViewableStorage {
        // Player id -> list of offsets to ignore (32:32 bits)
        private final Int2ObjectMap<LongArrayList> entityIdMap = new Int2ObjectOpenHashMap<>();
        private final NetworkBuffer buffer = pool().acquireResizeable(ServerFlag.POOLED_BUFFER_SIZE);
        private boolean processed = false; // guarded by this

        private synchronized boolean append(ServerPacket serverPacket, @Nullable Player exception) {
            if (processed) return false; // retry
            final NetworkBuffer buffer = this.buffer;
            final long start = buffer.writeIndex();
            // Viewable storage is only used for play packets, so fine to assume this.
            PacketWriting.writeFramedPacket(pool(), buffer, ConnectionState.PLAY, serverPacket, MinecraftServer.getCompressionThreshold());
            final long end = buffer.writeIndex();
            if (exception != null) {
                final long offsets = start << 32 | end & 0xFFFFFFFFL;
                LongList list = entityIdMap.computeIfAbsent(exception.getEntityId(), _ -> new LongArrayList());
                list.add(offsets);
            }
            return true;
        }

        private synchronized void process(Viewable viewable) {
            processed = true;
            final NetworkBuffer buffer = this.buffer;
            if (buffer.writeIndex() > 0) {
                NetworkBuffer copy = buffer.copy(0, buffer.writeIndex()).readOnly();
                viewable.getViewers().forEach(player -> processPlayer(player, copy));
            }
            pool().release(buffer);
        }

        private void processPlayer(Player player, NetworkBuffer buffer) {
            final long capacity = buffer.capacity();
            final PlayerConnection connection = player.getPlayerConnection();
            final LongArrayList pairs = entityIdMap.get(player.getEntityId());
            if (pairs == null) {
                // No range exception, write the whole buffer
                connection.sendPacket(new BufferedPacket(buffer, 0, capacity));
                return;
            }
            // Player has range exception(s)
            // Ensure that we skip the specified parts of the buffer
            int lastWrite = 0;
            final long[] elements = pairs.elements();
            for (int i = 0; i < pairs.size(); ++i) {
                final long offsets = elements[i];
                final int start = (int) (offsets >> 32);
                if (start != lastWrite) connection.sendPacket(new BufferedPacket(buffer, lastWrite, start - lastWrite));
                lastWrite = (int) offsets; // End = last 32 bits
            }
            if (capacity != lastWrite) connection.sendPacket(new BufferedPacket(buffer, lastWrite, capacity - lastWrite));
        }

    }
}
