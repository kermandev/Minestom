package net.minestom.server.network;

import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.lang.classfile.ClassBuilder;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

interface NetworkBufferTemplateImpl<T extends @UnknownNullability Object> extends NetworkBufferTypeImpl<T> {
    static final String PACKAGE = "net.minestom.server.network";
    static final ClassDesc CD_OBJECT = ConstantDescs.CD_Object;
    static final ClassDesc CD_STRING = ConstantDescs.CD_String;
    static final ClassDesc CD_CLASS = ConstantDescs.CD_Class;
    static final ClassDesc CD_INT = ConstantDescs.CD_int;
    static final ClassDesc CD_LONG = ConstantDescs.CD_long;
    static final ClassDesc CD_FLOAT = ConstantDescs.CD_float;
    static final ClassDesc CD_DOUBLE = ConstantDescs.CD_double;
    static final ClassDesc CD_SHORT = ConstantDescs.CD_short;
    static final ClassDesc CD_BYTE = ConstantDescs.CD_byte;
    static final ClassDesc CD_VOID = ConstantDescs.CD_void;
    static final ClassDesc CD_BOOLEAN_WRAPPER = ConstantDescs.CD_Boolean;
    static final ClassDesc CD_BYTE_WRAPPER = ConstantDescs.CD_Byte;
    static final ClassDesc CD_SHORT_WRAPPER = ConstantDescs.CD_Short;
    static final ClassDesc CD_INTEGER_WRAPPER = ConstantDescs.CD_Integer;
    static final ClassDesc CD_LONG_WRAPPER = ConstantDescs.CD_Long;
    static final ClassDesc CD_FLOAT_WRAPPER = ConstantDescs.CD_Float;
    static final ClassDesc CD_DOUBLE_WRAPPER = ConstantDescs.CD_Double;
    static final ClassDesc CD_METHOD_HANDLES = ConstantDescs.CD_MethodHandles;
    static final ClassDesc CD_METHOD_HANDLES_LOOKUP = ConstantDescs.CD_MethodHandles_Lookup;
    static final ClassDesc CD_NETWORK_BUFFER = NetworkBuffer.class.describeConstable().orElseThrow();
    static final ClassDesc CD_NETWORK_BUFFER_IMPL = NetworkBufferImpl.class.describeConstable().orElseThrow();
    static final ClassDesc CD_TYPE = NetworkBuffer.Type.class.describeConstable().orElseThrow();
    static final ClassDesc CD_OPTIONAL_TYPE = NetworkBufferTypeImpl.OptionalType.class.describeConstable().orElseThrow();
    static final ClassDesc CD_TRANSFORM_TYPE = NetworkBufferTypeImpl.TransformType.class.describeConstable().orElseThrow();
    static final ClassDesc CD_TEMPLATE_IMPL = NetworkBufferTemplateImpl.class.describeConstable().orElseThrow();
    static final ClassDesc CD_TEMPLATE_METADATA = TemplateMetadata.class.describeConstable().orElseThrow();
    static final ClassDesc CD_FUNCTION = Function.class.describeConstable().orElseThrow();
    static final ClassDesc CD_UNIT = net.minestom.server.utils.Unit.class.describeConstable().orElseThrow();

