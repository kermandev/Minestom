package net.minestom.server.network;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import net.minestom.server.utils.Either;

import java.lang.classfile.TypeKind;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import static net.minestom.server.utils.validate.Check.argCondition;

record NetworkIr<T extends @UnknownNullability Object>(
        String debugName,
        List<IrField<T, ?>> fields,
        IrConstructor<T> constructor
) {
}

record IrField<R extends @UnknownNullability Object, P extends @UnknownNullability Object>(
        int index,
        String path,
        NetworkBuffer.Type<P> originalType,
        IrType<P> irType,
        Function<? super R, ? extends P> getter
) {
}

record IrConstructor<T extends @UnknownNullability Object>(Object raw, int fieldCount) {
}

interface NetworkIrBacked<T extends @UnknownNullability Object> extends NetworkBuffer.Type<T> {
    NetworkIr<T> ir();
}

sealed interface IrType<T extends @UnknownNullability Object> {
    record Delegated<T extends @UnknownNullability Object>(NetworkBuffer.Type<T> type) implements IrType<T> {
    }

    record Unit() implements IrType<net.minestom.server.utils.Unit> {
    }

    record Primitive<T extends @UnknownNullability Object>(PrimitiveKind kind) implements IrType<T> {
    }

    record VarInt() implements IrType<Integer> {
    }

    record VarLong() implements IrType<Long> {
    }

    record Optional<T extends @UnknownNullability Object>(IrType<T> parent) implements IrType<@Nullable T> {
    }

    record Transform<T extends @UnknownNullability Object, S extends @UnknownNullability Object>(
            IrType<T> parent,
            Function<T, S> to,
            Function<S, T> from
    ) implements IrType<S> {
    }

    record Template<T extends @UnknownNullability Object>(NetworkIr<T> ir) implements IrType<T> {
    }

    record FixedBytes(int length) implements IrType<byte[]> {
    }

    record RawBytes() implements IrType<byte[]> {
    }

    record StringType() implements IrType<String> {
    }

    record ByteArray() implements IrType<byte[]> {
    }

    record LengthPrefixed<T extends @UnknownNullability Object>(IrType<T> parent, int maxLength) implements IrType<T> {
    }

    record ListType<E extends @UnknownNullability Object>(IrType<E> elementType, int maxSize) implements IrType<List<E>> {
    }

    record SetType<E extends @UnknownNullability Object>(IrType<E> elementType, int maxSize) implements IrType<java.util.Set<E>> {
    }

    record MapType<K extends @UnknownNullability Object, V extends @UnknownNullability Object>(
            IrType<K> keyType,
            IrType<V> valueType,
            int maxSize
    ) implements IrType<java.util.Map<K, V>> {
    }

    record EitherType<L extends @UnknownNullability Object, R extends @UnknownNullability Object>(
            IrType<L> left,
            IrType<R> right
    ) implements IrType<Either<L, R>> {
    }

    record UnionType<T extends @UnknownNullability Object, K extends @UnknownNullability Object, TR extends T>(
            IrType<K> keyType,
            Function<T, ? extends K> keyFunc,
            Function<K, NetworkBuffer.Type<TR>> serializers
    ) implements IrType<T> {
    }
}

enum PrimitiveKind {
    UNIT(0, TypeKind.REFERENCE),
    BOOLEAN(1, TypeKind.INT),
    BYTE(1, TypeKind.INT),
    UNSIGNED_BYTE(1, TypeKind.INT),
    SHORT(2, TypeKind.INT),
    UNSIGNED_SHORT(2, TypeKind.INT),
    INT(4, TypeKind.INT),
    UNSIGNED_INT(4, TypeKind.LONG),
    LONG(8, TypeKind.LONG),
    FLOAT(4, TypeKind.FLOAT),
    DOUBLE(8, TypeKind.DOUBLE);

    private final int size;
    private final TypeKind localKind;

