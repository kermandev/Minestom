package net.minestom.server.network;

import org.junit.jupiter.api.Test;

import static net.minestom.server.network.NetworkBuffer.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NetworkBufferIrTest {
    private record TemplateSingle(int value) {}
    private record TemplateNested(TemplateSingle single, int extra) {}

    @Test
    public void nestedTemplate() {
        NetworkBuffer.Type<TemplateSingle> singleType = NetworkBufferTemplate.template(
                INT, TemplateSingle::value,
                TemplateSingle::new
        );
        NetworkBuffer.Type<TemplateNested> nestedType = NetworkBufferTemplate.template(
                singleType, TemplateNested::single,
                INT, TemplateNested::extra,
                TemplateNested::new
        );

        var value = new TemplateNested(new TemplateSingle(123), 456);
        var array = NetworkBuffer.makeArray(nestedType, value);
        assertEquals(8, array.length); // 4 for single.value (INT), 4 for extra (INT)
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(nestedType);
        assertEquals(value, readValue);
    }

    private record TemplateOptional(Integer value) {}

    @Test
    public void templateOptional() {
        NetworkBuffer.Type<TemplateOptional> type = NetworkBufferTemplate.template(
                INT.optional(), TemplateOptional::value,
                TemplateOptional::new
        );

        // Present
        var value1 = new TemplateOptional(123);
        var array1 = NetworkBuffer.makeArray(type, value1);
        assertEquals(5, array1.length); // 1 for boolean, 4 for INT
        assertEquals(value1, NetworkBuffer.wrap(array1, 0, array1.length).read(type));

        // Absent
        var value2 = new TemplateOptional(null);
        var array2 = NetworkBuffer.makeArray(type, value2);
        assertEquals(1, array2.length); // 1 for boolean
        assertEquals(value2, NetworkBuffer.wrap(array2, 0, array2.length).read(type));
    }

    private record TemplateBytes(byte[] value) {}

    @Test
    public void templateByteArray() {
        NetworkBuffer.Type<TemplateBytes> type = NetworkBufferTemplate.template(
                BYTE_ARRAY, TemplateBytes::value,
                TemplateBytes::new
        );

        var value = new TemplateBytes(new byte[]{1, 2, 3});
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(1 + 3, array.length); // 1 for VarInt length, 3 for bytes
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value.value().length, readValue.value().length);
        for (int i = 0; i < value.value().length; i++) {
            assertEquals(value.value()[i], readValue.value()[i]);
        }
    }

    private record TemplateString(String value) {}

    @Test
    public void templateString() {
        NetworkBuffer.Type<TemplateString> type = NetworkBufferTemplate.template(
                STRING, TemplateString::value,
                TemplateString::new
        );

        var value = new TemplateString("hello");
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(1 + 5, array.length); // 1 for VarInt length, 5 for bytes
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    private record TemplateList(java.util.List<Integer> value) {}

    @Test
    public void templateList() {
        NetworkBuffer.Type<TemplateList> type = NetworkBufferTemplate.template(
                INT.list(), TemplateList::value,
                TemplateList::new
        );

        var value = new TemplateList(java.util.List.of(1, 2, 3));
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(1 + 3 * 4, array.length); // 1 for VarInt length, 3 * 4 for INTs
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    private record TemplateMap(java.util.Map<String, Integer> value) {}

    @Test
    public void templateMap() {
        NetworkBuffer.Type<TemplateMap> type = NetworkBufferTemplate.template(
                STRING.mapValue(INT), TemplateMap::value,
                TemplateMap::new
        );

        var value = new TemplateMap(java.util.Map.of("a", 1, "b", 2));
        var array = NetworkBuffer.makeArray(type, value);
        // Size: 1 (len) + ("a": 1 + 1 + 4) + ("b": 1 + 1 + 4) = 1 + 6 + 6 = 13
        assertEquals(13, array.length);
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    private record TemplateTransformed(byte v1, boolean v2, boolean v3) {}

    @Test
    public void templateTransformed() {
        // This mirrors the user's example where they saw multiple reserveWrite(1L)
        NetworkBuffer.Type<TemplateTransformed> type = NetworkBufferTemplate.template(
                BYTE.transform(v -> v, v -> v), TemplateTransformed::v1,
                BOOLEAN, TemplateTransformed::v2,
                BOOLEAN, TemplateTransformed::v3,
                TemplateTransformed::new
        );

        var value = new TemplateTransformed((byte) 1, true, false);
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(3, array.length); // 1 + 1 + 1

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    private record TemplateEither(net.minestom.server.utils.Either<Integer, String> either) {}

    @Test
    public void templateEitherLeft() {
        NetworkBuffer.Type<TemplateEither> type = NetworkBufferTemplate.template(
                NetworkBuffer.Either(INT, STRING), TemplateEither::either,
                TemplateEither::new
        );

        var value = new TemplateEither(net.minestom.server.utils.Either.left(123));
        var array = NetworkBuffer.makeArray(type, value);
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateEitherRight() {
        NetworkBuffer.Type<TemplateEither> type = NetworkBufferTemplate.template(
                NetworkBuffer.Either(INT, STRING), TemplateEither::either,
                TemplateEither::new
        );

        var value = new TemplateEither(net.minestom.server.utils.Either.right("hello"));
        var array = NetworkBuffer.makeArray(type, value);
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    private record TemplateEitherFixed(net.minestom.server.utils.Either<Integer, Float> either) {}

    @Test
    public void templateEitherFixedLeft() {
        NetworkBuffer.Type<TemplateEitherFixed> type = NetworkBufferTemplate.template(
                NetworkBuffer.Either(INT, FLOAT), TemplateEitherFixed::either,
                TemplateEitherFixed::new
        );

        var value = new TemplateEitherFixed(net.minestom.server.utils.Either.left(123));
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(5, array.length);
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateEitherFixedRight() {
        NetworkBuffer.Type<TemplateEitherFixed> type = NetworkBufferTemplate.template(
                NetworkBuffer.Either(INT, FLOAT), TemplateEitherFixed::either,
                TemplateEitherFixed::new
        );

        var value = new TemplateEitherFixed(net.minestom.server.utils.Either.right(123.45f));
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(5, array.length);
        
        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }
}
