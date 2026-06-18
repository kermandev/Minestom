package net.minestom.server.network;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.foreign.Arena;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 8, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class NetworkBufferPoolBenchmark {

    private NetworkBufferPool concurrentPool;

    @Setup
    public void setup() {
        concurrentPool = NetworkBufferPool.pool(32 * 1024 * 1024);
    }

    @TearDown
    public void teardown() {
        concurrentPool.clear();
    }

    @State(Scope.Thread)
    public static class ThreadState {
        private final int[] sizeSequence = {
                256, 512, 256, 1024, 512, 4096, 1024, 16384, 256, 65536, 4096, 262144
        };
        private int sequenceIndex = 0;
        private Arena confinedArena;
        private NetworkBufferPool confinedPool;

        @Setup
        public void setup() {
            confinedArena = Arena.ofConfined();
            confinedPool = NetworkBufferPool.confined(confinedArena, 4 * 1024 * 1024);
        }

        @TearDown
        public void teardown() {
            confinedPool.clear();
            confinedArena.close();
        }

        int nextSize() {
            int size = sizeSequence[sequenceIndex];
            sequenceIndex = (sequenceIndex + 1) % sizeSequence.length;
            return size;
        }
    }

    @Benchmark
    @Threads(1)
    public void concurrentAcquireRelease_1Thread(ThreadState threadState, Blackhole blackhole) {
        int size = threadState.nextSize();
        NetworkBuffer buffer = concurrentPool.acquireStatic(size);
        blackhole.consume(buffer);
        concurrentPool.release(buffer);
    }

    @Benchmark
    @Threads(4)
    public void concurrentAcquireRelease_4Threads(ThreadState threadState, Blackhole blackhole) {
        int size = threadState.nextSize();
        NetworkBuffer buffer = concurrentPool.acquireStatic(size);
        blackhole.consume(buffer);
        concurrentPool.release(buffer);
    }

    @Benchmark
    @Threads(8)
    public void concurrentAcquireRelease_8Threads(ThreadState threadState, Blackhole blackhole) {
        int size = threadState.nextSize();
        NetworkBuffer buffer = concurrentPool.acquireStatic(size);
        blackhole.consume(buffer);
        concurrentPool.release(buffer);
    }

    @Benchmark
    @Threads(16)
    public void concurrentAcquireRelease_16Threads(ThreadState threadState, Blackhole blackhole) {
        int size = threadState.nextSize();
        NetworkBuffer buffer = concurrentPool.acquireStatic(size);
        blackhole.consume(buffer);
        concurrentPool.release(buffer);
    }

    @Benchmark
    @Threads(4)
    public void confinedAcquireRelease_4Threads(ThreadState threadState, Blackhole blackhole) {
        int size = threadState.nextSize();
        NetworkBuffer buffer = threadState.confinedPool.acquireStatic(size);
        blackhole.consume(buffer);
        threadState.confinedPool.release(buffer);
    }

    @Benchmark
    @Threads(4)
    public void concurrentResizeable_4Threads(Blackhole blackhole) {
        int initialSize = 256;
        NetworkBuffer buffer = concurrentPool.acquireResizeable(initialSize);
        buffer.writeIndex(initialSize);
        // Force resize twice
        buffer.ensureWritable(512);
        buffer.writeIndex(512);
        buffer.ensureWritable(2048);
        buffer.writeIndex(2048);
        blackhole.consume(buffer);
        concurrentPool.release(buffer);
    }

    @Benchmark
    @Threads(4)
    public void allocateOnDemand_4Threads(ThreadState threadState, Blackhole blackhole) {
        int size = threadState.nextSize();
        NetworkBuffer buffer = NetworkBuffer.staticBuffer(size);
        blackhole.consume(buffer);
    }
}