    PrimitiveKind(int size, TypeKind localKind) {
        this.size = size;
        this.localKind = localKind;
    }

    int size() {
        return size;
    }

    TypeKind localKind() {
        return localKind;
    }
}

record TemplateInput(Object[] values, int fieldCount) {
    static TemplateInput parse(Object[] values) {
        return new TemplateInput(values, values.length / 2);
    }
}

final class ClassDataLayout {
    private final List<Object> values = new ArrayList<>();

    int add(Object value) {
        final int index = values.size();
        values.add(value);
        return index;
    }

    List<Object> values() {
        return List.copyOf(values);
    }
}

final class InterpretedNetworkType<T extends @UnknownNullability Object> implements NetworkIrBacked<T> {
    private final NetworkIr<T> ir;

    InterpretedNetworkType(NetworkIr<T> ir) {
        this.ir = ir;
    }

    @Override
    public void write(NetworkBuffer buffer, T value) {
        IrInterpreter.writeTemplate(ir, buffer, value);
    }

    @Override
    public T read(NetworkBuffer buffer) {
        return IrInterpreter.readTemplate(ir, buffer);
    }

    @Override
    public NetworkIr<T> ir() {
        return ir;
    }
}

final class IrInterpreter {
    private IrInterpreter() {
    }

    static <T extends @UnknownNullability Object> void writeTemplate(NetworkIr<T> ir, NetworkBuffer buffer, T value) {
        for (IrField<T, ?> field : ir.fields()) {
            writeField(buffer, value, field);
        }
    }

    static <T extends @UnknownNullability Object> T readTemplate(NetworkIr<T> ir, NetworkBuffer buffer) {
        final Object[] values = new Object[ir.fields().size()];
        for (IrField<T, ?> field : ir.fields()) {
            values[field.index()] = readType(field.irType(), buffer);
        }
        return construct(ir.constructor(), values);
    }

