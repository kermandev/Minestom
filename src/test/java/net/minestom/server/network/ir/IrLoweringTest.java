package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IrLoweringTest {

    @Test
    public void testLowerInt() {
        IrLowering.WriteBuilderImpl builder = new IrLowering.WriteBuilderImpl(new Local(new LocalType.Reference(Integer.class)));
        builder.lower(NetworkBuffer.INT, new Value.LocalValue(builder.source()));
        
        List<Op> ops = builder.result();
        // NetworkBuffer.INT.lowerWrite pushes:
        // 1. Cast source to Integer
        // 2. Unbox Integer to int
        // 3. WritePrimitive(INT, unboxed)
        
        assertEquals(3, ops.size());
        assertInstanceOf(Op.Cast.class, ops.get(0));
        assertInstanceOf(Op.Unbox.class, ops.get(1));
        assertInstanceOf(Op.WritePrimitive.class, ops.get(2));
        
        Op.WritePrimitive write = (Op.WritePrimitive) ops.get(2);
        assertEquals(PrimitiveKind.INT, write.kind());
    }

    @Test
    public void testLowerVarInt() {
        IrLowering.WriteBuilderImpl builder = new IrLowering.WriteBuilderImpl(new Local(new LocalType.Reference(Integer.class)));
        builder.lower(NetworkBuffer.VAR_INT, new Value.LocalValue(builder.source()));
        
        List<Op> ops = builder.result();
        assertEquals(3, ops.size());
        assertInstanceOf(Op.Cast.class, ops.get(0));
        assertInstanceOf(Op.Unbox.class, ops.get(1));
        assertInstanceOf(Op.WriteVarInt.class, ops.get(2));
    }

    @Test
    public void testLowerReadInt() {
        IrLowering.ReadBuilderImpl builder = new IrLowering.ReadBuilderImpl();
        Value result = builder.lower(NetworkBuffer.INT);
        
        List<Op> ops = builder.result();
        // NetworkBuffer.INT.lowerRead pushes:
        // 1. ReadPrimitive(INT, out)
        // 2. Box(int, out, boxedOut)
        // Returns boxedOut
        
        assertEquals(2, ops.size());
        assertInstanceOf(Op.ReadPrimitive.class, ops.get(0));
        assertInstanceOf(Op.Box.class, ops.get(1));

        assertInstanceOf(Value.LocalValue.class, result);
    }
}
