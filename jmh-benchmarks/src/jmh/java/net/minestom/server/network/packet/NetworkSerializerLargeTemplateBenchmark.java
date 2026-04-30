package net.minestom.server.network.packet;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class NetworkSerializerLargeTemplateBenchmark {

    record Packet(boolean var1, boolean var2, boolean var3, boolean var4, boolean var5, boolean var6, boolean var7, boolean var8, boolean var9, boolean var10, boolean var11, boolean var12, boolean var13, boolean var14, boolean var15, boolean var16, boolean var17, boolean var18, boolean var19, boolean var20) {
        private static final NetworkBuffer.Type<Packet> SERIALIZER =
                NetworkBufferTemplate.template(
                        NetworkBuffer.BOOLEAN.optional(), Packet::var1,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var2,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var3,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var4,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var5,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var6,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var7,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var8,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var9,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var10,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var11,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var12,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var13,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var14,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var15,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var16,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var17,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var18,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var19,
                        NetworkBuffer.BOOLEAN.optional(), Packet::var20,
                        Packet::new
                );

        public Packet() {
            this(true, false, true, true, false, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false);
        }
    }
    
    private NetworkBuffer.Type<Packet> serializer;
    private Packet packet;
    private NetworkBuffer readBuffer;
    private NetworkBuffer writeBuffer;

    @Setup
    public void setup() {
        serializer = Packet.SERIALIZER;
        packet = new Packet();
        readBuffer = NetworkBuffer.staticBuffer(256);
        readBuffer.write(serializer, new Packet());
        writeBuffer = NetworkBuffer.staticBuffer(256);
    }

    @Benchmark
    public void writePacket(Blackhole blackhole) {
        var writeBuffer = this.writeBuffer;
        writeBuffer.writeAt(0, serializer, packet);
        blackhole.consume(writeBuffer);
    }

    @Benchmark
    public void readPacket(Blackhole blackhole) {
        blackhole.consume(readBuffer.readAt(0, serializer));
    }

    @TearDown
    public void teardown(Blackhole blackhole) {
        blackhole.consume(serializer);
        blackhole.consume(packet);
        blackhole.consume(readBuffer);
        blackhole.consume(writeBuffer);
    }
}
