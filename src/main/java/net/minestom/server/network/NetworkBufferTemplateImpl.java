package net.minestom.server.network;

import net.minestom.server.utils.Unit;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
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
    static final ClassDesc CD_STANDARD_CHARSETS = ClassDesc.of("java.nio.charset.StandardCharsets");
    static final ClassDesc CD_CHARSET = ClassDesc.of("java.nio.charset.Charset");
    static final ClassDesc CD_LIST = ClassDesc.of("java.util.List");
    static final ClassDesc CD_COLLECTION = ClassDesc.of("java.util.Collection");
    static final ClassDesc CD_ITERABLE = ClassDesc.of("java.lang.Iterable");
    static final ClassDesc CD_ITERATOR = ClassDesc.of("java.util.Iterator");
    static final ClassDesc CD_MAP = ClassDesc.of("java.util.Map");
    static final ClassDesc CD_MAP_ENTRY = CD_MAP.nested("Entry");
    static final ClassDesc CD_SET = ClassDesc.of("java.util.Set");
    static final ClassDesc CD_COLLECTION_FACTORY = ClassDesc.of(PACKAGE, "CollectionFactory");
    static final ClassDesc CD_MAP_FACTORY = ClassDesc.of(PACKAGE, "MapFactory");

    static final MethodTypeDesc MT_VOID = MethodTypeDesc.of(CD_VOID);
    static final MethodTypeDesc MT_LOOKUP = MethodTypeDesc.of(CD_METHOD_HANDLES_LOOKUP);
    static final MethodTypeDesc MT_CLASS_DATA_AT = MethodTypeDesc.of(CD_OBJECT, CD_METHOD_HANDLES_LOOKUP, CD_STRING, CD_CLASS, CD_INT);
    static final MethodTypeDesc MT_READ_OBJECT = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER);
    static final MethodTypeDesc MT_WRITE_OBJECT = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_OBJECT);
    static final MethodTypeDesc MT_NETWORK_IR = MethodTypeDesc.of(CD_NETWORK_IR);
    static final MethodTypeDesc MT_RESERVE = MethodTypeDesc.of(CD_LONG, CD_LONG);
    static final MethodTypeDesc MT_FUNCTION_APPLY = MethodTypeDesc.of(CD_OBJECT, CD_OBJECT);
    static final MethodTypeDesc MT_STRING_GET_BYTES_CHARSET = MethodTypeDesc.of(CD_BYTE_ARRAY, CD_CHARSET);
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

    static final String TYPE_PREFIX = "t";
    static final String GETTER_PREFIX = "g";
    static final String TRANSFORM_TO_PREFIX = "to";
    static final String TRANSFORM_FROM_PREFIX = "from";
    static final String FACTORY_PREFIX = "fac";
    static final String CTOR_NAME = "ctor";
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
            final IrClassData irData = collectIrClassData(classData, ir);
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
            final MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, List.copyOf(classData), true);
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
        for (ExternalTypeFieldData external : data.externalTypes()) {
            classBuilder.withField(external.name(), CD_TYPE, FIELD_FLAGS);
        }
        for (TransformFieldData transform : data.transforms()) {
            classBuilder.withField(transform.name(), CD_FUNCTION, FIELD_FLAGS);
        }
        for (FactoryFieldData factory : data.factories()) {
            final boolean isMap = factory.factory() instanceof MapFactory;
            classBuilder.withField(factory.name(), isMap ? CD_MAP_FACTORY : CD_COLLECTION_FACTORY, FIELD_FLAGS);
        }
        for (Map.Entry<String, Integer> entry : data.constructors().entrySet()) {
            final int fieldCount = data.constructorIr(entry.getKey()).fieldCount();
            classBuilder.withField(entry.getKey(), constructorInterface(fieldCount), FIELD_FLAGS);
        }
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
        for (ExternalTypeFieldData external : data.externalTypes()) {
            loadClassDataAt(codeBuilder, CD_TYPE, external.dataIndex())
                    .putstatic(classDesc, external.name(), CD_TYPE);
        }
        for (TransformFieldData transform : data.transforms()) {
            loadClassDataAt(codeBuilder, CD_FUNCTION, transform.dataIndex())
                    .putstatic(classDesc, transform.name(), CD_FUNCTION);
        }
        for (FactoryFieldData factory : data.factories()) {
            final boolean isMap = factory.factory() instanceof MapFactory;
            final ClassDesc factoryType = isMap ? CD_MAP_FACTORY : CD_COLLECTION_FACTORY;
            loadClassDataAt(codeBuilder, factoryType, factory.dataIndex())
                    .putstatic(classDesc, factory.name(), factoryType);
        }
        for (Map.Entry<String, Integer> entry : data.constructors().entrySet()) {
            final int fieldCount = data.constructorIr(entry.getKey()).fieldCount();
            final ClassDesc constructorType = constructorInterface(fieldCount);
            loadClassDataAt(codeBuilder, constructorType, entry.getValue())
                    .putstatic(classDesc, entry.getKey(), constructorType);
        }
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
        for (Op programOp : program.ops()) {
            emitOp(codeBuilder, context, programOp);
        }
    }

    private static void emitOp(CodeBuilder codeBuilder, EmitContext context, Op op) {
        switch (op) {
            case Op.GetField getField -> {
                codeBuilder.getstatic(context.classDesc(), getterName(getField.path()), CD_FUNCTION);
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
            case Op.Store store -> {
                emitValue(codeBuilder, context, store.value());
                emitStoreLocal(codeBuilder, context, store.out());
            }
            case Op.Check check -> {
                final Label endLabel = codeBuilder.newLabel();
                emitValue(codeBuilder, context, check.condition());
                codeBuilder.ifne(endLabel);
                codeBuilder.new_(classDesc(IllegalArgumentException.class))
                        .dup()
                        .ldc(check.message())
                        .invokespecial(classDesc(IllegalArgumentException.class), ConstantDescs.INIT_NAME, MethodTypeDesc.of(CD_VOID, CD_STRING))
                        .athrow()
                        .labelBinding(endLabel);
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
            case Op.If branch -> {
                final Label elseLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                emitValue(codeBuilder, context, branch.condition());
                codeBuilder.ifeq(elseLabel);
                for (Op childOp : branch.thenOps()) emitOp(codeBuilder, context, childOp);
                codeBuilder.goto_(endLabel);
                codeBuilder.labelBinding(elseLabel);
                for (Op childOp : branch.elseOps()) emitOp(codeBuilder, context, childOp);
                codeBuilder.labelBinding(endLabel);
            }
            case Op.ForEach loop -> {
                final Label startLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();

                emitValue(codeBuilder, context, loop.source());
                codeBuilder.checkcast(CD_ITERABLE);
                codeBuilder.invokeinterface(CD_ITERABLE, "iterator", MethodTypeDesc.of(CD_ITERATOR));
                final int iteratorSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                codeBuilder.astore(iteratorSlot);

                codeBuilder.labelBinding(startLabel);
                codeBuilder.aload(iteratorSlot);
                codeBuilder.invokeinterface(CD_ITERATOR, "hasNext", MethodTypeDesc.of(ConstantDescs.CD_boolean));
                codeBuilder.ifeq(endLabel);

                codeBuilder.aload(iteratorSlot);
                codeBuilder.invokeinterface(CD_ITERATOR, "next", MethodTypeDesc.of(CD_OBJECT));
                emitStoreLocal(codeBuilder, context, loop.element());

                for (Op programOp : loop.body()) emitOp(codeBuilder, context, programOp);

                codeBuilder.goto_(startLabel);
                codeBuilder.labelBinding(endLabel);
            }
            case Op.ForIndex loop -> {
                final Label startLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                final int loopIndexSlot = localSlot(codeBuilder, context, loop.index());

                emitIntValue(codeBuilder, context, loop.start());
                codeBuilder.istore(loopIndexSlot);

                codeBuilder.labelBinding(startLabel);
                codeBuilder.iload(loopIndexSlot);
                emitIntValue(codeBuilder, context, loop.end());
                codeBuilder.if_icmpge(endLabel);

                for (Op programOp : loop.body()) emitOp(codeBuilder, context, programOp);

                codeBuilder.iinc(loopIndexSlot, 1);
                codeBuilder.goto_(startLabel);
                codeBuilder.labelBinding(endLabel);
            }
            case Op.ElementAt elementAt -> {
                emitValue(codeBuilder, context, elementAt.source());
                codeBuilder.checkcast(CD_LIST);
                emitIntValue(codeBuilder, context, elementAt.index());
                codeBuilder.invokeinterface(CD_LIST, "get", MethodTypeDesc.of(CD_OBJECT, CD_INT));
                emitStoreLocal(codeBuilder, context, elementAt.out());
            }
            case Op.MapEntrySet mapEntrySet -> {
                emitValue(codeBuilder, context, mapEntrySet.map());
                codeBuilder.checkcast(CD_MAP);
                codeBuilder.invokeinterface(CD_MAP, "entrySet", MethodTypeDesc.of(CD_SET));
                emitStoreLocal(codeBuilder, context, mapEntrySet.out());
            }
            case Op.CollectionAdd collectionAdd -> {
                codeBuilder.getstatic(context.classDesc(), factoryName(collectionAdd.path()), CD_COLLECTION_FACTORY);
                emitLoadLocal(codeBuilder, context, collectionAdd.collection());
                emitValue(codeBuilder, context, collectionAdd.value());
                codeBuilder.invokeinterface(CD_COLLECTION_FACTORY, "add", MethodTypeDesc.of(CD_VOID, CD_OBJECT, CD_OBJECT));
            }
            case Op.MapPut mapPut -> {
                codeBuilder.getstatic(context.classDesc(), factoryName(mapPut.path()), CD_MAP_FACTORY);
                emitLoadLocal(codeBuilder, context, mapPut.map());
                emitValue(codeBuilder, context, mapPut.key());
                emitValue(codeBuilder, context, mapPut.value());
                codeBuilder.invokeinterface(CD_MAP_FACTORY, "put", MethodTypeDesc.of(CD_VOID, CD_OBJECT, CD_OBJECT, CD_OBJECT));
            }
            case Op.CollectionCreate collectionCreate -> {
                codeBuilder.getstatic(context.classDesc(), factoryName(collectionCreate.path()), CD_COLLECTION_FACTORY);
                emitIntValue(codeBuilder, context, collectionCreate.size());
                codeBuilder.invokeinterface(CD_COLLECTION_FACTORY, "create", MethodTypeDesc.of(CD_OBJECT, CD_INT));
                emitStoreLocal(codeBuilder, context, collectionCreate.out());
            }
            case Op.CollectionFinish collectionFinish -> {
                codeBuilder.getstatic(context.classDesc(), factoryName(collectionFinish.path()), CD_COLLECTION_FACTORY);
                emitLoadLocal(codeBuilder, context, collectionFinish.collection());
                codeBuilder.invokeinterface(CD_COLLECTION_FACTORY, "finish", MethodTypeDesc.of(CD_OBJECT, CD_OBJECT));
                emitStoreLocal(codeBuilder, context, collectionFinish.out());
            }
            case Op.MapCreate mapCreate -> {
                codeBuilder.getstatic(context.classDesc(), factoryName(mapCreate.path()), CD_MAP_FACTORY);
                emitIntValue(codeBuilder, context, mapCreate.size());
                codeBuilder.invokeinterface(CD_MAP_FACTORY, "create", MethodTypeDesc.of(CD_OBJECT, CD_INT));
                emitStoreLocal(codeBuilder, context, mapCreate.out());
            }
            case Op.MapFinish mapFinish -> {
                codeBuilder.getstatic(context.classDesc(), factoryName(mapFinish.path()), CD_MAP_FACTORY);
                emitLoadLocal(codeBuilder, context, mapFinish.map());
                codeBuilder.invokeinterface(CD_MAP_FACTORY, "finish", MethodTypeDesc.of(CD_OBJECT, CD_OBJECT));
                emitStoreLocal(codeBuilder, context, mapFinish.out());
            }
            case Op.MapEntryKey mapEntryKey -> {
                emitLoadLocal(codeBuilder, context, mapEntryKey.entry());
                codeBuilder.checkcast(CD_MAP_ENTRY);
                codeBuilder.invokeinterface(CD_MAP_ENTRY, "getKey", MethodTypeDesc.of(CD_OBJECT));
                emitStoreLocal(codeBuilder, context, mapEntryKey.out());
            }
            case Op.MapEntryValue mapEntryValue -> {
                emitLoadLocal(codeBuilder, context, mapEntryValue.entry());
                codeBuilder.checkcast(CD_MAP_ENTRY);
                codeBuilder.invokeinterface(CD_MAP_ENTRY, "getValue", MethodTypeDesc.of(CD_OBJECT));
                emitStoreLocal(codeBuilder, context, mapEntryValue.out());
            }
            case Op.Construct construct -> {
                final ClassDesc constructorType = constructorInterface(construct.constructor().fieldCount());
                codeBuilder.getstatic(context.classDesc(), ctorName(construct.path()), constructorType);
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
            case Value.IsNull isNull -> {
                emitValue(codeBuilder, context, isNull.value());
                final Label isNullLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                codeBuilder.ifnull(isNullLabel);
                codeBuilder.iconst_0();
                codeBuilder.goto_(endLabel);
                codeBuilder.labelBinding(isNullLabel);
                codeBuilder.iconst_1();
                codeBuilder.labelBinding(endLabel);
            }
            case Value.IsNotNull isNotNull -> {
                emitValue(codeBuilder, context, isNotNull.value());
                final Label isNotNullLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                codeBuilder.ifnonnull(isNotNullLabel);
                codeBuilder.iconst_0();
                codeBuilder.goto_(endLabel);
                codeBuilder.labelBinding(isNotNullLabel);
                codeBuilder.iconst_1();
                codeBuilder.labelBinding(endLabel);
            }
            case Value.Not not -> {
                emitValue(codeBuilder, context, not.value());
                final Label isZero = codeBuilder.newLabel();
                final Label end = codeBuilder.newLabel();
                codeBuilder.ifeq(isZero);
                codeBuilder.iconst_0();
                codeBuilder.goto_(end);
                codeBuilder.labelBinding(isZero);
                codeBuilder.iconst_1();
                codeBuilder.labelBinding(end);
            }
            case Value.BoolByte boolByte -> emitValue(codeBuilder, context, boolByte.booleanValue());
            case Value.UnsignedByte unsignedByte -> {
                emitValue(codeBuilder, context, unsignedByte.byteValue());
                codeBuilder.sipush(0xFF)
                        .iand();
            }
            case Value.LessThanOrEqual lessThanOrEqual -> {
                emitLongValue(codeBuilder, context, lessThanOrEqual.left());
                emitLongValue(codeBuilder, context, lessThanOrEqual.right());
                final Label trueLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                codeBuilder.lcmp()
                        .ifgt(trueLabel)
                        .iconst_1()
                        .goto_(endLabel)
                        .labelBinding(trueLabel)
                        .iconst_0()
                        .labelBinding(endLabel);
            }
            case Value.GreaterThan greaterThan -> {
                emitLongValue(codeBuilder, context, greaterThan.left());
                emitLongValue(codeBuilder, context, greaterThan.right());
                final Label trueLabel = codeBuilder.newLabel();
                final Label endLabel = codeBuilder.newLabel();
                codeBuilder.lcmp()
                        .ifgt(trueLabel)
                        .iconst_1()
                        .goto_(endLabel)
                        .labelBinding(trueLabel)
                        .iconst_0()
                        .labelBinding(endLabel);
            }
            case Value.ArrayLength arrayLength -> {
                emitValue(codeBuilder, context, arrayLength.array());
                codeBuilder.arraylength();
            }
            case Value.CollectionSize collectionSize -> {
                emitValue(codeBuilder, context, collectionSize.collection());
                codeBuilder.checkcast(CD_COLLECTION);
                codeBuilder.invokeinterface(CD_COLLECTION, "size", MethodTypeDesc.of(CD_INT));
            }
            case Value.MapSize mapSize -> {
                emitValue(codeBuilder, context, mapSize.map());
                codeBuilder.checkcast(CD_MAP);
                codeBuilder.invokeinterface(CD_MAP, "size", MethodTypeDesc.of(CD_INT));
            }
            case Value.StringUtf8Bytes stringUtf8Bytes -> {
                emitValue(codeBuilder, context, stringUtf8Bytes.string());
                codeBuilder.getstatic(CD_STANDARD_CHARSETS, "UTF_8", CD_CHARSET);
                codeBuilder.invokevirtual(CD_STRING, "getBytes", MT_STRING_GET_BYTES_CHARSET);
                codeBuilder.arraylength();
            }
            case Value.VarIntSize varIntSize -> {
                emitIntValue(codeBuilder, context, varIntSize.intValue());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varIntSize", MT_VAR_INT_SIZE, true);
            }
            case Value.Add add -> {
                emitLongValue(codeBuilder, context, add.left());
                emitLongValue(codeBuilder, context, add.right());
                codeBuilder.ladd();
            }
            case Value.Mul mul -> {
                emitLongValue(codeBuilder, context, mul.left());
                emitLongValue(codeBuilder, context, mul.right());
                codeBuilder.lmul();
            }
            default -> throw new UnsupportedOperationException("Unsupported IR value: " + value);
        }
    }

    private static void emitIntValue(CodeBuilder codeBuilder, EmitContext context, Value value) {
        switch (value) {
            case Value.LocalValue localValue -> {
                emitLoadLocal(codeBuilder, context, localValue.local());
                if (localValue.local().type() instanceof LocalType.Kind kind && kind.kind() == TypeKind.LONG) {
                    codeBuilder.l2i();
                }
            }
            case Value.Const constant -> codeBuilder.loadConstant(((Number) constant.value()).intValue());
            case Value.ArrayLength arrayLength -> {
                emitValue(codeBuilder, context, arrayLength.array());
                codeBuilder.arraylength();
            }
            case Value.VarIntSize varIntSize -> {
                emitIntValue(codeBuilder, context, varIntSize.intValue());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varIntSize", MT_VAR_INT_SIZE, true);
            }
            case Value.Add add -> {
                emitLongValue(codeBuilder, context, add.left());
                emitLongValue(codeBuilder, context, add.right());
                codeBuilder.ladd();
                codeBuilder.l2i();
            }
            case Value.Mul mul -> {
                emitLongValue(codeBuilder, context, mul.left());
                emitLongValue(codeBuilder, context, mul.right());
                codeBuilder.lmul();
                codeBuilder.l2i();
            }
            default -> emitValue(codeBuilder, context, value);
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
            case Value.ArrayLength arrayLength -> {
                emitIntValue(codeBuilder, context, arrayLength);
                codeBuilder.i2l();
            }
            case Value.VarIntSize varIntSize -> {
                emitIntValue(codeBuilder, context, varIntSize);
                codeBuilder.i2l();
            }
            case Value.Add add -> {
                emitLongValue(codeBuilder, context, add.left());
                emitLongValue(codeBuilder, context, add.right());
                codeBuilder.ladd();
            }
            default -> {
                emitValue(codeBuilder, context, value);
                codeBuilder.i2l();
            }
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
        for (ExternalTypeFieldData external : data.externalTypes()) {
            if (external.type() == type) return external.name();
        }
        throw new IllegalStateException("Missing type field for " + type);
    }

    private static String transformFunctionName(IrClassData data, Function<?, ?> function) {
        for (TransformFieldData transform : data.transforms()) {
            if (transform.function() == function) return transform.name();
        }
        throw new IllegalStateException("Missing transform function field");
    }

    private static String ctorName(IrClassData data, ConstructorIr<?> constructor) {
        for (Map.Entry<String, Integer> entry : data.constructors().entrySet()) {
            if (data.constructorIr(entry.getKey()).object() == constructor.object()) return entry.getKey();
        }
        throw new IllegalStateException("Missing constructor field");
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

    private static IrClassData collectIrClassData(List<Object> classData, NetworkIr<?> ir) {
        final List<IrFieldData> fields = new ArrayList<>();
        final List<TransformFieldData> transforms = new ArrayList<>();
        final List<FactoryFieldData> factories = new ArrayList<>();
        final List<ExternalTypeFieldData> externalTypes = new ArrayList<>();
        final Map<String, Integer> constructors = new LinkedHashMap<>();
        final Map<String, ConstructorIr<?>> constructorIrs = new HashMap<>();
        final IdentityHashMap<Function<?, ?>, Boolean> usedTransforms = new IdentityHashMap<>();
        collectApplyFunctions(ir.write(), usedTransforms);
        collectApplyFunctions(ir.read(), usedTransforms);
        collectIrMetadata("", ir, classData, fields, transforms, constructors, constructorIrs, factories, usedTransforms);

        // Add standalone transforms that were not found in TypeIr.Transform
        int standaloneIndex = 0;
        for (Function<?, ?> function : usedTransforms.keySet()) {
            boolean alreadyAdded = false;
            for (TransformFieldData t : transforms) {
                if (t.function() == function) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                transforms.add(new TransformFieldData("fn" + standaloneIndex++, function, addClassData(classData, function)));
            }
        }

        // Add standalone types used in WriteExternal/ReadExternal
        final IdentityHashMap<NetworkBuffer.Type<?>, Boolean> usedExternalTypes = new IdentityHashMap<>();
        collectExternalTypes(ir.write(), usedExternalTypes);
        collectExternalTypes(ir.read(), usedExternalTypes);
        int extIndex = 0;
        for (NetworkBuffer.Type<?> type : usedExternalTypes.keySet()) {
            boolean alreadyAdded = false;
            for (IrFieldData field : fields) {
                if (field.ir().originalType() == type) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                externalTypes.add(new ExternalTypeFieldData("ext" + extIndex++, type, addClassData(classData, type)));
            }
        }

        return new IrClassData(ir, "", fields, transforms, constructors, constructorIrs, factories, externalTypes);
    }

    private static void collectExternalTypes(ProgramIr program, IdentityHashMap<NetworkBuffer.Type<?>, Boolean> types) {
        for (Op op : program.ops()) {
            collectExternalTypes(op, types);
        }
    }

    private static void collectExternalTypes(Op op, IdentityHashMap<NetworkBuffer.Type<?>, Boolean> types) {
        switch (op) {
            case Op.WriteExternal write -> types.put(write.type(), Boolean.TRUE);
            case Op.ReadExternal read -> types.put(read.type(), Boolean.TRUE);
            case Op.If ifOp -> {
                collectExternalTypes(new ProgramIr(ifOp.thenOps()), types);
                collectExternalTypes(new ProgramIr(ifOp.elseOps()), types);
            }
            case Op.ForEach forEach -> collectExternalTypes(new ProgramIr(forEach.body()), types);
            case Op.ForIndex forIndex -> collectExternalTypes(new ProgramIr(forIndex.body()), types);
            default -> {
            }
        }
    }

    private static void collectIrMetadata(String path, NetworkIr<?> ir, List<Object> classData,
                                          List<IrFieldData> allFields, List<TransformFieldData> allTransforms,
                                          Map<String, Integer> allConstructors, Map<String, ConstructorIr<?>> allConstructorIrs,
                                          List<FactoryFieldData> allFactories,
                                          IdentityHashMap<Function<?, ?>, Boolean> usedTransforms) {
        final String ctorName = ctorName(path);
        allConstructors.put(ctorName, addClassData(classData, ir.constructor().object()));
        allConstructorIrs.put(ctorName, ir.constructor());

        final List<? extends FieldIr<?, ?>> irFields = ir.fields();
        for (int i = 0; i < irFields.size(); i++) {
            final FieldIr<?, ?> field = irFields.get(i);
            final String fieldPath = childPath(path, i);
            allFields.add(new IrFieldData(field, fieldPath,
                    addClassData(classData, field.originalType()),
                    addClassData(classData, field.getter())));
            collectTypeMetadata(fieldPath, field.type(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
        }
    }

    private static void collectTypeMetadata(String path, TypeIr<?> type,
                                            IdentityHashMap<Function<?, ?>, Boolean> usedTransforms,
                                            List<Object> classData, List<IrFieldData> allFields,
                                            List<TransformFieldData> allTransforms,
                                            Map<String, Integer> allConstructors, Map<String, ConstructorIr<?>> allConstructorIrs,
                                            List<FactoryFieldData> allFactories) {
        switch (type) {
            case TypeIr.Template<?> template ->
                    collectIrMetadata(path, template.ir(), classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories, usedTransforms);
            case TypeIr.Transform<?, ?> transform -> {
                if (usedTransforms.containsKey(transform.from())) {
                    allTransforms.add(new TransformFieldData(transformFromName(path, 0), transform.from(), addClassData(classData, transform.from())));
                }
                if (usedTransforms.containsKey(transform.to())) {
                    allTransforms.add(new TransformFieldData(transformToName(path, 0), transform.to(), addClassData(classData, transform.to())));
                }
                collectTypeMetadata(path + "X", transform.parent(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            }
            case TypeIr.Optional<?> optional ->
                    collectTypeMetadata(path + "Opt", optional.parent(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            case TypeIr.ListType<?, ?> list -> {
                allFactories.add(new FactoryFieldData(factoryName(path), list.factory(), addClassData(classData, list.factory())));
                collectTypeMetadata(path + "E", list.element(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            }
            case TypeIr.MapType<?, ?, ?> map -> {
                allFactories.add(new FactoryFieldData(factoryName(path), map.factory(), addClassData(classData, map.factory())));
                collectTypeMetadata(path + "K", map.key(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
                collectTypeMetadata(path + "V", map.value(), usedTransforms, classData, allFields, allTransforms, allConstructors, allConstructorIrs, allFactories);
            }
            default -> {
            }
        }
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
            case Op.WriteRun writeRun -> collectApplyFunctions(writeRun.run(), functions);
            case Op.ReadRun readRun -> collectApplyFunctions(readRun.run(), functions);
            default -> {
            }
        }
    }

    private static void collectApplyFunctions(RunIr run, IdentityHashMap<Function<?, ?>, Boolean> functions) {
        for (RunItem item : run.items()) {
            if (item instanceof RunItem.ForIndex loop) {
                loop.body().forEach(step -> collectApplyFunctions(step, functions));
            }
        }
    }

    private static void collectApplyFunctions(RunStep step, IdentityHashMap<Function<?, ?>, Boolean> functions) {
        switch (step) {
            case RunStep.Apply apply -> functions.put(apply.function(), Boolean.TRUE);
            case RunStep.Construct construct -> {
                // Constructor is not a Function<?, ?>, so skip.
            }
            default -> {
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

    private static ProgramIr writeProgram(FieldIr<?, ?>[] fields) {
        final List<Op> writeOps = new ArrayList<>();
        final Local source = referenceLocal("value");
        for (int i = 0; i < fields.length; i++) {
            lowerWrite(writeOps, fields[i].type(), fields[i], source, Integer.toString(i + 1), i, 0);
        }
        return new ProgramIr(mergeWriteRuns(writeOps));
    }

    private static void lowerWrite(List<Op> ops, TypeIr<?> type, @Nullable FieldIr<?, ?> field, Local source, String path, int fieldIndex, int depth) {
        if (type instanceof TypeIr.Template<?> template) {
            final Local nested;
            if (field != null) {
                nested = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, nested));
            } else {
                nested = source;
            }
            for (int i = 0; i < template.ir().fields().size(); i++) {
                final FieldIr<?, ?> subField = template.ir().fields().get(i);
                lowerWrite(ops, subField.type(), subField, nested, path + "_" + (i + 1), i, depth + 1);
            }
            return;
        }

        if (type instanceof TypeIr.Transform<?, ?> transform) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local parentValue = referenceLocal("path" + path + "From");
            ops.add(new Op.Apply(transform.from(), raw, parentValue));
            lowerWrite(ops, transform.parent(), null, parentValue, path + "X", -1, depth + 1);
            return;
        }

        if (type instanceof TypeIr.Optional<?> optional) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }

            final Value present = new Value.IsNotNull(new Value.LocalValue(raw));
            ops.add(new Op.WriteRun(new RunIr(new Value.Const(1L),
                    List.of(new RunItem.Put(StoreKind.BOOLEAN, new Value.Const(0L), new Value.BoolByte(present))))));

            final List<Op> thenOps = new ArrayList<>();
            lowerWrite(thenOps, optional.parent(), null, raw, path + "Opt", -1, depth + 1);
            ops.add(new Op.If(present, thenOps, List.of()));
            return;
        }

        if (type instanceof TypeIr.ListType<?, ?> listType) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Value size = new Value.CollectionSize(new Value.LocalValue(raw));
            ops.add(new Op.Check(new Value.LessThanOrEqual(size, new Value.Const((long) listType.maxLength())), "Collection too large"));

            ops.add(new Op.WriteRun(new RunIr(new Value.VarIntSize(size),
                    List.of(new RunItem.PutVarInt(new Value.Const(0L), size, new Value.VarIntSize(size))))));

            final Local index = new Local("path" + path + "Idx", new LocalType.Kind(TypeKind.INT));
            final Local element = referenceLocal("path" + path + "Elem");
            final List<Op> body = new ArrayList<>();
            body.add(new Op.ElementAt(new Value.LocalValue(raw), new Value.LocalValue(index), element));
            lowerWrite(body, listType.element(), null, element, path + "E", -1, depth + 1);

            ops.add(new Op.ForIndex(index, new Value.Const(0), size, body));
            return;
        }

        if (type instanceof TypeIr.MapType<?, ?, ?> mapType) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Value size = new Value.MapSize(new Value.LocalValue(raw));
            ops.add(new Op.Check(new Value.LessThanOrEqual(size, new Value.Const((long) mapType.maxLength())), "Map too large"));

            ops.add(new Op.WriteRun(new RunIr(new Value.VarIntSize(size),
                    List.of(new RunItem.PutVarInt(new Value.Const(0L), size, new Value.VarIntSize(size))))));

            final Local entrySet = referenceLocal("path" + path + "Entries");
            ops.add(new Op.MapEntrySet(new Value.LocalValue(raw), entrySet));

            final Local entry = referenceLocal("path" + path + "Entry");
            final Local key = referenceLocal("path" + path + "Key");
            final Local value = referenceLocal("path" + path + "Value");
            final List<Op> body = new ArrayList<>();
            body.add(new Op.MapEntryKey(entry, key));
            body.add(new Op.MapEntryValue(entry, value));
            lowerWrite(body, mapType.key(), null, key, path + "K", -1, depth + 1);
            lowerWrite(body, mapType.value(), null, value, path + "V", -1, depth + 1);

            ops.add(new Op.ForEach(new Value.LocalValue(entrySet), entry, body));
            return;
        }

        if (type instanceof TypeIr.Constant<?> constant) {
            final Object value = constant.value();
            if (value == net.minestom.server.utils.Unit.INSTANCE) return;
            final PrimitiveKind primitive = constantPrimitive(value);
            if (primitive != null) {
                ops.add(new Op.WriteRun(new RunIr(new Value.Const((long) primitive.storeKind().byteSize()),
                        List.of(new RunItem.Put(primitive.storeKind(), new Value.Const(0L), new Value.Const(value))))));
            } else {
                ops.add(new Op.WriteExternal(field != null ? field.originalType() : ((TypeIr.External<?>) type).type(), new Value.Const(value)));
            }
            return;
        }

        if (type instanceof TypeIr.Constant _) return; // Should be unreachable now

        final PrimitiveKind primitive = runPrimitive(type);
        if (primitive != null) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local normalized = normalizeWritePrimitive(ops, type, raw, fieldIndex, depth);
            ops.add(new Op.WriteRun(new RunIr(new Value.Const((long) primitive.storeKind().byteSize()),
                    List.of(new RunItem.Put(primitive.storeKind(), new Value.Const(0L), new Value.LocalValue(normalized))))));
            return;
        }

        if (isVarInt(type)) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local normalized = normalizeWriteVarInt(ops, type, raw, fieldIndex, depth);
            final Value value = new Value.LocalValue(normalized);
            final Value encodedSize = new Value.VarIntSize(value);
            ops.add(new Op.WriteRun(new RunIr(encodedSize, List.of(new RunItem.PutVarInt(new Value.Const(0L), value, encodedSize)))));
            return;
        }

        if (isVarLong(type)) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local normalized = normalizeWriteVarLong(ops, type, raw, fieldIndex, depth);
            ops.add(new Op.WriteVarLong(new Value.LocalValue(normalized)));
            return;
        }

        if (fixedBytesLength(type) >= 0) {
            final int length = fixedBytesLength(type);
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local bytes = materializeWriteReference(ops, type, TypeIr.FixedBytes.class, raw, fieldIndex, depth, byte[].class);
            ops.add(new Op.WriteRun(new RunIr(
                    new Value.Const((long) length),
                    List.of(new RunItem.PutBytes(new Value.Const(0L), new Value.LocalValue(bytes), new Value.Const(length)))
            )));
            return;
        }

        if (type instanceof TypeIr.ByteArray(int maxSize)) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local bytes = new Local("path" + path + "Cast", new LocalType.Reference(byte[].class));
            ops.add(new Op.Cast(raw, byte[].class, bytes));

            final Value length = new Value.ArrayLength(new Value.LocalValue(bytes));
            ops.add(new Op.Check(new Value.LessThanOrEqual(length, new Value.Const((long) maxSize)), "Array too long"));
            final Value encodedSize = new Value.VarIntSize(length);
            ops.add(new Op.WriteRun(new RunIr(new Value.Add(encodedSize, length), List.of(
                    new RunItem.PutVarInt(new Value.Const(0L), length, encodedSize),
                    new RunItem.PutBytes(encodedSize, new Value.LocalValue(bytes), length)
            ))));
            return;
        }

        if (type instanceof TypeIr.StringUtf8(int maxSize)) {
            final Local raw;
            if (field != null) {
                raw = referenceLocal("path" + path);
                ops.add(new Op.GetField(field, path, source, raw));
            } else {
                raw = source;
            }
            final Local str = new Local("path" + path + "Cast", new LocalType.Reference(String.class));
            ops.add(new Op.Cast(raw, String.class, str));

            final Local bytes = referenceLocal("path" + path + "Bytes");
            ops.add(new Op.Apply(STRING_TO_BYTES, str, bytes));
            lowerWrite(ops, new TypeIr.ByteArray(maxSize), null, bytes, path + "Str", -1, depth + 1);
            return;
        }

        final Local raw;
        if (field != null) {
            raw = referenceLocal("path" + path);
            ops.add(new Op.GetField(field, path, source, raw));
        } else {
            raw = source;
        }
        ops.add(new Op.WriteExternal(field != null ? field.originalType() : ((TypeIr.External<?>) type).type(), new Value.LocalValue(raw)));
    }

    private static List<Op> mergeWriteRuns(List<Op> ops) {
        final List<Op> merged = new ArrayList<>();
        Op.WriteRun current = null;
        for (Op op : ops) {
            if (op instanceof Op.WriteRun next) {
                if (current == null) {
                    current = next;
                } else {
                    current = mergeWriteRun(current, next);
                }
            } else {
                if (current != null) {
                    merged.add(current);
                    current = null;
                }
                merged.add(op);
            }
        }
        if (current != null) merged.add(current);
        return merged;
    }

    private static Op.WriteRun mergeWriteRun(Op.WriteRun left, Op.WriteRun right) {
        final Value newSize = addValues(left.run().size(), right.run().size());
        final List<RunItem> newItems = new ArrayList<>(left.run().items());
        for (RunItem item : right.run().items()) {
            newItems.add(shiftItem(item, left.run().size()));
        }
        return new Op.WriteRun(new RunIr(newSize, newItems));
    }

    private static RunItem shiftItem(RunItem item, Value shift) {
        return switch (item) {
            case RunItem.Put put -> new RunItem.Put(put.kind(), addValues(shift, put.offset()), put.value());
            case RunItem.PutVarInt putVarInt ->
                    new RunItem.PutVarInt(addValues(shift, putVarInt.offset()), putVarInt.value(), putVarInt.encodedSize());
            case RunItem.PutBytes putBytes ->
                    new RunItem.PutBytes(addValues(shift, putBytes.offset()), putBytes.byteArray(), putBytes.length());
            case RunItem.Get get -> new RunItem.Get(get.kind(), addValues(shift, get.offset()), get.out());
            case RunItem.GetBytes getBytes ->
                    new RunItem.GetBytes(addValues(shift, getBytes.offset()), getBytes.byteArray(), getBytes.length());
            case RunItem.ForIndex forIndex -> {
                final List<RunStep> body = new ArrayList<>();
                for (RunStep step : forIndex.body()) {
                    body.add(shiftStep(step, shift));
                }
                yield new RunItem.ForIndex(forIndex.index(), forIndex.start(), forIndex.end(), body);
            }
        };
    }

    private static RunStep shiftStep(RunStep step, Value shift) {
        return switch (step) {
            case RunStep.Put put -> new RunStep.Put(put.kind(), addValues(shift, put.offset()), put.value());
            case RunStep.Get get -> new RunStep.Get(get.kind(), addValues(shift, get.offset()), get.out());
            case RunStep.PutBytes putBytes ->
                    new RunStep.PutBytes(addValues(shift, putBytes.offset()), putBytes.byteArray(), putBytes.length());
            case RunStep.GetBytes getBytes ->
                    new RunStep.GetBytes(addValues(shift, getBytes.offset()), getBytes.byteArray(), getBytes.length());
            default -> step;
        };
    }

    private static ProgramIr readProgram(FieldIr<?, ?>[] fields, ConstructorIr<?> constructor) {
        final List<Op> readOps = new ArrayList<>();
        final List<Value> args = new ArrayList<>(fields.length);
        for (int i = 0; i < fields.length; i++) {
            args.add(lowerRead(readOps, fields[i].type(), Integer.toString(i + 1), i, 0));
        }
        final Local result = referenceLocal("result");
        readOps.add(new Op.Construct(constructor, "", args, result));
        readOps.add(new Op.Return(new Value.LocalValue(result)));
        return new ProgramIr(mergeReadRuns(readOps));
    }

    private static Value lowerRead(List<Op> ops, TypeIr<?> type, String path, int fieldIndex, int depth) {
        if (type instanceof TypeIr.Template<?> template) {
            final List<Value> args = new ArrayList<>();
            for (int i = 0; i < template.ir().fields().size(); i++) {
                final FieldIr<?, ?> subField = template.ir().fields().get(i);
                args.add(lowerRead(ops, subField.type(), path + "_" + (i + 1), i, depth + 1));
            }
            final Local result = referenceLocal("path" + path + "Result");
            ops.add(new Op.Construct(template.ir().constructor(), path, args, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.Transform<?, ?> transform) {
            final Value parentValue = lowerRead(ops, transform.parent(), path + "X", -1, depth + 1);
            final Local parentLocal = ensureLocal(ops, parentValue, path + "XL");
            final Local result = referenceLocal("path" + path + "To");
            ops.add(new Op.Apply(transform.to(), parentLocal, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.Optional<?> optional) {
            final Local present = new Local("path" + path + "Present", new LocalType.Kind(TypeKind.BOOLEAN));
            ops.add(new Op.ReadRun(new RunIr(new Value.Const(1L),
                    List.of(new RunItem.Get(StoreKind.BOOLEAN, new Value.Const(0L), present)))));

            final Local result = referenceLocal("path" + path + "Result");
            final List<Op> thenOps = new ArrayList<>();
            final Value parentValue = lowerRead(thenOps, optional.parent(), path + "Opt", -1, depth + 1);
            thenOps.add(new Op.Store(parentValue, result));

            final List<Op> elseOps = new ArrayList<>();
            elseOps.add(new Op.Store(new Value.Const(null), result));

            ops.add(new Op.If(new Value.LocalValue(present), thenOps, elseOps));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.ListType<?, ?> listType) {
            final Local size = new Local("path" + path + "Size", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(size));
            ops.add(new Op.Check(new Value.LessThanOrEqual(new Value.LocalValue(size), new Value.Const((long) listType.maxLength())), "Collection too large"));

            final Local collection = referenceLocal("path" + path + "Coll");
            ops.add(new Op.CollectionCreate(listType.factory(), path, new Value.LocalValue(size), collection));

            final Local index = new Local("path" + path + "Idx", new LocalType.Kind(TypeKind.INT));
            final List<Op> body = new ArrayList<>();
            final Value element = lowerRead(body, listType.element(), path + "E", -1, depth + 1);
            body.add(new Op.CollectionAdd(listType.factory(), path, collection, element));

            ops.add(new Op.ForIndex(index, new Value.Const(0), new Value.LocalValue(size), body));

            final Local result = referenceLocal("path" + path + "Res");
            ops.add(new Op.CollectionFinish(listType.factory(), path, collection, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.MapType<?, ?, ?> mapType) {
            final Local size = new Local("path" + path + "Size", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(size));
            ops.add(new Op.Check(new Value.LessThanOrEqual(new Value.LocalValue(size), new Value.Const((long) mapType.maxLength())), "Map too large"));

            final Local map = referenceLocal("path" + path + "Map");
            ops.add(new Op.MapCreate(mapType.factory(), path, new Value.LocalValue(size), map));

            final Local index = new Local("path" + path + "Idx", new LocalType.Kind(TypeKind.INT));
            final List<Op> body = new ArrayList<>();
            final Value key = lowerRead(body, mapType.key(), path + "K", -1, depth + 1);
            final Value value = lowerRead(body, mapType.value(), path + "V", -1, depth + 1);
            body.add(new Op.MapPut(mapType.factory(), path, map, key, value));

            ops.add(new Op.ForIndex(index, new Value.Const(0), new Value.LocalValue(size), body));

            final Local result = referenceLocal("path" + path + "Res");
            ops.add(new Op.MapFinish(mapType.factory(), path, map, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.ByteArray(int maxSize)) {
            final Local length = new Local("path" + path + "Length", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(length));
            ops.add(new Op.Check(new Value.LessThanOrEqual(new Value.LocalValue(length), new Value.Const((long) maxSize)), "Array too long"));

            final Local bytes = new Local("path" + path + "Bytes", new LocalType.Reference(byte[].class));
            ops.add(new Op.ReadRun(new RunIr(new Value.LocalValue(length),
                    List.of(new RunItem.GetBytes(new Value.Const(0L), bytes, new Value.LocalValue(length))))));
            return new Value.LocalValue(bytes);
        }

        if (type instanceof TypeIr.StringUtf8(int maxSize)) {
            final Value bytes = lowerRead(ops, new TypeIr.ByteArray(maxSize), path + "Str", -1, depth + 1);
            final Local bytesLocal = ensureLocal(ops, bytes, path + "StrL");
            final Local result = referenceLocal("path" + path + "Result");
            ops.add(new Op.Apply(BYTES_TO_STRING, bytesLocal, result));
            return new Value.LocalValue(result);
        }

        if (type instanceof TypeIr.Constant(Object value)) return new Value.Const(value);

        final PrimitiveKind primitive = runPrimitive(type);
        if (primitive != null) {
            final Local normalized = new Local("path" + path + "Value", new LocalType.Kind(primitive.localKind()));
            ops.add(new Op.ReadRun(new RunIr(new Value.Const((long) primitive.storeKind().byteSize()),
                    List.of(new RunItem.Get(primitive.storeKind(), new Value.Const(0L), normalized)))));
            return new Value.LocalValue(materializeReadPrimitive(ops, type, normalized, fieldIndex, depth));
        }

        if (isVarInt(type)) {
            final Local normalized = new Local("path" + path + "Value", new LocalType.Kind(TypeKind.INT));
            ops.add(new Op.ReadVarInt(normalized));
            return new Value.LocalValue(materializeReadVarInt(ops, type, normalized, fieldIndex, depth));
        }

        if (isVarLong(type)) {
            final Local normalized = new Local("path" + path + "Value", new LocalType.Kind(TypeKind.LONG));
            ops.add(new Op.ReadVarLong(normalized));
            return new Value.LocalValue(materializeReadVarLong(ops, type, normalized, fieldIndex, depth));
        }

        if (fixedBytesLength(type) >= 0) {
            final int length = fixedBytesLength(type);
            final Local bytes = new Local("path" + path + "Bytes", new LocalType.Reference(byte[].class));
            ops.add(new Op.ReadRun(new RunIr(
                    new Value.Const((long) length),
                    List.of(new RunItem.GetBytes(new Value.Const(0L), bytes, new Value.Const(length)))
            )));
            return new Value.LocalValue(materializeReadReference(ops, type, TypeIr.FixedBytes.class, bytes, fieldIndex, depth));
        }

        final Local readLocal = referenceLocal("path" + path);
        ops.add(new Op.ReadExternal(((TypeIr.External<?>) type).type(), readLocal));
        return new Value.LocalValue(readLocal);
    }

    private static List<Op> mergeReadRuns(List<Op> ops) {
        final List<Op> merged = new ArrayList<>();
        Op.ReadRun current = null;
        for (Op op : ops) {
            if (op instanceof Op.ReadRun next) {
                if (current == null) {
                    current = next;
                } else {
                    current = mergeReadRun(current, next);
                }
            } else {
                if (current != null) {
                    merged.add(current);
                    current = null;
                }
                merged.add(op);
            }
        }
        if (current != null) merged.add(current);
        return merged;
    }

    private static Op.ReadRun mergeReadRun(Op.ReadRun left, Op.ReadRun right) {
        final Value newSize = addValues(left.run().size(), right.run().size());
        final List<RunItem> newItems = new ArrayList<>(left.run().items());
        for (RunItem item : right.run().items()) {
            newItems.add(shiftItem(item, left.run().size()));
        }
        return new Op.ReadRun(new RunIr(newSize, newItems));
    }

    private static Local ensureLocal(List<Op> ops, Value value, String path) {
        if (value instanceof Value.LocalValue localValue) return localValue.local();
        final Local local = referenceLocal("path" + path);
        ops.add(new Op.Store(value, local));
        return local;
    }

    private static Value addValues(Value left, Value right) {
        if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
            if (lv instanceof Long l && rv instanceof Long r) return new Value.Const(l + r);
            if (lv instanceof Integer l && rv instanceof Integer r) return new Value.Const((long) l + r);
            if (lv instanceof Long l && rv instanceof Integer r) return new Value.Const(l + r);
            if (lv instanceof Integer l && rv instanceof Long r) return new Value.Const(l + r);
        }
        if (left instanceof Value.Const(Object lv)) {
            if (lv instanceof Long l && l == 0) return right;
            if (lv instanceof Integer l && l == 0) return right;
        }
        if (right instanceof Value.Const(Object rv)) {
            if (rv instanceof Long r && r == 0) return left;
            if (rv instanceof Integer r && r == 0) return left;
        }
        return new Value.Add(left, right);
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
                final Local cast = referenceLocal("field" + fieldIndex + "Cast" + depth);
                final Local normalized = new Local("field" + fieldIndex + "Value" + depth, new LocalType.Kind(kind.localKind()));
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
                final Local boxed = referenceLocal("field" + fieldIndex + "Boxed" + depth);
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
                final Local cast = referenceLocal("field" + fieldIndex + "Cast" + depth);
                final Local normalized = new Local("field" + fieldIndex + "Value" + depth, new LocalType.Kind(TypeKind.INT));
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
                final Local boxed = referenceLocal("field" + fieldIndex + "Boxed" + depth);
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
            case TypeIr.Optional<?> optional -> isVarInt(optional.parent());
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
                final Local cast = referenceLocal("field" + fieldIndex + "Cast" + depth);
                final Local normalized = new Local("field" + fieldIndex + "Value" + depth, new LocalType.Kind(TypeKind.LONG));
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
                final Local boxed = referenceLocal("field" + fieldIndex + "Boxed" + depth);
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
            case TypeIr.Optional<?> optional -> isVarLong(optional.parent());
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
                final Local cast = new Local("field" + fieldIndex + "Cast" + depth, new LocalType.Reference(targetClass));
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
            case TypeIr.Optional<?> optional -> fixedBytesLength(optional.parent());
            default -> -1;
        };
    }

    private static boolean isByteArray(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.ByteArray _ -> true;
            case TypeIr.Transform<?, ?> transform -> isByteArray(transform.parent());
            case TypeIr.Optional<?> optional -> isByteArray(optional.parent());
            default -> false;
        };
    }

    private static boolean isStringUtf8(TypeIr<?> type) {
        return switch (type) {
            case TypeIr.StringUtf8 _ -> true;
            case TypeIr.Transform<?, ?> transform -> isStringUtf8(transform.parent());
            case TypeIr.Optional<?> optional -> isStringUtf8(optional.parent());
            default -> false;
        };
    }

    private static @Nullable PrimitiveKind constantPrimitive(Object value) {
        if (value instanceof Boolean) return PrimitiveKind.BOOLEAN;
        if (value instanceof Byte) return PrimitiveKind.BYTE;
        if (value instanceof Short) return PrimitiveKind.SHORT;
        if (value instanceof Integer) return PrimitiveKind.INT;
        if (value instanceof Long) return PrimitiveKind.LONG;
        if (value instanceof Float) return PrimitiveKind.FLOAT;
        if (value instanceof Double) return PrimitiveKind.DOUBLE;
        return null;
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
                case NetworkBufferTypeImpl.ListType<?> list ->
                        new TypeIr.ListType(list, typeIr(list.parent(), visiting), list.maxSize(), LIST_FACTORY);
                case NetworkBufferTypeImpl.MapType<?, ?> map ->
                        new TypeIr.MapType(map, typeIr(map.parent(), visiting), typeIr(map.valueType(), visiting), map.maxSize(), MAP_FACTORY);
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

    Function<String, byte[]> STRING_TO_BYTES = s -> s.getBytes(StandardCharsets.UTF_8);
    Function<byte[], String> BYTES_TO_STRING = b -> new String(b, StandardCharsets.UTF_8);

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
                       Map<String, Integer> constructors, Map<String, ConstructorIr<?>> constructorIrs,
                       List<FactoryFieldData> factories, List<ExternalTypeFieldData> externalTypes) {
        public IrClassData {
            fields = List.copyOf(fields);
            transforms = List.copyOf(transforms);
            constructors = Map.copyOf(constructors);
            constructorIrs = Map.copyOf(constructorIrs);
            factories = List.copyOf(factories);
            externalTypes = List.copyOf(externalTypes);
        }

        ConstructorIr<?> constructorIr(String name) {
            return constructorIrs.get(name);
        }
    }

    record ExternalTypeFieldData(String name, NetworkBuffer.Type<?> type, int dataIndex) {}

    record IrFieldData(FieldIr<?, ?> ir, String path, int typeDataIndex, int getterDataIndex) {
    }

    record IrCtorData(String name, int fieldCount, int dataIndex) {
    }

    record TransformFieldData(String name, Function<?, ?> function, int dataIndex) {
    }

    record FactoryFieldData(String name, Object factory, int dataIndex) {
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

    private static String factoryName(String path) {
        return FACTORY_PREFIX + path;
    }
}
