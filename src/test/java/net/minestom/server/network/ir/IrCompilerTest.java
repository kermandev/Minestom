package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IrCompilerTest {

    @Test
    public void testCompileSimple() {
        NetworkBuffer.Type<Integer> compiled = IrCompiler.compile(NetworkBuffer.INT);
        assertNotNull(compiled);
        
        var value = 12345;
        var array = NetworkBuffer.makeArray(compiled, value);
        assertEquals(4, array.length);
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(compiled);
        assertEquals(value, readValue);
    }

    @Test
    public void testCompileVarInt() {
        NetworkBuffer.Type<Integer> compiled = IrCompiler.compile(NetworkBuffer.VAR_INT);
        assertNotNull(compiled);
        
        var value = 123456;
        var array = NetworkBuffer.makeArray(compiled, value);
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(compiled);
        assertEquals(value, readValue);
    }

    @Test
    public void testCompileOptional() {
        NetworkBuffer.Type<Integer> type = NetworkBuffer.INT.optional();
        NetworkBuffer.Type<Integer> compiled = IrCompiler.compile(type);
        assertNotNull(compiled);

        // Present
        var value1 = 123;
        var array1 = NetworkBuffer.makeArray(compiled, value1);
        assertEquals(5, array1.length);
        assertEquals(value1, NetworkBuffer.wrap(array1, 0, array1.length).read(compiled));

        // Absent
        Integer value2 = null;
        var array2 = NetworkBuffer.makeArray(compiled, value2);
        assertEquals(1, array2.length);
        assertEquals(value2, NetworkBuffer.wrap(array2, 0, array2.length).read(compiled));
    }

    @Test
    public void testCompileList() {
        NetworkBuffer.Type<java.util.List<Integer>> type = NetworkBuffer.INT.list();
        NetworkBuffer.Type<java.util.List<Integer>> compiled = IrCompiler.compile(type);
        assertNotNull(compiled);

        var value = java.util.List.of(1, 2, 3);
        var array = NetworkBuffer.makeArray(compiled, value);
        assertEquals(1 + 3 * 4, array.length);
        assertEquals(value, NetworkBuffer.wrap(array, 0, array.length).read(compiled));
    }

    record Inner(int value) {}
    record Outer(Inner inner) {}

    @Test
    public void testCompileNestedTemplate() {
        NetworkBuffer.Type<Inner> innerType = net.minestom.server.network.NetworkBufferTemplate.template(
                NetworkBuffer.INT, Inner::value,
                Inner::new
        );
        // Compile inner template
        NetworkBuffer.Type<Inner> innerCompiled = IrCompiler.compile(innerType);

        NetworkBuffer.Type<Outer> outerType = net.minestom.server.network.NetworkBufferTemplate.template(
                innerCompiled, Outer::inner,
                Outer::new
        );
        // Compile outer template (this should inline innerCompiled)
        NetworkBuffer.Type<Outer> outerCompiled = IrCompiler.compile(outerType);
        assertNotNull(outerCompiled);

        var value = new Outer(new Inner(123456));
        var array = NetworkBuffer.makeArray(outerCompiled, value);
        assertEquals(4, array.length); // 4 bytes for INT, no overhead

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(outerCompiled);
        assertEquals(value, readValue);
    }
}
