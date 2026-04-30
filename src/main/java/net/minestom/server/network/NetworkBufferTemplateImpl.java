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
    private static final ClassDesc CD_OPTIONAL_TYPE = NetworkBufferTypeImpl.OptionalType.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_TRANSFORM_TYPE = NetworkBufferTypeImpl.TransformType.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_FUNCTION = Function.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_UNIT = net.minestom.server.utils.Unit.class.describeConstable().orElseThrow();

    private static final MethodTypeDesc MT_VOID = MethodTypeDesc.of(CD_VOID);
    private static final MethodTypeDesc MT_LOOKUP = MethodTypeDesc.of(CD_METHOD_HANDLES_LOOKUP);
    private static final MethodTypeDesc MT_CLASS_DATA_AT = MethodTypeDesc.of(CD_OBJECT, CD_METHOD_HANDLES_LOOKUP, CD_STRING, CD_CLASS, CD_INT);
    private static final MethodTypeDesc MT_READ_OBJECT = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER);
    private static final MethodTypeDesc MT_WRITE_OBJECT = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_OBJECT);
    private static final MethodTypeDesc MT_PARENT = MethodTypeDesc.of(CD_TYPE);
    private static final MethodTypeDesc MT_INDEX_GETTER = MethodTypeDesc.of(CD_LONG);
    private static final MethodTypeDesc MT_INDEX_SETTER = MethodTypeDesc.of(CD_NETWORK_BUFFER, CD_LONG);
    private static final MethodTypeDesc MT_ENSURE = MethodTypeDesc.of(CD_VOID, CD_LONG);
    private static final MethodTypeDesc MT_FUNCTION_APPLY = MethodTypeDesc.of(CD_OBJECT, CD_OBJECT);
    private static final MethodTypeDesc MT_FUNCTION = MethodTypeDesc.of(CD_FUNCTION);
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
    private static final String TRANSFORM_TO_PREFIX = "to";
    private static final String TRANSFORM_FROM_PREFIX = "from";
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
            final Intrinsic[] fieldIntrinsics = fieldIntrinsics(values, fieldCount);
            final byte[] bytes = ClassFile.of().build(classDesc, classBuilder -> {
                classBuilder.withFlags(CLASS_FLAGS)
                        .withSuperclass(CD_OBJECT)
                        .withInterfaceSymbols(CD_IMPL_TYPE);

                for (int i = 0; i < fieldCount; i++) {
                    if (fieldIntrinsics[i] == null) {
                        classBuilder.withField(typeName(i), CD_TYPE, FIELD_FLAGS);
                    } else if (fieldIntrinsics[i].transformed()) {
                        for (int level = 0; level < fieldIntrinsics[i].transformDepth(); level++) {
                            classBuilder.withField(transformToName(i, level), CD_FUNCTION, FIELD_FLAGS);
                            classBuilder.withField(transformFromName(i, level), CD_FUNCTION, FIELD_FLAGS);
                        }
                    }
                    classBuilder.withField(getterName(i), CD_FUNCTION, FIELD_FLAGS);
                }
                final ClassDesc ctor = constructorInterface(fieldCount);
                classBuilder.withField(CTOR_NAME, ctor, FIELD_FLAGS);

                classBuilder.withMethodBody(ConstantDescs.CLASS_INIT_NAME, MT_VOID, ClassFile.ACC_STATIC | ClassFile.ACC_SYNTHETIC,
                        codeBuilder -> buildClassInitializer(codeBuilder, classDesc, fieldCount, ctor, fieldIntrinsics));
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

    private static void buildClassInitializer(CodeBuilder codeBuilder, ClassDesc classDesc, int fieldCount, ClassDesc ctor, Intrinsic[] fieldIntrinsics) {
        codeBuilder.invokestatic(CD_METHOD_HANDLES, "lookup", MT_LOOKUP)
                .astore(0);
        for (int i = 0; i < fieldCount; i++) {
            if (fieldIntrinsics[i] == null) {
                loadClassDataAt(codeBuilder, CD_TYPE, i * 2)
                        .putstatic(classDesc, typeName(i), CD_TYPE);
            } else if (fieldIntrinsics[i].transformed()) {
                final boolean optional = fieldIntrinsics[i] instanceof OptionalIntrinsic;
                for (int level = 0; level < fieldIntrinsics[i].transformDepth(); level++) {
                    loadTransformFunction(codeBuilder, i, optional, level, false)
                            .putstatic(classDesc, transformFromName(i, level), CD_FUNCTION);
                    loadTransformFunction(codeBuilder, i, optional, level, true)
                            .putstatic(classDesc, transformToName(i, level), CD_FUNCTION);
                }
            }
            loadClassDataAt(codeBuilder, CD_FUNCTION, i * 2 + 1)
                    .putstatic(classDesc, getterName(i), CD_FUNCTION);
        }
        loadClassDataAt(codeBuilder, ctor, fieldCount * 2)
                .putstatic(classDesc, CTOR_NAME, ctor)
                .return_();
    }

    private static void buildWrite(CodeBuilder codeBuilder, ClassDesc classDesc, int fieldCount, Intrinsic[] fieldIntrinsics) {
        for (int i = 0; i < fieldCount; ) {
            if (isFixedIntrinsic(fieldIntrinsics[i])) {
                final int start = i;
                long size = 0;
                do {
                    size += fieldIntrinsics[i++].size();
                } while (i < fieldCount && isFixedIntrinsic(fieldIntrinsics[i]));
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
            } else if (fieldIntrinsics[i] instanceof OptionalIntrinsic optional) {
                emitOptionalWrite(codeBuilder, classDesc, i, optional);
                i++;
            } else {
                emitWrite(codeBuilder, classDesc, i++);
            }
        }
        codeBuilder.return_();
    }

    private static void buildRead(CodeBuilder codeBuilder, ClassDesc classDesc, int fieldCount, ClassDesc ctor, Intrinsic[] fieldIntrinsics) {
        final int[] valueSlots = new int[fieldCount];
        final Intrinsic[] valueIntrinsics = new Intrinsic[fieldCount];

        for (int i = 0; i < fieldCount; ) {
            if (isFixedIntrinsic(fieldIntrinsics[i])) {
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.aload(1)
                        .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_GETTER)
                        .lstore(indexSlot);
                emitDirectBuffer(codeBuilder, directSlot);
                // TODO: call NetworkBuffer.ensureReadable(totalSize) here once it exists.
                long size = 0;
                do {
                    final Intrinsic intrinsic = fieldIntrinsics[i];
                    emitIntrinsicRead(codeBuilder, intrinsic, size, directSlot, indexSlot);
                    valueIntrinsics[i] = intrinsic;
                    valueSlots[i] = codeBuilder.allocateLocal(intrinsic.localKind());
                    codeBuilder.storeLocal(intrinsic.localKind(), valueSlots[i]);
                    size += intrinsic.size();
                    i++;
                } while (i < fieldCount && isFixedIntrinsic(fieldIntrinsics[i]));
                codeBuilder.aload(1)
                        .lload(indexSlot)
                        .loadConstant(size)
                        .ladd()
                        .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_SETTER)
                        .pop();
            } else if (fieldIntrinsics[i] instanceof OptionalIntrinsic optional) {
                emitOptionalRead(codeBuilder, classDesc, i, optional, valueSlots);
                i++;
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
            emitConstructorValue(codeBuilder, classDesc, i, valueIntrinsics[i], valueSlots[i]);
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

    private static boolean isFixedIntrinsic(Intrinsic intrinsic) {
        return intrinsic != null && !(intrinsic instanceof OptionalIntrinsic);
    }

    private static void emitIntrinsicWrite(CodeBuilder codeBuilder, ClassDesc classDesc, int index, Intrinsic intrinsic,
                                           long offset, int directSlot, int indexSlot) {
        if (intrinsic.primitive() == PrimitiveIntrinsic.UNIT) {
            codeBuilder.getstatic(classDesc, getterName(index), CD_FUNCTION)
                    .aload(2)
                    .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
            if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, index, intrinsic, false);
            codeBuilder.pop();
            return;
        }

        codeBuilder.aload(directSlot);
        emitOffsetIndex(codeBuilder, offset, indexSlot);
        codeBuilder.getstatic(classDesc, getterName(index), CD_FUNCTION)
                .aload(2)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
        if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, index, intrinsic, false);
        emitIntrinsicWriteValue(codeBuilder, intrinsic.primitive());
    }

    private static void emitIntrinsicRead(CodeBuilder codeBuilder, Intrinsic intrinsic, long offset, int directSlot, int indexSlot) {
        codeBuilder.aload(directSlot);
        emitOffsetIndex(codeBuilder, offset, indexSlot);
        emitIntrinsicReadValue(codeBuilder, intrinsic.primitive());
    }

    private static void emitOffsetIndex(CodeBuilder codeBuilder, long offset, int indexSlot) {
        codeBuilder.lload(indexSlot);
        if (offset > 0) {
            codeBuilder.loadConstant(offset)
                    .ladd();
        }
    }

    private static void emitIntrinsicWriteValue(CodeBuilder codeBuilder, PrimitiveIntrinsic kind) {
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

    private static void emitIntrinsicReadValue(CodeBuilder codeBuilder, PrimitiveIntrinsic kind) {
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

    private static void emitConstructorValue(CodeBuilder codeBuilder, ClassDesc classDesc, int index, Intrinsic intrinsic, int valueSlot) {
        if (intrinsic == null) {
            codeBuilder.aload(valueSlot);
            return;
        }
        codeBuilder.loadLocal(intrinsic.localKind(), valueSlot);
        emitBoxedIntrinsicValue(codeBuilder, intrinsic.primitive());
        if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, index, intrinsic, true);
    }

    private static void emitBoxedIntrinsicValue(CodeBuilder codeBuilder, PrimitiveIntrinsic kind) {
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

    private static void emitOptionalWrite(CodeBuilder codeBuilder, ClassDesc classDesc, int index, OptionalIntrinsic optional) {
        final Intrinsic intrinsic = optional.parent();
        final int valueSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final var notNull = codeBuilder.newLabel();
        final var end = codeBuilder.newLabel();

        codeBuilder.getstatic(classDesc, getterName(index), CD_FUNCTION)
                .aload(2)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY)
                .astore(valueSlot)
                .aload(valueSlot)
                .ifnonnull(notNull);
        emitEnsureWritable(codeBuilder, 1);
        codeBuilder.aload(1)
                .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_GETTER)
                .lstore(indexSlot);
        emitDirectBuffer(codeBuilder, directSlot);
        codeBuilder.aload(directSlot)
                .lload(indexSlot)
                .iconst_0()
                .i2b()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByte", MT_PUT_BYTE)
                .aload(1)
                .lload(indexSlot)
                .lconst_1()
                .ladd()
                .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_SETTER)
                .pop()
                .goto_(end)
                .labelBinding(notNull);
        emitEnsureWritable(codeBuilder, 1L + intrinsic.size());
        codeBuilder.aload(1)
                .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_GETTER)
                .lstore(indexSlot);
        emitDirectBuffer(codeBuilder, directSlot);
        codeBuilder.aload(directSlot)
                .lload(indexSlot)
                .iconst_1()
                .i2b()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByte", MT_PUT_BYTE);
        if (intrinsic.primitive() != PrimitiveIntrinsic.UNIT) {
            codeBuilder.aload(directSlot);
            emitOffsetIndex(codeBuilder, 1, indexSlot);
            codeBuilder.aload(valueSlot);
            if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, index, intrinsic, false);
            emitIntrinsicWriteValue(codeBuilder, intrinsic.primitive());
        } else if (intrinsic.transformed()) {
            codeBuilder.aload(valueSlot);
            emitTransformApply(codeBuilder, classDesc, index, intrinsic, false);
            codeBuilder.pop();
        }
        codeBuilder.aload(1)
                .lload(indexSlot)
                .loadConstant(1L + intrinsic.size())
                .ladd()
                .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_SETTER)
                .pop()
                .labelBinding(end);
    }

    private static void emitOptionalRead(CodeBuilder codeBuilder, ClassDesc classDesc, int index, OptionalIntrinsic optional, int[] valueSlots) {
        final Intrinsic intrinsic = optional.parent();
        final int valueSlot = valueSlots[index] = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final var present = codeBuilder.newLabel();
        final var end = codeBuilder.newLabel();

        codeBuilder.aload(1)
                .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_GETTER)
                .lstore(indexSlot);
        emitDirectBuffer(codeBuilder, directSlot);
        codeBuilder.aload(directSlot)
                .lload(indexSlot)
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByte", MT_GET_BYTE)
                .iconst_1()
                .if_icmpeq(present)
                .aconst_null()
                .astore(valueSlot)
                .aload(1)
                .lload(indexSlot)
                .lconst_1()
                .ladd()
                .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_SETTER)
                .pop()
                .goto_(end)
                .labelBinding(present);
        emitIntrinsicRead(codeBuilder, intrinsic, 1, directSlot, indexSlot);
        emitBoxedIntrinsicValue(codeBuilder, intrinsic.primitive());
        if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, index, intrinsic, true);
        codeBuilder.astore(valueSlot)
                .aload(1)
                .lload(indexSlot)
                .loadConstant(1L + intrinsic.size())
                .ladd()
                .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_SETTER)
                .pop()
                .labelBinding(end);
    }

    private static void emitTransformApply(CodeBuilder codeBuilder, ClassDesc classDesc, int index, Intrinsic intrinsic, boolean to) {
        if (to) {
            for (int level = intrinsic.transformDepth() - 1; level >= 0; level--) {
                emitTransformFunctionApply(codeBuilder, classDesc, transformToName(index, level));
            }
        } else {
            for (int level = 0; level < intrinsic.transformDepth(); level++) {
                emitTransformFunctionApply(codeBuilder, classDesc, transformFromName(index, level));
            }
        }
    }

    private static void emitTransformFunctionApply(CodeBuilder codeBuilder, ClassDesc classDesc, String functionName) {
        codeBuilder.getstatic(classDesc, functionName, CD_FUNCTION)
                .swap()
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
    }

    private static Intrinsic[] fieldIntrinsics(Object[] values, int fieldCount) {
        final Intrinsic[] fieldIntrinsics = new Intrinsic[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            fieldIntrinsics[i] = intrinsic(values[i * 2]);
        }
        return fieldIntrinsics;
    }

    private static Intrinsic intrinsic(Object type) {
        if (type instanceof NetworkBufferTypeImpl.OptionalType<?> optional) {
            final Intrinsic parent = intrinsic(optional.parent());
            return parent != null ? new OptionalIntrinsic(parent) : null;
        }
        if (type instanceof NetworkBufferTypeImpl.TransformType<?, ?> transform) {
            final Intrinsic parent = intrinsic(transform.parent());
            return parent != null && !(parent instanceof OptionalIntrinsic) ? new TransformIntrinsic(parent) : null;
        }
        if (type instanceof NetworkBufferTypeImpl.UnitType) return PrimitiveIntrinsic.UNIT;
        if (type instanceof NetworkBufferTypeImpl.BooleanType) return PrimitiveIntrinsic.BOOLEAN;
        if (type instanceof NetworkBufferTypeImpl.ByteType) return PrimitiveIntrinsic.BYTE;
        if (type instanceof NetworkBufferTypeImpl.UnsignedByteType) return PrimitiveIntrinsic.UNSIGNED_BYTE;
        if (type instanceof NetworkBufferTypeImpl.ShortType) return PrimitiveIntrinsic.SHORT;
        if (type instanceof NetworkBufferTypeImpl.UnsignedShortType) return PrimitiveIntrinsic.UNSIGNED_SHORT;
        if (type instanceof NetworkBufferTypeImpl.IntType) return PrimitiveIntrinsic.INT;
        if (type instanceof NetworkBufferTypeImpl.UnsignedIntType) return PrimitiveIntrinsic.UNSIGNED_INT;
        if (type instanceof NetworkBufferTypeImpl.LongType) return PrimitiveIntrinsic.LONG;
        if (type instanceof NetworkBufferTypeImpl.FloatType) return PrimitiveIntrinsic.FLOAT;
        if (type instanceof NetworkBufferTypeImpl.DoubleType) return PrimitiveIntrinsic.DOUBLE;
        return null;
    }

    private interface Intrinsic {
        int size();

        TypeKind localKind();

        PrimitiveIntrinsic primitive();

        default boolean transformed() {
            return false;
        }

        default int transformDepth() {
            return 0;
        }
    }

    private record TransformIntrinsic(Intrinsic parent) implements Intrinsic {
        @Override
        public int size() {
            return parent.size();
        }

        @Override
        public TypeKind localKind() {
            return parent.localKind();
        }

        @Override
        public PrimitiveIntrinsic primitive() {
            return parent.primitive();
        }

        @Override
        public boolean transformed() {
            return true;
        }

        @Override
        public int transformDepth() {
            return parent.transformDepth() + 1;
        }
    }

    private record OptionalIntrinsic(Intrinsic parent) implements Intrinsic {
        @Override
        public int size() {
            return -1;
        }

        @Override
        public TypeKind localKind() {
            return TypeKind.REFERENCE;
        }

        @Override
        public PrimitiveIntrinsic primitive() {
            return parent.primitive();
        }

        @Override
        public boolean transformed() {
            return parent.transformed();
        }

        @Override
        public int transformDepth() {
            return parent.transformDepth();
        }
    }

    private enum PrimitiveIntrinsic implements Intrinsic {
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

        PrimitiveIntrinsic(int size, TypeKind localKind) {
            this.size = size;
            this.localKind = localKind;
        }

        public int size() {
            return size;
        }

        public TypeKind localKind() {
            return localKind;
        }

        public PrimitiveIntrinsic primitive() {
            return this;
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

    private static CodeBuilder loadTransformFunction(CodeBuilder codeBuilder, int index, boolean optional, int level, boolean to) {
        loadClassDataAt(codeBuilder, CD_TYPE, index * 2);
        if (optional) {
            codeBuilder.checkcast(CD_OPTIONAL_TYPE)
                    .invokevirtual(CD_OPTIONAL_TYPE, "parent", MT_PARENT);
        }
        for (int i = 0; i < level; i++) {
            codeBuilder.checkcast(CD_TRANSFORM_TYPE)
                    .invokevirtual(CD_TRANSFORM_TYPE, "parent", MT_PARENT);
        }
        return codeBuilder.checkcast(CD_TRANSFORM_TYPE)
                .invokevirtual(CD_TRANSFORM_TYPE, to ? "to" : "from", MT_FUNCTION);
    }

    private static String typeName(int index) {
        return TYPE_PREFIX + (index + 1);
    }

    private static String getterName(int index) {
        return GETTER_PREFIX + (index + 1);
    }

    private static String transformToName(int index, int level) {
        return TRANSFORM_TO_PREFIX + (index + 1) + "_" + (level + 1);
    }

    private static String transformFromName(int index, int level) {
        return TRANSFORM_FROM_PREFIX + (index + 1) + "_" + (level + 1);
    }
}
