package net.minestom.server.network;

import net.minestom.server.utils.Either;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static net.minestom.server.network.NetworkBuffer.*;
import static org.junit.jupiter.api.Assertions.*;

public class NetworkBufferTemplateTest {
    private static <T> void assertRoundTripMatchesDirect(NetworkBuffer.Type<T> fieldType, T value) {
        NetworkBuffer.Type<Box<T>> direct = directBox(fieldType);
        NetworkBuffer.Type<Box<T>> compiled = compiledBox(fieldType);
        Box<T> boxed = new Box<>(value);

        byte[] directBytes = NetworkBuffer.makeArray(direct, boxed);
        byte[] compiledBytes = NetworkBuffer.makeArray(compiled, boxed);
        assertArrayEquals(directBytes, compiledBytes);
        assertEquals(NetworkBuffer.wrap(directBytes, 0, directBytes.length).read(direct),
                NetworkBuffer.wrap(compiledBytes, 0, compiledBytes.length).read(compiled));
    }

    private static void assertByteArrayRoundTripMatchesDirect(NetworkBuffer.Type<byte[]> fieldType, byte[] value) {
        NetworkBuffer.Type<Box<byte[]>> direct = directBox(fieldType);
        NetworkBuffer.Type<Box<byte[]>> compiled = compiledBox(fieldType);
        Box<byte[]> boxed = new Box<>(value);

        byte[] directBytes = NetworkBuffer.makeArray(direct, boxed);
        byte[] compiledBytes = NetworkBuffer.makeArray(compiled, boxed);
        assertArrayEquals(directBytes, compiledBytes);
        assertArrayEquals(NetworkBuffer.wrap(directBytes, 0, directBytes.length).read(direct).value(),
                NetworkBuffer.wrap(compiledBytes, 0, compiledBytes.length).read(compiled).value());
    }

    private static <T> void assertReadFailureMatchesDirect(NetworkBuffer.Type<T> fieldType, byte[] bytes) {
        NetworkBuffer.Type<Box<T>> direct = directBox(fieldType);
        NetworkBuffer.Type<Box<T>> compiled = compiledBox(fieldType);

        Throwable directFailure = assertThrows(Throwable.class, () -> NetworkBuffer.wrap(bytes, 0, bytes.length).read(direct));
        Throwable compiledFailure = assertThrows(Throwable.class, () -> NetworkBuffer.wrap(bytes, 0, bytes.length).read(compiled));
        assertEquals(directFailure.getClass(), compiledFailure.getClass());
    }

    private static <T> void assertWriteFailureMatchesDirect(NetworkBuffer.Type<T> fieldType, T value) {
        NetworkBuffer.Type<Box<T>> direct = directBox(fieldType);
        NetworkBuffer.Type<Box<T>> compiled = compiledBox(fieldType);
        Box<T> boxed = new Box<>(value);

        Throwable directFailure = assertThrows(Throwable.class, () -> NetworkBuffer.makeArray(direct, boxed));
        Throwable compiledFailure = assertThrows(Throwable.class, () -> NetworkBuffer.makeArray(compiled, boxed));
        assertEquals(directFailure.getClass(), compiledFailure.getClass());
    }

    private static <T> NetworkBuffer.Type<Box<T>> compiledBox(NetworkBuffer.Type<T> fieldType) {
        return NetworkBufferTemplate.template(
                fieldType, Box<T>::value,
                Box<T>::new
        );
    }

    private static <T> NetworkBuffer.Type<Box<T>> directBox(NetworkBuffer.Type<T> fieldType) {
        return new NetworkBuffer.Type<>() {
            @Override
            public void write(NetworkBuffer buffer, Box<T> value) {
                buffer.write(fieldType, value.value());
            }

            @Override
            public Box<T> read(NetworkBuffer buffer) {
                return new Box<>(buffer.read(fieldType));
            }
        };
    }

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

