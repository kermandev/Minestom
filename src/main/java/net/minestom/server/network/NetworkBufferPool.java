package net.minestom.server.network;

import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

@ApiStatus.Experimental
public interface NetworkBufferPool {
    static NetworkBufferPool pool() {
        return pool(32 * 1024 * 1024);
    }

    static NetworkBufferPool pool(long maxCachedBytes) {
        return shared((size, align) -> Arena.ofAuto().allocate(size, align), maxCachedBytes);
    }

    static NetworkBufferPool shared(SegmentAllocator allocator, long maxCachedBytes) {
        return new NetworkBufferPoolImpl(allocator, maxCachedBytes, true);
    }

    static NetworkBufferPool confined(SegmentAllocator allocator, long maxCachedBytes) {
        return new NetworkBufferPoolImpl(allocator, maxCachedBytes, false);
    }

    @ApiStatus.OverrideOnly
    MemorySegment leaseSegment(long minimumCapacity);

    @ApiStatus.OverrideOnly
    void returnSegment(MemorySegment segment);

    default NetworkBuffer acquireStatic(long minimumCapacity) {
        return acquireStatic(minimumCapacity, null);
    }

    default NetworkBuffer acquireStatic(long minimumCapacity, @Nullable Registries registries) {
        MemorySegment segment = leaseSegment(minimumCapacity);
        return new NetworkBufferImpl(segment, 0, 0, null, registries);
    }

    default NetworkBuffer acquireResizeable(long minimumInitialCapacity) {
        return acquireResizeable(minimumInitialCapacity, NetworkBuffer.AutoResize.DOUBLE);
    }

    default NetworkBuffer acquireResizeable(long minimumInitialCapacity, @Nullable Registries registries) {
        return acquireResizeable(minimumInitialCapacity, NetworkBuffer.AutoResize.DOUBLE, registries);
    }

    default NetworkBuffer acquireResizeable(long minimumInitialCapacity, NetworkBuffer.AutoResize resizeStrategy) {
        return acquireResizeable(minimumInitialCapacity, resizeStrategy, null);
    }

    default NetworkBuffer acquireResizeable(long minimumInitialCapacity, NetworkBuffer.AutoResize resizeStrategy, @Nullable Registries registries) {
        NetworkBufferImpl.ResizeableSegmentAllocator allocator = new NetworkBufferImpl.ResizeableSegmentAllocator.Pooled(this, resizeStrategy);
        MemorySegment segment = leaseSegment(minimumInitialCapacity);
        return new NetworkBufferImpl(segment, 0, 0, allocator, registries);
    }

    default void release(NetworkBuffer buffer) {
        NetworkBufferImpl bufferImpl = (NetworkBufferImpl) buffer;
        MemorySegment segmentToRelease = bufferImpl.extractSegment();
        returnSegment(segmentToRelease);
    }

    void clear();

    int size();

    long byteSize();
}
