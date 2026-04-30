package net.minestom.server.network;

import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

final class NetworkBufferTemplateImpl {
    private static final String PACKAGE = "net.minestom.server.network";
    private static final ClassDesc CD_OBJECT = ConstantDescs.CD_Object;
    private static final ClassDesc CD_STRING = ConstantDescs.CD_String;
    private static final ClassDesc CD_CLASS = ConstantDescs.CD_Class;
    private static final ClassDesc CD_INT = ConstantDescs.CD_int;
    private static final ClassDesc CD_LONG = ConstantDescs.CD_long;
    private static final ClassDesc CD_FLOAT = ConstantDescs.CD_float;
    private static final ClassDesc CD_DOUBLE = ConstantDescs.CD_double;
    private static final ClassDesc CD_SHORT = ConstantDescs.CD_short;
    private static final ClassDesc CD_BYTE = ConstantDescs.CD_byte;
    private static final ClassDesc CD_VOID = ConstantDescs.CD_void;
    private static final ClassDesc CD_BOOLEAN_WRAPPER = ConstantDescs.CD_Boolean;
    private static final ClassDesc CD_BYTE_WRAPPER = ConstantDescs.CD_Byte;
    private static final ClassDesc CD_SHORT_WRAPPER = ConstantDescs.CD_Short;
    private static final ClassDesc CD_INTEGER_WRAPPER = ConstantDescs.CD_Integer;
    private static final ClassDesc CD_LONG_WRAPPER = ConstantDescs.CD_Long;
    private static final ClassDesc CD_FLOAT_WRAPPER = ConstantDescs.CD_Float;
    private static final ClassDesc CD_DOUBLE_WRAPPER = ConstantDescs.CD_Double;
    private static final ClassDesc CD_METHOD_HANDLES = ConstantDescs.CD_MethodHandles;
    private static final ClassDesc CD_METHOD_HANDLES_LOOKUP = ConstantDescs.CD_MethodHandles_Lookup;
    private static final ClassDesc CD_NETWORK_BUFFER = NetworkBuffer.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_NETWORK_BUFFER_IMPL = NetworkBufferImpl.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_TYPE = NetworkBuffer.Type.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_IMPL_TYPE = NetworkBufferTypeImpl.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_FUNCTION = Function.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_UNIT = net.minestom.server.utils.Unit.class.describeConstable().orElseThrow();