    @Test
    public void templateList() {
        NetworkBuffer.Type<TemplateList> type = NetworkBufferTemplate.template(
                INT.list(), TemplateList::value,
                TemplateList::new
        );

        var value = new TemplateList(List.of(1, 2, 3));
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(1 + 3 * 4, array.length); // 1 for VarInt length, 3 * 4 for INTs

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateMap() {
        NetworkBuffer.Type<TemplateMap> type = NetworkBufferTemplate.template(
                STRING.mapValue(INT), TemplateMap::value,
                TemplateMap::new
        );

        var value = new TemplateMap(Map.of("a", 1, "b", 2));
        var array = NetworkBuffer.makeArray(type, value);

        assertEquals(13, array.length);

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateTransformed() {
        NetworkBuffer.Type<TemplateTransformed> type = NetworkBufferTemplate.template(
                BYTE.transform(v -> v, v -> v), TemplateTransformed::v1,
                BOOLEAN, TemplateTransformed::v2,
                BOOLEAN, TemplateTransformed::v3,
                TemplateTransformed::new
        );

        var value = new TemplateTransformed((byte) 1, true, false);
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(3, array.length);

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateEitherLeft() {
        NetworkBuffer.Type<TemplateEither> type = NetworkBufferTemplate.template(
                NetworkBuffer.Either(INT, STRING), TemplateEither::either,
                TemplateEither::new
        );

        var value = new TemplateEither(Either.left(123));
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

        var value = new TemplateEither(Either.right("hello"));
        var array = NetworkBuffer.makeArray(type, value);

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateEitherFixedLeft() {
        NetworkBuffer.Type<TemplateEitherFixed> type = NetworkBufferTemplate.template(
                NetworkBuffer.Either(INT, FLOAT), TemplateEitherFixed::either,
                TemplateEitherFixed::new
        );

        var value = new TemplateEitherFixed(Either.left(123));
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

    @Test
    public void templateVarInt() {
        NetworkBuffer.Type<TemplateVarInt> type = NetworkBufferTemplate.template(
                VAR_INT, TemplateVarInt::value,
                TemplateVarInt::new
        );

        var value = new TemplateVarInt(123456);
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(NetworkBufferTypeImpl.varIntSize(123456), array.length);

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateUUID() {
        NetworkBuffer.Type<TemplateUUID> type = NetworkBufferTemplate.template(
                UUID, TemplateUUID::value,
                TemplateUUID::new
        );

        var value = new TemplateUUID(java.util.UUID.randomUUID());
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(16, array.length);

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateEnum() {
        NetworkBuffer.Type<TemplateEnum> type = NetworkBufferTemplate.template(
                DIRECTION, TemplateEnum::value,
                TemplateEnum::new
        );

        var value = new TemplateEnum(net.minestom.server.utils.Direction.NORTH);
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(1, array.length); // Direction has < 128 values, so it's a BYTE

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateMultiField() {
        NetworkBuffer.Type<TemplateMultiField> type = NetworkBufferTemplate.template(
                INT, TemplateMultiField::v1,
                LONG, TemplateMultiField::v2,
                STRING, TemplateMultiField::v3,
                UUID, TemplateMultiField::v4,
                TemplateMultiField::new
        );

        var value = new TemplateMultiField(1, 2L, "three", java.util.UUID.randomUUID());
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(4 + 8 + (1 + 5) + 16, array.length);

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateComponent() {
        NetworkBuffer.Type<TemplateComponent> type = NetworkBufferTemplate.template(
                COMPONENT, TemplateComponent::value,
                TemplateComponent::new
        );

        var value = new TemplateComponent(net.kyori.adventure.text.Component.text("hello"));
        var array = NetworkBuffer.makeArray(type, value);

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateNbt() {
        NetworkBuffer.Type<TemplateNbt> type = NetworkBufferTemplate.template(
                NBT, TemplateNbt::value,
                TemplateNbt::new
        );

        var value = new TemplateNbt(net.kyori.adventure.nbt.CompoundBinaryTag.builder()
                .putString("key", "value")
                .putInt("int", 123)
                .build());
        var array = NetworkBuffer.makeArray(type, value);

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateComplexOptional() {
        NetworkBuffer.Type<TemplateComplexOptional> type = NetworkBufferTemplate.template(
                STRING.list().optional(), TemplateComplexOptional::value,
                TemplateComplexOptional::new
        );

        // Present
        var value1 = new TemplateComplexOptional(java.util.List.of("a", "b"));
        var array1 = NetworkBuffer.makeArray(type, value1);
        var readValue1 = NetworkBuffer.wrap(array1, 0, array1.length).read(type);
        assertEquals(value1, readValue1);

        // Absent
        var value2 = new TemplateComplexOptional(null);
        var array2 = NetworkBuffer.makeArray(type, value2);
        var readValue2 = NetworkBuffer.wrap(array2, 0, array2.length).read(type);
        assertEquals(value2, readValue2);
    }

    @Test
    public void templateLarge() {
        NetworkBuffer.Type<TemplateLarge> type = NetworkBufferTemplate.template(
                INT, TemplateLarge::v1, INT, TemplateLarge::v2, INT, TemplateLarge::v3, INT, TemplateLarge::v4, INT, TemplateLarge::v5,
                INT, TemplateLarge::v6, INT, TemplateLarge::v7, INT, TemplateLarge::v8, INT, TemplateLarge::v9, INT, TemplateLarge::v10,
                INT, TemplateLarge::v11, INT, TemplateLarge::v12, INT, TemplateLarge::v13, INT, TemplateLarge::v14, INT, TemplateLarge::v15,
                TemplateLarge::new
        );

        var value = new TemplateLarge(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(15 * 4, array.length);

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value, readValue);
    }

    @Test
    public void templateBlockPosition() {
        NetworkBuffer.Type<TemplateBlockPosition> type = NetworkBufferTemplate.template(
                BLOCK_POSITION, TemplateBlockPosition::value,
                TemplateBlockPosition::new
        );

        var value = new TemplateBlockPosition(new net.minestom.server.coordinate.Vec(10, 20, 30));
        var array = NetworkBuffer.makeArray(type, value);
        assertEquals(8, array.length);

        var readValue = NetworkBuffer.wrap(array, 0, array.length).read(type);
        assertEquals(value.value().blockX(), readValue.value().blockX());
        assertEquals(value.value().blockY(), readValue.value().blockY());
        assertEquals(value.value().blockZ(), readValue.value().blockZ());
    }

    @Test
    public void compiledTemplateMatchesDirectForEmptyVariableContainers() {
        assertRoundTripMatchesDirect(INT.list(4), List.of());
        assertRoundTripMatchesDirect(STRING.mapValue(INT, 4), Map.of());
        assertByteArrayRoundTripMatchesDirect(BYTE_ARRAY, new byte[0]);
    }

    @Test
    public void compiledTemplateMatchesDirectForNewTypes() {
        assertRoundTripMatchesDirect(VAR_INT, 123456);
        assertRoundTripMatchesDirect(UUID, java.util.UUID.randomUUID());
        assertRoundTripMatchesDirect(DIRECTION, net.minestom.server.utils.Direction.NORTH);
        assertRoundTripMatchesDirect(BLOCK_POSITION, new net.minestom.server.coordinate.Vec(10, 20, 30));
        assertRoundTripMatchesDirect(COMPONENT, net.kyori.adventure.text.Component.text("hello"));
    }

    @Test
    public void compiledTemplateMatchesDirectForInvalidContainerReads() {
        byte[] negativeLength = NetworkBuffer.makeArray(buffer -> buffer.write(VAR_INT, -1));
        assertReadFailureMatchesDirect(BYTE_ARRAY, negativeLength);
        assertReadFailureMatchesDirect(INT.list(), negativeLength);
        assertReadFailureMatchesDirect(STRING.mapValue(INT), negativeLength);

        byte[] tooLargeLength = NetworkBuffer.makeArray(buffer -> buffer.write(VAR_INT, 2));
        assertReadFailureMatchesDirect(INT.list(1), tooLargeLength);
        assertReadFailureMatchesDirect(STRING.mapValue(INT, 1), tooLargeLength);
    }

    @Test
    public void compiledTemplateMatchesDirectForInvalidFixedByteWrites() {
        assertWriteFailureMatchesDirect(NetworkBuffer.FixedRawBytes(2), new byte[]{1});
        assertWriteFailureMatchesDirect(NetworkBuffer.FixedRawBytes(2), new byte[]{1, 2, 3});
    }

    @Test
    public void compiledTemplateMatchesDirectForNestedContainersAndEither() {
        NetworkBuffer.Type<List<Map<String, List<Either<Integer, String>>>>> type =
                STRING.mapValue(NetworkBuffer.Either(INT, STRING).list(4), 4).list(4);

        List<Map<String, List<Either<Integer, String>>>> value = List.of(
                Map.of("mixed", List.of(Either.left(12), Either.right("hello"))),
                Map.of()
        );
        assertRoundTripMatchesDirect(type, value);
    }

    private record Box<T>(T value) {
    }

    private record TemplateSingle(int value) {
    }

    private record TemplateNested(TemplateSingle single, int extra) {
    }

    private record TemplateOptional(Integer value) {
    }

    private record TemplateBytes(byte[] value) {
    }

    private record TemplateString(String value) {
    }

    private record TemplateList(List<Integer> value) {
    }

    private record TemplateMap(Map<String, Integer> value) {
    }

    private record TemplateTransformed(byte v1, boolean v2, boolean v3) {
    }

    private record TemplateEither(Either<Integer, String> either) {
    }

    private record TemplateEitherFixed(Either<Integer, Float> either) {
    }

    private record TemplateVarInt(int value) {
    }

    private record TemplateUUID(java.util.UUID value) {
    }

    private record TemplateEnum(net.minestom.server.utils.Direction value) {
    }

    private record TemplateMultiField(int v1, long v2, String v3, java.util.UUID v4) {
    }

    private record TemplateComponent(net.kyori.adventure.text.Component value) {
    }

    private record TemplateNbt(net.kyori.adventure.nbt.BinaryTag value) {
    }

    private record TemplateComplexOptional(java.util.List<String> value) {
    }

    private record TemplateLarge(
            int v1, int v2, int v3, int v4, int v5,
            int v6, int v7, int v8, int v9, int v10,
            int v11, int v12, int v13, int v14, int v15
    ) {
    }

    private record TemplateBlockPosition(net.minestom.server.coordinate.Point value) {
    }
}
