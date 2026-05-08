package net.minestom.server.network.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IrOptimizerTest {

    @Test
    public void testFoldConstantIf() {
        // Test that If(Const(true), then, else) is replaced by then
        RunIr thenRun = new RunIr(new Value.Const(4L), List.of(new RunItem.Put(StoreKind.INT, new Value.Const(0L), new Value.Const(123))));
        RunIr elseRun = new RunIr(new Value.Const(8L), List.of(new RunItem.Put(StoreKind.LONG, new Value.Const(0L), new Value.Const(456L))));

        RunIr runWithIf = new RunIr(new Value.Const(0L), List.of(
                new RunItem.If(new Value.Const(true), List.of(thenRun), List.of(elseRun))
        ));

        List<RunIr> optimized = IrOptimizer.optimize(List.of(runWithIf));

        assertEquals(1, optimized.size());
        assertEquals(thenRun, optimized.getFirst());
    }

    @Test
    public void testFoldConstantIfFalse() {
        // Test that If(Const(false), then, else) is replaced by else
        RunIr thenRun = new RunIr(new Value.Const(4L), List.of(new RunItem.Put(StoreKind.INT, new Value.Const(0L), new Value.Const(123))));
        RunIr elseRun = new RunIr(new Value.Const(8L), List.of(new RunItem.Put(StoreKind.LONG, new Value.Const(0L), new Value.Const(456L))));

        RunIr runWithIf = new RunIr(new Value.Const(0L), List.of(
                new RunItem.If(new Value.Const(false), List.of(thenRun), List.of(elseRun))
        ));

        List<RunIr> optimized = IrOptimizer.optimize(List.of(runWithIf));

        assertEquals(1, optimized.size());
        assertEquals(elseRun, optimized.getFirst());
    }

    @Test
    public void testMergeRuns() {
        // Test that two consecutive runs with constant sizes are merged
        RunIr run1 = new RunIr(new Value.Const(4L), List.of(new RunItem.Put(StoreKind.INT, new Value.Const(0L), new Value.Const(1))));
        RunIr run2 = new RunIr(new Value.Const(4L), List.of(new RunItem.Put(StoreKind.INT, new Value.Const(0L), new Value.Const(2))));

        List<RunIr> optimized = IrOptimizer.optimize(List.of(run1, run2));

        assertEquals(1, optimized.size());
        RunIr merged = optimized.getFirst();
        assertEquals(new Value.Const(8L), merged.size());
        assertEquals(2, merged.items().size());
        assertEquals(new RunItem.Put(StoreKind.INT, new Value.Const(0L), new Value.Const(1)), merged.items().get(0));
        assertEquals(new RunItem.Put(StoreKind.INT, new Value.Const(4L), new Value.Const(2)), merged.items().get(1));
    }
}
