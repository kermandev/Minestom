package net.minestom.server.network;

import net.minestom.server.utils.collection.ConcurrentMessageQueues;
import org.jetbrains.annotations.ApiStatus;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.Queue;

@ApiStatus.Internal
final class NetworkBufferPoolImpl implements NetworkBufferPool {
    private static final int MIN_POOL_SIZE = 256;
    private static final int POOL_COUNT = 14; // 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152
    private static final int[] BASE_MAX_QUEUE_SIZES = {
        4096, // 256B (1MB)
        4096, // 512B (2MB)
        4096, // 1KB (4MB)
        4096, // 2KB (8MB)
        4096, // 4KB (16MB)
        2048, // 8KB (16MB)
        1024, // 16KB (16MB)
        512,  // 32KB (16MB)
        256,  // 64KB (16MB)
        128,  // 128KB (16MB)
        64,   // 256KB (16MB)
        32,   // 512KB (16MB)
        16,   // 1MB (16MB)
        8     // 2MB (16MB)
    };

    private static final VarHandle CACHED_BYTES;
    static {
        try {
            CACHED_BYTES = MethodHandles.lookup().findVarHandle(NetworkBufferPoolImpl.class, "cachedBytes", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private final Queue<MemorySegment>[] pools = new Queue[POOL_COUNT];
    private final int[] maxQueueSizes = new int[POOL_COUNT];
    private final SegmentAllocator allocator;
    private final boolean concurrent;

    // Padding to prevent false sharing with write-heavy cachedBytes
    @SuppressWarnings("unused")
    private long p00, p01, p02, p03, p04, p05, p06, p07;
    private long cachedBytes = 0L;
    @SuppressWarnings("unused")
    private long p10, p11, p12, p13, p14, p15, p16, p17;

    private final long maxMemoryLimit;

    public NetworkBufferPoolImpl(SegmentAllocator allocator, long maxCachedBytes, boolean concurrent) {
        if (maxCachedBytes < 0) {
            throw new IllegalArgumentException("maxCachedBytes must be non-negative: " + maxCachedBytes);
        }
        this.allocator = allocator;
        this.concurrent = concurrent;
        this.maxMemoryLimit = maxCachedBytes;
        double scale = maxCachedBytes / (32.0 * 1024 * 1024);
        for (int i = 0; i < POOL_COUNT; i++) {
            int scaledSize = maxCachedBytes == 0 ? 0 : Math.max(1, (int) Math.round(BASE_MAX_QUEUE_SIZES[i] * scale));
            maxQueueSizes[i] = Math.min(scaledSize, 4096);
            pools[i] = concurrent ? ConcurrentMessageQueues.mpmcArrayQueue(maxQueueSizes[i]) : new ArrayDeque<>();
        }
    }

    @Override
    public MemorySegment leaseSegment(long minimumCapacity) {
        int index = poolIndex(minimumCapacity);
        if (index >= 0 && index < POOL_COUNT) {
            final boolean concurrent = this.concurrent;
            Queue<MemorySegment> queue = pools[index];
            MemorySegment segment;
            long discardedBytes = 0;
            while ((segment = queue.poll()) != null) {
                long size = segment.byteSize();
                if (segment.scope().isAlive()) {
                    long total = discardedBytes + size;
                    if (concurrent) {
                        CACHED_BYTES.getAndAdd(this, -total);
                    } else {
                        this.cachedBytes -= total;
                    }
                    return segment;
                }
                discardedBytes += size;
            }
            if (discardedBytes > 0) {
                if (concurrent) {
                    CACHED_BYTES.getAndAdd(this, -discardedBytes);
                } else {
                    this.cachedBytes -= discardedBytes;
                }
            }
            long size = MIN_POOL_SIZE << index;
            return allocator.allocate(size);
        }
        // If requested capacity is larger than the max pool size, allocate on-demand
        return allocator.allocate(minimumCapacity);
    }

    @Override
    public void returnSegment(MemorySegment segment) {
        if (!segment.scope().isAlive()) return;
        long capacity = segment.byteSize();
        int index = poolIndex(capacity);
        if (index >= 0 && index < POOL_COUNT && (MIN_POOL_SIZE << index) == capacity) {
            Queue<MemorySegment> queue = pools[index];
            int limit = maxQueueSizes[index];
            if (queue.size() < limit) {
                final long maxMemoryLimit = this.maxMemoryLimit;
                if (this.concurrent) {
                    long current = (long) CACHED_BYTES.getAndAdd(this, capacity);
                    if (current + capacity > maxMemoryLimit) {
                        CACHED_BYTES.getAndAdd(this, -capacity);
                        return;
                    }

                    if (!queue.offer(segment)) {
                        CACHED_BYTES.getAndAdd(this, -capacity); // Refund on queue capacity limit hit or offer failure
                    }
                } else {
                    long current = this.cachedBytes;
                    if (current + capacity > maxMemoryLimit) return;
                    this.cachedBytes = current + capacity;
                    queue.offer(segment);
                }
            }
        }
    }

    static int poolIndex(long capacity) {
        if (capacity <= MIN_POOL_SIZE) return 0;
        return (64 - Long.numberOfLeadingZeros(capacity - 1)) - 8;
    }

    @Override
    public void clear() {
        long clearedBytes = 0;
        for (int i = 0; i < POOL_COUNT; i++) {
            Queue<MemorySegment> queue = pools[i];
            MemorySegment segment;
            while ((segment = queue.poll()) != null) {
                clearedBytes += segment.byteSize();
            }
        }
        if (clearedBytes > 0) {
            if (this.concurrent) {
                CACHED_BYTES.getAndAdd(this, -clearedBytes);
            } else {
                this.cachedBytes -= clearedBytes;
            }
        }
    }

    @Override
    public int size() {
        int total = 0;
        for (int i = 0; i < POOL_COUNT; i++) {
            total += pools[i].size();
        }
        return total;
    }

    @Override
    public long byteSize() {
        return this.concurrent ? (long) CACHED_BYTES.getOpaque(this) : this.cachedBytes;
    }
}
