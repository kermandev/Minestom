package net.minestom.server.network;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.I_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

@JCStressTest
@Outcome(id = "2", expect = ACCEPTABLE)
@State
public class NetworkBufferPoolStressTest {
    private final NetworkBufferPool pool;

    public NetworkBufferPoolStressTest() {
        pool = NetworkBufferPool.pool(32 * 1024 * 1024);
        // Pre-populate with two segments
        NetworkBuffer b1 = pool.acquireStatic(512);
        NetworkBuffer b2 = pool.acquireStatic(512);
        pool.release(b1);
        pool.release(b2);
    }

    @Actor
    public void actor1() {
        NetworkBuffer buffer = pool.acquireStatic(512);
        pool.release(buffer);
    }

    @Actor
    public void actor2() {
        NetworkBuffer buffer = pool.acquireStatic(512);
        pool.release(buffer);
    }

    @Arbiter
    public void arbiter(I_Result r) {
        r.r1 = pool.size();
    }
}
