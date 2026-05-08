package net.minestom.server.network.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void testSimplifyZeroSizeRun() {
        // Test that a zero-size run is marked with reserve = false
        RunIr zeroRun = new RunIr(new Value.Const(0L), List.of(new RunItem.Store(new Value.Const(123), new Local(new LocalType.Kind(java.lang.classfile.TypeKind.INT)))));
        assertTrue(zeroRun.reserve());

        List<RunIr> optimized = IrOptimizer.optimize(List.of(zeroRun));

        assertEquals(1, optimized.size());
        assertFalse(optimized.getFirst().reserve());
    }

    @Test
    public void testRecursiveOptimization() {
        // Test that optimizations are applied inside nested structures
        // Add(Const(1), Const(2)) -> Const(3)
        RunIr innerRun = new RunIr(new Value.Add(new Value.Const(1L), new Value.Const(2L)), List.of());
        
        RunIr outerRun = new RunIr(new Value.Const(0L), List.of(
                new RunItem.If(new Value.Const(true), List.of(innerRun), List.of())
        ));

        List<RunIr> optimized = IrOptimizer.optimize(List.of(outerRun));

        assertEquals(1, optimized.size());
        RunIr result = optimized.getFirst();
        assertEquals(new Value.Const(3L), result.size());
        assertTrue(result.items().isEmpty());
    }

    @Test
    public void testFixedPointOptimization() {
        // Test that one optimization enables another
        // If(true) -> inlines inner, then merge with next
        RunIr inner = new RunIr(new Value.Const(4L), List.of(new RunItem.Put(StoreKind.INT, new Value.Const(0L), new Value.Const(1))));
        RunIr outer = new RunIr(new Value.Const(0L), List.of(new RunItem.If(new Value.Const(true), List.of(inner), List.of())));
        RunIr next = new RunIr(new Value.Const(4L), List.of(new RunItem.Put(StoreKind.INT, new Value.Const(0L), new Value.Const(2))));

        List<RunIr> optimized = IrOptimizer.optimize(List.of(outer, next));

        assertEquals(1, optimized.size());
        assertEquals(new Value.Const(8L), optimized.getFirst().size());
    }

    @Test
    public void testValueOptimizationThroughOptimize() {
        // Test Add optimization: Add(Const(1), Const(2)) -> Const(3)
        RunIr run = new RunIr(new Value.Add(new Value.Const(1L), new Value.Const(2L)), List.of());
        List<RunIr> optimized = IrOptimizer.optimize(List.of(run));
        assertEquals(new Value.Const(3L), optimized.getFirst().size());

        // Test Mul optimization: Mul(Const(2), Const(3)) -> Const(6)
        run = new RunIr(new Value.Mul(new Value.Const(2L), new Value.Const(3L)), List.of());
        optimized = IrOptimizer.optimize(List.of(run));
        assertEquals(new Value.Const(6L), optimized.getFirst().size());

        // Test VarIntSize optimization: VarIntSize(Const(10)) -> Const(1)
        run = new RunIr(new Value.VarIntSize(new Value.Const(10)), List.of());
        optimized = IrOptimizer.optimize(List.of(run));
        assertEquals(new Value.Const(1L), optimized.getFirst().size());
    }
}
