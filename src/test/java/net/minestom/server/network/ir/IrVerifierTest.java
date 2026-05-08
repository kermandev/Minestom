package net.minestom.server.network.ir;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IrVerifierTest {

    @Test
    public void testVerifyValidWrite() {
        ProgramIr program = new ProgramIr(List.of(
                new RunIr(new Value.Const(4L), List.of(
                        new RunItem.Put(StoreKind.INT, new Value.Const(0L), new Value.Const(123))
                ))
        ));
        assertDoesNotThrow(() -> IrVerifier.verifyWrite(program));
    }

    @Test
    public void testVerifyWriteWithReturnFails() {
        ProgramIr program = new ProgramIr(List.of(
                new RunIr(new Value.Const(0L), List.of(
                        new RunItem.Return(new Value.Const("hello"))
                ))
        ));
        assertThrows(IllegalStateException.class, () -> IrVerifier.verifyWrite(program));
    }

    @Test
    public void testVerifyValidRead() {
        ProgramIr program = new ProgramIr(List.of(
                new RunIr(new Value.Const(0L), List.of(
                        new RunItem.Return(new Value.Const("hello"))
                ))
        ));
        assertDoesNotThrow(() -> IrVerifier.verifyRead(program));
    }

    @Test
    public void testVerifyReadWithoutReturnFails() {
        ProgramIr program = new ProgramIr(List.of(
                new RunIr(new Value.Const(0L), List.of())
        ));
        assertThrows(IllegalStateException.class, () -> IrVerifier.verifyRead(program));
    }

    @Test
    public void testVerifyReadWithMultipleReturnsFails() {
        ProgramIr program = new ProgramIr(List.of(
                new RunIr(new Value.Const(0L), List.of(
                        new RunItem.Return(new Value.Const("hello"))
                )),
                new RunIr(new Value.Const(0L), List.of(
                        new RunItem.Return(new Value.Const("world"))
                ))
        ));
        assertThrows(IllegalStateException.class, () -> IrVerifier.verifyRead(program));
    }
}