    private static final MethodTypeDesc MT_VOID = MethodTypeDesc.of(CD_VOID);
    private static final MethodTypeDesc MT_LOOKUP = MethodTypeDesc.of(CD_METHOD_HANDLES_LOOKUP);
    private static final MethodTypeDesc MT_CLASS_DATA_AT = MethodTypeDesc.of(CD_OBJECT, CD_METHOD_HANDLES_LOOKUP, CD_STRING, CD_CLASS, CD_INT);
    private static final MethodTypeDesc MT_READ_OBJECT = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER);
    private static final MethodTypeDesc MT_WRITE_OBJECT = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_OBJECT);
    private static final MethodTypeDesc MT_INDEX_GETTER = MethodTypeDesc.of(CD_LONG);
    private static final MethodTypeDesc MT_INDEX_SETTER = MethodTypeDesc.of(CD_NETWORK_BUFFER, CD_LONG);
    private static final MethodTypeDesc MT_ENSURE = MethodTypeDesc.of(CD_VOID, CD_LONG);
    private static final MethodTypeDesc MT_FUNCTION_APPLY = MethodTypeDesc.of(CD_OBJECT, CD_OBJECT);
    private static final MethodTypeDesc MT_BOOLEAN_VALUE = MethodTypeDesc.of(ConstantDescs.CD_boolean);
    private static final MethodTypeDesc MT_BYTE_VALUE = MethodTypeDesc.of(CD_BYTE);
    private static final MethodTypeDesc MT_SHORT_VALUE = MethodTypeDesc.of(CD_SHORT);
    private static final MethodTypeDesc MT_INT_VALUE = MethodTypeDesc.of(CD_INT);
    private static final MethodTypeDesc MT_LONG_VALUE = MethodTypeDesc.of(CD_LONG);
    private static final MethodTypeDesc MT_FLOAT_VALUE = MethodTypeDesc.of(CD_FLOAT);
    private static final MethodTypeDesc MT_DOUBLE_VALUE = MethodTypeDesc.of(CD_DOUBLE);
    private static final MethodTypeDesc MT_BOX_BOOLEAN = MethodTypeDesc.of(CD_BOOLEAN_WRAPPER, ConstantDescs.CD_boolean);
    private static final MethodTypeDesc MT_BOX_BYTE = MethodTypeDesc.of(CD_BYTE_WRAPPER, CD_BYTE);
    private static final MethodTypeDesc MT_BOX_SHORT = MethodTypeDesc.of(CD_SHORT_WRAPPER, CD_SHORT);
    private static final MethodTypeDesc MT_BOX_INT = MethodTypeDesc.of(CD_INTEGER_WRAPPER, CD_INT);
    private static final MethodTypeDesc MT_BOX_LONG = MethodTypeDesc.of(CD_LONG_WRAPPER, CD_LONG);
    private static final MethodTypeDesc MT_BOX_FLOAT = MethodTypeDesc.of(CD_FLOAT_WRAPPER, CD_FLOAT);
    private static final MethodTypeDesc MT_BOX_DOUBLE = MethodTypeDesc.of(CD_DOUBLE_WRAPPER, CD_DOUBLE);
    private static final MethodTypeDesc MT_PUT_BYTE = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_BYTE);
    private static final MethodTypeDesc MT_GET_BYTE = MethodTypeDesc.of(CD_BYTE, CD_LONG);
    private static final MethodTypeDesc MT_PUT_SHORT = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_SHORT);
    private static final MethodTypeDesc MT_GET_SHORT = MethodTypeDesc.of(CD_SHORT, CD_LONG);
    private static final MethodTypeDesc MT_PUT_INT = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_INT);
    private static final MethodTypeDesc MT_GET_INT = MethodTypeDesc.of(CD_INT, CD_LONG);
    private static final MethodTypeDesc MT_PUT_LONG = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_LONG);
    private static final MethodTypeDesc MT_GET_LONG = MethodTypeDesc.of(CD_LONG, CD_LONG);
    private static final MethodTypeDesc MT_PUT_FLOAT = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_FLOAT);
    private static final MethodTypeDesc MT_GET_FLOAT = MethodTypeDesc.of(CD_FLOAT, CD_LONG);
    private static final MethodTypeDesc MT_PUT_DOUBLE = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_DOUBLE);
    private static final MethodTypeDesc MT_GET_DOUBLE = MethodTypeDesc.of(CD_DOUBLE, CD_LONG);

    private static final int FIELD_FLAGS = ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    private static final int METHOD_FLAGS = ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    private static final int CLASS_FLAGS = ClassFile.ACC_FINAL | ClassFile.ACC_SUPER | ClassFile.ACC_SYNTHETIC;

    private static final String CTOR_NAME = "ctor";
    private static final String TYPE_PREFIX = "t";
    private static final String GETTER_PREFIX = "g";
    private static final String READ = "read";
    private static final String WRITE = "write";

    private NetworkBufferTemplateImpl() {}

    // pairs of [Type<T>, Function (getter)] for N fields, up to 20
    // always odd because ends in ctor applicable to N.
    @SuppressWarnings("unchecked")
    static <T extends @UnknownNullability Object> NetworkBuffer.Type<T> template(Object... values) {
        Objects.requireNonNull(values, "values");
        Check.argCondition(values.length % 2 == 0, "Expected an odd number of values, got: {0}", values.length);
        Check.argCondition(values.length < 3, "Expected at least three values ([type, getter], ctor), got: {0}", values.length);
        final int fieldCount = values.length / 2;
        Check.argCondition(fieldCount > 20, "Templates only support up to 20 fields, got: {0}", fieldCount);
        for (int i = 0; i < fieldCount; i++) {
            Objects.requireNonNull(values[i * 2], typeName(i));
            Objects.requireNonNull(values[i * 2 + 1], getterName(i));
        }
        Objects.requireNonNull(values[values.length - 1], CTOR_NAME);
        try {
            final ClassDesc classDesc = ClassDesc.of(PACKAGE, "NetworkTemplate");
            final IntrinsicKind[] fieldIntrinsics = fieldIntrinsics(values, fieldCount);
            final byte[] bytes = ClassFile.of().build(classDesc, classBuilder -> {
                classBuilder.withFlags(CLASS_FLAGS)
                        .withSuperclass(CD_OBJECT)
                        .withInterfaceSymbols(CD_IMPL_TYPE);

                for (int i = 0; i < fieldCount; i++) {
                    classBuilder.withField(typeName(i), CD_TYPE, FIELD_FLAGS);
                    classBuilder.withField(getterName(i), CD_FUNCTION, FIELD_FLAGS);
                }
                final ClassDesc ctor = constructorInterface(fieldCount);
                classBuilder.withField(CTOR_NAME, ctor, FIELD_FLAGS);

                classBuilder.withMethodBody(ConstantDescs.CLASS_INIT_NAME, MT_VOID, ClassFile.ACC_STATIC | ClassFile.ACC_SYNTHETIC,
                        codeBuilder -> buildClassInitializer(codeBuilder, classDesc, fieldCount, ctor));
                classBuilder.withMethodBody(ConstantDescs.INIT_NAME, MT_VOID, ClassFile.ACC_PRIVATE | ClassFile.ACC_SYNTHETIC,
                        codeBuilder -> codeBuilder.aload(0).invokespecial(CD_OBJECT, ConstantDescs.INIT_NAME, MT_VOID).return_());
                classBuilder.withMethodBody(WRITE, MT_WRITE_OBJECT, METHOD_FLAGS,
                        codeBuilder -> buildWrite(codeBuilder, classDesc, fieldCount, fieldIntrinsics));
                classBuilder.withMethodBody(READ, MT_READ_OBJECT, METHOD_FLAGS,
                        codeBuilder -> buildRead(codeBuilder, classDesc, fieldCount, ctor, fieldIntrinsics));
            });

            if (true) {
                Files.write(Paths.get("GeneratedClass%s.class".formatted(fieldCount)), bytes);
            }
            final MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, Arrays.asList(values), true, MethodHandles.Lookup.ClassOption.NESTMATE);
            final MethodHandle constructor = lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class));
            return (NetworkBuffer.Type<T>) constructor.invoke();
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to generate network type template", throwable);
        }
    }

    private static void buildClassInitializer(CodeBuilder codeBuilder, ClassDesc classDesc, int fieldCount, ClassDesc ctor) {
        codeBuilder.invokestatic(CD_METHOD_HANDLES, "lookup", MT_LOOKUP)
                .astore(0);
        for (int i = 0; i < fieldCount; i++) {
            loadClassDataAt(codeBuilder, CD_TYPE, i * 2)
                    .putstatic(classDesc, typeName(i), CD_TYPE);
            loadClassDataAt(codeBuilder, CD_FUNCTION, i * 2 + 1)
                    .putstatic(classDesc, getterName(i), CD_FUNCTION);
        }
        loadClassDataAt(codeBuilder, ctor, fieldCount * 2)
                .putstatic(classDesc, CTOR_NAME, ctor)
                .return_();
    }

    private static void buildWrite(CodeBuilder codeBuilder, ClassDesc classDesc, int fieldCount, IntrinsicKind[] fieldIntrinsics) {
        for (int i = 0; i < fieldCount; ) {
            if (fieldIntrinsics[i] != null) {
                final int start = i;
                long size = 0;
                do {
                    size += fieldIntrinsics[i++].size();
                } while (i < fieldCount && fieldIntrinsics[i] != null);
                if (size > 0) emitEnsureWritable(codeBuilder, size);
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.aload(1)
                        .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_GETTER)
                        .lstore(indexSlot);
                emitDirectBuffer(codeBuilder, directSlot);
                long offset = 0;
                for (int j = start; j < i; j++) {
                    emitIntrinsicWrite(codeBuilder, classDesc, j, fieldIntrinsics[j], offset, directSlot, indexSlot);
                    offset += fieldIntrinsics[j].size();
                }
                codeBuilder.aload(1)
                        .lload(indexSlot)
                        .loadConstant(size)
                        .ladd()
                        .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_SETTER)
                        .pop();
            } else {
                emitWrite(codeBuilder, classDesc, i++);
            }
        }
        codeBuilder.return_();
    }

    private static void buildRead(CodeBuilder codeBuilder, ClassDesc classDesc, int fieldCount, ClassDesc ctor, IntrinsicKind[] fieldIntrinsics) {
        final int[] valueSlots = new int[fieldCount];
        final IntrinsicKind[] valueKinds = new IntrinsicKind[fieldCount];

        for (int i = 0; i < fieldCount; ) {
            if (fieldIntrinsics[i] != null) {
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.aload(1)
                        .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_GETTER)
                        .lstore(indexSlot);
                emitDirectBuffer(codeBuilder, directSlot);
                // TODO: call NetworkBuffer.ensureReadable(totalSize) here once it exists.
                long size = 0;
                do {
                    final IntrinsicKind kind = fieldIntrinsics[i];
                    emitIntrinsicRead(codeBuilder, kind, size, directSlot, indexSlot);
                    valueKinds[i] = kind;
                    valueSlots[i] = codeBuilder.allocateLocal(kind.localKind());
                    codeBuilder.storeLocal(kind.localKind(), valueSlots[i]);
                    size += kind.size();
                    i++;
                } while (i < fieldCount && fieldIntrinsics[i] != null);
                codeBuilder.aload(1)
                        .lload(indexSlot)
                        .loadConstant(size)
                        .ladd()
                        .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_SETTER)
                        .pop();
            } else {
                codeBuilder.getstatic(classDesc, typeName(i), CD_TYPE)
                        .aload(1)
                        .invokeinterface(CD_TYPE, READ, MT_READ_OBJECT)
                        .astore(valueSlots[i] = codeBuilder.allocateLocal(TypeKind.REFERENCE));
                i++;
            }
        }
        codeBuilder.getstatic(classDesc, CTOR_NAME, ctor);
        for (int i = 0; i < fieldCount; i++) {
            emitConstructorValue(codeBuilder, valueKinds[i], valueSlots[i]);
        }
        codeBuilder.invokeinterface(ctor, "apply", constructorApplyType(fieldCount))
                .areturn();
    }

    private static void emitEnsureWritable(CodeBuilder codeBuilder, long size) {
        codeBuilder.aload(1)
                .loadConstant(size)
                .invokeinterface(CD_NETWORK_BUFFER, "ensureWritable", MT_ENSURE);
    }

    private static void emitWrite(CodeBuilder codeBuilder, ClassDesc classDesc, int index) {
        codeBuilder.getstatic(classDesc, typeName(index), CD_TYPE)
                .aload(1)
                .getstatic(classDesc, getterName(index), CD_FUNCTION)
                .aload(2)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY)
                .invokeinterface(CD_TYPE, WRITE, MT_WRITE_OBJECT);
    }

    private static void emitDirectBuffer(CodeBuilder codeBuilder, int directSlot) {
        codeBuilder.aload(1)
                .checkcast(CD_NETWORK_BUFFER_IMPL)
                .astore(directSlot);
    }

    private static void emitIntrinsicWrite(CodeBuilder codeBuilder, ClassDesc classDesc, int index, IntrinsicKind intrinsic, long offset, int directSlot, int indexSlot) {
        if (intrinsic == IntrinsicKind.UNIT) {
            codeBuilder.getstatic(classDesc, getterName(index), CD_FUNCTION)
                    .aload(2)
                    .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY)
                    .pop();
            return;
        }

        codeBuilder.aload(directSlot);
        emitOffsetIndex(codeBuilder, offset, indexSlot);
        codeBuilder.getstatic(classDesc, getterName(index), CD_FUNCTION)
                .aload(2)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
        emitIntrinsicWriteValue(codeBuilder, intrinsic);
    }

    private static void emitIntrinsicRead(CodeBuilder codeBuilder, IntrinsicKind intrinsic, long offset, int directSlot, int indexSlot) {
        codeBuilder.aload(directSlot);
        emitOffsetIndex(codeBuilder, offset, indexSlot);
        emitIntrinsicReadValue(codeBuilder, intrinsic);
    }

    private static void emitOffsetIndex(CodeBuilder codeBuilder, long offset, int indexSlot) {
        codeBuilder.lload(indexSlot);
        if (offset > 0) {
            codeBuilder.loadConstant(offset)
                    .ladd();
        }
    }

    private static void emitIntrinsicWriteValue(CodeBuilder codeBuilder, IntrinsicKind kind) {
        switch (kind) {
            case UNIT -> throw new UnsupportedOperationException("Should not be writing unit values");
            case BOOLEAN -> codeBuilder.checkcast(CD_BOOLEAN_WRAPPER)
                    .invokevirtual(CD_BOOLEAN_WRAPPER, "booleanValue", MT_BOOLEAN_VALUE)
                    .i2b()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByte", MT_PUT_BYTE);
            case BYTE -> codeBuilder.checkcast(CD_BYTE_WRAPPER)
                    .invokevirtual(CD_BYTE_WRAPPER, "byteValue", MT_BYTE_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByte", MT_PUT_BYTE);
            case UNSIGNED_BYTE -> codeBuilder.checkcast(CD_SHORT_WRAPPER)
                    .invokevirtual(CD_SHORT_WRAPPER, "shortValue", MT_SHORT_VALUE)
                    .sipush(0xFF)
                    .iand()
                    .i2b()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByte", MT_PUT_BYTE);
            case SHORT -> codeBuilder.checkcast(CD_SHORT_WRAPPER)
                    .invokevirtual(CD_SHORT_WRAPPER, "shortValue", MT_SHORT_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putShort", MT_PUT_SHORT);
            case UNSIGNED_SHORT -> codeBuilder.checkcast(CD_INTEGER_WRAPPER)
                    .invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE)
                    .loadConstant(0xFFFF)
                    .iand()
                    .i2s()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putShort", MT_PUT_SHORT);
            case INT -> codeBuilder.checkcast(CD_INTEGER_WRAPPER)
                    .invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putInt", MT_PUT_INT);
            case UNSIGNED_INT -> codeBuilder.checkcast(CD_LONG_WRAPPER)
                    .invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE)
                    .loadConstant(0xFFFFFFFFL)
                    .land()
                    .l2i()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putInt", MT_PUT_INT);
            case LONG -> codeBuilder.checkcast(CD_LONG_WRAPPER)
                    .invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putLong", MT_PUT_LONG);
            case FLOAT -> codeBuilder.checkcast(CD_FLOAT_WRAPPER)
                    .invokevirtual(CD_FLOAT_WRAPPER, "floatValue", MT_FLOAT_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putFloat", MT_PUT_FLOAT);
            case DOUBLE -> codeBuilder.checkcast(CD_DOUBLE_WRAPPER)
                    .invokevirtual(CD_DOUBLE_WRAPPER, "doubleValue", MT_DOUBLE_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putDouble", MT_PUT_DOUBLE);
        }
    }

    private static void emitIntrinsicReadValue(CodeBuilder codeBuilder, IntrinsicKind kind) {
        switch (kind) {
            case UNIT -> codeBuilder.pop2()
                    .pop()
                    .getstatic(CD_UNIT, "INSTANCE", CD_UNIT);
            case BOOLEAN -> {
                final var falseLabel = codeBuilder.newLabel();
                final var endLabel = codeBuilder.newLabel();
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByte", MT_GET_BYTE)
                        .iconst_1()
                        .if_icmpne(falseLabel)
                        .iconst_1()
                        .goto_(endLabel)
                        .labelBinding(falseLabel)
                        .iconst_0()
                        .labelBinding(endLabel);
            }
            case BYTE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByte", MT_GET_BYTE);
            case UNSIGNED_BYTE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByte", MT_GET_BYTE)
                    .sipush(0xFF)
                    .iand();
            case SHORT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getShort", MT_GET_SHORT);
            case UNSIGNED_SHORT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getShort", MT_GET_SHORT)
                    .loadConstant(0xFFFF)
                    .iand();
            case INT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getInt", MT_GET_INT);
            case UNSIGNED_INT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getInt", MT_GET_INT)
                    .i2l()
                    .loadConstant(0xFFFFFFFFL)
                    .land();
            case LONG -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getLong", MT_GET_LONG);
            case FLOAT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getFloat", MT_GET_FLOAT);
            case DOUBLE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getDouble", MT_GET_DOUBLE);
        }
    }

    private static void emitConstructorValue(CodeBuilder codeBuilder, IntrinsicKind kind, int valueSlot) {
        if (kind == null) {
            codeBuilder.aload(valueSlot);
            return;
        }
        codeBuilder.loadLocal(kind.localKind(), valueSlot);
        switch (kind) {
            case UNIT -> {
            }
            case BOOLEAN -> codeBuilder.invokestatic(CD_BOOLEAN_WRAPPER, "valueOf", MT_BOX_BOOLEAN);
            case BYTE -> codeBuilder.i2b()
                    .invokestatic(CD_BYTE_WRAPPER, "valueOf", MT_BOX_BYTE);
            case UNSIGNED_BYTE, SHORT -> codeBuilder.i2s()
                    .invokestatic(CD_SHORT_WRAPPER, "valueOf", MT_BOX_SHORT);
            case UNSIGNED_SHORT, INT -> codeBuilder.invokestatic(CD_INTEGER_WRAPPER, "valueOf", MT_BOX_INT);
            case UNSIGNED_INT, LONG -> codeBuilder.invokestatic(CD_LONG_WRAPPER, "valueOf", MT_BOX_LONG);
            case FLOAT -> codeBuilder.invokestatic(CD_FLOAT_WRAPPER, "valueOf", MT_BOX_FLOAT);
            case DOUBLE -> codeBuilder.invokestatic(CD_DOUBLE_WRAPPER, "valueOf", MT_BOX_DOUBLE);
        }
    }

    private static IntrinsicKind[] fieldIntrinsics(Object[] values, int fieldCount) {
        final IntrinsicKind[] fieldIntrinsics = new IntrinsicKind[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            fieldIntrinsics[i] = intrinsic(values[i * 2]);
        }
        return fieldIntrinsics;
    }

    private static IntrinsicKind intrinsic(Object type) {
        if (type instanceof NetworkBufferTypeImpl.UnitType) return IntrinsicKind.UNIT;
        if (type instanceof NetworkBufferTypeImpl.BooleanType) return IntrinsicKind.BOOLEAN;
        if (type instanceof NetworkBufferTypeImpl.ByteType) return IntrinsicKind.BYTE;
        if (type instanceof NetworkBufferTypeImpl.UnsignedByteType) return IntrinsicKind.UNSIGNED_BYTE;
        if (type instanceof NetworkBufferTypeImpl.ShortType) return IntrinsicKind.SHORT;
        if (type instanceof NetworkBufferTypeImpl.UnsignedShortType) return IntrinsicKind.UNSIGNED_SHORT;
        if (type instanceof NetworkBufferTypeImpl.IntType) return IntrinsicKind.INT;
        if (type instanceof NetworkBufferTypeImpl.UnsignedIntType) return IntrinsicKind.UNSIGNED_INT;
        if (type instanceof NetworkBufferTypeImpl.LongType) return IntrinsicKind.LONG;
        if (type instanceof NetworkBufferTypeImpl.FloatType) return IntrinsicKind.FLOAT;
        if (type instanceof NetworkBufferTypeImpl.DoubleType) return IntrinsicKind.DOUBLE;
        return null;
    }

    private enum IntrinsicKind {
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

        IntrinsicKind(int size, TypeKind localKind) {
            this.size = size;
            this.localKind = localKind;
        }

        private int size() {
            return size;
        }

        private TypeKind localKind() {
            return localKind;
        }
    }

    private static ClassDesc constructorInterface(int fieldCount) {
        return ClassDesc.of(PACKAGE, "NetworkBufferTemplate$F" + fieldCount);
    }

    private static MethodTypeDesc constructorApplyType(int fieldCount) {
        ClassDesc[] parameters = new ClassDesc[fieldCount];
        Arrays.fill(parameters, CD_OBJECT);
        return MethodTypeDesc.of(CD_OBJECT, parameters);
    }

    private static CodeBuilder loadClassDataAt(CodeBuilder codeBuilder, ClassDesc type, int index) {
        return codeBuilder.aload(0) // assumes lookup is at slot 0
                .ldc("_")
                .ldc(type)
                .loadConstant(index)
                .invokestatic(CD_METHOD_HANDLES, "classDataAt", MT_CLASS_DATA_AT)
                .checkcast(type);
    }

    private static String typeName(int index) {
        return TYPE_PREFIX + (index + 1);
    }

    private static String getterName(int index) {
        return GETTER_PREFIX + (index + 1);
    }
}
