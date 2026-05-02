package net.minestom.server.network;

import net.minestom.server.utils.Unit;
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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
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
    static final ClassDesc CD_TEMPLATE_IMPL = NetworkBufferTemplateImpl.class.describeConstable().orElseThrow();
    static final ClassDesc CD_NETWORK_IR = NetworkIr.class.describeConstable().orElseThrow();
    static final ClassDesc CD_FUNCTION = Function.class.describeConstable().orElseThrow();
    static final ClassDesc CD_UNIT = Unit.class.describeConstable().orElseThrow();

    static final MethodTypeDesc MT_VOID = MethodTypeDesc.of(CD_VOID);
    static final MethodTypeDesc MT_LOOKUP = MethodTypeDesc.of(CD_METHOD_HANDLES_LOOKUP);
    static final MethodTypeDesc MT_CLASS_DATA_AT = MethodTypeDesc.of(CD_OBJECT, CD_METHOD_HANDLES_LOOKUP, CD_STRING, CD_CLASS, CD_INT);
    static final MethodTypeDesc MT_READ_OBJECT = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER);
    static final MethodTypeDesc MT_WRITE_OBJECT = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_OBJECT);
    static final MethodTypeDesc MT_NETWORK_IR = MethodTypeDesc.of(CD_NETWORK_IR);
    static final MethodTypeDesc MT_RESERVE = MethodTypeDesc.of(CD_LONG, CD_LONG);
    static final MethodTypeDesc MT_FUNCTION_APPLY = MethodTypeDesc.of(CD_OBJECT, CD_OBJECT);
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
    static final Path DUMP_ROOT = Path.of("build", "generated", "network-templates");
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
            final ConstructorIr<T> constructorIr = new ConstructorIr<>(values[values.length - 1], fieldCount);
            final NetworkIr<T> ir = networkIr("NetworkTemplate", values, fieldCount, constructorIr);
            final IrClassData irData = irClassData("", classData, ir);
            final int irIndex = addClassData(classData, ir);
            final byte[] bytes = ClassFile.of().build(classDesc, classBuilder -> {
                classBuilder.withFlags(CLASS_FLAGS)
                        .withSuperclass(CD_OBJECT)
                        .withInterfaceSymbols(CD_TEMPLATE_IMPL);

                classBuilder.withField(IR_FIELD_NAME, CD_NETWORK_IR, FIELD_FLAGS);
                declareIrFields(classBuilder, irData);

                classBuilder.withMethodBody(ConstantDescs.CLASS_INIT_NAME, MT_VOID, ClassFile.ACC_STATIC | ClassFile.ACC_SYNTHETIC,
                        codeBuilder -> buildClassInitializer(codeBuilder, classDesc, irData, irIndex));
                classBuilder.withMethodBody(ConstantDescs.INIT_NAME, MT_VOID, ClassFile.ACC_PRIVATE | ClassFile.ACC_SYNTHETIC,
                        codeBuilder -> codeBuilder.aload(0).invokespecial(CD_OBJECT, ConstantDescs.INIT_NAME, MT_VOID).return_());
                classBuilder.withMethodBody(IR, MT_NETWORK_IR, METHOD_FLAGS,
                        codeBuilder -> codeBuilder.getstatic(classDesc, IR_FIELD_NAME, CD_NETWORK_IR).areturn());
                classBuilder.withMethodBody(WRITE, MT_WRITE_OBJECT, METHOD_FLAGS,
                        codeBuilder -> buildWrite(codeBuilder, classDesc, irData, ir.write(), 2));
                classBuilder.withMethodBody(READ, MT_READ_OBJECT, METHOD_FLAGS,
                        codeBuilder -> buildRead(codeBuilder, classDesc, irData, ir.read()));
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

    private static void declareIrFields(ClassBuilder classBuilder, IrClassData data) {
        for (IrFieldData field : data.fields()) {
            classBuilder.withField(typeName(field.path()), CD_TYPE, FIELD_FLAGS);
            classBuilder.withField(getterName(field.path()), CD_FUNCTION, FIELD_FLAGS);
        }
        for (TransformFieldData transform : data.transforms()) {
            classBuilder.withField(transform.name(), CD_FUNCTION, FIELD_FLAGS);
        }
        classBuilder.withField(ctorName(data.path()), constructorInterface(data.ir().constructor().fieldCount()), FIELD_FLAGS);
    }

    private static void buildClassInitializer(CodeBuilder codeBuilder, ClassDesc classDesc, IrClassData data, int irIndex) {
        codeBuilder.invokestatic(CD_METHOD_HANDLES, "lookup", MT_LOOKUP)
                .astore(0);
        loadClassDataAt(codeBuilder, CD_NETWORK_IR, irIndex)
                .putstatic(classDesc, IR_FIELD_NAME, CD_NETWORK_IR);
        initIrFields(codeBuilder, classDesc, data);
        codeBuilder.return_();
    }

    private static void initIrFields(CodeBuilder codeBuilder, ClassDesc classDesc, IrClassData data) {
        for (IrFieldData field : data.fields()) {
            loadClassDataAt(codeBuilder, CD_TYPE, field.typeDataIndex())
                    .putstatic(classDesc, typeName(field.path()), CD_TYPE);
            loadClassDataAt(codeBuilder, CD_FUNCTION, field.getterDataIndex())
                    .putstatic(classDesc, getterName(field.path()), CD_FUNCTION);
        }
        for (TransformFieldData transform : data.transforms()) {
            loadClassDataAt(codeBuilder, CD_FUNCTION, transform.dataIndex())
                    .putstatic(classDesc, transform.name(), CD_FUNCTION);
        }
        final ClassDesc constructorType = constructorInterface(data.ir().constructor().fieldCount());
        loadClassDataAt(codeBuilder, constructorType, data.ctorDataIndex())
                .putstatic(classDesc, ctorName(data.path()), constructorType);
    }

    private static void buildWrite(CodeBuilder codeBuilder, ClassDesc classDesc, IrClassData data, ProgramIr program, int objectSlot) {
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        emitDirectBuffer(codeBuilder, directSlot);
        final EmitContext context = new EmitContext(classDesc, data, directSlot, new HashMap<>());
        context.locals().put(referenceLocal("value"), objectSlot);
        emitProgram(codeBuilder, context, program);
        codeBuilder.return_();
    }

    private static void buildRead(CodeBuilder codeBuilder, ClassDesc classDesc, IrClassData data, ProgramIr program) {
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        emitDirectBuffer(codeBuilder, directSlot);
        final EmitContext context = new EmitContext(classDesc, data, directSlot, new HashMap<>());
        emitProgram(codeBuilder, context, program);
    }

    private static void emitProgram(CodeBuilder codeBuilder, EmitContext context, ProgramIr program) {
        for (Op op : program.ops()) {
            emitOp(codeBuilder, context, op);
        }
    }

    private static void emitOp(CodeBuilder codeBuilder, EmitContext context, Op op) {
        switch (op) {
            case Op.GetField getField -> {
                final IrFieldData field = context.data().fields().get(getField.field().index());
                codeBuilder.getstatic(context.classDesc(), getterName(field.path()), CD_FUNCTION);
                emitLoadLocal(codeBuilder, context, getField.source());
                codeBuilder.invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
                emitStoreLocal(codeBuilder, context, getField.out());
            }
            case Op.Apply apply -> {
                codeBuilder.getstatic(context.classDesc(), transformFunctionName(context.data(), apply.function()), CD_FUNCTION);
                emitLoadLocal(codeBuilder, context, apply.in());
                codeBuilder.invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
                emitStoreLocal(codeBuilder, context, apply.out());
            }
            case Op.Cast cast -> {
                emitLoadLocal(codeBuilder, context, cast.in());
                codeBuilder.checkcast(classDesc(cast.targetClass()));
                emitStoreLocal(codeBuilder, context, cast.out());
            }
            case Op.Unbox unbox -> {
                emitLoadLocal(codeBuilder, context, unbox.in());
                emitUnboxedTypeIrValue(codeBuilder, unbox.kind());
                emitStoreLocal(codeBuilder, context, unbox.out());
            }
            case Op.Box box -> {
                emitLoadLocal(codeBuilder, context, box.in());
                emitBoxedTypeIrValue(codeBuilder, box.kind());
                emitStoreLocal(codeBuilder, context, box.out());
            }
            case Op.WriteExternal writeExternal -> {
                codeBuilder.getstatic(context.classDesc(), typeFieldName(context.data(), writeExternal.type()), CD_TYPE)
                        .aload(1);
                emitValue(codeBuilder, context, writeExternal.value());
                codeBuilder.invokeinterface(CD_TYPE, WRITE, MT_WRITE_OBJECT);
            }
            case Op.ReadExternal readExternal -> {
                codeBuilder.getstatic(context.classDesc(), typeFieldName(context.data(), readExternal.type()), CD_TYPE)
                        .aload(1)
                        .invokeinterface(CD_TYPE, READ, MT_READ_OBJECT);
                emitStoreLocal(codeBuilder, context, readExternal.out());
            }
            case Op.ReadVarInt readVarInt -> {
                codeBuilder.aload(context.directSlot())
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "readVarInt", MT_READ_VAR_INT, true);
                emitStoreLocal(codeBuilder, context, readVarInt.out());
            }
            case Op.WriteVarLong writeVarLong -> {
                final int longSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                final int sizeSlot = codeBuilder.allocateLocal(TypeKind.INT);
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                emitValue(codeBuilder, context, writeVarLong.value());
                codeBuilder.lstore(longSlot)
                        .lload(longSlot)
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varLongSize", MT_VAR_LONG_SIZE, true)
                        .istore(sizeSlot)
                        .aload(context.directSlot())
                        .iload(sizeSlot)
                        .i2l()
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                        .lstore(indexSlot)
                        .aload(context.directSlot())
                        .lload(indexSlot)
                        .lload(longSlot)
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarLongUnchecked", MT_WRITE_VAR_LONG_UNCHECKED, true);
            }
            case Op.ReadVarLong readVarLong -> {
                codeBuilder.aload(context.directSlot())
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "readVarLong", MT_READ_VAR_LONG, true);
                emitStoreLocal(codeBuilder, context, readVarLong.out());
            }
            case Op.WriteRun writeRun -> emitWriteRun(codeBuilder, context, writeRun.run());
            case Op.ReadRun readRun -> emitReadRun(codeBuilder, context, readRun.run());
            case Op.Construct construct -> {
                final ClassDesc constructorType = constructorInterface(construct.constructor().fieldCount());
                codeBuilder.getstatic(context.classDesc(), ctorName(context.data().path()), constructorType);
                for (Value arg : construct.args()) {
                    emitValue(codeBuilder, context, arg);
                }
                codeBuilder.invokeinterface(constructorType, "apply", constructorApplyType(construct.args().size()));
                emitStoreLocal(codeBuilder, context, construct.out());
            }
            case Op.Return ret -> {
                emitValue(codeBuilder, context, ret.value());
                codeBuilder.areturn();
            }
            default -> throw new UnsupportedOperationException("Unsupported IR op: " + op);
        }
    }

    private static void emitWriteRun(CodeBuilder codeBuilder, EmitContext context, RunIr run) {
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        codeBuilder.aload(context.directSlot());
        emitLongValue(codeBuilder, context, run.size());
        codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                .lstore(indexSlot);
        for (RunItem item : run.items()) {
            switch (item) {
                case RunItem.Put put -> {
                    codeBuilder.aload(context.directSlot());
                    emitOffsetValue(codeBuilder, context, indexSlot, put.offset());
                    emitValue(codeBuilder, context, put.value());
                    emitStoreKindWrite(codeBuilder, put.kind(), put.value());
                }
                case RunItem.PutVarInt putVarInt -> {
                    codeBuilder.aload(context.directSlot());
                    emitOffsetValue(codeBuilder, context, indexSlot, putVarInt.offset());
                    emitValue(codeBuilder, context, putVarInt.value());
                    codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarIntUnchecked", MT_WRITE_VAR_INT_UNCHECKED, true);
                }
                case RunItem.PutBytes putBytes -> {
                    codeBuilder.aload(context.directSlot());
                    emitOffsetValue(codeBuilder, context, indexSlot, putBytes.offset());
                    emitValue(codeBuilder, context, putBytes.byteArray());
                    codeBuilder.iconst_0();
                    emitIntValue(codeBuilder, context, putBytes.length());
                    codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putBytesUnchecked", MT_PUT_BYTES);
                }
                default -> throw new UnsupportedOperationException("Unsupported write run item: " + item);
            }
        }
    }

    private static void emitReadRun(CodeBuilder codeBuilder, EmitContext context, RunIr run) {
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        codeBuilder.aload(context.directSlot());
        emitLongValue(codeBuilder, context, run.size());
        codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveRead", MT_RESERVE)
                .lstore(indexSlot);
        for (RunItem item : run.items()) {
            switch (item) {
                case RunItem.Get get -> {
                    codeBuilder.aload(context.directSlot());
                    emitOffsetValue(codeBuilder, context, indexSlot, get.offset());
                    emitStoreKindRead(codeBuilder, get.kind(), get.out());
                    emitStoreLocal(codeBuilder, context, get.out());
                }
                case RunItem.GetBytes getBytes -> {
                    final int lengthSlot = codeBuilder.allocateLocal(TypeKind.INT);
                    emitIntValue(codeBuilder, context, getBytes.length());
                    codeBuilder.dup()
                            .istore(lengthSlot)
                            .newarray(TypeKind.BYTE);
                    emitStoreLocal(codeBuilder, context, getBytes.byteArray());
                    codeBuilder.aload(context.directSlot());
                    emitOffsetValue(codeBuilder, context, indexSlot, getBytes.offset());
                    emitLoadLocal(codeBuilder, context, getBytes.byteArray());
                    codeBuilder.iconst_0()
                            .iload(lengthSlot)
                            .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getBytesUnchecked", MT_GET_BYTES);
                }
                default -> throw new UnsupportedOperationException("Unsupported read run item: " + item);
            }
        }
    }

    private static void emitValue(CodeBuilder codeBuilder, EmitContext context, Value value) {
        switch (value) {
            case Value.LocalValue localValue -> emitLoadLocal(codeBuilder, context, localValue.local());
            case Value.Const constant -> emitConstant(codeBuilder, constant.value());
            case Value.VarIntSize varIntSize -> {
                emitIntValue(codeBuilder, context, varIntSize.intValue());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varIntSize", MT_VAR_INT_SIZE, true);
            }
            case Value.Add add -> {
                emitLongValue(codeBuilder, context, add.left());
                emitLongValue(codeBuilder, context, add.right());
                codeBuilder.ladd();
            }
            default -> throw new UnsupportedOperationException("Unsupported IR value: " + value);
        }
    }

    private static void emitIntValue(CodeBuilder codeBuilder, EmitContext context, Value value) {
        switch (value) {
            case Value.LocalValue localValue -> emitLoadLocal(codeBuilder, context, localValue.local());
            case Value.Const constant -> codeBuilder.loadConstant(((Number) constant.value()).intValue());
            case Value.VarIntSize varIntSize -> {
                emitIntValue(codeBuilder, context, varIntSize.intValue());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varIntSize", MT_VAR_INT_SIZE, true);
            }
            default -> throw new UnsupportedOperationException("Unsupported int IR value: " + value);
        }
    }

    private static void emitLongValue(CodeBuilder codeBuilder, EmitContext context, Value value) {
        switch (value) {
            case Value.LocalValue localValue -> {
                emitLoadLocal(codeBuilder, context, localValue.local());
                if (localValue.local().type() instanceof LocalType.Kind kind && kind.kind() != TypeKind.LONG) {
                    codeBuilder.i2l();
                }
            }
            case Value.Const constant -> codeBuilder.loadConstant(((Number) constant.value()).longValue());
            case Value.VarIntSize varIntSize -> {
                emitIntValue(codeBuilder, context, varIntSize);
                codeBuilder.i2l();
            }
            case Value.Add add -> {
                emitLongValue(codeBuilder, context, add.left());
                emitLongValue(codeBuilder, context, add.right());
                codeBuilder.ladd();
            }
            default -> throw new UnsupportedOperationException("Unsupported long IR value: " + value);
        }
    }

    private static void emitOffsetValue(CodeBuilder codeBuilder, EmitContext context, int indexSlot, Value offset) {
        codeBuilder.lload(indexSlot);
        emitLongValue(codeBuilder, context, offset);
        codeBuilder.ladd();
    }

    private static void emitConstant(CodeBuilder codeBuilder, @Nullable Object value) {
        if (value == null) {
            codeBuilder.aconst_null();
        } else if (value instanceof Integer intValue) {
            codeBuilder.loadConstant(intValue);
        } else if (value instanceof Long longValue) {
            codeBuilder.loadConstant(longValue);
        } else if (value instanceof String stringValue) {
            codeBuilder.loadConstant(stringValue);
        } else if (value == Unit.INSTANCE) {
            codeBuilder.getstatic(CD_UNIT, "INSTANCE", CD_UNIT);
        } else {
            throw new UnsupportedOperationException("Unsupported constant IR value: " + value);
        }
    }

    private static void emitStoreKindWrite(CodeBuilder codeBuilder, StoreKind kind, Value value) {
        switch (kind) {
            case BOOLEAN, BYTE -> {
                if (longValue(value)) codeBuilder.l2i();
                codeBuilder.i2b()
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putByteUnchecked", MT_PUT_BYTE);
            }
            case SHORT -> {
                if (longValue(value)) codeBuilder.l2i();
                codeBuilder.i2s()
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putShortUnchecked", MT_PUT_SHORT);
            }
            case INT -> {
                if (longValue(value)) codeBuilder.l2i();
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putIntUnchecked", MT_PUT_INT);
            }
            case LONG -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putLongUnchecked", MT_PUT_LONG);
            case FLOAT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putFloatUnchecked", MT_PUT_FLOAT);
            case DOUBLE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putDoubleUnchecked", MT_PUT_DOUBLE);
        }
    }

    private static boolean longValue(Value value) {
        return switch (value) {
            case Value.LocalValue localValue -> localValue.local().type() instanceof LocalType.Kind kind &&
                    kind.kind() == TypeKind.LONG;
            case Value.Const constant -> constant.value() instanceof Long;
            case Value.Add _, Value.Mul _, Value.ShiftLeft _, Value.ShiftRightUnsigned _ -> true;
            default -> false;
        };
    }

    private static void emitStoreKindRead(CodeBuilder codeBuilder, StoreKind kind, Local out) {
        final TypeKind targetKind = localTypeKind(out.type());
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
            case BYTE -> {
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getByteUnchecked", MT_GET_BYTE);
                if (targetKind == TypeKind.INT) {
                    codeBuilder.sipush(0xFF)
                            .iand();
                }
            }
            case SHORT -> {
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getShortUnchecked", MT_GET_SHORT);
                if (targetKind == TypeKind.INT) {
                    codeBuilder.loadConstant(0xFFFF)
                            .iand();
                }
            }
            case INT -> {
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getIntUnchecked", MT_GET_INT);
                if (targetKind == TypeKind.LONG) {
                    codeBuilder.i2l()
                            .loadConstant(0xFFFFFFFFL)
                            .land();
                }
            }
            case LONG -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getLongUnchecked", MT_GET_LONG);
            case FLOAT -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getFloatUnchecked", MT_GET_FLOAT);
            case DOUBLE -> codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getDoubleUnchecked", MT_GET_DOUBLE);
        }
    }

    private static void emitLoadLocal(CodeBuilder codeBuilder, EmitContext context, Local local) {
        codeBuilder.loadLocal(localTypeKind(local.type()), localSlot(codeBuilder, context, local));
    }

    private static void emitStoreLocal(CodeBuilder codeBuilder, EmitContext context, Local local) {
        codeBuilder.storeLocal(localTypeKind(local.type()), localSlot(codeBuilder, context, local));
    }

    private static int localSlot(CodeBuilder codeBuilder, EmitContext context, Local local) {
        return context.locals().computeIfAbsent(local, ignored -> codeBuilder.allocateLocal(localTypeKind(local.type())));
    }

    private static TypeKind localTypeKind(LocalType type) {
        return switch (type) {
            case LocalType.Kind kind -> kind.kind();
            case LocalType.Reference _ -> TypeKind.REFERENCE;
        };
    }

    private static Local referenceLocal(String name) {
        return new Local(name, new LocalType.Kind(TypeKind.REFERENCE));
    }

    private static String typeFieldName(IrClassData data, NetworkBuffer.Type<?> type) {
        for (IrFieldData field : data.fields()) {
            if (field.ir().originalType() == type) return typeName(field.path());
        }
        throw new IllegalStateException("Missing type field for " + type);
    }

    private static String transformFunctionName(IrClassData data, Function<?, ?> function) {
        for (TransformFieldData transform : data.transforms()) {
            if (transform.function() == function) return transform.name();
        }
        throw new IllegalStateException("Missing transform function field");
    }

    private static ClassDesc classDesc(Class<?> type) {
        return type.describeConstable().orElseThrow();
    }

    private static void emitDirectBuffer(CodeBuilder codeBuilder, int directSlot) {
        codeBuilder.aload(1)
                .checkcast(CD_NETWORK_BUFFER_IMPL)
                .astore(directSlot);
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

    private static void emitUnboxedTypeIrValue(CodeBuilder codeBuilder, PrimitiveKind kind) {
        switch (kind) {
            case BOOLEAN -> codeBuilder.invokevirtual(CD_BOOLEAN_WRAPPER, "booleanValue", MT_BOOLEAN_VALUE);
            case BYTE -> codeBuilder.invokevirtual(CD_BYTE_WRAPPER, "byteValue", MT_BYTE_VALUE);
            case UNSIGNED_BYTE -> codeBuilder.invokevirtual(CD_SHORT_WRAPPER, "shortValue", MT_SHORT_VALUE)
                    .sipush(0xFF)
                    .iand();
            case SHORT -> codeBuilder.invokevirtual(CD_SHORT_WRAPPER, "shortValue", MT_SHORT_VALUE);
            case UNSIGNED_SHORT -> codeBuilder.invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE)
                    .loadConstant(0xFFFF)
                    .iand();
            case INT -> codeBuilder.invokevirtual(CD_INTEGER_WRAPPER, "intValue", MT_INT_VALUE);
            case UNSIGNED_INT -> codeBuilder.invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE)
                    .loadConstant(0xFFFFFFFFL)
                    .land();
            case LONG -> codeBuilder.invokevirtual(CD_LONG_WRAPPER, "longValue", MT_LONG_VALUE);
            case FLOAT -> codeBuilder.invokevirtual(CD_FLOAT_WRAPPER, "floatValue", MT_FLOAT_VALUE);
            case DOUBLE -> codeBuilder.invokevirtual(CD_DOUBLE_WRAPPER, "doubleValue", MT_DOUBLE_VALUE);
        }
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

    private static IrClassData irClassData(String path, List<Object> classData, NetworkIr<?> ir) {
        final List<? extends FieldIr<?, ?>> irFields = ir.fields();
        final List<IrFieldData> fields = new ArrayList<>(irFields.size());
        final List<TransformFieldData> transforms = new ArrayList<>();
        final IdentityHashMap<Function<?, ?>, Boolean> usedTransforms = new IdentityHashMap<>();
        collectApplyFunctions(ir.write(), usedTransforms);
        collectApplyFunctions(ir.read(), usedTransforms);
        for (int i = 0; i < irFields.size(); i++) {
            final String fieldPath = childPath(path, i);
            final FieldIr<?, ?> field = irFields.get(i);
            fields.add(new IrFieldData(field, fieldPath,
                    addClassData(classData, field.originalType()),
                    addClassData(classData, field.getter())));
            collectTransformFields(fieldPath, field.type(), usedTransforms, classData, transforms, 0);
        }
        return new IrClassData(ir, path, fields, List.copyOf(transforms), addClassData(classData, ir.constructor().object()));
    }

    private static void collectApplyFunctions(ProgramIr program, IdentityHashMap<Function<?, ?>, Boolean> functions) {
        for (Op op : program.ops()) {
            collectApplyFunctions(op, functions);
        }
    }

    private static void collectApplyFunctions(Op op, IdentityHashMap<Function<?, ?>, Boolean> functions) {
        switch (op) {
            case Op.Apply apply -> functions.put(apply.function(), Boolean.TRUE);
            case Op.If branch -> {
                branch.thenOps().forEach(child -> collectApplyFunctions(child, functions));
                branch.elseOps().forEach(child -> collectApplyFunctions(child, functions));
            }
            case Op.ForEach loop -> loop.body().forEach(child -> collectApplyFunctions(child, functions));
            case Op.ForIndex loop -> loop.body().forEach(child -> collectApplyFunctions(child, functions));
            default -> {
            }
        }
    }

    private static void collectTransformFields(String path, TypeIr<?> type,
                                               IdentityHashMap<Function<?, ?>, Boolean> usedTransforms,
                                               List<Object> classData, List<TransformFieldData> fields, int level) {
        switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                if (usedTransforms.containsKey(transform.from())) {
                    fields.add(new TransformFieldData(transformFromName(path, level), transform.from(), addClassData(classData, transform.from())));
                }
                if (usedTransforms.containsKey(transform.to())) {
                    fields.add(new TransformFieldData(transformToName(path, level), transform.to(), addClassData(classData, transform.to())));
                }
                collectTransformFields(path, transform.parent(), usedTransforms, classData, fields, level + 1);
            }
            case TypeIr.Optional<?> optional -> collectTransformFields(path, optional.parent(), usedTransforms, classData, fields, level);
            case TypeIr.Template<?> _, TypeIr.External<?> _, TypeIr.Constant<?> _, TypeIr.Primitive<?> _,
                 TypeIr.VarInt _, TypeIr.VarLong _, TypeIr.StringUtf8 _, TypeIr.ByteArray _, TypeIr.FixedBytes _,
                 TypeIr.ListType<?, ?> _, TypeIr.MapType<?, ?, ?> _ -> {
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends @UnknownNullability Object> NetworkIr<T> networkIr(String name, Object[] values, int fieldCount,
                                                                                 ConstructorIr<T> constructor) {
        final List<FieldIr<T, ?>> fields = new ArrayList<>(fieldCount);
        final FieldIr<T, ?>[] fieldArray = new FieldIr[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            final NetworkBuffer.Type<?> originalType = (NetworkBuffer.Type<?>) values[i * 2];
            final Function<? super T, ?> getter = (Function<? super T, ?>) values[i * 2 + 1];
            final FieldIr<T, ?> field = new FieldIr(i, "field" + i, originalType, typeIr(originalType), getter);
            fields.add(field);
            fieldArray[i] = field;
        }
        final ProgramIr write = writeProgram(fieldArray);
        final ProgramIr read = readProgram(fieldArray, constructor);
        return new NetworkIr<>(name, fields, constructor, write, read);
    }

    @SuppressWarnings({"rawtypes"})
    private static ProgramIr writeProgram(FieldIr<?, ?>[] fields) {
        final List<Op> writeOps = new ArrayList<>(fields.length * 2);
        final Local source = referenceLocal("value");
        for (int i = 0; i < fields.length; ) {
            final PrimitiveKind primitive = runPrimitive(fields[i].type());
            if (primitive != null) {
                final List<RunItem> items = new ArrayList<>();
                long offset = 0;
                do {
                    final PrimitiveKind kind = runPrimitive(fields[i].type());
                    final Local raw = referenceLocal("field" + i);
                    writeOps.add(new Op.GetField(fields[i], source, raw));
                    final Local normalized = normalizeWritePrimitive(writeOps, fields[i].type(), raw, i, 0);
                    items.add(new RunItem.Put(kind.storeKind(), new Value.Const(offset), new Value.LocalValue(normalized)));
                    offset += kind.storeKind().byteSize();
                    i++;
                } while (i < fields.length && runPrimitive(fields[i].type()) != null);
                writeOps.add(new Op.WriteRun(new RunIr(new Value.Const(offset), items)));
                continue;
            }
            if (fields[i].type() instanceof TypeIr.Constant _) {
                i++;
                continue;
            }
            if (isVarInt(fields[i].type())) {
                final Local raw = referenceLocal("field" + i);
                writeOps.add(new Op.GetField(fields[i], source, raw));
                final Local normalized = normalizeWriteVarInt(writeOps, fields[i].type(), raw, i, 0);
                final Value value = new Value.LocalValue(normalized);
                final Value encodedSize = new Value.VarIntSize(value);
                writeOps.add(new Op.WriteRun(new RunIr(encodedSize, List.of(new RunItem.PutVarInt(new Value.Const(0L), value, encodedSize)))));
                i++;
                continue;
            }
            if (isVarLong(fields[i].type())) {
                final Local raw = referenceLocal("field" + i);
                writeOps.add(new Op.GetField(fields[i], source, raw));
                final Local normalized = normalizeWriteVarLong(writeOps, fields[i].type(), raw, i, 0);
                writeOps.add(new Op.WriteVarLong(new Value.LocalValue(normalized)));
                i++;
                continue;
            }
            if (fixedBytesLength(fields[i].type()) >= 0) {
                final int length = fixedBytesLength(fields[i].type());
                final Local raw = referenceLocal("field" + i);
                writeOps.add(new Op.GetField(fields[i], source, raw));
                final Local bytes = materializeWriteReference(writeOps, fields[i].type(), TypeIr.FixedBytes.class, raw, i, 0, byte[].class);
                writeOps.add(new Op.WriteRun(new RunIr(
                        new Value.Const((long) length),
                        List.of(new RunItem.PutBytes(new Value.Const(0L), new Value.LocalValue(bytes), new Value.Const(length)))
                )));
                i++;
                continue;
            }
            if (isByteArray(fields[i].type()) || isStringUtf8(fields[i].type()) ||
                    fields[i].originalType() instanceof NetworkBufferTypeImpl.ListType<?> ||
                    fields[i].originalType() instanceof NetworkBufferTypeImpl.MapType<?, ?>) {
                final Local writeLocal = referenceLocal("field" + i);
                writeOps.add(new Op.GetField(fields[i], source, writeLocal));
                writeOps.add(new Op.WriteExternal(fields[i].originalType(), new Value.LocalValue(writeLocal)));
                i++;
                continue;
            }
            final Local writeLocal = referenceLocal("field" + i);
            writeOps.add(new Op.GetField(fields[i], source, writeLocal));
            writeOps.add(new Op.WriteExternal(fields[i].originalType(), new Value.LocalValue(writeLocal)));
            i++;
        }
        return new ProgramIr(writeOps);
    }

    private static ProgramIr readProgram(FieldIr<?, ?>[] fields, ConstructorIr<?> constructor) {
        final List<Op> readOps = new ArrayList<>(fields.length + 2);
        final List<Value> constructorArgs = new ArrayList<>(fields.length);
        final Value[] valuesOut = new Value[fields.length];
        for (int i = 0; i < fields.length; ) {
            final PrimitiveKind primitive = runPrimitive(fields[i].type());
            if (primitive != null) {
                final int start = i;
                final List<RunItem> items = new ArrayList<>();
                long offset = 0;
                do {
                    final PrimitiveKind kind = runPrimitive(fields[i].type());
                    final Local normalized = new Local("field" + i + "Value", new LocalType.Kind(kind.localKind()));
                    items.add(new RunItem.Get(kind.storeKind(), new Value.Const(offset), normalized));
                    offset += kind.storeKind().byteSize();
                    i++;
                } while (i < fields.length && runPrimitive(fields[i].type()) != null);
                readOps.add(new Op.ReadRun(new RunIr(new Value.Const(offset), items)));
                for (int j = start; j < i; j++) {
                    final PrimitiveKind kind = runPrimitive(fields[j].type());
                    final Local normalized = new Local("field" + j + "Value", new LocalType.Kind(kind.localKind()));
                    valuesOut[j] = new Value.LocalValue(materializeReadPrimitive(readOps, fields[j].type(), normalized, j, 0));
                }
                continue;
            }
            if (fields[i].type() instanceof TypeIr.Constant(Object value)) {
                valuesOut[i] = new Value.Const(value);
                i++;
                continue;
            }
            if (isVarInt(fields[i].type())) {
                final Local normalized = new Local("field" + i + "Value", new LocalType.Kind(TypeKind.INT));
                readOps.add(new Op.ReadVarInt(normalized));
                valuesOut[i] = new Value.LocalValue(materializeReadVarInt(readOps, fields[i].type(), normalized, i, 0));
                i++;
                continue;
            }
            if (isVarLong(fields[i].type())) {
                final Local normalized = new Local("field" + i + "Value", new LocalType.Kind(TypeKind.LONG));
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
            if (isByteArray(fields[i].type()) || isStringUtf8(fields[i].type()) ||
                    fields[i].originalType() instanceof NetworkBufferTypeImpl.ListType<?> ||
                    fields[i].originalType() instanceof NetworkBufferTypeImpl.MapType<?, ?>) {
                final Local readLocal = referenceLocal("field" + i);
                readOps.add(new Op.ReadExternal(fields[i].originalType(), readLocal));
                valuesOut[i] = new Value.LocalValue(readLocal);
                i++;
                continue;
            }
            final Local readLocal = referenceLocal("field" + i);
            readOps.add(new Op.ReadExternal(fields[i].originalType(), readLocal));
            valuesOut[i] = new Value.LocalValue(readLocal);
            i++;
        }
        constructorArgs.addAll(Arrays.asList(valuesOut));
        final Local result = referenceLocal("result");
        readOps.add(new Op.Construct(constructor, constructorArgs, result));
        readOps.add(new Op.Return(new Value.LocalValue(result)));
        return new ProgramIr(readOps);
    }

    private static Local normalizeWritePrimitive(List<Op> ops, TypeIr<?> type, Local in, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWritePrimitive(ops, transform.parent(), out, fieldIndex, depth + 1);
            }
            case TypeIr.Primitive<?> primitive -> {
                final PrimitiveKind kind = primitive.kind();
                final Local cast = referenceLocal("field" + fieldIndex + "Cast");
                final Local normalized = new Local("field" + fieldIndex + "Value", new LocalType.Kind(kind.localKind()));
                ops.add(new Op.Cast(in, kind.wrapperClass(), cast));
                ops.add(new Op.Unbox(kind, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not primitive run-compatible: " + type);
        };
    }

    private static Local materializeReadPrimitive(List<Op> ops, TypeIr<?> type, Local normalized, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadPrimitive(ops, transform.parent(), normalized, fieldIndex, depth + 1);
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.Primitive<?> primitive -> {
                final PrimitiveKind kind = primitive.kind();
                final Local boxed = referenceLocal("field" + fieldIndex);
                ops.add(new Op.Box(kind, normalized, boxed));
                yield boxed;
            }
            default -> throw new IllegalArgumentException("Type is not primitive run-compatible: " + type);
        };
    }

    private static Local normalizeWriteVarInt(List<Op> ops, TypeIr<?> type, Local in, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWriteVarInt(ops, transform.parent(), out, fieldIndex, depth + 1);
            }
            case TypeIr.VarInt _ -> {
                final Local cast = referenceLocal("field" + fieldIndex + "Cast");
                final Local normalized = new Local("field" + fieldIndex + "Value", new LocalType.Kind(TypeKind.INT));
                ops.add(new Op.Cast(in, Integer.class, cast));
                ops.add(new Op.Unbox(PrimitiveKind.INT, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not VarInt-compatible: " + type);
        };
    }

    private static Local materializeReadVarInt(List<Op> ops, TypeIr<?> type, Local normalized, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadVarInt(ops, transform.parent(), normalized, fieldIndex, depth + 1);
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.VarInt _ -> {
                final Local boxed = referenceLocal("field" + fieldIndex);
                ops.add(new Op.Box(PrimitiveKind.INT, normalized, boxed));
                yield boxed;
            }
            default -> throw new IllegalArgumentException("Type is not VarInt-compatible: " + type);
        };
    }

    private static boolean isVarInt(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.VarInt _ -> true;
            case TypeIr.Transform<?, ?> transform -> isVarInt(transform.parent());
            default -> false;
        };
    }

    private static Local normalizeWriteVarLong(List<Op> ops, TypeIr<?> type, Local in, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.from(), in, out));
                yield normalizeWriteVarLong(ops, transform.parent(), out, fieldIndex, depth + 1);
            }
            case TypeIr.VarLong _ -> {
                final Local cast = referenceLocal("field" + fieldIndex + "Cast");
                final Local normalized = new Local("field" + fieldIndex + "Value", new LocalType.Kind(TypeKind.LONG));
                ops.add(new Op.Cast(in, Long.class, cast));
                ops.add(new Op.Unbox(PrimitiveKind.LONG, cast, normalized));
                yield normalized;
            }
            default -> throw new IllegalArgumentException("Type is not VarLong-compatible: " + type);
        };
    }

    private static Local materializeReadVarLong(List<Op> ops, TypeIr<?> type, Local normalized, int fieldIndex, int depth) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadVarLong(ops, transform.parent(), normalized, fieldIndex, depth + 1);
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
                ops.add(new Op.Apply(transform.to(), parent, out));
                yield out;
            }
            case TypeIr.VarLong _ -> {
                final Local boxed = referenceLocal("field" + fieldIndex);
                ops.add(new Op.Box(PrimitiveKind.LONG, normalized, boxed));
                yield boxed;
            }
            default -> throw new IllegalArgumentException("Type is not VarLong-compatible: " + type);
        };
    }

    private static boolean isVarLong(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.VarLong _ -> true;
            case TypeIr.Transform<?, ?> transform -> isVarLong(transform.parent());
            default -> false;
        };
    }

    private static Local materializeWriteReference(List<Op> ops, TypeIr<?> type, Class<?> targetType, Local in,
                                                   int fieldIndex, int depth, Class<?> targetClass) {
        return switch (type) {
            case TypeIr.Transform<?, ?> transform -> {
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
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
            case TypeIr.Transform<?, ?> transform -> {
                final Local parent = materializeReadReference(ops, transform.parent(), targetType, in, fieldIndex, depth + 1);
                final Local out = referenceLocal("field" + fieldIndex + "Transform" + depth);
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
            case TypeIr.Transform<?, ?> transform -> fixedBytesLength(transform.parent());
            default -> -1;
        };
    }

    private static boolean isByteArray(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.ByteArray _ -> true;
            case TypeIr.Transform<?, ?> transform -> isByteArray(transform.parent());
            default -> false;
        };
    }

    private static boolean isStringUtf8(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.StringUtf8 _ -> true;
            case TypeIr.Transform<?, ?> transform -> isStringUtf8(transform.parent());
            default -> false;
        };
    }

    private static @Nullable PrimitiveKind runPrimitive(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.Primitive<?> primitive -> primitive.kind();
            case TypeIr.Transform<?, ?> transform -> runPrimitive(transform.parent());
            default -> null;
        };
    }

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
                case NetworkBufferTypeImpl.UnitType _ -> new TypeIr.Constant<>(Unit.INSTANCE);
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
            return new LinkedHashMap<>(size);
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

    private static int addClassData(List<Object> classData, Object value) {
        final int index = classData.size();
        classData.add(value);
        return index;
    }

    private static ClassDesc constructorInterface(int fieldCount) {
        return ClassDesc.of(PACKAGE, "NetworkBufferTemplate$F" + fieldCount);
    }

    private static MethodTypeDesc constructorApplyType(int fieldCount) {
        ClassDesc[] parameters = new ClassDesc[fieldCount];
        Arrays.fill(parameters, CD_OBJECT);
        return MethodTypeDesc.of(CD_OBJECT, parameters);
    }

    record IrClassData(NetworkIr<?> ir, String path, List<IrFieldData> fields, List<TransformFieldData> transforms,
                       int ctorDataIndex) {
        public IrClassData {
            fields = List.copyOf(fields);
            transforms = List.copyOf(transforms);
        }
    }

    record IrFieldData(FieldIr<?, ?> ir, String path, int typeDataIndex, int getterDataIndex) {
    }

    record TransformFieldData(String name, Function<?, ?> function, int dataIndex) {
    }

    record EmitContext(ClassDesc classDesc, IrClassData data, int directSlot, Map<Local, Integer> locals) {
    }

    private static CodeBuilder loadClassDataAt(CodeBuilder codeBuilder, ClassDesc type, int index) {
        return codeBuilder.aload(0) // assumes lookup is at slot 0
                .ldc("_")
                .ldc(type)
                .loadConstant(index)
                .invokestatic(CD_METHOD_HANDLES, "classDataAt", MT_CLASS_DATA_AT)
                .checkcast(type);
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