    private static <R extends @UnknownNullability Object, P extends @UnknownNullability Object> void writeField(
            NetworkBuffer buffer, R value, IrField<R, P> field
    ) {
        writeType(field.irType(), buffer, field.getter().apply(value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void writeType(IrType<?> type, NetworkBuffer buffer, Object value) {
        switch (type) {
            case IrType.Delegated<?> delegated -> ((NetworkBuffer.Type) delegated.type()).write(buffer, value);
            case IrType.Unit _ -> {
            }
            case IrType.Primitive<?> primitive -> ((NetworkBuffer.Type) primitiveType(primitive.kind())).write(buffer, value);
            case IrType.VarInt _ -> buffer.write(NetworkBuffer.VAR_INT, (Integer) value);
            case IrType.VarLong _ -> buffer.write(NetworkBuffer.VAR_LONG, (Long) value);
            case IrType.Optional<?> optional -> {
                buffer.write(NetworkBuffer.BOOLEAN, value != null);
                if (value != null) writeType(optional.parent(), buffer, value);
            }
            case IrType.Transform<?, ?> transform -> writeType(transform.parent(), buffer, ((Function) transform.from()).apply(value));
            case IrType.Template<?> template -> writeTemplate((NetworkIr) template.ir(), buffer, value);
            case IrType.FixedBytes fixedBytes -> buffer.write(NetworkBuffer.FixedRawBytes(fixedBytes.length()), (byte[]) value);
            case IrType.RawBytes _ -> buffer.write(NetworkBuffer.RAW_BYTES, (byte[]) value);
            case IrType.StringType _ -> buffer.write(NetworkBuffer.STRING, (String) value);
            case IrType.ByteArray _ -> buffer.write(NetworkBuffer.BYTE_ARRAY, (byte[]) value);
            case IrType.LengthPrefixed<?> lengthPrefixed -> {
                final byte[] bytes = NetworkBuffer.makeArray(inner -> writeType(lengthPrefixed.parent(), inner, value), buffer.registries());
                argCondition(bytes.length > lengthPrefixed.maxLength(), "Value is too long (length: {0}, max: {1})", bytes.length, lengthPrefixed.maxLength());
                buffer.write(NetworkBuffer.BYTE_ARRAY, bytes);
            }
            case IrType.ListType<?> list -> {
                final List<?> values = (List<?>) value;
                final int size = values == null ? 0 : values.size();
                argCondition(size > list.maxSize(), "Collection size ({0}) is higher than the maximum allowed size ({1})", size, list.maxSize());
                buffer.write(NetworkBuffer.VAR_INT, size);
                if (values != null) {
                    for (Object element : values) writeType(list.elementType(), buffer, element);
                }
            }
            case IrType.SetType<?> set -> {
                final java.util.Set<?> values = (java.util.Set<?>) value;
                final int size = values == null ? 0 : values.size();
                argCondition(size > set.maxSize(), "Collection size ({0}) is higher than the maximum allowed size ({1})", size, set.maxSize());
                buffer.write(NetworkBuffer.VAR_INT, size);
                if (values != null) {
                    for (Object element : values) writeType(set.elementType(), buffer, element);
                }
            }
            case IrType.MapType<?, ?> map -> {
                final java.util.Map<?, ?> values = (java.util.Map<?, ?>) value;
                final int size = values == null ? 0 : values.size();
                argCondition(size > map.maxSize(), "Map size ({0}) is higher than the maximum allowed size ({1})", size, map.maxSize());
                buffer.write(NetworkBuffer.VAR_INT, size);
                if (values != null) {
                    for (java.util.Map.Entry<?, ?> entry : values.entrySet()) {
                        writeType(map.keyType(), buffer, entry.getKey());
                        writeType(map.valueType(), buffer, entry.getValue());
                    }
                }
            }
            case IrType.EitherType<?, ?> eitherType -> {
                switch ((Either<?, ?>) value) {
                    case Either.Left<?, ?>(Object leftValue) -> {
                        buffer.write(NetworkBuffer.BOOLEAN, true);
                        writeType(eitherType.left(), buffer, leftValue);
                    }
                    case Either.Right<?, ?>(Object rightValue) -> {
                        buffer.write(NetworkBuffer.BOOLEAN, false);
                        writeType(eitherType.right(), buffer, rightValue);
                    }
                }
            }
            case IrType.UnionType<?, ?, ?> union -> {
                final Object key = ((Function) union.keyFunc()).apply(value);
                writeType(union.keyType(), buffer, key);
                final NetworkBuffer.Type serializer = (NetworkBuffer.Type) ((Function) union.serializers()).apply(key);
                if (serializer == null) throw new UnsupportedOperationException("Unrecognized type: " + key);
                serializer.write(buffer, value);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object readType(IrType<?> type, NetworkBuffer buffer) {
        return switch (type) {
            case IrType.Delegated<?> delegated -> ((NetworkBuffer.Type) delegated.type()).read(buffer);
            case IrType.Unit _ -> net.minestom.server.utils.Unit.INSTANCE;
            case IrType.Primitive<?> primitive -> buffer.read((NetworkBuffer.Type) primitiveType(primitive.kind()));
            case IrType.VarInt _ -> buffer.read(NetworkBuffer.VAR_INT);
            case IrType.VarLong _ -> buffer.read(NetworkBuffer.VAR_LONG);
            case IrType.Optional<?> optional -> buffer.read(NetworkBuffer.BOOLEAN) ? readType(optional.parent(), buffer) : null;
            case IrType.Transform<?, ?> transform -> ((Function) transform.to()).apply(readType(transform.parent(), buffer));
            case IrType.Template<?> template -> readTemplate((NetworkIr) template.ir(), buffer);
            case IrType.FixedBytes fixedBytes -> buffer.read(NetworkBuffer.FixedRawBytes(fixedBytes.length()));
            case IrType.RawBytes _ -> buffer.read(NetworkBuffer.RAW_BYTES);
            case IrType.StringType _ -> buffer.read(NetworkBuffer.STRING);
            case IrType.ByteArray _ -> buffer.read(NetworkBuffer.BYTE_ARRAY);
            case IrType.LengthPrefixed<?> lengthPrefixed -> readLengthPrefixed(lengthPrefixed, buffer);
            case IrType.ListType<?> list -> readList(list, buffer);
            case IrType.SetType<?> set -> java.util.Set.copyOf(readList(new IrType.ListType<>(set.elementType(), set.maxSize()), buffer));
            case IrType.MapType<?, ?> map -> readMap(map, buffer);
            case IrType.EitherType<?, ?> eitherType -> buffer.read(NetworkBuffer.BOOLEAN)
                    ? Either.left(readType(eitherType.left(), buffer))
                    : Either.right(readType(eitherType.right(), buffer));
            case IrType.UnionType<?, ?, ?> union -> readUnion(union, buffer);
        };
    }

    private static Object readLengthPrefixed(IrType.LengthPrefixed<?> lengthPrefixed, NetworkBuffer buffer) {
        final int length = buffer.read(NetworkBuffer.VAR_INT);
        argCondition(length > lengthPrefixed.maxLength(), "Value is too long (length: {0}, max: {1})", length, lengthPrefixed.maxLength());
        final long availableBytes = buffer.readableBytes();
        argCondition(length > availableBytes, "Value is too long (length: {0}, available: {1})", length, availableBytes);
        final Object value = readType(lengthPrefixed.parent(), buffer);
        argCondition(buffer.readableBytes() != availableBytes - length, "Value is too short (length: {0}, available: {1})", length, availableBytes);
        return value;
    }

    private static List<Object> readList(IrType.ListType<?> list, NetworkBuffer buffer) {
        final int size = buffer.read(NetworkBuffer.VAR_INT);
        argCondition(size > list.maxSize(), "Collection size ({0}) is higher than the maximum allowed size ({1})", size, list.maxSize());
        final List<Object> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(readType(list.elementType(), buffer));
        }
        return List.copyOf(values);
    }

    private static java.util.Map<Object, Object> readMap(IrType.MapType<?, ?> map, NetworkBuffer buffer) {
        final int size = buffer.read(NetworkBuffer.VAR_INT);
        argCondition(size > map.maxSize(), "Map size ({0}) is higher than the maximum allowed size ({1})", size, map.maxSize());
        final java.util.Map<Object, Object> values = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            values.put(readType(map.keyType(), buffer), readType(map.valueType(), buffer));
        }
        return java.util.Map.copyOf(values);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object readUnion(IrType.UnionType<?, ?, ?> union, NetworkBuffer buffer) {
        final Object key = readType(union.keyType(), buffer);
        final NetworkBuffer.Type serializer = (NetworkBuffer.Type) ((Function) union.serializers()).apply(key);
        if (serializer == null) throw new UnsupportedOperationException("Unrecognized type: " + key);
        return serializer.read(buffer);
    }

    @SuppressWarnings("unchecked")
    private static <T extends @UnknownNullability Object> T construct(IrConstructor<T> constructor, Object[] values) {
        final Object raw = constructor.raw();
        try {
            for (Method method : raw.getClass().getMethods()) {
                if (method.getName().equals("apply") && method.getParameterCount() == constructor.fieldCount()) {
                    return (T) method.invoke(raw, values);
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke template constructor", exception);
        }
        throw new IllegalStateException("Could not find template constructor apply method with " + constructor.fieldCount() + " parameters");
    }

    private static NetworkBuffer.Type<?> primitiveType(PrimitiveKind kind) {
        return switch (kind) {
            case UNIT -> NetworkBuffer.UNIT;
            case BOOLEAN -> NetworkBuffer.BOOLEAN;
            case BYTE -> NetworkBuffer.BYTE;
            case UNSIGNED_BYTE -> NetworkBuffer.UNSIGNED_BYTE;
            case SHORT -> NetworkBuffer.SHORT;
            case UNSIGNED_SHORT -> NetworkBuffer.UNSIGNED_SHORT;
            case INT -> NetworkBuffer.INT;
            case UNSIGNED_INT -> NetworkBuffer.UNSIGNED_INT;
            case LONG -> NetworkBuffer.LONG;
            case FLOAT -> NetworkBuffer.FLOAT;
            case DOUBLE -> NetworkBuffer.DOUBLE;
        };
    }
}

final class TemplateIrBuilder {
    private TemplateIrBuilder() {
    }

    @SuppressWarnings("unchecked")
    static <T extends @UnknownNullability Object> NetworkIr<T> build(TemplateInput input) {
        final Object[] values = input.values();
        final int fieldCount = input.fieldCount();
        final List<IrField<T, ?>> fields = new ArrayList<>(fieldCount);
        for (int i = 0; i < fieldCount; i++) {
            final NetworkBuffer.Type<Object> type = (NetworkBuffer.Type<Object>) values[i * 2];
            final Function<? super T, ?> getter = (Function<? super T, ?>) values[i * 2 + 1];
            fields.add(new IrField<>(i, Integer.toString(i + 1), type, TypeResolver.resolve(type), getter));
        }
        return new NetworkIr<>("NetworkTemplate", List.copyOf(fields), new IrConstructor<>(values[fieldCount * 2], fieldCount));
    }
}

final class TypeResolver {
    private TypeResolver() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T extends @UnknownNullability Object> IrType<T> resolve(NetworkBuffer.Type<T> type) {
        return switch (type) {
            case NetworkIrBacked<?> backed -> (IrType<T>) new IrType.Template<>(backed.ir());
            case NetworkBufferTypeImpl.UnitType _ -> (IrType<T>) new IrType.Unit();
            case NetworkBufferTypeImpl.BooleanType _ -> primitive(PrimitiveKind.BOOLEAN);
            case NetworkBufferTypeImpl.ByteType _ -> primitive(PrimitiveKind.BYTE);
            case NetworkBufferTypeImpl.UnsignedByteType _ -> primitive(PrimitiveKind.UNSIGNED_BYTE);
            case NetworkBufferTypeImpl.ShortType _ -> primitive(PrimitiveKind.SHORT);
            case NetworkBufferTypeImpl.UnsignedShortType _ -> primitive(PrimitiveKind.UNSIGNED_SHORT);
            case NetworkBufferTypeImpl.IntType _ -> primitive(PrimitiveKind.INT);
            case NetworkBufferTypeImpl.UnsignedIntType _ -> primitive(PrimitiveKind.UNSIGNED_INT);
            case NetworkBufferTypeImpl.LongType _ -> primitive(PrimitiveKind.LONG);
            case NetworkBufferTypeImpl.FloatType _ -> primitive(PrimitiveKind.FLOAT);
            case NetworkBufferTypeImpl.DoubleType _ -> primitive(PrimitiveKind.DOUBLE);
            case NetworkBufferTypeImpl.VarIntType _ -> (IrType<T>) new IrType.VarInt();
            case NetworkBufferTypeImpl.VarLongType _ -> (IrType<T>) new IrType.VarLong();
            case NetworkBufferTypeImpl.OptionalType<?> optional ->
                    (IrType<T>) new IrType.Optional<>(resolve((NetworkBuffer.Type) optional.parent()));
            case NetworkBufferTypeImpl.TransformType<?, ?> transform -> (IrType<T>) new IrType.Transform<>(
                    resolve((NetworkBuffer.Type) transform.parent()),
                    transform.to(),
                    (Function) transform.from());
            case NetworkBufferTypeImpl.RawBytesType rawBytes when rawBytes.length() >= 0 ->
                    (IrType<T>) new IrType.FixedBytes(rawBytes.length());
            case NetworkBufferTypeImpl.RawBytesType _ -> (IrType<T>) new IrType.RawBytes();
            case NetworkBufferTypeImpl.StringType _ -> (IrType<T>) new IrType.StringType();
            case NetworkBufferTypeImpl.ByteArrayType _ -> (IrType<T>) new IrType.ByteArray();
            case NetworkBufferTypeImpl.LengthPrefixedType<?> lengthPrefixed ->
                    (IrType<T>) new IrType.LengthPrefixed<>(resolve((NetworkBuffer.Type) lengthPrefixed.parent()), lengthPrefixed.maxLength());
            case NetworkBufferTypeImpl.ListType<?> list ->
                    (IrType<T>) new IrType.ListType<>(resolve((NetworkBuffer.Type) list.parent()), list.maxSize());
            case NetworkBufferTypeImpl.SetType<?> set ->
                    (IrType<T>) new IrType.SetType<>(resolve((NetworkBuffer.Type) set.parent()), set.maxSize());
            case NetworkBufferTypeImpl.MapType<?, ?> map -> (IrType<T>) new IrType.MapType<>(
                    resolve((NetworkBuffer.Type) map.parent()),
                    resolve((NetworkBuffer.Type) map.valueType()),
                    map.maxSize());
            case NetworkBufferTypeImpl.EitherType<?, ?> either -> (IrType<T>) new IrType.EitherType<>(
                    resolve((NetworkBuffer.Type) either.left()),
                    resolve((NetworkBuffer.Type) either.right()));
            case NetworkBufferTypeImpl.UnionType<?, ?, ?> union -> (IrType<T>) new IrType.UnionType<>(
                    resolve((NetworkBuffer.Type) union.keyType()),
                    union.keyFunc(),
                    (Function) union.serializers());
            default -> new IrType.Delegated<>(type);
        };
    }

    private static <T extends @UnknownNullability Object> IrType<T> primitive(PrimitiveKind kind) {
        return new IrType.Primitive<>(kind);
    }
}

final class DebugIrPrinter {
    private DebugIrPrinter() {
    }

    static String print(NetworkIr<?> ir) {
        final StringBuilder builder = new StringBuilder();
        builder.append("NetworkIr ").append(ir.debugName()).append('\n');
        for (IrField<?, ?> field : ir.fields()) {
            builder.append("  field ")
                    .append(field.index())
                    .append(" path=")
                    .append(field.path())
                    .append(" type=")
                    .append(printType(field.irType()))
                    .append('\n');
        }
        builder.append("  ctor fields=").append(ir.constructor().fieldCount()).append('\n');
        return builder.toString();
    }

    static String print(WriteProgram program) {
        final StringBuilder builder = new StringBuilder();
        builder.append("WriteProgram\n");
        for (WriteOp op : program.ops()) {
            builder.append("  ").append(printWriteOp(op)).append('\n');
        }
        return builder.toString();
    }

    static String print(ReadProgram program) {
        final StringBuilder builder = new StringBuilder();
        builder.append("ReadProgram\n");
        for (ReadOp op : program.ops()) {
            builder.append("  ").append(printReadOp(op)).append('\n');
        }
        return builder.toString();
    }

    private static String printType(IrType<?> type) {
        return switch (type) {
            case IrType.Delegated<?> delegated -> "Delegated(" + delegated.type().getClass().getSimpleName() + ")";
            case IrType.Unit _ -> "Unit";
            case IrType.Primitive<?> primitive -> "Primitive(" + primitive.kind() + ")";
            case IrType.VarInt _ -> "VarInt";
            case IrType.VarLong _ -> "VarLong";
            case IrType.Optional<?> optional -> "Optional(" + printType(optional.parent()) + ")";
            case IrType.Transform<?, ?> transform -> "Transform(" + printType(transform.parent()) + ")";
            case IrType.Template<?> template -> "Template(" + template.ir().debugName() + ")";
            case IrType.FixedBytes fixedBytes -> "FixedBytes(" + fixedBytes.length() + ")";
            case IrType.RawBytes _ -> "RawBytes";
            case IrType.StringType _ -> "String";
            case IrType.ByteArray _ -> "ByteArray";
            case IrType.LengthPrefixed<?> lengthPrefixed -> "LengthPrefixed(max=" + lengthPrefixed.maxLength() + ", " + printType(lengthPrefixed.parent()) + ")";
            case IrType.ListType<?> list -> "List(max=" + list.maxSize() + ", " + printType(list.elementType()) + ")";
            case IrType.SetType<?> set -> "Set(max=" + set.maxSize() + ", " + printType(set.elementType()) + ")";
            case IrType.MapType<?, ?> map -> "Map(max=" + map.maxSize() + ", " + printType(map.keyType()) + ", " + printType(map.valueType()) + ")";
            case IrType.EitherType<?, ?> either -> "Either(" + printType(either.left()) + ", " + printType(either.right()) + ")";
            case IrType.UnionType<?, ?, ?> union -> "Union(" + printType(union.keyType()) + ")";
        };
    }

    private static String printWriteOp(WriteOp op) {
        return switch (op) {
            case WriteOp.ApplyTransform applyTransform -> "ApplyTransform(" + applyTransform.fieldIndex() + ")";
            case WriteOp.WriteField writeField -> "WriteField(" + writeField.fieldIndex() + ")";
            case WriteOp.WriteDelegated writeDelegated -> "WriteDelegated(" + writeDelegated.fieldIndex() + ")";
            case WriteOp.WriteIntrinsic writeIntrinsic -> "WriteIntrinsic(" + writeIntrinsic.fieldIndex() + ")";
            case WriteOp.WriteVarInt writeVarInt -> "WriteVarInt(" + writeVarInt.fieldIndex() + ")";
            case WriteOp.WriteVarLong writeVarLong -> "WriteVarLong(" + writeVarLong.fieldIndex() + ")";
            case WriteOp.WriteString writeString -> "WriteString(" + writeString.fieldIndex() + ")";
            case WriteOp.WriteByteArray writeByteArray -> "WriteByteArray(" + writeByteArray.fieldIndex() + ")";
            case WriteOp.WriteRawBytes writeRawBytes -> "WriteRawBytes(" + writeRawBytes.fieldIndex() + ")";
            case WriteOp.WriteOptionalDelegated writeOptionalDelegated -> "WriteOptionalDelegated(" + writeOptionalDelegated.fieldIndex() + ")";
            case WriteOp.WriteOptionalRun optionalRun ->
                    "WriteOptionalRun(" + optionalRun.startInclusive() + ".." + optionalRun.endExclusive() + ", base=" + optionalRun.baseSize().value() + ")";
            case WriteOp.WriteRun run -> "WriteRun(size=" + run.totalSize().value() + ", writes=" + run.writes() + ")";
        };
    }

    private static String printReadOp(ReadOp op) {
        return switch (op) {
            case ReadOp.ReadField readField -> "ReadField(" + readField.fieldIndex() + ")";
            case ReadOp.ReadDelegated readDelegated -> "ReadDelegated(" + readDelegated.fieldIndex() + ")";
            case ReadOp.ReadIntrinsic readIntrinsic -> "ReadIntrinsic(" + readIntrinsic.fieldIndex() + ")";
            case ReadOp.ReadVarInt readVarInt -> "ReadVarInt(" + readVarInt.fieldIndex() + ")";
            case ReadOp.ReadVarLong readVarLong -> "ReadVarLong(" + readVarLong.fieldIndex() + ")";
            case ReadOp.ReadString readString -> "ReadString(" + readString.fieldIndex() + ")";
            case ReadOp.ReadByteArray readByteArray -> "ReadByteArray(" + readByteArray.fieldIndex() + ")";
            case ReadOp.ReadRawBytes readRawBytes -> "ReadRawBytes(" + readRawBytes.fieldIndex() + ")";
            case ReadOp.ReadUnit readUnit -> "ReadUnit(" + readUnit.fieldIndex() + ")";
            case ReadOp.ReadOptionalDelegated readOptionalDelegated -> "ReadOptionalDelegated(" + readOptionalDelegated.fieldIndex() + ")";
            case ReadOp.ApplyTransform applyTransform -> "ApplyTransform(" + applyTransform.fieldIndex() + ")";
            case ReadOp.ReadOptionalRun optionalRun ->
                    "ReadOptionalRun(" + optionalRun.startInclusive() + ".." + optionalRun.endExclusive() + ", base=" + optionalRun.baseSize().value() + ")";
            case ReadOp.ReadRun run -> "ReadRun(size=" + run.totalSize().value() + ", reads=" + run.reads() + ")";
        };
    }
}

record WriteProgram(List<WriteOp> ops) {
}

sealed interface WriteOp {
    record ApplyTransform(int fieldIndex) implements WriteOp {
    }

    record WriteField(int fieldIndex) implements WriteOp {
    }

    record WriteDelegated(int fieldIndex) implements WriteOp {
    }

    record WriteIntrinsic(int fieldIndex) implements WriteOp {
    }

    record WriteVarInt(int fieldIndex) implements WriteOp {
    }

    record WriteVarLong(int fieldIndex) implements WriteOp {
    }

    record WriteString(int fieldIndex) implements WriteOp {
    }

    record WriteByteArray(int fieldIndex) implements WriteOp {
    }

    record WriteRawBytes(int fieldIndex) implements WriteOp {
    }

    record WriteOptionalDelegated(int fieldIndex) implements WriteOp {
    }

    record WriteOptionalRun(int startInclusive, int endExclusive, SizeExpr.Const baseSize) implements WriteOp {
    }

    record WriteRun(SizeExpr.Const totalSize, List<DirectWrite> writes) implements WriteOp {
    }
}

sealed interface DirectWrite {
    record Field(int fieldIndex, SizeExpr.Const offset) implements DirectWrite {
    }

    record PackedBytes(PrimitiveKind kind, SizeExpr.Const offset, List<Field> fields) implements DirectWrite {
    }
}

record ReadProgram(List<ReadOp> ops) {
}

sealed interface ReadOp {
    record ReadField(int fieldIndex) implements ReadOp {
    }

    record ReadDelegated(int fieldIndex) implements ReadOp {
    }

    record ReadIntrinsic(int fieldIndex) implements ReadOp {
    }

    record ReadVarInt(int fieldIndex) implements ReadOp {
    }

    record ReadVarLong(int fieldIndex) implements ReadOp {
    }

    record ReadString(int fieldIndex) implements ReadOp {
    }

    record ReadByteArray(int fieldIndex) implements ReadOp {
    }

    record ReadRawBytes(int fieldIndex) implements ReadOp {
    }

    record ReadUnit(int fieldIndex) implements ReadOp {
    }

    record ReadOptionalDelegated(int fieldIndex) implements ReadOp {
    }

    record ApplyTransform(int fieldIndex) implements ReadOp {
    }

    record ReadOptionalRun(int startInclusive, int endExclusive, SizeExpr.Const baseSize) implements ReadOp {
    }

    record ReadRun(SizeExpr.Const totalSize, List<DirectRead> reads) implements ReadOp {
    }
}

sealed interface DirectRead {
    record Field(int fieldIndex, SizeExpr.Const offset) implements DirectRead {
    }
}

sealed interface SizeExpr {
    record Const(long value) implements SizeExpr {
    }
}
