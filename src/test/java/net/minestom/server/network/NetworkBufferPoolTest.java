package net.minestom.server.network;

import org.junit.jupiter.api.Test;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import static org.junit.jupiter.api.Assertions.*;

public class NetworkBufferPoolTest {

    @Test
    public void testPoolAcquireReleaseStatic() {
        NetworkBufferPool pool = NetworkBufferPool.pool(32 * 1024 * 1024);
        NetworkBuffer buffer1 = pool.acquireStatic(512);
        assertTrue(buffer1.capacity() >= 512);
        assertFalse(buffer1.isReadOnly());

        // Write and read
        buffer1.write(NetworkBuffer.INT, 42);
        assertEquals(42, buffer1.read(NetworkBuffer.INT));

        // Release back to the pool
        pool.release(buffer1);

        // After release, buffer operations should throw
        assertThrows(UnsupportedOperationException.class, () -> buffer1.read(NetworkBuffer.INT));

        // Verify segment reuse by leasing/returning directly via pool API
        MemorySegment segment1 = pool.leaseSegment(512);
        assertNotNull(segment1);
        pool.returnSegment(segment1);

        MemorySegment segment2 = pool.leaseSegment(512);
        assertSame(segment1, segment2);
        pool.returnSegment(segment2);
    }

    @Test
    public void testPoolAcquireReleaseResizeable() {
        NetworkBufferPool pool = NetworkBufferPool.pool(32 * 1024 * 1024);
        NetworkBuffer buffer = pool.acquireResizeable(256);
        assertEquals(256, buffer.capacity());

        // Force resize by writing more than capacity
        buffer.writeIndex(256);
        buffer.write(NetworkBuffer.INT, 100); // Exceeds 256
        assertTrue(buffer.capacity() > 256);

        // At this point, the old 256 segment should be back in the pool
        assertEquals(1, pool.size());
        assertEquals(256, pool.byteSize());

        // Release the buffer (which now holds the resized segment, e.g. 512)
        pool.release(buffer);

        // Both the 256 and the resized (512) segments should be back in the pool
        assertEquals(2, pool.size());
        assertEquals(768, pool.byteSize());

        // Let's verify by leasing size 512, which should be returned from the pool (no new allocation)
        MemorySegment leased512 = pool.leaseSegment(512);
        assertNotNull(leased512);
        assertTrue(leased512.byteSize() >= 512);

        // After leasing 512, pool should only have the 256 segment left
        assertEquals(1, pool.size());
        assertEquals(256, pool.byteSize());

        pool.returnSegment(leased512);
    }

    @Test
    public void testPoolReadOnlyRelease() {
        NetworkBufferPool pool = NetworkBufferPool.pool(32 * 1024 * 1024);
        NetworkBuffer buffer = pool.acquireStatic(512);

        // Make read-only
        NetworkBuffer readOnlyBuffer = buffer.readOnly();
        assertTrue(readOnlyBuffer.isReadOnly());
        assertThrows(UnsupportedOperationException.class, () -> readOnlyBuffer.write(NetworkBuffer.INT, 1));

        // Release should throw IllegalArgumentException on the read-only view
        assertThrows(IllegalArgumentException.class, () -> pool.release(readOnlyBuffer));

        // Releasing the original buffer should succeed
        pool.release(buffer);
    }

    @Test
    public void testDoubleReleaseThrows() {
        NetworkBufferPool pool = NetworkBufferPool.pool(32 * 1024 * 1024);
        NetworkBuffer buffer = pool.acquireStatic(512);

        // First release succeeds
        pool.release(buffer);

        // Second release throws because segment is null (IllegalStateException)
        assertThrows(IllegalStateException.class, () -> pool.release(buffer));

        // Reading throws because it is now a dummy buffer (UnsupportedOperationException)
        assertThrows(UnsupportedOperationException.class, () -> buffer.read(NetworkBuffer.INT));
    }

    @Test
    public void testPoolUpperBound() {
        NetworkBufferPool pool = NetworkBufferPool.shared(Arena.ofAuto(), 16 * 1024);

        MemorySegment segment1 = pool.leaseSegment(512);
        MemorySegment segment2 = pool.leaseSegment(512);
        MemorySegment segment3 = pool.leaseSegment(512);

        // Release all three. Since upper bound is 2, the 3rd should be discarded
        pool.returnSegment(segment1);
        pool.returnSegment(segment2);
        pool.returnSegment(segment3);

        // Acquire 3 again
        MemorySegment s1 = pool.leaseSegment(512);
        MemorySegment s2 = pool.leaseSegment(512);
        MemorySegment s3 = pool.leaseSegment(512);

        // The first two must be reused segments (from segment1 and segment2, in poll order)
        assertTrue(s1 == segment1 || s1 == segment2);
        assertTrue(s2 == segment1 || s2 == segment2);
        assertNotSame(s1, s2);

        // The third must be newly allocated (different from all three original segments)
        assertNotSame(segment1, s3);
        assertNotSame(segment2, s3);
        assertNotSame(segment3, s3);

        pool.returnSegment(s1);
        pool.returnSegment(s2);
        pool.returnSegment(s3);
    }

