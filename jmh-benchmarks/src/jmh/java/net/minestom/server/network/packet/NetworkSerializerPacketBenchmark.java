package net.minestom.server.network.packet;

import net.minestom.server.entity.GameMode;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.JoinGamePacket;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class NetworkSerializerPacketBenchmark {
    private static final JoinGamePacket PACKET = new JoinGamePacket(
            12345, true, List.of("world", "nether", "end"), 100, 10, 10,
            false, true, false, 0, "world", 123456789L, GameMode.SURVIVAL,
            GameMode.CREATIVE, false, false, null, 0, 64, true
    );

    private JoinGamePacket packet;
    private NetworkBuffer readBuffer;
    private NetworkBuffer writeBuffer;

    @Setup(Level.Iteration)
    public void setup() {
        packet = PACKET;
        readBuffer = NetworkBuffer.staticBuffer(256);
        readBuffer.write(JoinGamePacket.SERIALIZER, PACKET);
        writeBuffer = NetworkBuffer.staticBuffer(256);
    }

    @Benchmark
    public void writePacket(Blackhole blackhole) {
        writeBuffer.writeAt(0, JoinGamePacket.SERIALIZER, packet);
        blackhole.consume(writeBuffer);
    }

    @Benchmark
    public void readPacket(Blackhole blackhole) {
        blackhole.consume(readBuffer.readAt(0, JoinGamePacket.SERIALIZER));
    }

    @TearDown
    public void teardown(Blackhole blackhole) {
        blackhole.consume(packet);
        blackhole.consume(readBuffer);
        blackhole.consume(writeBuffer);
    }
}