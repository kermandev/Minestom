package net.minestom.server.network;

import net.minestom.server.utils.ObjectPool;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 8, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class NetworkBufferComparisonBenchmark {

    private ObjectPool<NetworkBuffer> objectPool;
    private NetworkBufferPool ourPool;

    @Setup
    public void setup() {
        objectPool = ObjectPool.pool(
                () -> NetworkBuffer.resizableBuffer(512),
                NetworkBuffer::clear
        );
        ourPool = NetworkBufferPool.pool(32 * 1024 * 1024);
    }

    @TearDown
    public void teardown() {
        objectPool.clear();
        ourPool.clear();
    }

    @State(Scope.Thread)
    public static class ThreadState {
        private final int[] sizeSequence = {
                256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536
        };
        private int index = 0;

        int nextSize() {
            int size = sizeSequence[index];
            index = (index + 1) % sizeSequence.length;
            return size;
        }
    }

    @Benchmark
    @Threads(4)
    public void fixed_ObjectPool(Blackhole blackhole) {
        NetworkBuffer buffer = objectPool.get();
        buffer.writeIndex(0);
        buffer.write(NetworkBuffer.INT, 42);
        blackhole.consume(buffer);
        objectPool.add(buffer);
    }

    @Benchmark
    @Threads(4)
    public void fixed_NetworkBufferPool(Blackhole blackhole) {
        NetworkBuffer buffer = ourPool.acquireStatic(512);
        buffer.write(NetworkBuffer.INT, 42);
        blackhole.consume(buffer);
        ourPool.release(buffer);
    }

    @Benchmark
    @Threads(4)
    public void dynamic_ObjectPool(ThreadState state, Blackhole blackhole) {
        int size = state.nextSize();
        NetworkBuffer buffer = objectPool.get();
        buffer.writeIndex(0);
        if (buffer.capacity() < size) {
            buffer.resize(size);
        }
        buffer.write(NetworkBuffer.INT, 42);
        blackhole.consume(buffer);
        objectPool.add(buffer);
    }

    @Benchmark
    @Threads(4)
    public void dynamic_NetworkBufferPool(ThreadState state, Blackhole blackhole) {
        int size = state.nextSize();
        NetworkBuffer buffer = ourPool.acquireResizeable(256);
        buffer.ensureWritable(size);
        buffer.write(NetworkBuffer.INT, 42);
        blackhole.consume(buffer);
        ourPool.release(buffer);
    }
}
