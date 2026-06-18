package net.minestom.server.network;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 8, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class NetworkBufferSyntheticBenchmark {

    private NetworkBufferPool concurrentPool;

    @Setup
    public void setup() {
        concurrentPool = NetworkBufferPool.pool(64 * 1024 * 1024);
    }

    @TearDown
    public void teardown() {
        concurrentPool.clear();
    }

    @State(Scope.Thread)
    public static class BurstState {
        final int[] sizes = { 256, 512, 1024, 2048, 4096, 8192, 16384 };
        final NetworkBuffer[] buffers = new NetworkBuffer[8];
        int index = 0;

        int nextSize() {
            int size = sizes[index];
            index = (index + 1) % sizes.length;
            return size;
        }
    }

    @Benchmark
    @Threads(4)
    public void burstAcquireRelease(BurstState state, Blackhole blackhole) {
        for (int i = 0; i < 8; i++) {
            state.buffers[i] = concurrentPool.acquireStatic(state.nextSize());
        }
        for (int i = 0; i < 8; i++) {
            blackhole.consume(state.buffers[i]);
        }
        for (int i = 7; i >= 0; i--) {
            concurrentPool.release(state.buffers[i]);
            state.buffers[i] = null;
        }
    }

    @State(Scope.Benchmark)
    public static class PipelineState {
        final ConcurrentLinkedQueue<NetworkBuffer> transferQueue = new ConcurrentLinkedQueue<>();
    }

    @Benchmark
    @Group("pipeline")
    @GroupThreads(2)
    public void pipelineProducer(PipelineState state, BurstState burstState) {
        if (state.transferQueue.size() > 500) {
            Thread.onSpinWait();
            return;
        }
        NetworkBuffer buffer = concurrentPool.acquireStatic(burstState.nextSize());
        buffer.writeIndex(128); 
        state.transferQueue.offer(buffer);
    }

    @Benchmark
    @Group("pipeline")
    @GroupThreads(2)
    public void pipelineConsumer(PipelineState state, Blackhole blackhole) {
        NetworkBuffer buffer = state.transferQueue.poll();
        if (buffer != null) {
            blackhole.consume(buffer);
            concurrentPool.release(buffer);
        }
    }
}