    @Test
    public void testConfinedPool() {
        try (Arena arena = Arena.ofConfined()) {
            NetworkBufferPool pool = NetworkBufferPool.confined(arena, 32 * 1024 * 1024);
            MemorySegment segment = pool.leaseSegment(512);
            assertNotNull(segment);

            // Release back to the pool
            pool.returnSegment(segment);

            // Re-acquire and verify it gets the same segment (reused)
            MemorySegment segment2 = pool.leaseSegment(512);
            assertSame(segment, segment2);

            pool.returnSegment(segment2);
        }
    }

    @Test
    public void testPoolClearAndClose() {
        NetworkBufferPool pool = NetworkBufferPool.pool(32 * 1024 * 1024);
        MemorySegment segment1 = pool.leaseSegment(512);

        pool.returnSegment(segment1);

        // Re-acquire to prove it was pooled
        MemorySegment segment2 = pool.leaseSegment(512);
        assertSame(segment1, segment2);
        pool.returnSegment(segment2);

        // Now clear the pool
        pool.clear();

        // Re-acquire. It should be a new segment now because the pool was cleared
        MemorySegment segment3 = pool.leaseSegment(512);
        assertNotSame(segment1, segment3);
        pool.returnSegment(segment3);

        // Test clear explicitly clears
        NetworkBufferPool p = NetworkBufferPool.pool(32 * 1024 * 1024);
        MemorySegment s1 = p.leaseSegment(512);
        p.returnSegment(s1);
        assertEquals(1, p.size());
        p.clear();
        assertEquals(0, p.size());
    }

    @Test
    public void testPoolSizeAndByteSize() {
        NetworkBufferPool pool = NetworkBufferPool.pool(32 * 1024 * 1024);
        assertEquals(0, pool.size());
        assertEquals(0, pool.byteSize());

        // Acquire two segments of different sizes
        NetworkBuffer buffer1 = pool.acquireStatic(256); // index 0, size 256
        NetworkBuffer buffer2 = pool.acquireStatic(1024); // index 2, size 1024

        // Pool is still empty because they are checked out
        assertEquals(0, pool.size());
        assertEquals(0, pool.byteSize());

        // Release first buffer
        pool.release(buffer1);
        assertEquals(1, pool.size());
        assertEquals(256, pool.byteSize());

        // Release second buffer
        pool.release(buffer2);
        assertEquals(2, pool.size());
        assertEquals(1280, pool.byteSize()); // 256 + 1024 = 1280

        // Clear pool
        pool.clear();
        assertEquals(0, pool.size());
        assertEquals(0, pool.byteSize());
    }

    @Test
    public void testGlobalMemoryCap() {
        // maxCachedBytes = 1024 * 1024 -> maxMemoryLimit = 1MB = 1048576 bytes
        // 1MB segment size class: 1048576 bytes. maxQueueSizes[12] = Math.max(1, 4 / 32) = 1.
        // 512KB segment size class: 524288 bytes. maxQueueSizes[11] = Math.max(1, 8 / 32) = 1.
        NetworkBufferPool pool = NetworkBufferPool.pool(1024 * 1024);

        MemorySegment segment1MB = pool.leaseSegment(1048576);
        MemorySegment segment512KB = pool.leaseSegment(524288);

        // Release the 1MB segment first
        pool.returnSegment(segment1MB);
        assertEquals(1048576, pool.byteSize());
        assertEquals(1, pool.size());

        // Now releasing the 512KB segment should exceed the 1MB global cap (1MB + 512KB = 1.5MB > 1MB)
        // So the 512KB segment must be discarded (not added to pool, cachedBytes remains 1MB)
        pool.returnSegment(segment512KB);
        assertEquals(1048576, pool.byteSize());
        assertEquals(1, pool.size()); // Still 1 segment (the 1MB one)

        // Verify that the 1MB segment is reused
        MemorySegment reacquired1MB = pool.leaseSegment(1048576);
        assertSame(segment1MB, reacquired1MB);

        // Verify that the 512KB segment is NOT reused (since it was discarded)
        MemorySegment reacquired512KB = pool.leaseSegment(524288);
        assertNotSame(segment512KB, reacquired512KB);

        pool.returnSegment(reacquired1MB);
        pool.returnSegment(reacquired512KB);
    }
}