    static final MethodTypeDesc MT_VOID = MethodTypeDesc.of(CD_VOID);
    static final MethodTypeDesc MT_LOOKUP = MethodTypeDesc.of(CD_METHOD_HANDLES_LOOKUP);
    static final MethodTypeDesc MT_CLASS_DATA_AT = MethodTypeDesc.of(CD_OBJECT, CD_METHOD_HANDLES_LOOKUP, CD_STRING, CD_CLASS, CD_INT);
    static final MethodTypeDesc MT_READ_OBJECT = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER);
    static final MethodTypeDesc MT_WRITE_OBJECT = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_OBJECT);
    static final MethodTypeDesc MT_PARENT = MethodTypeDesc.of(CD_TYPE);
    static final MethodTypeDesc MT_TEMPLATE_METADATA = MethodTypeDesc.of(CD_TEMPLATE_METADATA);
    static final MethodTypeDesc MT_INDEX_GETTER = MethodTypeDesc.of(CD_LONG);
    static final MethodTypeDesc MT_INDEX_SETTER = MethodTypeDesc.of(CD_NETWORK_BUFFER, CD_LONG);
    static final MethodTypeDesc MT_ENSURE = MethodTypeDesc.of(CD_VOID, CD_LONG);
    static final MethodTypeDesc MT_FUNCTION_APPLY = MethodTypeDesc.of(CD_OBJECT, CD_OBJECT);
    static final MethodTypeDesc MT_FUNCTION = MethodTypeDesc.of(CD_FUNCTION);
    static final MethodTypeDesc MT_BOOLEAN_VALUE = MethodTypeDesc.of(ConstantDescs.CD_boolean);
    static final MethodTypeDesc MT_BYTE_VALUE = MethodTypeDesc.of(CD_BYTE);
    static final MethodTypeDesc MT_SHORT_VALUE = MethodTypeDesc.of(CD_SHORT);
    static final MethodTypeDesc MT_INT_VALUE = MethodTypeDesc.of(CD_INT);
    static final MethodTypeDesc MT_LONG_VALUE = MethodTypeDesc.of(CD_LONG);
    static final MethodTypeDesc MT_FLOAT_VALUE = MethodTypeDesc.of(CD_FLOAT);
    static final MethodTypeDesc MT_DOUBLE_VALUE = MethodTypeDesc.of(CD_DOUBLE);
    static final MethodTypeDesc MT_BOX_BOOLEAN = MethodTypeDesc.of(CD_BOOLEAN_WRAPPER, ConstantDescs.CD_boolean);
    static final MethodTypeDesc MT_BOX_BYTE = MethodTypeDesc.of(CD_BYTE_WRAPPER, CD_BYTE);
    static final MethodTypeDesc MT_BOX_SHORT = MethodTypeDesc.of(CD_SHORT_WRAPPER, CD_SHORT);
    static final MethodTypeDesc MT_BOX_INT = MethodTypeDesc.of(CD_INTEGER_WRAPPER, CD_INT);
    static final MethodTypeDesc MT_BOX_LONG = MethodTypeDesc.of(CD_LONG_WRAPPER, CD_LONG);
    static final MethodTypeDesc MT_BOX_FLOAT = MethodTypeDesc.of(CD_FLOAT_WRAPPER, CD_FLOAT);
    static final MethodTypeDesc MT_BOX_DOUBLE = MethodTypeDesc.of(CD_DOUBLE_WRAPPER, CD_DOUBLE);
    static final MethodTypeDesc MT_PUT_BYTE = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_BYTE);
    static final MethodTypeDesc MT_GET_BYTE = MethodTypeDesc.of(CD_BYTE, CD_LONG);
    static final MethodTypeDesc MT_PUT_SHORT = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_SHORT);
    static final MethodTypeDesc MT_GET_SHORT = MethodTypeDesc.of(CD_SHORT, CD_LONG);
    static final MethodTypeDesc MT_PUT_INT = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_INT);
    static final MethodTypeDesc MT_GET_INT = MethodTypeDesc.of(CD_INT, CD_LONG);
    static final MethodTypeDesc MT_PUT_LONG = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_LONG);
    static final MethodTypeDesc MT_GET_LONG = MethodTypeDesc.of(CD_LONG, CD_LONG);
    static final MethodTypeDesc MT_PUT_FLOAT = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_FLOAT);
    static final MethodTypeDesc MT_GET_FLOAT = MethodTypeDesc.of(CD_FLOAT, CD_LONG);
    static final MethodTypeDesc MT_PUT_DOUBLE = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_DOUBLE);
    static final MethodTypeDesc MT_GET_DOUBLE = MethodTypeDesc.of(CD_DOUBLE, CD_LONG);

    static final int FIELD_FLAGS = ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    static final int METHOD_FLAGS = ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    static final int CLASS_FLAGS = ClassFile.ACC_FINAL | ClassFile.ACC_SUPER | ClassFile.ACC_SYNTHETIC;

    static final String CTOR_NAME = "ctor";
    static final String TYPE_PREFIX = "t";
    static final String GETTER_PREFIX = "g";
    static final String TRANSFORM_TO_PREFIX = "to";
    static final String TRANSFORM_FROM_PREFIX = "from";
    static final String TEMPLATE_METADATA_NAME = "metadata";
    static final String READ = "read";
    static final String WRITE = "write";

    static final boolean DEBUG = true;
    static final Path DUMP_ROOT = Path.of("generated", "network-templates");
    static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    TemplateMetadata metadata();

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
            final List<Object> classData = new ArrayList<>();
            final int metadataIndex = addClassData(classData, new TemplateMetadata(values.clone(), fieldCount));
            final TemplatePlan plan = templatePlan("", classData, values, fieldCount);
            final byte[] bytes = ClassFile.of().build(classDesc, classBuilder -> {
                classBuilder.withFlags(CLASS_FLAGS)
                        .withSuperclass(CD_OBJECT)
                        .withInterfaceSymbols(CD_TEMPLATE_IMPL);

                classBuilder.withField(TEMPLATE_METADATA_NAME, CD_TEMPLATE_METADATA, FIELD_FLAGS);
                declareTemplateFields(classBuilder, plan);
                final ClassDesc ctor = constructorInterface(fieldCount);

                classBuilder.withMethodBody(ConstantDescs.CLASS_INIT_NAME, MT_VOID, ClassFile.ACC_STATIC | ClassFile.ACC_SYNTHETIC,
                        codeBuilder -> buildClassInitializer(codeBuilder, classDesc, plan, ctor, metadataIndex));
                classBuilder.withMethodBody(ConstantDescs.INIT_NAME, MT_VOID, ClassFile.ACC_PRIVATE | ClassFile.ACC_SYNTHETIC,
                        codeBuilder -> codeBuilder.aload(0).invokespecial(CD_OBJECT, ConstantDescs.INIT_NAME, MT_VOID).return_());
                classBuilder.withMethodBody(TEMPLATE_METADATA_NAME, MT_TEMPLATE_METADATA, METHOD_FLAGS,
                        codeBuilder -> codeBuilder.getstatic(classDesc, TEMPLATE_METADATA_NAME, CD_TEMPLATE_METADATA).areturn());
                classBuilder.withMethodBody(WRITE, MT_WRITE_OBJECT, METHOD_FLAGS,
                        codeBuilder -> buildWrite(codeBuilder, classDesc, plan, 2));
                classBuilder.withMethodBody(READ, MT_READ_OBJECT, METHOD_FLAGS,
                        codeBuilder -> buildRead(codeBuilder, classDesc, plan, ctor));
            });
            if (DEBUG) dump(bytes, fieldCount);
            final MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, List.copyOf(classData), true, MethodHandles.Lookup.ClassOption.NESTMATE);
            final MethodHandle constructor = lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class));
            return (NetworkBuffer.Type<T>) constructor.invoke();
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to generate network type template", throwable);
        }
    }

    private static void dump(byte[] bytes, int fieldCount) throws IOException {
        final StackWalker.StackFrame caller = STACK_WALKER.walk(frames -> frames
                .filter(frame -> {
                    final Class<?> declaringClass = frame.getDeclaringClass();
                    return declaringClass != NetworkBufferTemplateImpl.class && declaringClass != NetworkBufferTemplate.class;
                })
                .findFirst()
                .orElseThrow());
        final Path directory = DUMP_ROOT
                .resolve(sanitize(caller.getClassName()))
                .resolve(sanitize(caller.getMethodName()))
                .resolve("line%s-bci%s".formatted(caller.getLineNumber(), caller.getByteCodeIndex()));
        Files.createDirectories(directory);
        Files.write(directory.resolve("NetworkTemplate-%s-F%s.class".formatted(sanitize(caller.getDeclaringClass().getSimpleName()), fieldCount)), bytes);
    }

    private static String sanitize(String value) {
        final StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            builder.append(Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_' ? c : '_');
        }
        return builder.toString();
    }

    private static void declareTemplateFields(ClassBuilder classBuilder, TemplatePlan plan) {
        for (FieldPlan field : plan.fields()) {
            final Intrinsic intrinsic = field.intrinsic();
            if (intrinsic == null) {
                classBuilder.withField(typeName(field.path()), CD_TYPE, FIELD_FLAGS);
            } else {
                declareIntrinsicFields(classBuilder, field);
            }
            classBuilder.withField(getterName(field.path()), CD_FUNCTION, FIELD_FLAGS);
        }
        classBuilder.withField(ctorName(plan.path()), constructorInterface(plan.fieldCount()), FIELD_FLAGS);
    }

    private static void declareIntrinsicFields(ClassBuilder classBuilder, FieldPlan field) {
        final Intrinsic intrinsic = field.intrinsic();
        assert intrinsic != null;
        if (intrinsic.transformed()) {
            for (int level = 0; level < intrinsic.transformDepth(); level++) {
                classBuilder.withField(transformToName(field.path(), level), CD_FUNCTION, FIELD_FLAGS);
                classBuilder.withField(transformFromName(field.path(), level), CD_FUNCTION, FIELD_FLAGS);
            }
        }
        declareIntrinsicNodeFields(classBuilder, intrinsic);
    }

    private static void declareIntrinsicNodeFields(ClassBuilder classBuilder, Intrinsic intrinsic) {
        switch (intrinsic) {
            case TransformIntrinsic transform -> declareIntrinsicNodeFields(classBuilder, transform.parent());
            case OptionalIntrinsic optional -> declareIntrinsicNodeFields(classBuilder, optional.parent());
            case TemplateIntrinsic template -> declareTemplateFields(classBuilder, template.plan());
            case PrimitiveIntrinsic _ -> {
            }
            default -> throw new UnsupportedOperationException("Unsupported intrinsic: " + intrinsic);
        }
    }

    private static void buildClassInitializer(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, ClassDesc ctor, int metadataIndex) {
        codeBuilder.invokestatic(CD_METHOD_HANDLES, "lookup", MT_LOOKUP)
                .astore(0);
        loadClassDataAt(codeBuilder, CD_TEMPLATE_METADATA, metadataIndex)
                .putstatic(classDesc, TEMPLATE_METADATA_NAME, CD_TEMPLATE_METADATA);
        initTemplateFields(codeBuilder, classDesc, plan);
        codeBuilder.return_();
    }

    private static void initTemplateFields(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan) {
        for (FieldPlan field : plan.fields()) {
            final Intrinsic intrinsic = field.intrinsic();
            if (intrinsic == null) {
                loadClassDataAt(codeBuilder, CD_TYPE, field.typeDataIndex())
                        .putstatic(classDesc, typeName(field.path()), CD_TYPE);
            } else {
                initIntrinsicFields(codeBuilder, classDesc, field);
            }
            loadClassDataAt(codeBuilder, CD_FUNCTION, field.getterDataIndex())
                    .putstatic(classDesc, getterName(field.path()), CD_FUNCTION);
        }
        loadClassDataAt(codeBuilder, constructorInterface(plan.fieldCount()), plan.ctorDataIndex())
                .putstatic(classDesc, ctorName(plan.path()), constructorInterface(plan.fieldCount()));
    }

    private static void initIntrinsicFields(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field) {
        final Intrinsic intrinsic = field.intrinsic();
        assert intrinsic != null;
        if (intrinsic.transformed()) {
            final boolean optional = intrinsic instanceof OptionalIntrinsic;
            for (int level = 0; level < intrinsic.transformDepth(); level++) {
                loadTransformFunction(codeBuilder, field, optional, level, false)
                        .putstatic(classDesc, transformFromName(field.path(), level), CD_FUNCTION);
                loadTransformFunction(codeBuilder, field, optional, level, true)
                        .putstatic(classDesc, transformToName(field.path(), level), CD_FUNCTION);
            }
        }
        initIntrinsicNodeFields(codeBuilder, classDesc, intrinsic);
    }

    private static void initIntrinsicNodeFields(CodeBuilder codeBuilder, ClassDesc classDesc, Intrinsic intrinsic) {
        switch (intrinsic) {
            case TransformIntrinsic transform -> initIntrinsicNodeFields(codeBuilder, classDesc, transform.parent());
            case OptionalIntrinsic optional -> initIntrinsicNodeFields(codeBuilder, classDesc, optional.parent());
            case TemplateIntrinsic template -> initTemplateFields(codeBuilder, classDesc, template.plan());
            case PrimitiveIntrinsic _ -> {
            }
            default -> throw new UnsupportedOperationException("Unsupported intrinsic: " + intrinsic);
        }
    }

    private static void buildWrite(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, int objectSlot) {
        emitTemplateWriteBody(codeBuilder, classDesc, plan, objectSlot);
        codeBuilder.return_();
    }

    private static void emitTemplateWriteBody(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, int objectSlot) {
        final FieldPlan[] fields = plan.fields();
        for (int i = 0; i < fields.length; ) {
            if (isFixedIntrinsic(fields[i].intrinsic())) {
                final int start = i;
                long size = 0;
                do {
                    size += fields[i++].intrinsic().size();
                } while (i < fields.length && isFixedIntrinsic(fields[i].intrinsic()));
                if (size > 0) emitEnsureWritable(codeBuilder, size);
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.aload(1)
                        .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_GETTER)
                        .lstore(indexSlot);
                emitDirectBuffer(codeBuilder, directSlot);
                long offset = 0;
                for (int j = start; j < i; j++) {
                    final Intrinsic intrinsic = fields[j].intrinsic();
                    assert intrinsic != null;
                    emitIntrinsicWrite(codeBuilder, classDesc, fields[j], intrinsic, objectSlot, offset, directSlot, indexSlot);
                    offset += intrinsic.size();
                }
                codeBuilder.aload(1)
                        .lload(indexSlot)
                        .loadConstant(size)
                        .ladd()
                        .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_SETTER)
                        .pop();
            } else if (isFixedOptionalIntrinsic(fields[i].intrinsic())) {
                final int start = i;
                long baseSize = 0;
                do {
                    baseSize++;
                    i++;
                } while (i < fields.length && isFixedOptionalIntrinsic(fields[i].intrinsic()));
                emitOptionalWriteRun(codeBuilder, classDesc, fields, start, i, baseSize, objectSlot);
            } else if (fields[i].intrinsic() != null) {
                emitVariableIntrinsicWrite(codeBuilder, classDesc, fields[i++], objectSlot);
            } else {
                emitWrite(codeBuilder, classDesc, fields[i++], objectSlot);
            }
        }
    }

    private static void buildRead(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, ClassDesc ctor) {
        emitTemplateRead(codeBuilder, classDesc, plan, ctor);
        codeBuilder.areturn();
    }

    private static void emitTemplateRead(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, ClassDesc ctor) {
        final FieldPlan[] fields = plan.fields();
        final int[] valueSlots = new int[fields.length];
        final Intrinsic[] valueIntrinsics = new Intrinsic[fields.length];

        for (int i = 0; i < fields.length; ) {
            if (isFixedIntrinsic(fields[i].intrinsic())) {
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.aload(1)
                        .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_GETTER)
                        .lstore(indexSlot);
                emitDirectBuffer(codeBuilder, directSlot);
                // TODO: call NetworkBuffer.ensureReadable(totalSize) here once it exists.
                long size = 0;
                do {
                    final Intrinsic intrinsic = fields[i].intrinsic();
                    emitIntrinsicRead(codeBuilder, classDesc, fields[i], intrinsic, size, directSlot, indexSlot);
                    valueIntrinsics[i] = intrinsic;
                    valueSlots[i] = codeBuilder.allocateLocal(intrinsic.localKind());
                    codeBuilder.storeLocal(intrinsic.localKind(), valueSlots[i]);
                    size += intrinsic.size();
                    i++;
                } while (i < fields.length && isFixedIntrinsic(fields[i].intrinsic()));
                codeBuilder.aload(1)
                        .lload(indexSlot)
                        .loadConstant(size)
                        .ladd()
                        .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_SETTER)
                        .pop();
            } else if (isFixedOptionalIntrinsic(fields[i].intrinsic())) {
                final int start = i;
                do {
                    i++;
                } while (i < fields.length && isFixedOptionalIntrinsic(fields[i].intrinsic()));
                emitOptionalReadRun(codeBuilder, classDesc, fields, start, i, valueSlots);
            } else if (fields[i].intrinsic() != null) {
                emitVariableIntrinsicRead(codeBuilder, classDesc, fields[i], valueSlots);
                i++;
            } else {
                codeBuilder.getstatic(classDesc, typeName(fields[i].path()), CD_TYPE)
                        .aload(1)
                        .invokeinterface(CD_TYPE, READ, MT_READ_OBJECT)
                        .astore(valueSlots[i] = codeBuilder.allocateLocal(TypeKind.REFERENCE));
                i++;
            }
        }
        codeBuilder.getstatic(classDesc, ctorName(plan.path()), ctor);
        for (int i = 0; i < fields.length; i++) {
            emitConstructorValue(codeBuilder, classDesc, fields[i], valueIntrinsics[i], valueSlots[i]);
        }
        codeBuilder.invokeinterface(ctor, "apply", constructorApplyType(fields.length));
    }

    private static void emitEnsureWritable(CodeBuilder codeBuilder, long size) {
        codeBuilder.aload(1)
                .loadConstant(size)
                .invokeinterface(CD_NETWORK_BUFFER, "ensureWritable", MT_ENSURE);
    }

    private static void emitWrite(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, int objectSlot) {
        codeBuilder.getstatic(classDesc, typeName(field.path()), CD_TYPE)
                .aload(1)
                .getstatic(classDesc, getterName(field.path()), CD_FUNCTION)
                .aload(objectSlot)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY)
                .invokeinterface(CD_TYPE, WRITE, MT_WRITE_OBJECT);
    }

    private static void emitDirectBuffer(CodeBuilder codeBuilder, int directSlot) {
        codeBuilder.aload(1)
                .checkcast(CD_NETWORK_BUFFER_IMPL)
                .astore(directSlot);
    }

    private static boolean isFixedIntrinsic(@Nullable Intrinsic intrinsic) {
        return intrinsic != null && intrinsic.fixedSize();
    }

    private static boolean isFixedOptionalIntrinsic(@Nullable Intrinsic intrinsic) {
        return intrinsic instanceof OptionalIntrinsic optional && optional.parent().fixedSize();
    }

    private static void emitIntrinsicWrite(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, Intrinsic intrinsic,
                                           int objectSlot, long offset, int directSlot, int indexSlot) {
        final int valueSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.getstatic(classDesc, getterName(field.path()), CD_FUNCTION)
                .aload(objectSlot)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
        if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, field, intrinsic, false);
        codeBuilder.astore(valueSlot);
        emitIntrinsicWriteValue(codeBuilder, classDesc, field, intrinsic, valueSlot, offset, directSlot, indexSlot);
    }

    private static void emitIntrinsicWriteValue(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, Intrinsic intrinsic,
                                                int valueSlot, long offset, int directSlot, int indexSlot) {
        switch (intrinsic) {
            case TransformIntrinsic transform -> emitIntrinsicWriteValue(codeBuilder, classDesc, field, transform.parent(), valueSlot, offset, directSlot, indexSlot);
            case OptionalIntrinsic _ -> throw new UnsupportedOperationException("Optional values are not fixed-size");
            case TemplateIntrinsic template -> emitTemplateFixedWrite(codeBuilder, classDesc, template.plan(), valueSlot, offset, directSlot, indexSlot);
            case PrimitiveIntrinsic primitive -> {
                if (primitive == PrimitiveIntrinsic.UNIT) {
                    codeBuilder.aload(valueSlot).pop();
                    return;
                }
                codeBuilder.aload(directSlot);
                emitOffsetIndex(codeBuilder, offset, indexSlot);
                codeBuilder.aload(valueSlot);
                emitIntrinsicWriteValue(codeBuilder, primitive);
            }
            default -> throw new UnsupportedOperationException("Unsupported intrinsic: " + intrinsic);
        }
    }

    private static void emitTemplateFixedWrite(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, int objectSlot,
                                               long offset, int directSlot, int indexSlot) {
        long currentOffset = offset;
        for (FieldPlan field : plan.fields()) {
            final Intrinsic intrinsic = field.intrinsic();
            assert intrinsic != null && intrinsic.fixedSize();
            emitIntrinsicWrite(codeBuilder, classDesc, field, intrinsic, objectSlot, currentOffset, directSlot, indexSlot);
            currentOffset += intrinsic.size();
        }
    }

    private static void emitIntrinsicRead(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, Intrinsic intrinsic,
                                          long offset, int directSlot, int indexSlot) {
        switch (intrinsic) {
            case TransformIntrinsic transform -> emitIntrinsicRead(codeBuilder, classDesc, field, transform.parent(), offset, directSlot, indexSlot);
            case OptionalIntrinsic _ -> throw new UnsupportedOperationException("Optional values are not fixed-size");
            case TemplateIntrinsic template -> emitTemplateFixedRead(codeBuilder, classDesc, template.plan(), offset, directSlot, indexSlot);
            case PrimitiveIntrinsic primitive -> {
                codeBuilder.aload(directSlot);
                emitOffsetIndex(codeBuilder, offset, indexSlot);
                emitIntrinsicReadValue(codeBuilder, primitive);
            }
            default -> throw new UnsupportedOperationException("Unsupported intrinsic: " + intrinsic);
        }
    }

    private static void emitTemplateFixedRead(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan,
                                              long offset, int directSlot, int indexSlot) {
        final FieldPlan[] fields = plan.fields();
        final int[] valueSlots = new int[fields.length];
        long currentOffset = offset;
        for (int i = 0; i < fields.length; i++) {
            final Intrinsic intrinsic = fields[i].intrinsic();
            assert intrinsic != null && intrinsic.fixedSize();
            emitIntrinsicRead(codeBuilder, classDesc, fields[i], intrinsic, currentOffset, directSlot, indexSlot);
            valueSlots[i] = codeBuilder.allocateLocal(intrinsic.localKind());
            codeBuilder.storeLocal(intrinsic.localKind(), valueSlots[i]);
            currentOffset += intrinsic.size();
        }
        codeBuilder.getstatic(classDesc, ctorName(plan.path()), constructorInterface(fields.length));
        for (int i = 0; i < fields.length; i++) {
            emitConstructorValue(codeBuilder, classDesc, fields[i], fields[i].intrinsic(), valueSlots[i]);
        }
        codeBuilder.invokeinterface(constructorInterface(fields.length), "apply", constructorApplyType(fields.length));
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

    private static void emitConstructorValue(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, @Nullable Intrinsic intrinsic, int valueSlot) {
        if (intrinsic == null) {
            codeBuilder.aload(valueSlot);
            return;
        }
        final PrimitiveIntrinsic primitive = intrinsic.primitive();
        if (primitive == null) {
            codeBuilder.aload(valueSlot);
            if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, field, intrinsic, true);
            return;
        }
        codeBuilder.loadLocal(intrinsic.localKind(), valueSlot);
        emitBoxedIntrinsicValue(codeBuilder, primitive);
        if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, field, intrinsic, true);
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

    private static void emitVariableIntrinsicWrite(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, int objectSlot) {
        final Intrinsic intrinsic = field.intrinsic();
        assert intrinsic != null;
        if (intrinsic instanceof OptionalIntrinsic optional) {
            emitVariableOptionalWrite(codeBuilder, classDesc, field, optional, objectSlot);
            return;
        }
        final int valueSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.getstatic(classDesc, getterName(field.path()), CD_FUNCTION)
                .aload(objectSlot)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
        if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, field, intrinsic, false);
        codeBuilder.astore(valueSlot);
        emitIntrinsicBodyWrite(codeBuilder, classDesc, field, intrinsic, valueSlot);
    }

    private static void emitVariableIntrinsicRead(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, int[] valueSlots) {
        final Intrinsic intrinsic = field.intrinsic();
        assert intrinsic != null;
        if (intrinsic instanceof OptionalIntrinsic optional) {
            emitVariableOptionalRead(codeBuilder, classDesc, field, optional, valueSlots);
            return;
        }
        final int valueSlot = valueSlots[field.index()] = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        emitIntrinsicBodyRead(codeBuilder, classDesc, field, intrinsic);
        if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, field, intrinsic, true);
        codeBuilder.astore(valueSlot);
    }

    private static void emitIntrinsicBodyWrite(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, Intrinsic intrinsic, int valueSlot) {
        switch (intrinsic) {
            case TransformIntrinsic transform -> emitIntrinsicBodyWrite(codeBuilder, classDesc, field, transform.parent(), valueSlot);
            case TemplateIntrinsic template -> emitTemplateWriteBody(codeBuilder, classDesc, template.plan(), valueSlot);
            case PrimitiveIntrinsic primitive -> {
                if (primitive == PrimitiveIntrinsic.UNIT) {
                    codeBuilder.aload(valueSlot).pop();
                    return;
                }
                emitEnsureWritable(codeBuilder, primitive.size());
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.aload(1)
                        .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_GETTER)
                        .lstore(indexSlot);
                emitDirectBuffer(codeBuilder, directSlot);
                emitIntrinsicWriteValue(codeBuilder, classDesc, field, primitive, valueSlot, 0, directSlot, indexSlot);
                codeBuilder.aload(1)
                        .lload(indexSlot)
                        .loadConstant((long) primitive.size())
                        .ladd()
                        .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_SETTER)
                        .pop();
            }
            default -> throw new UnsupportedOperationException("Unsupported intrinsic body: " + intrinsic);
        }
    }

    private static void emitIntrinsicBodyRead(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, Intrinsic intrinsic) {
        switch (intrinsic) {
            case TransformIntrinsic transform -> emitIntrinsicBodyRead(codeBuilder, classDesc, field, transform.parent());
            case TemplateIntrinsic template -> emitTemplateRead(codeBuilder, classDesc, template.plan(), constructorInterface(template.plan().fieldCount()));
            case PrimitiveIntrinsic primitive -> {
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.aload(1)
                        .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_GETTER)
                        .lstore(indexSlot);
                emitDirectBuffer(codeBuilder, directSlot);
                emitIntrinsicRead(codeBuilder, classDesc, field, primitive, 0, directSlot, indexSlot);
                emitBoxedIntrinsicValue(codeBuilder, primitive);
                codeBuilder.aload(1)
                        .lload(indexSlot)
                        .loadConstant((long) primitive.size())
                        .ladd()
                        .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_SETTER)
                        .pop();
            }
            default -> throw new UnsupportedOperationException("Unsupported intrinsic body: " + intrinsic);
        }
    }

    private static void emitVariableOptionalWrite(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, OptionalIntrinsic optional, int objectSlot) {
        final Intrinsic intrinsic = optional.parent();
        final int valueSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final var notNull = codeBuilder.newLabel();
        final var end = codeBuilder.newLabel();

        emitEnsureWritable(codeBuilder, 1);
        codeBuilder.aload(1)
                .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_GETTER)
                .lstore(indexSlot);
        emitDirectBuffer(codeBuilder, directSlot);
        codeBuilder.getstatic(classDesc, getterName(field.path()), CD_FUNCTION)
                .aload(objectSlot)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY)
                .astore(valueSlot)
                .aload(valueSlot)
                .ifnonnull(notNull);
        codeBuilder.aload(directSlot)
                .lload(indexSlot)
                .iconst_0()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByte", MT_PUT_BYTE)
                .aload(1)
                .lload(indexSlot)
                .lconst_1()
                .ladd()
                .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_SETTER)
                .pop()
                .goto_(end)
                .labelBinding(notNull);
        codeBuilder.aload(directSlot)
                .lload(indexSlot)
                .iconst_1()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByte", MT_PUT_BYTE)
                .aload(1)
                .lload(indexSlot)
                .lconst_1()
                .ladd()
                .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_SETTER)
                .pop();
        if (intrinsic.transformed()) {
            codeBuilder.aload(valueSlot);
            emitTransformApply(codeBuilder, classDesc, field, intrinsic, false);
            codeBuilder.astore(valueSlot);
        }
        emitIntrinsicBodyWrite(codeBuilder, classDesc, field, intrinsic, valueSlot);
        codeBuilder.labelBinding(end);
    }

    private static void emitVariableOptionalRead(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, OptionalIntrinsic optional, int[] valueSlots) {
        final Intrinsic intrinsic = optional.parent();
        final int valueSlot = valueSlots[field.index()] = codeBuilder.allocateLocal(TypeKind.REFERENCE);
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
        codeBuilder.aload(1)
                .lload(indexSlot)
                .lconst_1()
                .ladd()
                .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_SETTER)
                .pop();
        emitIntrinsicBodyRead(codeBuilder, classDesc, field, intrinsic);
        if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, field, intrinsic, true);
        codeBuilder.astore(valueSlot)
                .labelBinding(end);
    }

    private static void emitOptionalWriteRun(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan[] fields,
                                             int start, int end, long baseSize, int objectSlot) {
        emitEnsureWritable(codeBuilder, baseSize);
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.aload(1)
                .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_GETTER)
                .lstore(indexSlot);
        emitDirectBuffer(codeBuilder, directSlot);
        for (int i = start; i < end; i++) {
            emitOptionalWrite(codeBuilder, classDesc, fields[i], (OptionalIntrinsic) fields[i].intrinsic(), objectSlot, directSlot, indexSlot);
        }
        codeBuilder.aload(1)
                .lload(indexSlot)
                .invokeinterface(CD_NETWORK_BUFFER, "writeIndex", MT_INDEX_SETTER)
                .pop();
    }

    private static void emitOptionalWrite(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, OptionalIntrinsic optional,
                                          int objectSlot, int directSlot, int indexSlot) {
        final Intrinsic intrinsic = optional.parent();
        final int valueSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final var notNull = codeBuilder.newLabel();
        final var advance = codeBuilder.newLabel();

        codeBuilder.getstatic(classDesc, getterName(field.path()), CD_FUNCTION)
                .aload(objectSlot)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY)
                .astore(valueSlot)
                .aload(valueSlot)
                .ifnonnull(notNull);
        codeBuilder.aload(directSlot)
                .lload(indexSlot)
                .iconst_0()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByte", MT_PUT_BYTE)
                .goto_(advance)
                .labelBinding(notNull);
        codeBuilder.aload(directSlot)
                .lload(indexSlot)
                .iconst_1()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByte", MT_PUT_BYTE);
        emitEnsureWritable(codeBuilder, 1L + intrinsic.size());
        if (intrinsic.transformed()) {
            codeBuilder.aload(valueSlot);
            emitTransformApply(codeBuilder, classDesc, field, intrinsic, false);
            codeBuilder.astore(valueSlot);
        }
        emitIntrinsicWriteValue(codeBuilder, classDesc, field, intrinsic, valueSlot, 1, directSlot, indexSlot);
        codeBuilder.lload(indexSlot)
                .loadConstant((long) intrinsic.size())
                .ladd()
                .lstore(indexSlot)
                .labelBinding(advance)
                .lload(indexSlot)
                .lconst_1()
                .ladd()
                .lstore(indexSlot);
    }

    private static void emitOptionalReadRun(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan[] fields,
                                            int start, int end, int[] valueSlots) {
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.aload(1)
                .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_GETTER)
                .lstore(indexSlot);
        emitDirectBuffer(codeBuilder, directSlot);
        // TODO: call NetworkBuffer.ensureReadable(totalSize) here once it exists.
        for (int i = start; i < end; i++) {
            emitOptionalRead(codeBuilder, classDesc, fields[i], (OptionalIntrinsic) fields[i].intrinsic(), directSlot, indexSlot, valueSlots);
        }
        codeBuilder.aload(1)
                .lload(indexSlot)
                .invokeinterface(CD_NETWORK_BUFFER, "readIndex", MT_INDEX_SETTER)
                .pop();
    }

    private static void emitOptionalRead(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, OptionalIntrinsic optional,
                                         int directSlot, int indexSlot, int[] valueSlots) {
        final Intrinsic intrinsic = optional.parent();
        final int valueSlot = valueSlots[field.index()] = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final var present = codeBuilder.newLabel();
        final var advance = codeBuilder.newLabel();

        codeBuilder.aload(directSlot)
                .lload(indexSlot)
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByte", MT_GET_BYTE)
                .iconst_1()
                .if_icmpeq(present)
                .aconst_null()
                .astore(valueSlot)
                .goto_(advance)
                .labelBinding(present);
        emitIntrinsicRead(codeBuilder, classDesc, field, intrinsic, 1, directSlot, indexSlot);
        final PrimitiveIntrinsic primitive = intrinsic.primitive();
        if (primitive != null) emitBoxedIntrinsicValue(codeBuilder, primitive);
        if (intrinsic.transformed()) emitTransformApply(codeBuilder, classDesc, field, intrinsic, true);
        codeBuilder.astore(valueSlot)
                .lload(indexSlot)
                .loadConstant((long) intrinsic.size())
                .ladd()
                .lstore(indexSlot)
                .labelBinding(advance)
                .lload(indexSlot)
                .lconst_1()
                .ladd()
                .lstore(indexSlot);
    }

    private static void emitTransformApply(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, Intrinsic intrinsic, boolean to) {
        if (to) {
            for (int level = intrinsic.transformDepth() - 1; level >= 0; level--) {
                emitTransformFunctionApply(codeBuilder, classDesc, transformToName(field.path(), level));
            }
        } else {
            for (int level = 0; level < intrinsic.transformDepth(); level++) {
                emitTransformFunctionApply(codeBuilder, classDesc, transformFromName(field.path(), level));
            }
        }
    }

    private static void emitTransformFunctionApply(CodeBuilder codeBuilder, ClassDesc classDesc, String functionName) {
        codeBuilder.getstatic(classDesc, functionName, CD_FUNCTION)
                .swap()
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
    }

    private static TemplatePlan templatePlan(String path, List<Object> classData, Object[] values, int fieldCount) {
        final FieldPlan[] fields = new FieldPlan[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            final String fieldPath = childPath(path, i);
            final Object type = values[i * 2];
            fields[i] = new FieldPlan(i, fieldPath,
                    addClassData(classData, type),
                    addClassData(classData, values[i * 2 + 1]),
                    intrinsic(fieldPath, classData, type));
        }
        return new TemplatePlan(path, fieldCount, fields, addClassData(classData, values[fieldCount * 2]));
    }

    @Nullable
    private static Intrinsic intrinsic(String path, List<Object> classData, Object type) {
        return switch (type) {
            case NetworkBufferTypeImpl.OptionalType<?>(NetworkBuffer.Type<?> parent1) -> {
                final Intrinsic parent = intrinsic(path, classData, parent1);
                yield parent != null ? new OptionalIntrinsic(parent) : null;
            }
            case NetworkBufferTypeImpl.TransformType<?, ?> transform -> {
                final Intrinsic parent = intrinsic(path, classData, transform.parent());
                yield parent != null && !(parent instanceof OptionalIntrinsic) ? new TransformIntrinsic(parent) : null;
            }
            case NetworkBufferTemplateImpl<?> template -> {
                final TemplateMetadata metadata = template.metadata();
                yield new TemplateIntrinsic(templatePlan(path, classData, metadata.values(), metadata.fieldCount()));
            }
            case NetworkBufferTypeImpl.UnitType _ -> PrimitiveIntrinsic.UNIT;
            case NetworkBufferTypeImpl.BooleanType _ -> PrimitiveIntrinsic.BOOLEAN;
            case NetworkBufferTypeImpl.ByteType _ -> PrimitiveIntrinsic.BYTE;
            case NetworkBufferTypeImpl.UnsignedByteType _ -> PrimitiveIntrinsic.UNSIGNED_BYTE;
            case NetworkBufferTypeImpl.ShortType _ -> PrimitiveIntrinsic.SHORT;
            case NetworkBufferTypeImpl.UnsignedShortType _ -> PrimitiveIntrinsic.UNSIGNED_SHORT;
            case NetworkBufferTypeImpl.IntType _ -> PrimitiveIntrinsic.INT;
            case NetworkBufferTypeImpl.UnsignedIntType _ -> PrimitiveIntrinsic.UNSIGNED_INT;
            case NetworkBufferTypeImpl.LongType _ -> PrimitiveIntrinsic.LONG;
            case NetworkBufferTypeImpl.FloatType _ -> PrimitiveIntrinsic.FLOAT;
            case NetworkBufferTypeImpl.DoubleType _ -> PrimitiveIntrinsic.DOUBLE;
            default -> null;
        };
    }

    private static int addClassData(List<Object> classData, Object value) {
        final int index = classData.size();
        classData.add(value);
        return index;
    }

    record TemplateMetadata(Object[] values, int fieldCount) {
    }

    record TemplatePlan(String path, int fieldCount, FieldPlan[] fields, int ctorDataIndex) {
    }

    record FieldPlan(int index, String path, int typeDataIndex, int getterDataIndex, @Nullable Intrinsic intrinsic) {
    }

    interface Intrinsic {
        int size();

        TypeKind localKind();

        @Nullable
        PrimitiveIntrinsic primitive();

        default boolean fixedSize() {
            return size() >= 0;
        }

        default boolean transformed() {
            return false;
        }

        default int transformDepth() {
            return 0;
        }
    }

    record TransformIntrinsic(Intrinsic parent) implements Intrinsic {
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

    record OptionalIntrinsic(Intrinsic parent) implements Intrinsic {
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

    record TemplateIntrinsic(TemplatePlan plan) implements Intrinsic {
        @Override
        public int size() {
            long size = 0;
            for (FieldPlan field : plan.fields()) {
                final Intrinsic intrinsic = field.intrinsic();
                if (!isFixedIntrinsic(intrinsic)) return -1;
                size += intrinsic.size();
            }
            return Math.toIntExact(size);
        }

        @Override
        public TypeKind localKind() {
            return TypeKind.REFERENCE;
        }

        @Override
        public @Nullable PrimitiveIntrinsic primitive() {
            return null;
        }
    }

    enum PrimitiveIntrinsic implements Intrinsic {
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

    private static CodeBuilder loadTransformFunction(CodeBuilder codeBuilder, FieldPlan field, boolean optional, int level, boolean to) {
        loadClassDataAt(codeBuilder, CD_TYPE, field.typeDataIndex());
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

    private static String childPath(String parent, int index) {
        final String value = Integer.toString(index + 1);
        return parent.isEmpty() ? value : parent + "_" + value;
    }

    private static String ctorName(String path) {
        return path.isEmpty() ? CTOR_NAME : CTOR_NAME + path;
    }

    private static String typeName(int index) {
        return typeName(Integer.toString(index + 1));
    }

    private static String getterName(int index) {
        return getterName(Integer.toString(index + 1));
    }

    private static String transformToName(int index, int level) {
        return transformToName(Integer.toString(index + 1), level);
    }

    private static String transformFromName(int index, int level) {
        return transformFromName(Integer.toString(index + 1), level);
    }

    private static String typeName(String path) {
        return TYPE_PREFIX + path;
    }

    private static String getterName(String path) {
        return GETTER_PREFIX + path;
    }

    private static String transformToName(String path, int level) {
        return TRANSFORM_TO_PREFIX + path + "_" + (level + 1);
    }

    private static String transformFromName(String path, int level) {
        return TRANSFORM_FROM_PREFIX + path + "_" + (level + 1);
    }
}
