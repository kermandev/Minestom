package net.minestom.server.network;

import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
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
    private static final ClassDesc CD_VOID = ConstantDescs.CD_void;
    private static final ClassDesc CD_METHOD_HANDLES = ConstantDescs.CD_MethodHandles;
    private static final ClassDesc CD_METHOD_HANDLES_LOOKUP = ConstantDescs.CD_MethodHandles_Lookup;
    private static final ClassDesc CD_NETWORK_BUFFER = NetworkBuffer.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_NETWORK_BUFFER_INTRINSICS = NetworkBufferIntrinsics.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_TYPE = NetworkBuffer.Type.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_IMPL_TYPE = NetworkBufferTypeImpl.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_FUNCTION = Function.class.describeConstable().orElseThrow();

    private static final MethodTypeDesc MT_VOID = MethodTypeDesc.of(CD_VOID);
    private static final MethodTypeDesc MT_LOOKUP = MethodTypeDesc.of(CD_METHOD_HANDLES_LOOKUP);
    private static final MethodTypeDesc MT_CLASS_DATA_AT = MethodTypeDesc.of(CD_OBJECT, CD_METHOD_HANDLES_LOOKUP, CD_STRING, CD_CLASS, CD_INT);
    private static final MethodTypeDesc MT_READ_OBJECT = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER);
    private static final MethodTypeDesc MT_WRITE_OBJECT = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_OBJECT);
    private static final MethodTypeDesc MT_INDEX_GETTER = MethodTypeDesc.of(CD_LONG);
    private static final MethodTypeDesc MT_INDEX_SETTER = MethodTypeDesc.of(CD_NETWORK_BUFFER, CD_LONG);
    private static final MethodTypeDesc MT_ENSURE = MethodTypeDesc.of(CD_VOID, CD_LONG);
    private static final MethodTypeDesc MT_FUNCTION_APPLY = MethodTypeDesc.of(CD_OBJECT, CD_OBJECT);
    private static final MethodTypeDesc MT_INTRINSIC_WRITE = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_LONG, CD_OBJECT);
    private static final MethodTypeDesc MT_INTRINSIC_READ = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER, CD_LONG);

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
            final FieldIntrinsic[] fieldIntrinsics = fieldIntrinsics(values, fieldCount);
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

    private static void buildWrite(CodeBuilder codeBuilder, ClassDesc classDesc, int fieldCount, FieldIntrinsic[] fieldIntrinsics) {
        for (int i = 0; i < fieldCount; ) {
            if (fieldIntrinsics[i] != null) {
                final int start = i;
                long size = 0;
                do {
                    size += fieldIntrinsics[i++].size();
                } while (i < fieldCount && fieldIntrinsics[i] != null);
                if (size > 0) emitEnsureWritable(codeBuilder, size);
                final int indexSlot = codeBuilder.allocateLocal(java.lang.classfile.TypeKind.LONG);
                codeBuilder.aload(1)
                        .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_GETTER)
                        .lstore(indexSlot);
                long offset = 0;
                for (int j = start; j < i; j++) {
                    emitIntrinsicWrite(codeBuilder, classDesc, j, fieldIntrinsics[j], offset, indexSlot);
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

    private static void buildRead(CodeBuilder codeBuilder, ClassDesc classDesc, int fieldCount, ClassDesc ctor, FieldIntrinsic[] fieldIntrinsics) {
        codeBuilder.getstatic(classDesc, CTOR_NAME, ctor);

        for (int i = 0; i < fieldCount; ) {
            if (fieldIntrinsics[i] != null) {
                final int indexSlot = codeBuilder.allocateLocal(java.lang.classfile.TypeKind.LONG);
                codeBuilder.aload(1)
                        .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_GETTER)
                        .lstore(indexSlot);
                // TODO: call NetworkBuffer.ensureReadable(totalSize) here once it exists.
                long size = 0;
                do {
                    emitIntrinsicRead(codeBuilder, fieldIntrinsics[i], size, indexSlot);
                    size += fieldIntrinsics[i].size();
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
                        .invokeinterface(CD_TYPE, READ, MT_READ_OBJECT);
                i++;
            }
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

    private static void emitIntrinsicWrite(CodeBuilder codeBuilder, ClassDesc classDesc, int index, FieldIntrinsic intrinsic, long offset, int indexSlot) {
        codeBuilder.aload(1)
                .lload(indexSlot);
        if (offset > 0) {
            codeBuilder.loadConstant(offset)
                    .ladd();
        }
        codeBuilder.getstatic(classDesc, getterName(index), CD_FUNCTION)
                .aload(2)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY)
                .invokestatic(CD_NETWORK_BUFFER_INTRINSICS, intrinsic.writeMethod(), MT_INTRINSIC_WRITE);
    }

    private static void emitIntrinsicRead(CodeBuilder codeBuilder, FieldIntrinsic intrinsic, long offset, int indexSlot) {
        codeBuilder.aload(1)
                .lload(indexSlot);
        if (offset > 0) {
            codeBuilder.loadConstant(offset)
                    .ladd();
        }
        codeBuilder.invokestatic(CD_NETWORK_BUFFER_INTRINSICS, intrinsic.readMethod(), MT_INTRINSIC_READ);
    }

    private static FieldIntrinsic[] fieldIntrinsics(Object[] values, int fieldCount) {
        final FieldIntrinsic[] fieldIntrinsics = new FieldIntrinsic[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            fieldIntrinsics[i] = intrinsic(values[i * 2]);
        }
        return fieldIntrinsics;
    }

    private static FieldIntrinsic intrinsic(Object type) {
        if (type instanceof NetworkBufferTypeImpl.UnitType) return new FieldIntrinsic(0, "writeUnit", "readUnit");
        if (type instanceof NetworkBufferTypeImpl.BooleanType) return new FieldIntrinsic(1, "writeBoolean", "readBoolean");
        if (type instanceof NetworkBufferTypeImpl.ByteType) return new FieldIntrinsic(1, "writeByte", "readByte");
        if (type instanceof NetworkBufferTypeImpl.UnsignedByteType) return new FieldIntrinsic(1, "writeUnsignedByte", "readUnsignedByte");
        if (type instanceof NetworkBufferTypeImpl.ShortType) return new FieldIntrinsic(2, "writeShort", "readShort");
        if (type instanceof NetworkBufferTypeImpl.UnsignedShortType) return new FieldIntrinsic(2, "writeUnsignedShort", "readUnsignedShort");
        if (type instanceof NetworkBufferTypeImpl.IntType) return new FieldIntrinsic(4, "writeInt", "readInt");
        if (type instanceof NetworkBufferTypeImpl.UnsignedIntType) return new FieldIntrinsic(4, "writeUnsignedInt", "readUnsignedInt");
        if (type instanceof NetworkBufferTypeImpl.LongType) return new FieldIntrinsic(8, "writeLong", "readLong");
        if (type instanceof NetworkBufferTypeImpl.FloatType) return new FieldIntrinsic(4, "writeFloat", "readFloat");
        if (type instanceof NetworkBufferTypeImpl.DoubleType) return new FieldIntrinsic(8, "writeDouble", "readDouble");
        return null;
    }

    private record FieldIntrinsic(int size, String writeMethod, String readMethod) {}

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
