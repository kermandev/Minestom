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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

interface NetworkBufferTemplateImpl<T extends @UnknownNullability Object> extends NetworkBufferTypeImpl<T>, NetworkIrBacked<T> {
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
    static final ClassDesc CD_BYTE_ARRAY = ClassDesc.ofDescriptor("[B");
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
    static final ClassDesc CD_NETWORK_BUFFER_TYPE_IMPL = NetworkBufferTypeImpl.class.describeConstable().orElseThrow();
    static final ClassDesc CD_TYPE = NetworkBuffer.Type.class.describeConstable().orElseThrow();
    static final ClassDesc CD_CONSTRUCTOR_IR = ConstructorIr.class.describeConstable().orElseThrow();
    static final ClassDesc CD_OPTIONAL_TYPE = NetworkBufferTypeImpl.OptionalType.class.describeConstable().orElseThrow();
    static final ClassDesc CD_TRANSFORM_TYPE = NetworkBufferTypeImpl.TransformType.class.describeConstable().orElseThrow();
    static final ClassDesc CD_TEMPLATE_IMPL = NetworkBufferTemplateImpl.class.describeConstable().orElseThrow();
    static final ClassDesc CD_NETWORK_IR = NetworkIr.class.describeConstable().orElseThrow();
    static final ClassDesc CD_FUNCTION = Function.class.describeConstable().orElseThrow();
    static final ClassDesc CD_LIST = List.class.describeConstable().orElseThrow();
    static final ClassDesc CD_OBJECT_ARRAY = ClassDesc.ofDescriptor("[Ljava/lang/Object;");
    static final ClassDesc CD_UNIT = net.minestom.server.utils.Unit.class.describeConstable().orElseThrow();

