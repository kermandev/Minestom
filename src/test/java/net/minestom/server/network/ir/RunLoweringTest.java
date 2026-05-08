package net.minestom.server.network.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RunLoweringTest {

    @Test
    public void testLowerPrimitiveWrite() {
        Value value = new Value.Const(123);
        List<Op> ops = List.of(new Op.WritePrimitive(PrimitiveKind.INT, value));
        List<RunIr> runs = IrOptimizer.optimize(RunLowering.lower(ops));

        assertEquals(1, runs.size());
        RunIr run = runs.getFirst();
        assertEquals(new Value.Const(4L), run.size());
        assertEquals(1, run.items().size());
        assertEquals(new RunItem.Put(StoreKind.INT, new Value.Index(), value), run.items().getFirst());
    }

    @Test
    public void testLowerPrimitiveRead() {
        Local out = new Local(new LocalType.Reference(Integer.class));
        List<Op> ops = List.of(new Op.ReadPrimitive(PrimitiveKind.INT, out));
        List<RunIr> runs = IrOptimizer.optimize(RunLowering.lower(ops));

        assertEquals(1, runs.size());
        RunIr run = runs.getFirst();
        assertEquals(new Value.Const(4L), run.size());
        assertEquals(1, run.items().size());
        assertEquals(new RunItem.Get(StoreKind.INT, new Value.Index(), out), run.items().getFirst());
    }

    @Test
    public void testLowerVarIntWrite() {
        Local valLocal = new Local(new LocalType.Kind(java.lang.classfile.TypeKind.INT));
        Value value = new Value.LocalValue(valLocal);
        List<Op> ops = List.of(new Op.WriteVarInt(value));
        List<RunIr> runs = IrOptimizer.optimize(RunLowering.lower(ops));

        assertEquals(1, runs.size());
        RunIr run = runs.get(0);
        assertEquals(new Value.VarIntSize(value), run.size());
        assertEquals(1, run.items().size());
        assertEquals(new RunItem.PutVarInt(new Value.Index(), value, new Value.VarIntSize(value)), run.items().get(0));
    }

    @Test
    public void testLowerIf() {
        Local condLocal = new Local(new LocalType.Kind(java.lang.classfile.TypeKind.BOOLEAN));
        Value condition = new Value.LocalValue(condLocal);
        Value v1 = new Value.Const(1);
        List<Op> thenOps = List.of(new Op.WritePrimitive(PrimitiveKind.INT, v1));
        List<Op> elseOps = List.of();

        List<Op> ops = List.of(new Op.If(condition, thenOps, elseOps));
        List<RunIr> runs = IrOptimizer.optimize(RunLowering.lower(ops));

        // If flushes context, so we expect a run before/after or just the If in a run
        // In this case, 1 run containing the If
        assertEquals(1, runs.size());
        RunIr run = runs.get(0);
        assertEquals(1, run.items().size());
        RunItem.If ifItem = (RunItem.If) run.items().get(0);
        assertEquals(condition, ifItem.condition());
        assertEquals(1, ifItem.thenRuns().size());
        assertEquals(0, ifItem.elseRuns().size());
    }

    @Test
    public void testLowerForEach() {
        Local listLocal = new Local(new LocalType.Reference(List.class));
        Value listValue = new Value.LocalValue(listLocal);
        Local elementLocal = new Local(new LocalType.Reference(Integer.class));

        List<Op> body = List.of(new Op.WritePrimitive(PrimitiveKind.INT, new Value.LocalValue(elementLocal)));
        List<Op> ops = List.of(new Op.ForEach(listValue, elementLocal, body));

        List<RunIr> runs = IrOptimizer.optimize(RunLowering.lower(ops));

        assertEquals(1, runs.size());
        RunIr run = runs.get(0);
        assertEquals(1, run.items().size());
        RunItem.ForEach forEach = (RunItem.ForEach) run.items().get(0);
        assertEquals(listValue, forEach.source());
        assertEquals(elementLocal, forEach.element());
        assertEquals(1, forEach.body().size());
    }

    @Test
    public void testLowerMultipleOpsMerging() {
        Value v1 = new Value.Const(1);
        Value v2 = new Value.Const(2);
        List<Op> ops = List.of(
                new Op.WritePrimitive(PrimitiveKind.INT, v1),
                new Op.WritePrimitive(PrimitiveKind.INT, v2)
        );
        List<RunIr> runs = IrOptimizer.optimize(RunLowering.lower(ops));

        assertEquals(1, runs.size());
        RunIr run = runs.getFirst();
        assertEquals(new Value.Const(8L), run.size());
        assertEquals(2, run.items().size());
        assertEquals(new RunItem.Put(StoreKind.INT, new Value.Index(), v1), run.items().get(0));
        assertEquals(new RunItem.Put(StoreKind.INT, new Value.Add(new Value.Index(), new Value.Const(4L)), v2), run.items().get(1));
    }
}