    static final MethodTypeDesc MT_VOID = MethodTypeDesc.of(CD_VOID);
    static final MethodTypeDesc MT_LOOKUP = MethodTypeDesc.of(CD_METHOD_HANDLES_LOOKUP);
    static final MethodTypeDesc MT_CLASS_DATA_AT = MethodTypeDesc.of(CD_OBJECT, CD_METHOD_HANDLES_LOOKUP, CD_STRING, CD_CLASS, CD_INT);
    static final MethodTypeDesc MT_READ_OBJECT = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER);
    static final MethodTypeDesc MT_WRITE_OBJECT = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_OBJECT);
    static final MethodTypeDesc MT_PARENT = MethodTypeDesc.of(CD_TYPE);
    static final MethodTypeDesc MT_NETWORK_IR = MethodTypeDesc.of(CD_NETWORK_IR);
    static final MethodTypeDesc MT_CONSTRUCTOR_CONSTRUCT = MethodTypeDesc.of(CD_OBJECT, CD_LIST);
    static final MethodTypeDesc MT_LIST_OF_ARRAY = MethodTypeDesc.of(CD_LIST, CD_OBJECT_ARRAY);
    static final MethodTypeDesc MT_RESERVE = MethodTypeDesc.of(CD_LONG, CD_LONG);
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
    static final MethodTypeDesc MT_PUT_BYTES = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_BYTE_ARRAY, CD_INT, CD_INT);
    static final MethodTypeDesc MT_GET_BYTES = MethodTypeDesc.of(CD_VOID, CD_LONG, CD_BYTE_ARRAY, CD_INT, CD_INT);
    static final MethodTypeDesc MT_VAR_INT_SIZE = MethodTypeDesc.of(CD_INT, CD_INT);
    static final MethodTypeDesc MT_VAR_LONG_SIZE = MethodTypeDesc.of(CD_INT, CD_LONG);
    static final MethodTypeDesc MT_WRITE_VAR_INT_UNCHECKED = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER_IMPL, CD_LONG, CD_INT);
    static final MethodTypeDesc MT_WRITE_VAR_LONG_UNCHECKED = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER_IMPL, CD_LONG, CD_LONG);
    static final MethodTypeDesc MT_READ_VAR_INT = MethodTypeDesc.of(CD_INT, CD_NETWORK_BUFFER);
    static final MethodTypeDesc MT_READ_VAR_LONG = MethodTypeDesc.of(CD_LONG, CD_NETWORK_BUFFER);
    static final MethodTypeDesc MT_WRITE_FIXED_BYTES = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_BYTE_ARRAY, CD_INT);
    static final MethodTypeDesc MT_CHECK_FIXED_BYTES_LENGTH = MethodTypeDesc.of(CD_BYTE_ARRAY, CD_BYTE_ARRAY, CD_INT);
    static final MethodTypeDesc MT_READ_FIXED_BYTES = MethodTypeDesc.of(CD_BYTE_ARRAY, CD_NETWORK_BUFFER, CD_INT);
    static final MethodTypeDesc MT_WRITE_BYTE_ARRAY = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_BYTE_ARRAY, CD_INT);
    static final MethodTypeDesc MT_READ_BYTE_ARRAY = MethodTypeDesc.of(CD_BYTE_ARRAY, CD_NETWORK_BUFFER, CD_INT);
    static final MethodTypeDesc MT_WRITE_STRING_UTF8 = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_STRING, CD_INT);
    static final MethodTypeDesc MT_READ_STRING_UTF8 = MethodTypeDesc.of(CD_STRING, CD_NETWORK_BUFFER, CD_INT);

    static final int FIELD_FLAGS = ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    static final int METHOD_FLAGS = ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    static final int CLASS_FLAGS = ClassFile.ACC_FINAL | ClassFile.ACC_SUPER | ClassFile.ACC_SYNTHETIC;

    static final String CTOR_NAME = "ctor";
    static final String TYPE_PREFIX = "t";
    static final String GETTER_PREFIX = "g";
    static final String TRANSFORM_TO_PREFIX = "to";
    static final String TRANSFORM_FROM_PREFIX = "from";
    static final String IR_FIELD_NAME = "networkIr";
    static final String IR = "ir";
    static final String READ = "read";
    static final String WRITE = "write";

    static final boolean DEBUG = true;
    static final Path DUMP_ROOT = Path.of("generated", "network-templates");
    static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

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
            final ConstructorIr<T> constructorIr = constructorIr(values[fieldCount * 2], fieldCount);
            final TemplatePlan plan = templatePlan("", classData, values, fieldCount, constructorIr);
            final NetworkIr<T> ir = networkIr("NetworkTemplate", values, fieldCount, plan, constructorIr);
            final int irIndex = addClassData(classData, ir);
            final byte[] bytes = ClassFile.of().build(classDesc, classBuilder -> {
                classBuilder.withFlags(CLASS_FLAGS)
                        .withSuperclass(CD_OBJECT)
                        .withInterfaceSymbols(CD_TEMPLATE_IMPL);

                classBuilder.withField(IR_FIELD_NAME, CD_NETWORK_IR, FIELD_FLAGS);
                declareTemplateFields(classBuilder, plan);

                classBuilder.withMethodBody(ConstantDescs.CLASS_INIT_NAME, MT_VOID, ClassFile.ACC_STATIC | ClassFile.ACC_SYNTHETIC,
                        codeBuilder -> buildClassInitializer(codeBuilder, classDesc, plan, irIndex));
                classBuilder.withMethodBody(ConstantDescs.INIT_NAME, MT_VOID, ClassFile.ACC_PRIVATE | ClassFile.ACC_SYNTHETIC,
                        codeBuilder -> codeBuilder.aload(0).invokespecial(CD_OBJECT, ConstantDescs.INIT_NAME, MT_VOID).return_());
                classBuilder.withMethodBody(IR, MT_NETWORK_IR, METHOD_FLAGS,
                        codeBuilder -> codeBuilder.getstatic(classDesc, IR_FIELD_NAME, CD_NETWORK_IR).areturn());
                classBuilder.withMethodBody(WRITE, MT_WRITE_OBJECT, METHOD_FLAGS,
                        codeBuilder -> buildWrite(codeBuilder, classDesc, plan, 2));
                classBuilder.withMethodBody(READ, MT_READ_OBJECT, METHOD_FLAGS,
                        codeBuilder -> buildRead(codeBuilder, classDesc, plan));
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
            final TypeIr<?> shape = field.type();
            if (shape == null) {
                classBuilder.withField(typeName(field.path()), CD_TYPE, FIELD_FLAGS);
            } else {
                declareTypeIrFields(classBuilder, field);
            }
            classBuilder.withField(getterName(field.path()), CD_FUNCTION, FIELD_FLAGS);
        }
        classBuilder.withField(ctorName(plan.path()), CD_CONSTRUCTOR_IR, FIELD_FLAGS);
    }

    private static void declareTypeIrFields(ClassBuilder classBuilder, FieldPlan field) {
        final TypeIr<?> shape = field.type();
        assert shape != null;
        if (transformed(shape)) {
            for (int level = 0; level < transformDepth(shape); level++) {
                classBuilder.withField(transformToName(field.path(), level), CD_FUNCTION, FIELD_FLAGS);
                classBuilder.withField(transformFromName(field.path(), level), CD_FUNCTION, FIELD_FLAGS);
            }
        }
        if (field.nestedPlan() != null) declareTemplateFields(classBuilder, field.nestedPlan());
        declareTypeIrNodeFields(classBuilder, shape);
    }

    private static void declareTypeIrNodeFields(ClassBuilder classBuilder, TypeIr<?> shape) {
        switch (shape) {
            case TypeIr.Transform transform -> declareTypeIrNodeFields(classBuilder, transform.parent());
            case TypeIr.Optional optional -> declareTypeIrNodeFields(classBuilder, optional.parent());
            case TypeIr.Template _, TypeIr.Primitive _, TypeIr.VarInt _, TypeIr.VarLong _, TypeIr.FixedBytes _,
                 TypeIr.ByteArray _, TypeIr.StringUtf8 _, TypeIr.Constant _ -> {
            }
            default -> throw new UnsupportedOperationException("Unsupported shape: " + shape);
        }
    }

    private static void buildClassInitializer(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, int irIndex) {
        codeBuilder.invokestatic(CD_METHOD_HANDLES, "lookup", MT_LOOKUP)
                .astore(0);
        loadClassDataAt(codeBuilder, CD_NETWORK_IR, irIndex)
                .putstatic(classDesc, IR_FIELD_NAME, CD_NETWORK_IR);
        initTemplateFields(codeBuilder, classDesc, plan);
        codeBuilder.return_();
    }

    private static void initTemplateFields(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan) {
        for (FieldPlan field : plan.fields()) {
            final TypeIr<?> shape = field.type();
            if (shape == null) {
                loadClassDataAt(codeBuilder, CD_TYPE, field.typeDataIndex())
                        .putstatic(classDesc, typeName(field.path()), CD_TYPE);
            } else {
                initTypeIrFields(codeBuilder, classDesc, field);
            }
            loadClassDataAt(codeBuilder, CD_FUNCTION, field.getterDataIndex())
                    .putstatic(classDesc, getterName(field.path()), CD_FUNCTION);
        }
        loadClassDataAt(codeBuilder, CD_CONSTRUCTOR_IR, plan.ctorDataIndex())
                .putstatic(classDesc, ctorName(plan.path()), CD_CONSTRUCTOR_IR);
    }

    private static void initTypeIrFields(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field) {
        final TypeIr<?> shape = field.type();
        assert shape != null;
        if (transformed(shape)) {
            final boolean optional = shape instanceof TypeIr.Optional;
            for (int level = 0; level < transformDepth(shape); level++) {
                loadTransformFunction(codeBuilder, field, optional, level, false)
                        .putstatic(classDesc, transformFromName(field.path(), level), CD_FUNCTION);
                loadTransformFunction(codeBuilder, field, optional, level, true)
                        .putstatic(classDesc, transformToName(field.path(), level), CD_FUNCTION);
            }
        }
        if (field.nestedPlan() != null) initTemplateFields(codeBuilder, classDesc, field.nestedPlan());
        initTypeIrNodeFields(codeBuilder, classDesc, shape);
    }

    private static void initTypeIrNodeFields(CodeBuilder codeBuilder, ClassDesc classDesc, TypeIr<?> shape) {
        switch (shape) {
            case TypeIr.Transform transform -> initTypeIrNodeFields(codeBuilder, classDesc, transform.parent());
            case TypeIr.Optional optional -> initTypeIrNodeFields(codeBuilder, classDesc, optional.parent());
            case TypeIr.Template _, TypeIr.Primitive _, TypeIr.VarInt _, TypeIr.VarLong _, TypeIr.FixedBytes _,
                 TypeIr.ByteArray _, TypeIr.StringUtf8 _, TypeIr.Constant _ -> {
            }
            default -> throw new UnsupportedOperationException("Unsupported shape: " + shape);
        }
    }

    private static void buildWrite(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, int objectSlot) {
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        emitDirectBuffer(codeBuilder, directSlot);
        emitTemplateWriteBody(codeBuilder, classDesc, plan, objectSlot, directSlot);
        codeBuilder.return_();
    }

    private static void emitTemplateWriteBody(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, int objectSlot, int directSlot) {
        final FieldPlan[] fields = plan.fields();
        for (int i = 0; i < fields.length; ) {
            if (isFixedTypeIr(fields[i].type())) {
                final int start = i;
                long size = 0;
                do {
                    size += fixedSize(fields[i++].type());
                } while (i < fields.length && isFixedTypeIr(fields[i].type()));
                final List<WriteRunValue> runValues = new ArrayList<>();
                long offset = 0;
                for (int j = start; j < i; j++) {
                    final TypeIr<?> shape = fields[j].type();
                    assert shape != null;
                    offset = emitTypeIrWritePrecompute(codeBuilder, classDesc, fields[j], shape, objectSlot, offset, runValues);
                }
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                codeBuilder.aload(directSlot)
                        .loadConstant(size)
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                        .lstore(indexSlot);
                for (WriteRunValue runValue : runValues) {
                    if (runValue.type() != null) {
                        emitTypeIrWriteLocal(codeBuilder, runValue.type(), runValue.valueSlot(), runValue.byteOffset(), directSlot, indexSlot);
                    } else {
                        emitFixedBytesWriteLocal(codeBuilder, runValue.valueSlot(), runValue.byteOffset(), runValue.byteLength(), directSlot, indexSlot);
                    }
                }
            } else if (isFixedOptional(fields[i].type())) {
                final int start = i;
                do {
                    i++;
                } while (i < fields.length && isFixedOptional(fields[i].type()));
                emitOptionalWriteRun(codeBuilder, classDesc, fields, start, i, objectSlot, directSlot);
            } else if (fields[i].type() != null) {
                emitVariableTypeIrWrite(codeBuilder, classDesc, fields[i++], objectSlot, directSlot);
            } else {
                emitWrite(codeBuilder, classDesc, fields[i++], objectSlot);
            }
        }
    }

    private static void buildRead(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan) {
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        emitDirectBuffer(codeBuilder, directSlot);
        emitTemplateRead(codeBuilder, classDesc, plan, directSlot);
        codeBuilder.areturn();
    }

    private static void emitTemplateRead(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, int directSlot) {
        final FieldPlan[] fields = plan.fields();
        final int[] valueSlots = new int[fields.length];
        final TypeIr<?>[] valueTypes = new TypeIr<?>[fields.length];

        for (int i = 0; i < fields.length; ) {
            if (isFixedTypeIr(fields[i].type())) {
                final int start = i;
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                long size = 0;
                do {
                    size += fixedSize(fields[i++].type());
                } while (i < fields.length && isFixedTypeIr(fields[i].type()));
                final int end = i;
                codeBuilder.aload(directSlot)
                        .loadConstant(size)
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveRead", MT_RESERVE)
                        .lstore(indexSlot);
                long offset = 0;
                for (i = start; i < end; i++) {
                    final TypeIr<?> shape = fields[i].type();
                    emitTypeIrRead(codeBuilder, classDesc, fields[i], shape, offset, directSlot, indexSlot);
                    valueTypes[i] = shape;
                    valueSlots[i] = codeBuilder.allocateLocal(localKind(shape));
                    codeBuilder.storeLocal(localKind(shape), valueSlots[i]);
                    offset += fixedSize(shape);
                }
            } else if (isFixedOptional(fields[i].type())) {
                final int start = i;
                do {
                    i++;
                } while (i < fields.length && isFixedOptional(fields[i].type()));
                emitOptionalReadRun(codeBuilder, classDesc, fields, start, i, valueSlots, directSlot);
            } else if (fields[i].type() != null) {
                emitVariableTypeIrRead(codeBuilder, classDesc, fields[i], valueSlots, directSlot);
                i++;
            } else {
                codeBuilder.getstatic(classDesc, typeName(fields[i].path()), CD_TYPE)
                        .aload(1)
                        .invokeinterface(CD_TYPE, READ, MT_READ_OBJECT)
                        .astore(valueSlots[i] = codeBuilder.allocateLocal(TypeKind.REFERENCE));
                i++;
            }
        }
        codeBuilder.getstatic(classDesc, ctorName(plan.path()), CD_CONSTRUCTOR_IR);
        codeBuilder.loadConstant(fields.length)
                .anewarray(CD_OBJECT);
        for (int i = 0; i < fields.length; i++) {
            codeBuilder.dup()
                    .loadConstant(i);
            emitConstructorValue(codeBuilder, classDesc, fields[i], valueTypes[i], valueSlots[i]);
            codeBuilder.aastore();
        }
        codeBuilder.invokestatic(CD_LIST, "of", MT_LIST_OF_ARRAY, true)
                .invokeinterface(CD_CONSTRUCTOR_IR, "construct", MT_CONSTRUCTOR_CONSTRUCT);
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

    private static boolean isFixedTypeIr(@Nullable TypeIr<?> shape) {
        return shape != null && fixedSize(shape) >= 0;
    }

    private static boolean isFixedOptional(@Nullable TypeIr<?> shape) {
        return shape instanceof TypeIr.Optional optional && fixedSize(optional.parent()) >= 0;
    }

    private static long fixedSize(@Nullable TypeIr<?> type) {
        if (type == null) return -1;
        return switch (type) {
            case TypeIr.External _, TypeIr.Optional _, TypeIr.VarInt _, TypeIr.VarLong _,
                 TypeIr.StringUtf8 _, TypeIr.ByteArray _, TypeIr.ListType _, TypeIr.MapType _ -> -1;
            case TypeIr.Constant _ -> 0;
            case TypeIr.FixedBytes fixedBytes -> fixedBytes.length();
            case TypeIr.Primitive primitive -> primitive.kind().storeKind().byteSize();
            case TypeIr.Transform transform -> fixedSize(transform.parent());
            case TypeIr.Template template -> {
                long size = 0;
                for (Object item : template.ir().fields()) {
                    final FieldIr<?, ?> field = (FieldIr<?, ?>) item;
                    final long fieldSize = fixedSize(field.type());
                    if (fieldSize < 0) yield -1;
                    size = Math.addExact(size, fieldSize);
                }
                yield size;
            }
        };
    }

    private static TypeKind localKind(TypeIr<?> type) {
        final PrimitiveKind primitive = primitive(type);
        return primitive != null ? primitive.localKind() : TypeKind.REFERENCE;
    }

    private static @Nullable PrimitiveKind primitive(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.Primitive primitive -> primitive.kind();
            case TypeIr.Transform transform -> primitive(transform.parent());
            default -> null;
        };
    }

    private static boolean transformed(TypeIr<?> type) {
        return transformDepth(type) > 0;
    }

    private static int transformDepth(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.Transform transform -> transformDepth(transform.parent()) + 1;
            case TypeIr.Optional optional -> transformDepth(optional.parent());
            default -> 0;
        };
    }

    private static void emitTypeIrWrite(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, TypeIr<?> shape,
                                           int objectSlot, long offset, int directSlot, int indexSlot) {
        if (shape == null) return;
        final int valueSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.getstatic(classDesc, getterName(field.path()), CD_FUNCTION)
                .aload(objectSlot)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
        if (transformed(shape)) emitTransformApply(codeBuilder, classDesc, field, shape, false);
        codeBuilder.astore(valueSlot);
        emitTypeIrWriteValue(codeBuilder, classDesc, field, shape, valueSlot, offset, directSlot, indexSlot);
    }

    private static long emitTypeIrWritePrecompute(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, TypeIr<?> shape,
                                                     int objectSlot, long offset, List<WriteRunValue> runValues) {
        if (shape == null) return offset;
        final int valueSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.getstatic(classDesc, getterName(field.path()), CD_FUNCTION)
                .aload(objectSlot)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
        if (transformed(shape)) emitTransformApply(codeBuilder, classDesc, field, shape, false);
        codeBuilder.astore(valueSlot);
        return emitTypeIrValuePrecompute(codeBuilder, classDesc, field, stripTransforms(shape), valueSlot, offset, runValues);
    }

    private static long emitTypeIrValuePrecompute(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, TypeIr<?> shape,
                                                     int valueSlot, long offset, List<WriteRunValue> runValues) {
        return switch (shape) {
            case TypeIr.Template _ -> {
                final TemplatePlan nestedPlan = Objects.requireNonNull(field.nestedPlan());
                long currentOffset = offset;
                for (FieldPlan child : nestedPlan.fields()) {
                    final TypeIr<?> childShape = child.type();
                    assert childShape != null && fixedSize(childShape) >= 0;
                    currentOffset = emitTypeIrWritePrecompute(codeBuilder, classDesc, child, childShape, valueSlot, currentOffset, runValues);
                }
                yield currentOffset;
            }
            case TypeIr.Optional _ -> throw new UnsupportedOperationException("Optional values are not fixed-size");
            case TypeIr.Primitive primitiveType -> {
                final PrimitiveKind primitive = primitiveType.kind();
                final int primitiveSlot = codeBuilder.allocateLocal(primitive.localKind());
                codeBuilder.aload(valueSlot);
                emitUnboxedTypeIrValue(codeBuilder, primitive);
                codeBuilder.storeLocal(primitive.localKind(), primitiveSlot);
                runValues.add(new WriteRunValue(primitive, primitiveSlot, offset, primitive.storeKind().byteSize()));
                yield offset + primitive.storeKind().byteSize();
            }
            case TypeIr.FixedBytes fixedBytes -> {
                final int bytesSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.aload(valueSlot)
                        .checkcast(CD_BYTE_ARRAY)
                        .loadConstant(fixedBytes.length())
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "checkFixedBytesLength", MT_CHECK_FIXED_BYTES_LENGTH, true)
                        .astore(bytesSlot);
                runValues.add(new WriteRunValue(null, bytesSlot, offset, fixedBytes.length()));
                yield offset + fixedBytes.length();
            }
            default -> throw new UnsupportedOperationException("Unsupported shape: " + shape);
        };
    }

    private static TypeIr<?> stripTransforms(TypeIr<?> shape) {
        while (shape instanceof TypeIr.Transform transform) {
            shape = transform.parent();
        }
        return shape;
    }

    private static void emitUnboxedTypeIrValue(CodeBuilder codeBuilder, PrimitiveKind kind) {
        switch (kind) {
            case BOOLEAN -> codeBuilder.checkcast(CD_BOOLEAN_WRAPPER)
                    .invokevirtual(CD_BOOLEAN_WRAPPER, "booleanValue", MT_BOOLEAN_VALUE);
            case BYTE -> codeBuilder.checkcast(CD_BYTE_WRAPPER)
                    .invokevirtual(CD_BYTE_WRAPPER, "byteValue", MT_BYTE_VALUE);
            case UNSIGNED_BYTE -> codeBuilder.checkcast(CD_SHORT_WRAPPER)
                    .invokevirtual(CD_SHORT_WRAPPER, "shortValue", MT_SHORT_VALUE)
                    .sipush(0xFF)
                    .iand();
            case SHORT -> codeBuilder.checkcast(CD_SHORT_WRAPPER)
                    .invokevirtual(CD_SHORT_WRAPPER, "shortValue", MT_SHORT_VALUE);
            case UNSIGNED_SHORT -> codeBuilder.checkcast(CD_INTEGER_WRAPPER)
                    .invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE)
                    .loadConstant(0xFFFF)
                    .iand();
            case INT -> codeBuilder.checkcast(CD_INTEGER_WRAPPER)
                    .invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE);
            case UNSIGNED_INT -> codeBuilder.checkcast(CD_LONG_WRAPPER)
                    .invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE)
                    .loadConstant(0xFFFFFFFFL)
                    .land();
            case LONG -> codeBuilder.checkcast(CD_LONG_WRAPPER)
                    .invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE);
            case FLOAT -> codeBuilder.checkcast(CD_FLOAT_WRAPPER)
                    .invokevirtual(CD_FLOAT_WRAPPER, "floatValue", MT_FLOAT_VALUE);
            case DOUBLE -> codeBuilder.checkcast(CD_DOUBLE_WRAPPER)
                    .invokevirtual(CD_DOUBLE_WRAPPER, "doubleValue", MT_DOUBLE_VALUE);
        }
    }

    private static void emitTypeIrWriteLocal(CodeBuilder codeBuilder, PrimitiveKind kind, int valueSlot,
                                                long offset, int directSlot, int indexSlot) {
        codeBuilder.aload(directSlot);
        emitOffsetIndex(codeBuilder, offset, indexSlot);
        codeBuilder.loadLocal(kind.localKind(), valueSlot);
        switch (kind) {
            case BOOLEAN, BYTE -> codeBuilder.i2b()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByteUnchecked", MT_PUT_BYTE);
            case UNSIGNED_BYTE -> codeBuilder.i2b()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByteUnchecked", MT_PUT_BYTE);
            case SHORT, UNSIGNED_SHORT -> codeBuilder.i2s()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putShortUnchecked", MT_PUT_SHORT);
            case INT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putIntUnchecked", MT_PUT_INT);
            case UNSIGNED_INT -> codeBuilder.l2i()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putIntUnchecked", MT_PUT_INT);
            case LONG -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putLongUnchecked", MT_PUT_LONG);
            case FLOAT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putFloatUnchecked", MT_PUT_FLOAT);
            case DOUBLE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putDoubleUnchecked", MT_PUT_DOUBLE);
        }
    }

    private static void emitFixedBytesWriteLocal(CodeBuilder codeBuilder, int valueSlot, long offset, int length,
                                                 int directSlot, int indexSlot) {
        if (length == 0) return;
        codeBuilder.aload(directSlot);
        emitOffsetIndex(codeBuilder, offset, indexSlot);
        codeBuilder.aload(valueSlot)
                .iconst_0()
                .loadConstant(length)
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putBytesUnchecked", MT_PUT_BYTES);
    }

    private static void emitTypeIrWriteValue(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, TypeIr<?> shape,
                                                int valueSlot, long offset, int directSlot, int indexSlot) {
        switch (shape) {
            case TypeIr.Transform transform -> emitTypeIrWriteValue(codeBuilder, classDesc, field, transform.parent(), valueSlot, offset, directSlot, indexSlot);
            case TypeIr.Optional _ -> throw new UnsupportedOperationException("Optional values are not fixed-size");
            case TypeIr.Template _ -> emitTemplateFixedWrite(codeBuilder, classDesc, Objects.requireNonNull(field.nestedPlan()), valueSlot, offset, directSlot, indexSlot);
            case TypeIr.Constant _ -> {
                codeBuilder.aload(valueSlot).pop();
                return;
            }
            case TypeIr.FixedBytes fixedBytes -> {
                final int bytesSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.aload(valueSlot)
                        .checkcast(CD_BYTE_ARRAY)
                        .loadConstant(fixedBytes.length())
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "checkFixedBytesLength", MT_CHECK_FIXED_BYTES_LENGTH, true)
                        .astore(bytesSlot);
                emitFixedBytesWriteLocal(codeBuilder, bytesSlot, offset, fixedBytes.length(), directSlot, indexSlot);
            }
            case TypeIr.Primitive primitiveType -> {
                final PrimitiveKind primitive = primitiveType.kind();
                codeBuilder.aload(directSlot);
                emitOffsetIndex(codeBuilder, offset, indexSlot);
                codeBuilder.aload(valueSlot);
                emitTypeIrWriteValue(codeBuilder, primitive);
            }
            default -> throw new UnsupportedOperationException("Unsupported shape: " + shape);
        }
    }

    private static void emitTemplateFixedWrite(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan, int objectSlot,
                                               long offset, int directSlot, int indexSlot) {
        long currentOffset = offset;
        for (FieldPlan field : plan.fields()) {
            final TypeIr<?> shape = field.type();
            assert shape != null && fixedSize(shape) >= 0;
            emitTypeIrWrite(codeBuilder, classDesc, field, shape, objectSlot, currentOffset, directSlot, indexSlot);
            currentOffset += fixedSize(shape);
        }
    }

    private static void emitTypeIrRead(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, TypeIr<?> shape,
                                          long offset, int directSlot, int indexSlot) {
        switch (shape) {
            case TypeIr.Transform transform -> emitTypeIrRead(codeBuilder, classDesc, field, transform.parent(), offset, directSlot, indexSlot);
            case TypeIr.Optional _ -> throw new UnsupportedOperationException("Optional values are not fixed-size");
            case TypeIr.Template _ -> emitTemplateFixedRead(codeBuilder, classDesc, Objects.requireNonNull(field.nestedPlan()), offset, directSlot, indexSlot);
            case TypeIr.Constant _ -> codeBuilder.getstatic(CD_UNIT, "INSTANCE", CD_UNIT);
            case TypeIr.FixedBytes fixedBytes -> emitFixedBytesReadValue(codeBuilder, fixedBytes.length(), offset, directSlot, indexSlot);
            case TypeIr.Primitive primitiveType -> {
                final PrimitiveKind primitive = primitiveType.kind();
                codeBuilder.aload(directSlot);
                emitOffsetIndex(codeBuilder, offset, indexSlot);
                emitTypeIrReadValue(codeBuilder, primitive);
            }
            default -> throw new UnsupportedOperationException("Unsupported shape: " + shape);
        }
    }

    private static void emitTemplateFixedRead(CodeBuilder codeBuilder, ClassDesc classDesc, TemplatePlan plan,
                                              long offset, int directSlot, int indexSlot) {
        final FieldPlan[] fields = plan.fields();
        final int[] valueSlots = new int[fields.length];
        long currentOffset = offset;
        for (int i = 0; i < fields.length; i++) {
            final TypeIr<?> shape = fields[i].type();
            assert shape != null && fixedSize(shape) >= 0;
            emitTypeIrRead(codeBuilder, classDesc, fields[i], shape, currentOffset, directSlot, indexSlot);
            valueSlots[i] = codeBuilder.allocateLocal(localKind(shape));
            codeBuilder.storeLocal(localKind(shape), valueSlots[i]);
            currentOffset += fixedSize(shape);
        }
        codeBuilder.getstatic(classDesc, ctorName(plan.path()), CD_CONSTRUCTOR_IR);
        codeBuilder.loadConstant(fields.length)
                .anewarray(CD_OBJECT);
        for (int i = 0; i < fields.length; i++) {
            codeBuilder.dup()
                    .loadConstant(i);
            emitConstructorValue(codeBuilder, classDesc, fields[i], fields[i].type(), valueSlots[i]);
            codeBuilder.aastore();
        }
        codeBuilder.invokestatic(CD_LIST, "of", MT_LIST_OF_ARRAY, true)
                .invokeinterface(CD_CONSTRUCTOR_IR, "construct", MT_CONSTRUCTOR_CONSTRUCT);
    }

    private static void emitFixedBytesReadValue(CodeBuilder codeBuilder, int length, long offset, int directSlot, int indexSlot) {
        final int bytesSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.loadConstant(length)
                .newarray(TypeKind.BYTE)
                .astore(bytesSlot);
        if (length > 0) {
            codeBuilder.aload(directSlot);
            emitOffsetIndex(codeBuilder, offset, indexSlot);
            codeBuilder.aload(bytesSlot)
                    .iconst_0()
                    .loadConstant(length)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getBytesUnchecked", MT_GET_BYTES);
        }
        codeBuilder.aload(bytesSlot);
    }

    private static void emitOffsetIndex(CodeBuilder codeBuilder, long offset, int indexSlot) {
        codeBuilder.lload(indexSlot);
        if (offset > 0) {
            codeBuilder.loadConstant(offset)
                    .ladd();
        }
    }

    private static void emitTypeIrWriteValue(CodeBuilder codeBuilder, PrimitiveKind kind) {
        switch (kind) {
            case BOOLEAN -> codeBuilder.checkcast(CD_BOOLEAN_WRAPPER)
                    .invokevirtual(CD_BOOLEAN_WRAPPER, "booleanValue", MT_BOOLEAN_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByteUnchecked", MT_PUT_BYTE);
            case BYTE -> codeBuilder.checkcast(CD_BYTE_WRAPPER)
                    .invokevirtual(CD_BYTE_WRAPPER, "byteValue", MT_BYTE_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByteUnchecked", MT_PUT_BYTE);
            case UNSIGNED_BYTE -> codeBuilder.checkcast(CD_SHORT_WRAPPER)
                    .invokevirtual(CD_SHORT_WRAPPER, "shortValue", MT_SHORT_VALUE)
                    .sipush(0xFF)
                    .iand()
                    .i2b()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByteUnchecked", MT_PUT_BYTE);
            case SHORT -> codeBuilder.checkcast(CD_SHORT_WRAPPER)
                    .invokevirtual(CD_SHORT_WRAPPER, "shortValue", MT_SHORT_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putShortUnchecked", MT_PUT_SHORT);
            case UNSIGNED_SHORT -> codeBuilder.checkcast(CD_INTEGER_WRAPPER)
                    .invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE)
                    .loadConstant(0xFFFF)
                    .iand()
                    .i2s()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putShortUnchecked", MT_PUT_SHORT);
            case INT -> codeBuilder.checkcast(CD_INTEGER_WRAPPER)
                    .invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putIntUnchecked", MT_PUT_INT);
            case UNSIGNED_INT -> codeBuilder.checkcast(CD_LONG_WRAPPER)
                    .invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE)
                    .loadConstant(0xFFFFFFFFL)
                    .land()
                    .l2i()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putIntUnchecked", MT_PUT_INT);
            case LONG -> codeBuilder.checkcast(CD_LONG_WRAPPER)
                    .invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putLongUnchecked", MT_PUT_LONG);
            case FLOAT -> codeBuilder.checkcast(CD_FLOAT_WRAPPER)
                    .invokevirtual(CD_FLOAT_WRAPPER, "floatValue", MT_FLOAT_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putFloatUnchecked", MT_PUT_FLOAT);
            case DOUBLE -> codeBuilder.checkcast(CD_DOUBLE_WRAPPER)
                    .invokevirtual(CD_DOUBLE_WRAPPER, "doubleValue", MT_DOUBLE_VALUE)
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putDoubleUnchecked", MT_PUT_DOUBLE);
        }
    }

    private static void emitTypeIrReadValue(CodeBuilder codeBuilder, PrimitiveKind kind) {
        switch (kind) {
            case BOOLEAN -> {
                final var falseLabel = codeBuilder.newLabel();
                final var endLabel = codeBuilder.newLabel();
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByteUnchecked", MT_GET_BYTE)
                        .iconst_0()
                        .if_icmpeq(falseLabel)
                        .iconst_1()
                        .goto_(endLabel)
                        .labelBinding(falseLabel)
                        .iconst_0()
                        .labelBinding(endLabel);
            }
            case BYTE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByteUnchecked", MT_GET_BYTE);
            case UNSIGNED_BYTE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByteUnchecked", MT_GET_BYTE)
                    .sipush(0xFF)
                    .iand();
            case SHORT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getShortUnchecked", MT_GET_SHORT);
            case UNSIGNED_SHORT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getShortUnchecked", MT_GET_SHORT)
                    .loadConstant(0xFFFF)
                    .iand();
            case INT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getIntUnchecked", MT_GET_INT);
            case UNSIGNED_INT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getIntUnchecked", MT_GET_INT)
                    .i2l()
                    .loadConstant(0xFFFFFFFFL)
                    .land();
            case LONG -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getLongUnchecked", MT_GET_LONG);
            case FLOAT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getFloatUnchecked", MT_GET_FLOAT);
            case DOUBLE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getDoubleUnchecked", MT_GET_DOUBLE);
        }
    }

    private static void emitConstructorValue(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, @Nullable TypeIr<?> shape, int valueSlot) {
        if (shape == null) {
            codeBuilder.aload(valueSlot);
            return;
        }
        final PrimitiveKind primitive = primitive(shape);
        if (primitive == null) {
            codeBuilder.aload(valueSlot);
            if (transformed(shape)) emitTransformApply(codeBuilder, classDesc, field, shape, true);
            return;
        }
        codeBuilder.loadLocal(localKind(shape), valueSlot);
        emitBoxedTypeIrValue(codeBuilder, primitive);
        if (transformed(shape)) emitTransformApply(codeBuilder, classDesc, field, shape, true);
    }

    private static void emitBoxedTypeIrValue(CodeBuilder codeBuilder, PrimitiveKind kind) {
        switch (kind) {
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

    private static void emitVariableTypeIrWrite(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, int objectSlot, int directSlot) {
        final TypeIr<?> shape = field.type();
        assert shape != null;
        if (shape instanceof TypeIr.Optional optional) {
            emitVariableOptionalWrite(codeBuilder, classDesc, field, optional, objectSlot, directSlot);
            return;
        }
        final int valueSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        codeBuilder.getstatic(classDesc, getterName(field.path()), CD_FUNCTION)
                .aload(objectSlot)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
        if (transformed(shape)) emitTransformApply(codeBuilder, classDesc, field, shape, false);
        codeBuilder.astore(valueSlot);
        emitTypeIrBodyWrite(codeBuilder, classDesc, field, shape, valueSlot, directSlot);
    }

    private static void emitVariableTypeIrRead(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, int[] valueSlots, int directSlot) {
        final TypeIr<?> shape = field.type();
        assert shape != null;
        if (shape instanceof TypeIr.Optional optional) {
            emitVariableOptionalRead(codeBuilder, classDesc, field, optional, valueSlots, directSlot);
            return;
        }
        final int valueSlot = valueSlots[field.index()] = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        emitTypeIrBodyRead(codeBuilder, classDesc, field, shape, directSlot);
        if (transformed(shape)) emitTransformApply(codeBuilder, classDesc, field, shape, true);
        codeBuilder.astore(valueSlot);
    }

    private static void emitTypeIrBodyWrite(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, TypeIr<?> shape,
                                               int valueSlot, int directSlot) {
        switch (shape) {
            case TypeIr.Transform transform -> emitTypeIrBodyWrite(codeBuilder, classDesc, field, transform.parent(), valueSlot, directSlot);
            case TypeIr.Template _ -> emitTemplateWriteBody(codeBuilder, classDesc, Objects.requireNonNull(field.nestedPlan()), valueSlot, directSlot);
            case TypeIr.Constant _ -> {
                codeBuilder.aload(valueSlot).pop();
                return;
            }
            case TypeIr.Primitive primitiveType -> {
                final PrimitiveKind primitive = primitiveType.kind();
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                codeBuilder.aload(directSlot)
                        .loadConstant((long) primitive.storeKind().byteSize())
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                        .lstore(indexSlot);
                emitTypeIrWriteValue(codeBuilder, classDesc, field, shape, valueSlot, 0, directSlot, indexSlot);
            }
            case TypeIr.VarInt _ -> emitVarIntWrite(codeBuilder, valueSlot, directSlot);
            case TypeIr.VarLong _ -> emitVarLongWrite(codeBuilder, valueSlot, directSlot);
            case TypeIr.FixedBytes fixedBytes -> codeBuilder.aload(directSlot)
                    .aload(valueSlot)
                    .checkcast(CD_BYTE_ARRAY)
                    .loadConstant(fixedBytes.length())
                    .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeFixedBytes", MT_WRITE_FIXED_BYTES, true);
            case TypeIr.ByteArray byteArray -> codeBuilder.aload(directSlot)
                    .aload(valueSlot)
                    .checkcast(CD_BYTE_ARRAY)
                    .loadConstant(byteArray.maxLength())
                    .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeByteArray", MT_WRITE_BYTE_ARRAY, true);
            case TypeIr.StringUtf8 string -> codeBuilder.aload(directSlot)
                    .aload(valueSlot)
                    .checkcast(CD_STRING)
                    .loadConstant(string.maxLength())
                    .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeStringUtf8", MT_WRITE_STRING_UTF8, true);
            default -> throw new UnsupportedOperationException("Unsupported shape body: " + shape);
        }
    }

    private static void emitTypeIrBodyRead(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, TypeIr<?> shape, int directSlot) {
        switch (shape) {
            case TypeIr.Transform transform -> emitTypeIrBodyRead(codeBuilder, classDesc, field, transform.parent(), directSlot);
            case TypeIr.Template _ -> emitTemplateRead(codeBuilder, classDesc, Objects.requireNonNull(field.nestedPlan()), directSlot);
            case TypeIr.Constant _ -> codeBuilder.getstatic(CD_UNIT, "INSTANCE", CD_UNIT);
            case TypeIr.Primitive primitiveType -> {
                final PrimitiveKind primitive = primitiveType.kind();
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                codeBuilder.aload(directSlot)
                        .loadConstant((long) primitive.storeKind().byteSize())
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveRead", MT_RESERVE)
                        .lstore(indexSlot);
                emitTypeIrRead(codeBuilder, classDesc, field, shape, 0, directSlot, indexSlot);
                emitBoxedTypeIrValue(codeBuilder, primitive);
            }
            case TypeIr.VarInt _ -> codeBuilder.aload(directSlot)
                    .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "readVarInt", MT_READ_VAR_INT, true)
                    .invokestatic(CD_INTEGER_WRAPPER, "valueOf", MT_BOX_INT);
            case TypeIr.VarLong _ -> codeBuilder.aload(directSlot)
                    .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "readVarLong", MT_READ_VAR_LONG, true)
                    .invokestatic(CD_LONG_WRAPPER, "valueOf", MT_BOX_LONG);
            case TypeIr.FixedBytes fixedBytes -> codeBuilder.aload(directSlot)
                    .loadConstant(fixedBytes.length())
                    .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "readFixedBytes", MT_READ_FIXED_BYTES, true);
            case TypeIr.ByteArray byteArray -> codeBuilder.aload(directSlot)
                    .loadConstant(byteArray.maxLength())
                    .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "readByteArray", MT_READ_BYTE_ARRAY, true);
            case TypeIr.StringUtf8 string -> codeBuilder.aload(directSlot)
                    .loadConstant(string.maxLength())
                    .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "readStringUtf8", MT_READ_STRING_UTF8, true);
            default -> throw new UnsupportedOperationException("Unsupported shape body: " + shape);
        }
    }

    private static void emitVarIntWrite(CodeBuilder codeBuilder, int valueSlot, int directSlot) {
        final int intSlot = codeBuilder.allocateLocal(TypeKind.INT);
        final int sizeSlot = codeBuilder.allocateLocal(TypeKind.INT);
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        codeBuilder.aload(valueSlot)
                .checkcast(CD_INTEGER_WRAPPER)
                .invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE)
                .istore(intSlot)
                .iload(intSlot)
                .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varIntSize", MT_VAR_INT_SIZE, true)
                .istore(sizeSlot);
        codeBuilder.aload(directSlot)
                .iload(sizeSlot)
                .i2l()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                .lstore(indexSlot)
                .aload(directSlot)
                .lload(indexSlot)
                .iload(intSlot)
                .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarIntUnchecked", MT_WRITE_VAR_INT_UNCHECKED, true);
    }

    private static void emitVarLongWrite(CodeBuilder codeBuilder, int valueSlot, int directSlot) {
        final int longSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        final int sizeSlot = codeBuilder.allocateLocal(TypeKind.INT);
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        codeBuilder.aload(valueSlot)
                .checkcast(CD_LONG_WRAPPER)
                .invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE)
                .lstore(longSlot)
                .lload(longSlot)
                .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varLongSize", MT_VAR_LONG_SIZE, true)
                .istore(sizeSlot);
        codeBuilder.aload(directSlot)
                .iload(sizeSlot)
                .i2l()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                .lstore(indexSlot)
                .aload(directSlot)
                .lload(indexSlot)
                .lload(longSlot)
                .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarLongUnchecked", MT_WRITE_VAR_LONG_UNCHECKED, true);
    }

    private static void emitVariableOptionalWrite(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, TypeIr.Optional optional,
                                                  int objectSlot, int directSlot) {
        final TypeIr<?> shape = optional.parent();
        final int valueSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        final var notNull = codeBuilder.newLabel();
        final var end = codeBuilder.newLabel();

        codeBuilder.getstatic(classDesc, getterName(field.path()), CD_FUNCTION)
                .aload(objectSlot)
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY)
                .astore(valueSlot)
                .aload(valueSlot)
                .ifnonnull(notNull);
        codeBuilder.aload(directSlot)
                .lconst_1()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                .lstore(indexSlot)
                .aload(directSlot)
                .lload(indexSlot)
                .iconst_0()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByteUnchecked", MT_PUT_BYTE)
                .goto_(end)
                .labelBinding(notNull);
        if (transformed(shape)) {
            codeBuilder.aload(valueSlot);
            emitTransformApply(codeBuilder, classDesc, field, shape, false);
            codeBuilder.astore(valueSlot);
        }
        if (fixedSize(shape) >= 0) {
            codeBuilder.aload(directSlot)
                    .loadConstant(1L + fixedSize(shape))
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                    .lstore(indexSlot)
                    .aload(directSlot)
                    .lload(indexSlot)
                    .iconst_1()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByteUnchecked", MT_PUT_BYTE);
            emitTypeIrWriteValue(codeBuilder, classDesc, field, shape, valueSlot, 1, directSlot, indexSlot);
        } else {
            codeBuilder.aload(directSlot)
                    .lconst_1()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                    .lstore(indexSlot)
                    .aload(directSlot)
                    .lload(indexSlot)
                    .iconst_1()
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByteUnchecked", MT_PUT_BYTE);
            emitTypeIrBodyWrite(codeBuilder, classDesc, field, shape, valueSlot, directSlot);
        }
        codeBuilder.labelBinding(end);
    }

    private static void emitVariableOptionalRead(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, TypeIr.Optional optional,
                                                 int[] valueSlots, int directSlot) {
        final TypeIr<?> shape = optional.parent();
        final int valueSlot = valueSlots[field.index()] = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        final var present = codeBuilder.newLabel();
        final var end = codeBuilder.newLabel();

        codeBuilder.aload(directSlot)
                .lconst_1()
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveRead", MT_RESERVE)
                .lstore(indexSlot)
                .aload(directSlot)
                .lload(indexSlot)
                .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByteUnchecked", MT_GET_BYTE)
                .iconst_1()
                .if_icmpeq(present)
                .aconst_null()
                .astore(valueSlot)
                .goto_(end)
                .labelBinding(present);
        if (fixedSize(shape) >= 0) {
            codeBuilder.aload(directSlot)
                    .loadConstant((long) fixedSize(shape))
                    .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveRead", MT_RESERVE)
                    .lstore(indexSlot);
            emitTypeIrRead(codeBuilder, classDesc, field, shape, 0, directSlot, indexSlot);
            final PrimitiveKind primitive = primitive(shape);
            if (primitive != null) emitBoxedTypeIrValue(codeBuilder, primitive);
        } else {
            emitTypeIrBodyRead(codeBuilder, classDesc, field, shape, directSlot);
        }
        if (transformed(shape)) emitTransformApply(codeBuilder, classDesc, field, shape, true);
        codeBuilder.astore(valueSlot)
                .labelBinding(end);
    }

    private static void emitOptionalWriteRun(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan[] fields,
                                             int start, int end, int objectSlot, int directSlot) {
        for (int i = start; i < end; i++) {
            emitVariableOptionalWrite(codeBuilder, classDesc, fields[i], (TypeIr.Optional) fields[i].type(), objectSlot, directSlot);
        }
    }

    private static void emitOptionalReadRun(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan[] fields,
                                            int start, int end, int[] valueSlots, int directSlot) {
        for (int i = start; i < end; i++) {
            emitVariableOptionalRead(codeBuilder, classDesc, fields[i], (TypeIr.Optional) fields[i].type(), valueSlots, directSlot);
        }
    }

    private static void emitTransformApply(CodeBuilder codeBuilder, ClassDesc classDesc, FieldPlan field, TypeIr<?> shape, boolean to) {
        if (to) {
            for (int level = transformDepth(shape) - 1; level >= 0; level--) {
                emitTransformFunctionApply(codeBuilder, classDesc, transformToName(field.path(), level));
            }
        } else {
            for (int level = 0; level < transformDepth(shape); level++) {
                emitTransformFunctionApply(codeBuilder, classDesc, transformFromName(field.path(), level));
            }
        }
    }

    private static void emitTransformFunctionApply(CodeBuilder codeBuilder, ClassDesc classDesc, String functionName) {
        codeBuilder.getstatic(classDesc, functionName, CD_FUNCTION)
                .swap()
                .invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
    }

    private static TemplatePlan templatePlan(String path, List<Object> classData, Object[] values, int fieldCount,
                                             ConstructorIr<?> constructor) {
        final FieldPlan[] fields = new FieldPlan[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            final String fieldPath = childPath(path, i);
            final Object type = values[i * 2];
            final TypeIr<?> optimizedType = optimizedType(fieldPath, classData, type);
            fields[i] = new FieldPlan(i, fieldPath,
                    addClassData(classData, type),
                    addClassData(classData, values[i * 2 + 1]),
                    optimizedType,
                    nestedTemplatePlan(fieldPath, classData, optimizedType));
        }
        return new TemplatePlan(path, fieldCount, fields, addClassData(classData, constructor));
    }

    private static TemplatePlan templatePlan(String path, List<Object> classData, NetworkIr<?> ir) {
        final List<? extends FieldIr<?, ?>> irFields = ir.fields();
        final FieldPlan[] fields = new FieldPlan[irFields.size()];
        for (int i = 0; i < irFields.size(); i++) {
            final String fieldPath = childPath(path, i);
            final FieldIr<?, ?> field = irFields.get(i);
            final TypeIr<?> optimizedType = optimizedType(fieldPath, classData, field.type());
            fields[i] = new FieldPlan(i, fieldPath,
                    addClassData(classData, field.originalType()),
                    addClassData(classData, field.getter()),
                    optimizedType,
                    nestedTemplatePlan(fieldPath, classData, optimizedType));
        }
        return new TemplatePlan(path, irFields.size(), fields, addClassData(classData, ir.constructor()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends @UnknownNullability Object> NetworkIr<T> networkIr(String name, Object[] values, int fieldCount,
                                                                                 TemplatePlan plan, ConstructorIr<T> constructor) {
        final List<FieldIr<T, ?>> fields = new ArrayList<>(fieldCount);
        final FieldIr<T, ?>[] fieldArray = new FieldIr[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            final NetworkBuffer.Type<?> originalType = (NetworkBuffer.Type<?>) values[i * 2];
            final Function<? super T, ?> getter = (Function<? super T, ?>) values[i * 2 + 1];
            final FieldIr<T, ?> field = new FieldIr(i, "field" + i, originalType, typeIr(originalType), getter);
            fields.add(field);
            fieldArray[i] = field;
        }
        final ProgramIr write = writeProgram(values, plan, fieldArray);
        final ProgramIr read = readProgram(values, plan, fieldArray, constructor);
        return new NetworkIr<>(name, fields, constructor, write, read);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ProgramIr writeProgram(Object[] values, TemplatePlan plan, FieldIr<?, ?>[] fields) {
        final List<Op> writeOps = new ArrayList<>(plan.fieldCount() * 2);
        final Local source = new Local("value", new LocalType.Reference(Object.class));
        final FieldPlan[] plans = plan.fields();
        for (int i = 0; i < plans.length; ) {
            final PrimitiveKind primitive = runPrimitive(fields[i].type());
            if (primitive != null) {
                final List<RunItem> items = new ArrayList<>();
                long offset = 0;
                do {
                    final PrimitiveKind kind = runPrimitive(fields[i].type());
                    final Local raw = new Local("field" + i, new LocalType.Reference(Object.class));
                    writeOps.add(new Op.GetField(fields[i], source, raw));
                    final Local normalized = normalizeWritePrimitive(writeOps, fields[i].type(), raw, i, 0);
                    items.add(new RunItem.Put(kind.storeKind(), new Value.Const(offset), new Value.LocalValue(normalized)));
                    offset += kind.storeKind().byteSize();
                    i++;
                } while (i < plans.length && runPrimitive(fields[i].type()) != null);
                writeOps.add(new Op.WriteRun(new RunIr(new Value.Const(offset), items)));
                continue;
            }
            if (fields[i].type() instanceof TypeIr.Constant _) {
                i++;
                continue;
            }
            if (isVarInt(fields[i].type())) {
                final Local raw = new Local("field" + i, new LocalType.Reference(Object.class));
                writeOps.add(new Op.GetField(fields[i], source, raw));
                final Local normalized = normalizeWriteVarInt(writeOps, fields[i].type(), raw, i, 0);
                final Value value = new Value.LocalValue(normalized);
                final Value encodedSize = new Value.VarIntSize(value);
                writeOps.add(new Op.WriteRun(new RunIr(encodedSize, List.of(new RunItem.PutVarInt(new Value.Const(0L), value, encodedSize)))));
                i++;
                continue;
            }
            if (isVarLong(fields[i].type())) {
                final Local raw = new Local("field" + i, new LocalType.Reference(Object.class));
                writeOps.add(new Op.GetField(fields[i], source, raw));
                final Local normalized = normalizeWriteVarLong(writeOps, fields[i].type(), raw, i, 0);
                writeOps.add(new Op.WriteVarLong(new Value.LocalValue(normalized)));
                i++;
                continue;
            }
            if (fixedBytesLength(fields[i].type()) >= 0) {
                final int length = fixedBytesLength(fields[i].type());
                final Local raw = new Local("field" + i, new LocalType.Reference(Object.class));
                writeOps.add(new Op.GetField(fields[i], source, raw));
                final Local bytes = materializeWriteReference(writeOps, fields[i].type(), TypeIr.FixedBytes.class, raw, i, 0, byte[].class);
                writeOps.add(new Op.WriteRun(new RunIr(
                        new Value.Const((long) length),
                        List.of(new RunItem.PutBytes(new Value.Const(0L), new Value.LocalValue(bytes), new Value.Const(length)))
                )));
                i++;
                continue;
            }
            if (isByteArray(fields[i].type())) {
                final Local raw = new Local("field" + i, new LocalType.Reference(Object.class));
                writeOps.add(new Op.GetField(fields[i], source, raw));
                final Local bytes = materializeWriteReference(writeOps, fields[i].type(), TypeIr.ByteArray.class, raw, i, 0, byte[].class);
                final Value length = new Value.ArrayLength(new Value.LocalValue(bytes));
                final Value prefix = new Value.VarIntSize(length);
                writeOps.add(new Op.WriteRun(new RunIr(
                        new Value.Add(prefix, length),
                        List.of(
                                new RunItem.PutVarInt(new Value.Const(0L), length, prefix),
                                new RunItem.PutBytes(prefix, new Value.LocalValue(bytes), length)
                        )
                )));
                i++;
                continue;
            }
            if (isStringUtf8(fields[i].type())) {
                final Local raw = new Local("field" + i, new LocalType.Reference(Object.class));
                writeOps.add(new Op.GetField(fields[i], source, raw));
                final Local string = materializeWriteReference(writeOps, fields[i].type(), TypeIr.StringUtf8.class, raw, i, 0, String.class);
                final Value bytes = new Value.StringUtf8Bytes(new Value.LocalValue(string));
                final Value length = new Value.ArrayLength(bytes);
                final Value prefix = new Value.VarIntSize(length);
                writeOps.add(new Op.WriteRun(new RunIr(
                        new Value.Add(prefix, length),
                        List.of(
                                new RunItem.PutVarInt(new Value.Const(0L), length, prefix),
                                new RunItem.PutBytes(prefix, bytes, length)
                        )
                )));
                i++;
                continue;
            }
            if (fields[i].originalType() instanceof NetworkBufferTypeImpl.ListType<?> list) {
                final Local collection = new Local("field" + i, new LocalType.Reference(List.class));
                writeOps.add(new Op.GetField(fields[i], source, collection));
                final Value count = new Value.CollectionSize(new Value.LocalValue(collection));
                final Value prefix = new Value.VarIntSize(count);
                writeOps.add(new Op.WriteRun(new RunIr(
                        prefix,
                        List.of(new RunItem.PutVarInt(new Value.Const(0L), count, prefix))
                )));
                final Local element = new Local("field" + i + "Element", new LocalType.Reference(Object.class));
                writeOps.add(new Op.ForEach(
                        new Value.LocalValue(collection),
                        element,
                        List.of(new Op.WriteExternal(list.parent(), new Value.LocalValue(element)))
                ));
                i++;
                continue;
            }
            if (fields[i].originalType() instanceof NetworkBufferTypeImpl.MapType<?, ?> map) {
                final Local mapLocal = new Local("field" + i, new LocalType.Reference(Map.class));
                writeOps.add(new Op.GetField(fields[i], source, mapLocal));
                final Value count = new Value.CollectionSize(new Value.LocalValue(mapLocal));
                final Value prefix = new Value.VarIntSize(count);
                writeOps.add(new Op.WriteRun(new RunIr(
                        prefix,
                        List.of(new RunItem.PutVarInt(new Value.Const(0L), count, prefix))
                )));
                final Local entry = new Local("field" + i + "Entry", new LocalType.Reference(Map.Entry.class));
                final Local key = new Local("field" + i + "Key", new LocalType.Reference(Object.class));
                final Local mapValue = new Local("field" + i + "Value", new LocalType.Reference(Object.class));
                writeOps.add(new Op.ForEach(
                        new Value.LocalValue(mapLocal),
                        entry,
                        List.of(
                                new Op.MapEntryKey(entry, key),
                                new Op.WriteExternal(map.parent(), new Value.LocalValue(key)),
                                new Op.MapEntryValue(entry, mapValue),
                                new Op.WriteExternal(map.valueType(), new Value.LocalValue(mapValue))
                        )
                ));
                i++;
                continue;
            }
            final NetworkBuffer.Type<?> originalType = (NetworkBuffer.Type<?>) values[i * 2];
            final Local writeLocal = new Local("field" + i, new LocalType.Reference(Object.class));
            writeOps.add(new Op.GetField(fields[i], source, writeLocal));
            writeOps.add(new Op.WriteExternal(originalType, new Value.LocalValue(writeLocal)));
            i++;
        }
        return new ProgramIr(writeOps);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ProgramIr readProgram(Object[] values, TemplatePlan plan, FieldIr<?, ?>[] fields, ConstructorIr<?> constructor) {
        final List<Op> readOps = new ArrayList<>(plan.fieldCount() + 2);
        final List<Value> constructorArgs = new ArrayList<>(plan.fieldCount());
        final Value[] valuesOut = new Value[plan.fieldCount()];
        final FieldPlan[] plans = plan.fields();
        for (int i = 0; i < plans.length; ) {
            final PrimitiveKind primitive = runPrimitive(fields[i].type());
            if (primitive != null) {
                final int start = i;
                final List<RunItem> items = new ArrayList<>();
                long offset = 0;
                do {
                    final PrimitiveKind kind = runPrimitive(fields[i].type());
                    final Local normalized = new Local("field" + i + "Value", new LocalType.Primitive(kind.localKind()));
                    items.add(new RunItem.Get(kind.storeKind(), new Value.Const(offset), normalized));
                    offset += kind.storeKind().byteSize();
                    i++;
                } while (i < plans.length && runPrimitive(fields[i].type()) != null);
                readOps.add(new Op.ReadRun(new RunIr(new Value.Const(offset), items)));
                for (int j = start; j < i; j++) {
                    final PrimitiveKind kind = runPrimitive(fields[j].type());
                    final Local normalized = new Local("field" + j + "Value", new LocalType.Primitive(kind.localKind()));
                    valuesOut[j] = new Value.LocalValue(materializeReadPrimitive(readOps, fields[j].type(), normalized, j, 0));
                }
                continue;
            }
            if (fields[i].type() instanceof TypeIr.Constant constant) {
                valuesOut[i] = new Value.Const(constant.value());
                i++;
                continue;
            }
            if (isVarInt(fields[i].type())) {
                final Local normalized = new Local("field" + i + "Value", new LocalType.Primitive(TypeKind.INT));
                readOps.add(new Op.ReadVarInt(normalized));
                valuesOut[i] = new Value.LocalValue(materializeReadVarInt(readOps, fields[i].type(), normalized, i, 0));
                i++;
                continue;
            }
            if (isVarLong(fields[i].type())) {
                final Local normalized = new Local("field" + i + "Value", new LocalType.Primitive(TypeKind.LONG));
                readOps.add(new Op.ReadVarLong(normalized));
                valuesOut[i] = new Value.LocalValue(materializeReadVarLong(readOps, fields[i].type(), normalized, i, 0));
                i++;
                continue;
            }
            if (fixedBytesLength(fields[i].type()) >= 0) {
                final int length = fixedBytesLength(fields[i].type());
                final Local bytes = new Local("field" + i + "Bytes", new LocalType.Reference(byte[].class));
                readOps.add(new Op.ReadRun(new RunIr(
                        new Value.Const((long) length),
                        List.of(new RunItem.GetBytes(new Value.Const(0L), bytes, new Value.Const(length)))
                )));
                valuesOut[i] = new Value.LocalValue(materializeReadReference(readOps, fields[i].type(), TypeIr.FixedBytes.class, bytes, i, 0));
                i++;
                continue;
            }
            if (isByteArray(fields[i].type())) {
                final Local length = new Local("field" + i + "Length", new LocalType.Primitive(TypeKind.INT));
                final Local bytes = new Local("field" + i + "Bytes", new LocalType.Reference(byte[].class));
                readOps.add(new Op.ReadVarInt(length));
                readOps.add(new Op.ReadRun(new RunIr(
                        new Value.LocalValue(length),
                        List.of(new RunItem.GetBytes(new Value.Const(0L), bytes, new Value.LocalValue(length)))
                )));
                valuesOut[i] = new Value.LocalValue(materializeReadReference(readOps, fields[i].type(), TypeIr.ByteArray.class, bytes, i, 0));
                i++;
                continue;
            }
            if (isStringUtf8(fields[i].type())) {
                final Local length = new Local("field" + i + "Length", new LocalType.Primitive(TypeKind.INT));
                final Local bytes = new Local("field" + i + "Bytes", new LocalType.Reference(byte[].class));
                final Local string = new Local("field" + i + "String", new LocalType.Reference(String.class));
                readOps.add(new Op.ReadVarInt(length));
                readOps.add(new Op.ReadRun(new RunIr(
                        new Value.LocalValue(length),
                        List.of(new RunItem.GetBytes(new Value.Const(0L), bytes, new Value.LocalValue(length)))
                )));
                valuesOut[i] = new Value.LocalValue(materializeReadReference(readOps, fields[i].type(), TypeIr.StringUtf8.class, string, i, 0));
                i++;
                continue;
            }
            if (fields[i].originalType() instanceof NetworkBufferTypeImpl.ListType<?> list) {
                final Local count = new Local("field" + i + "Count", new LocalType.Primitive(TypeKind.INT));
                final Local index = new Local("field" + i + "Index", new LocalType.Primitive(TypeKind.INT));
                final Local result = new Local("field" + i, new LocalType.Reference(List.class));
                final Local element = new Local("field" + i + "Element", new LocalType.Reference(Object.class));
                readOps.add(new Op.ReadVarInt(count));
                readOps.add(new Op.ForIndex(
                        index,
                        new Value.Const(0),
                        new Value.LocalValue(count),
                        List.of(
                                new Op.ReadExternal(list.parent(), element),
                                new Op.CollectionAdd(result, new Value.LocalValue(element))
                        )
                ));
                valuesOut[i] = new Value.LocalValue(result);
                i++;
                continue;
            }
            if (fields[i].originalType() instanceof NetworkBufferTypeImpl.MapType<?, ?> map) {
                final Local count = new Local("field" + i + "Count", new LocalType.Primitive(TypeKind.INT));
                final Local index = new Local("field" + i + "Index", new LocalType.Primitive(TypeKind.INT));
                final Local result = new Local("field" + i, new LocalType.Reference(Map.class));
                final Local key = new Local("field" + i + "Key", new LocalType.Reference(Object.class));
                final Local mapValue = new Local("field" + i + "Value", new LocalType.Reference(Object.class));
                readOps.add(new Op.ReadVarInt(count));
                readOps.add(new Op.ForIndex(
                        index,
                        new Value.Const(0),
                        new Value.LocalValue(count),
                        List.of(
                                new Op.ReadExternal(map.parent(), key),
                                new Op.ReadExternal(map.valueType(), mapValue),
                                new Op.MapPut(result, new Value.LocalValue(key), new Value.LocalValue(mapValue))
                        )
                ));
                valuesOut[i] = new Value.LocalValue(result);
                i++;
                continue;
            }
            final NetworkBuffer.Type<?> originalType = (NetworkBuffer.Type<?>) values[i * 2];
            final Local readLocal = new Local("field" + i, new LocalType.Reference(Object.class));
            readOps.add(new Op.ReadExternal(originalType, readLocal));
            valuesOut[i] = new Value.LocalValue(readLocal);
            i++;
        }
        constructorArgs.addAll(Arrays.asList(valuesOut));
        final Local result = new Local("result", new LocalType.Reference(Object.class));
        readOps.add(new Op.Construct(constructor, constructorArgs, result));
        readOps.add(new Op.Return(new Value.LocalValue(result)));
        return new ProgramIr(readOps);
    }

    private static Local normalizeWritePrimitive(List<Op> ops, TypeIr<?> type, Local in, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform transform -> {
                final Local out = new Local("field" + fieldIndex + "Transform" + depth, new LocalType.Reference(Object.class));
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWritePrimitive(ops, transform.parent(), out, fieldIndex, depth + 1);
            }
            case TypeIr.Primitive primitive -> {
                final PrimitiveKind kind = primitive.kind();
                final Local cast = new Local("field" + fieldIndex + "Cast", new LocalType.Reference(kind.wrapperClass()));
                final Local normalized = new Local("field" + fieldIndex + "Value", new LocalType.Primitive(kind.localKind()));
                ops.add(new Op.Cast(in, kind.wrapperClass(), cast));
                ops.add(new Op.Unbox(kind, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not primitive run-compatible: " + type);
        };
    }

    private static Local materializeReadPrimitive(List<Op> ops, TypeIr<?> type, Local normalized, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform transform -> {
                final Local parent = materializeReadPrimitive(ops, transform.parent(), normalized, fieldIndex, depth + 1);
                final Local out = new Local("field" + fieldIndex + "Transform" + depth, new LocalType.Reference(Object.class));
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.Primitive primitive -> {
                final PrimitiveKind kind = primitive.kind();
                final Local boxed = new Local("field" + fieldIndex, new LocalType.Reference(kind.wrapperClass()));
                ops.add(new Op.Box(kind, normalized, boxed));
                yield boxed;
            }
            default -> throw new IllegalArgumentException("Type is not primitive run-compatible: " + type);
        };
    }

    private static Local normalizeWriteVarInt(List<Op> ops, TypeIr<?> type, Local in, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform transform -> {
                final Local out = new Local("field" + fieldIndex + "Transform" + depth, new LocalType.Reference(Object.class));
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWriteVarInt(ops, transform.parent(), out, fieldIndex, depth + 1);
            }
            case TypeIr.VarInt _ -> {
                final Local cast = new Local("field" + fieldIndex + "Cast", new LocalType.Reference(Integer.class));
                final Local normalized = new Local("field" + fieldIndex + "Value", new LocalType.Primitive(TypeKind.INT));
                ops.add(new Op.Cast(in, Integer.class, cast));
                ops.add(new Op.Unbox(PrimitiveKind.INT, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not VarInt-compatible: " + type);
        };
    }

    private static Local materializeReadVarInt(List<Op> ops, TypeIr<?> type, Local normalized, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform transform -> {
                final Local parent = materializeReadVarInt(ops, transform.parent(), normalized, fieldIndex, depth + 1);
                final Local out = new Local("field" + fieldIndex + "Transform" + depth, new LocalType.Reference(Object.class));
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.VarInt _ -> {
                final Local boxed = new Local("field" + fieldIndex, new LocalType.Reference(Integer.class));
                ops.add(new Op.Box(PrimitiveKind.INT, normalized, boxed));
                yield boxed;
            }
            default -> throw new IllegalArgumentException("Type is not VarInt-compatible: " + type);
        };
    }

    private static boolean isVarInt(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.VarInt _ -> true;
            case TypeIr.Transform transform -> isVarInt(transform.parent());
            default -> false;
        };
    }

    private static Local normalizeWriteVarLong(List<Op> ops, TypeIr<?> type, Local in, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform transform -> {
                final Local out = new Local("field" + fieldIndex + "Transform" + depth, new LocalType.Reference(Object.class));
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWriteVarLong(ops, transform.parent(), out, fieldIndex, depth + 1);
            }
            case TypeIr.VarLong _ -> {
                final Local cast = new Local("field" + fieldIndex + "Cast", new LocalType.Reference(Long.class));
                final Local normalized = new Local("field" + fieldIndex + "Value", new LocalType.Primitive(TypeKind.LONG));
                ops.add(new Op.Cast(in, Long.class, cast));
                ops.add(new Op.Unbox(PrimitiveKind.LONG, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not VarLong-compatible: " + type);
        };
    }

    private static Local materializeReadVarLong(List<Op> ops, TypeIr<?> type, Local normalized, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform transform -> {
                final Local parent = materializeReadVarLong(ops, transform.parent(), normalized, fieldIndex, depth + 1);
                final Local out = new Local("field" + fieldIndex + "Transform" + depth, new LocalType.Reference(Object.class));
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.VarLong _ -> {
                final Local boxed = new Local("field" + fieldIndex, new LocalType.Reference(Long.class));
                ops.add(new Op.Box(PrimitiveKind.LONG, normalized, boxed));
                yield boxed;
            }
            default -> throw new IllegalArgumentException("Type is not VarLong-compatible: " + type);
        };
    }

    private static boolean isVarLong(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.VarLong _ -> true;
            case TypeIr.Transform transform -> isVarLong(transform.parent());
            default -> false;
        };
    }

    private static Local materializeWriteReference(List<Op> ops, TypeIr<?> type, Class<?> targetType, Local in,
                                                   int fieldIndex, int depth, Class<?> targetClass) {
        return switch (type) {
            case TypeIr.Transform transform -> {
                final Local out = new Local("field" + fieldIndex + "Transform" + depth, new LocalType.Reference(Object.class));
                ops.add(new Op.Apply(transform.from(), in, out));
                yield materializeWriteReference(ops, transform.parent(), targetType, out, fieldIndex, depth + 1, targetClass);
            }
            default -> {
                if (!targetType.isInstance(type)) {
                    throw new IllegalArgumentException("Type is not " + targetType.getSimpleName() + "-compatible: " + type);
                }
                final Local cast = new Local("field" + fieldIndex + "Cast", new LocalType.Reference(targetClass));
                ops.add(new Op.Cast(in, targetClass, cast));
                yield cast;
            }
        };
    }

    private static Local materializeReadReference(List<Op> ops, TypeIr<?> type, Class<?> targetType, Local in,
                                                  int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform transform -> {
                final Local parent = materializeReadReference(ops, transform.parent(), targetType, in, fieldIndex, depth + 1);
                final Local out = new Local("field" + fieldIndex + "Transform" + depth, new LocalType.Reference(Object.class));
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            default -> {
                if (!targetType.isInstance(type)) {
                    throw new IllegalArgumentException("Type is not " + targetType.getSimpleName() + "-compatible: " + type);
                }
                yield in;
            }
        };
    }

    private static int fixedBytesLength(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.FixedBytes fixedBytes -> fixedBytes.length();
            case TypeIr.Transform transform -> fixedBytesLength(transform.parent());
            default -> -1;
        };
    }

    private static boolean isByteArray(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.ByteArray _ -> true;
            case TypeIr.Transform transform -> isByteArray(transform.parent());
            default -> false;
        };
    }

    private static boolean isStringUtf8(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.StringUtf8 _ -> true;
            case TypeIr.Transform transform -> isStringUtf8(transform.parent());
            default -> false;
        };
    }

    private static @Nullable PrimitiveKind runPrimitive(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.Primitive primitive -> primitive.kind();
            case TypeIr.Transform transform -> runPrimitive(transform.parent());
            default -> null;
        };
    }

    private static Object invokeConstructor(Object constructor, int fieldCount, List<?> args) {
        if (args.size() != fieldCount) {
            throw new IllegalArgumentException("Expected " + fieldCount + " constructor arguments, got " + args.size());
        }
        for (Method method : constructor.getClass().getMethods()) {
            if (method.getName().equals("apply") && method.getParameterCount() == fieldCount) {
                try {
                    return method.invoke(constructor, args.toArray());
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Failed to invoke network IR constructor", exception);
                }
            }
        }
        throw new IllegalStateException("Missing apply method for constructor with " + fieldCount + " arguments");
    }

    @SuppressWarnings("unchecked")
    private static <T extends @UnknownNullability Object> ConstructorIr<T> constructorIr(Object constructor, int fieldCount) {
        return args -> (T) invokeConstructor(constructor, fieldCount, args);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TypeIr<?> typeIr(NetworkBuffer.Type<?> type) {
        return typeIr(type, new IdentityHashMap<>());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TypeIr<?> typeIr(NetworkBuffer.Type<?> type, IdentityHashMap<NetworkBuffer.Type<?>, Boolean> visiting) {
        if (visiting.put(type, Boolean.TRUE) != null) {
            return new TypeIr.External(type);
        }
        final TypeIr<?> result;
        try {
            result = switch (type) {
                case NetworkIrBacked<?> backed -> new TypeIr.Template(backed.ir());
                case NetworkBufferTypeImpl.UnitType _ -> new TypeIr.Constant<>(net.minestom.server.utils.Unit.INSTANCE);
                case NetworkBufferTypeImpl.BooleanType _ -> new TypeIr.Primitive<>(PrimitiveKind.BOOLEAN);
                case NetworkBufferTypeImpl.ByteType _ -> new TypeIr.Primitive<>(PrimitiveKind.BYTE);
                case NetworkBufferTypeImpl.UnsignedByteType _ -> new TypeIr.Primitive<>(PrimitiveKind.UNSIGNED_BYTE);
                case NetworkBufferTypeImpl.ShortType _ -> new TypeIr.Primitive<>(PrimitiveKind.SHORT);
                case NetworkBufferTypeImpl.UnsignedShortType _ -> new TypeIr.Primitive<>(PrimitiveKind.UNSIGNED_SHORT);
                case NetworkBufferTypeImpl.IntType _ -> new TypeIr.Primitive<>(PrimitiveKind.INT);
                case NetworkBufferTypeImpl.UnsignedIntType _ -> new TypeIr.Primitive<>(PrimitiveKind.UNSIGNED_INT);
                case NetworkBufferTypeImpl.LongType _ -> new TypeIr.Primitive<>(PrimitiveKind.LONG);
                case NetworkBufferTypeImpl.FloatType _ -> new TypeIr.Primitive<>(PrimitiveKind.FLOAT);
                case NetworkBufferTypeImpl.DoubleType _ -> new TypeIr.Primitive<>(PrimitiveKind.DOUBLE);
                case NetworkBufferTypeImpl.VarIntType _ -> new TypeIr.VarInt();
                case NetworkBufferTypeImpl.VarLongType _ -> new TypeIr.VarLong();
                case NetworkBufferTypeImpl.StringType _ -> new TypeIr.StringUtf8(Integer.MAX_VALUE);
                case NetworkBufferTypeImpl.ByteArrayType _ -> new TypeIr.ByteArray(Integer.MAX_VALUE);
                case NetworkBufferTypeImpl.RawBytesType raw -> raw.length() >= 0 ? new TypeIr.FixedBytes(raw.length()) : new TypeIr.External(type);
                case NetworkBufferTypeImpl.OptionalType<?> optional -> new TypeIr.Optional(typeIr(optional.parent(), visiting));
                case NetworkBufferTypeImpl.TransformType<?, ?> transform ->
                        new TypeIr.Transform(typeIr(transform.parent(), visiting), transform.to(), transform.from());
                case NetworkBufferTypeImpl.ListType<?> list -> new TypeIr.ListType(typeIr(list.parent(), visiting), list.maxSize(), LIST_FACTORY);
                case NetworkBufferTypeImpl.MapType<?, ?> map -> new TypeIr.MapType(typeIr(map.parent(), visiting), typeIr(map.valueType(), visiting), map.maxSize(), MAP_FACTORY);
                default -> new TypeIr.External(type);
            };
        } finally {
            visiting.remove(type);
        }
        return result;
    }

    CollectionFactory<Object, List<Object>> LIST_FACTORY = new CollectionFactory<>() {
        @Override
        public Object create(int size) {
            return new ArrayList<>(size);
        }

        @Override
        public void add(Object collection, Object value) {
            ((List<Object>) collection).add(value);
        }

        @Override
        public List<Object> finish(Object collection) {
            return List.copyOf((List<Object>) collection);
        }
    };

    MapFactory<Object, Object, Map<Object, Object>> MAP_FACTORY = new MapFactory<>() {
        @Override
        public Object create(int size) {
            return new java.util.LinkedHashMap<>(size);
        }

        @Override
        public void put(Object map, Object key, Object value) {
            ((Map<Object, Object>) map).put(key, value);
        }

        @Override
        public Map<Object, Object> finish(Object map) {
            return Map.copyOf((Map<Object, Object>) map);
        }
    };

    @Nullable
    private static TypeIr<?> optimizedType(String path, List<Object> classData, Object type) {
        return optimizedType(path, classData, typeIr((NetworkBuffer.Type<?>) type));
    }

    @Nullable
    private static TypeIr<?> optimizedType(String path, List<Object> classData, TypeIr<?> type) {
        return switch (type) {
            case TypeIr.Optional optional -> {
                final TypeIr<?> parent = optimizedType(path, classData, optional.parent());
                yield parent != null ? new TypeIr.Optional(parent) : null;
            }
            case TypeIr.Transform transform -> {
                final TypeIr<?> parent = optimizedType(path, classData, transform.parent());
                yield parent != null && !(parent instanceof TypeIr.Optional) ? new TypeIr.Transform(parent, transform.to(), transform.from()) : null;
            }
            case TypeIr.Template template -> template;
            case TypeIr.VarInt _, TypeIr.VarLong _, TypeIr.StringUtf8 _, TypeIr.ByteArray _,
                 TypeIr.FixedBytes _, TypeIr.Constant _, TypeIr.Primitive _ -> type;
            default -> null;
        };
    }

    private static @Nullable TemplatePlan nestedTemplatePlan(String path, List<Object> classData, @Nullable TypeIr<?> type) {
        while (type instanceof TypeIr.Transform transform) {
            type = transform.parent();
        }
        if (type instanceof TypeIr.Template template) {
            return templatePlan(path, classData, template.ir());
        }
        return null;
    }

    private static int addClassData(List<Object> classData, Object value) {
        final int index = classData.size();
        classData.add(value);
        return index;
    }

    record TemplatePlan(String path, int fieldCount, FieldPlan[] fields, int ctorDataIndex) {
    }

    record FieldPlan(int index, String path, int typeDataIndex, int getterDataIndex,
                     @Nullable TypeIr<?> type, @Nullable TemplatePlan nestedPlan) {
    }

    record WriteRunValue(@Nullable PrimitiveKind type, int valueSlot, long byteOffset, int byteLength) {
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
