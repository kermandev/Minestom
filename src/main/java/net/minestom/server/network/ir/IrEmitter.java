package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.Nullable;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

import static net.minestom.server.network.ir.IrMetadata.*;
import static net.minestom.server.network.ir.IrCompiler.*;

final class IrEmitter {
    private IrEmitter() {}

    static final ClassDesc CD_OBJECT = ConstantDescs.CD_Object;
    static final ClassDesc CD_OBJECT_ARRAY = ClassDesc.ofDescriptor("[Ljava/lang/Object;");
    static final ClassDesc CD_STRING = ConstantDescs.CD_String;
    static final ClassDesc CD_INT = ConstantDescs.CD_int;
    static final ClassDesc CD_LONG = ConstantDescs.CD_long;
    static final ClassDesc CD_FLOAT = ConstantDescs.CD_float;
    static final ClassDesc CD_DOUBLE = ConstantDescs.CD_double;
    static final ClassDesc CD_SHORT = ConstantDescs.CD_short;
    static final ClassDesc CD_BYTE = ConstantDescs.CD_byte;
    static final ClassDesc CD_BOOLEAN = ConstantDescs.CD_boolean;
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
    static final ClassDesc CD_NETWORK_BUFFER = ClassDesc.of("net.minestom.server.network", "NetworkBuffer");
    static final ClassDesc CD_NETWORK_BUFFER_IMPL = ClassDesc.of("net.minestom.server.network", "NetworkBufferImpl");
    static final ClassDesc CD_NETWORK_BUFFER_TYPE_IMPL = ClassDesc.of("net.minestom.server.network", "NetworkBufferTypeImpl");
    static final ClassDesc CD_TYPE = NetworkBuffer.Type.class.describeConstable().orElseThrow();
    static final ClassDesc CD_FUNCTION = Function.class.describeConstable().orElseThrow();
    static final ClassDesc CD_UNIT = Unit.class.describeConstable().orElseThrow();
    static final ClassDesc CD_STANDARD_CHARSETS = ClassDesc.of("java.nio.charset", "StandardCharsets");
    static final ClassDesc CD_CHARSET = ClassDesc.of("java.nio.charset", "Charset");
    static final ClassDesc CD_COLLECTION = ClassDesc.of("java.util", "Collection");
    static final ClassDesc CD_LIST = ClassDesc.of("java.util", "List");
    static final ClassDesc CD_ARRAY_LIST = ClassDesc.of("java.util", "ArrayList");
    static final ClassDesc CD_ARRAY_UTILS = ClassDesc.of("net.minestom.server.utils", "ArrayUtils");
    static final ClassDesc CD_ITERABLE = ClassDesc.of("java.lang", "Iterable");
    static final ClassDesc CD_ITERATOR = ClassDesc.of("java.util", "Iterator");
    static final ClassDesc CD_MAP = ClassDesc.of("java.util", "Map");
    static final ClassDesc CD_MAP_ENTRY = CD_MAP.nested("Entry");
    static final ClassDesc CD_SET = ClassDesc.of("java.util", "Set");
    static final ClassDesc CD_EITHER = ClassDesc.of("net.minestom.server.utils", "Either");
    static final ClassDesc CD_EITHER_LEFT = CD_EITHER.nested("Left");
    static final ClassDesc CD_EITHER_RIGHT = CD_EITHER.nested("Right");

    static final String READ = "read";
    static final String WRITE = "write";

    static final MethodTypeDesc MT_VOID = MethodTypeDesc.of(CD_VOID);
    static final MethodTypeDesc MT_LOOKUP = MethodTypeDesc.of(CD_METHOD_HANDLES_LOOKUP);
    static final MethodTypeDesc MT_CLASS_DATA_AT = MethodTypeDesc.of(CD_OBJECT, CD_METHOD_HANDLES_LOOKUP, CD_STRING, ConstantDescs.CD_Class, CD_INT);
    static final MethodTypeDesc MT_READ_OBJECT = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER);
    static final MethodTypeDesc MT_TO_MAP = MethodTypeDesc.of(CD_MAP, CD_OBJECT_ARRAY, CD_OBJECT_ARRAY, CD_INT);
    static final MethodTypeDesc MT_WRITE_OBJECT = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_OBJECT);
    static final MethodTypeDesc MT_RESERVE = MethodTypeDesc.of(CD_LONG, CD_LONG);
    static final MethodTypeDesc MT_FUNCTION_APPLY = MethodTypeDesc.of(CD_OBJECT, CD_OBJECT);
    static final MethodTypeDesc MT_WRITE_VAR_INT_UNCHECKED = MethodTypeDesc.of(CD_LONG, CD_NETWORK_BUFFER_IMPL, CD_LONG, CD_INT);
    static final MethodTypeDesc MT_WRITE_VAR_LONG_UNCHECKED = MethodTypeDesc.of(CD_LONG, CD_NETWORK_BUFFER_IMPL, CD_LONG, CD_LONG);
    static final MethodTypeDesc MT_READ_VAR_INT = MethodTypeDesc.of(CD_INT, CD_NETWORK_BUFFER);
    static final MethodTypeDesc MT_READ_VAR_LONG = MethodTypeDesc.of(CD_LONG, CD_NETWORK_BUFFER);
    static final MethodTypeDesc MT_VAR_INT_SIZE = MethodTypeDesc.of(CD_INT, CD_INT);
    static final MethodTypeDesc MT_VAR_LONG_SIZE = MethodTypeDesc.of(CD_INT, CD_LONG);
    static final MethodTypeDesc MT_BOX_BOOLEAN = MethodTypeDesc.of(CD_BOOLEAN_WRAPPER, CD_BOOLEAN);
    static final MethodTypeDesc MT_BOX_BYTE = MethodTypeDesc.of(CD_BYTE_WRAPPER, CD_BYTE);
    static final MethodTypeDesc MT_BOX_SHORT = MethodTypeDesc.of(CD_SHORT_WRAPPER, CD_SHORT);
    static final MethodTypeDesc MT_BOX_INT = MethodTypeDesc.of(CD_INTEGER_WRAPPER, CD_INT);
    static final MethodTypeDesc MT_BOX_LONG = MethodTypeDesc.of(CD_LONG_WRAPPER, CD_LONG);
    static final MethodTypeDesc MT_BOX_FLOAT = MethodTypeDesc.of(CD_FLOAT_WRAPPER, CD_FLOAT);
    static final MethodTypeDesc MT_BOX_DOUBLE = MethodTypeDesc.of(CD_DOUBLE_WRAPPER, CD_DOUBLE);
    static final MethodTypeDesc MT_BOOLEAN_VALUE = MethodTypeDesc.of(CD_BOOLEAN);
    static final MethodTypeDesc MT_BYTE_VALUE = MethodTypeDesc.of(CD_BYTE);
    static final MethodTypeDesc MT_SHORT_VALUE = MethodTypeDesc.of(CD_SHORT);
    static final MethodTypeDesc MT_INT_VALUE = MethodTypeDesc.of(CD_INT);
    static final MethodTypeDesc MT_LONG_VALUE = MethodTypeDesc.of(CD_LONG);
    static final MethodTypeDesc MT_FLOAT_VALUE = MethodTypeDesc.of(CD_FLOAT);
    static final MethodTypeDesc MT_DOUBLE_VALUE = MethodTypeDesc.of(CD_DOUBLE);
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

    static final int FIELD_FLAGS = ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    static final int METHOD_FLAGS = ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    static final int CLASS_FLAGS = ClassFile.ACC_FINAL | ClassFile.ACC_SUPER | ClassFile.ACC_SYNTHETIC;

    static byte[] buildClass(ClassDesc classDesc, IrClassData data) {
        return ClassFile.of().build(classDesc, classBuilder -> {
            classBuilder.withFlags(CLASS_FLAGS)
                    .withSuperclass(CD_OBJECT)
                    .withInterfaceSymbols(CD_TYPE);

            declareIrFields(classBuilder, data);

            classBuilder.withMethodBody(ConstantDescs.CLASS_INIT_NAME, MT_VOID, ClassFile.ACC_STATIC | ClassFile.ACC_SYNTHETIC,
                    codeBuilder -> buildClassInitializer(codeBuilder, classDesc, data));
            classBuilder.withMethodBody(ConstantDescs.INIT_NAME, MT_VOID, ClassFile.ACC_PRIVATE | ClassFile.ACC_SYNTHETIC,
                    codeBuilder -> codeBuilder.aload(0).invokespecial(CD_OBJECT, ConstantDescs.INIT_NAME, MT_VOID).return_());
            classBuilder.withMethodBody(WRITE, MT_WRITE_OBJECT, METHOD_FLAGS,
                    codeBuilder -> buildWrite(codeBuilder, classDesc, data, data.write(), 2));
            classBuilder.withMethodBody(READ, MT_READ_OBJECT, METHOD_FLAGS,
                    codeBuilder -> buildRead(codeBuilder, classDesc, data, data.read()));
        });
    }

    private static void declareIrFields(ClassBuilder classBuilder, IrClassData data) {
        for (ExternalTypeFieldData external : data.externalTypes()) {
            classBuilder.withField(external.name(), CD_TYPE, FIELD_FLAGS);
        }
        for (TransformFieldData transform : data.transforms()) {
            classBuilder.withField(transform.name(), CD_FUNCTION, FIELD_FLAGS);
        }
        for (Map.Entry<String, Integer> entry : data.constructors().entrySet()) {
            final int fieldCount = data.constructorIr(entry.getKey()).fieldCount();
            classBuilder.withField(entry.getKey(), constructorInterface(fieldCount), FIELD_FLAGS);
        }
    }

    private static void buildClassInitializer(CodeBuilder codeBuilder, ClassDesc classDesc, IrClassData data) {
        codeBuilder.invokestatic(CD_METHOD_HANDLES, "lookup", MT_LOOKUP)
                .astore(0);
        initIrFields(codeBuilder, classDesc, data);
        codeBuilder.return_();
    }

    private static void initIrFields(CodeBuilder codeBuilder, ClassDesc classDesc, IrClassData data) {
        for (ExternalTypeFieldData external : data.externalTypes()) {
            loadClassDataAt(codeBuilder, CD_TYPE, external.dataIndex())
                    .putstatic(classDesc, external.name(), CD_TYPE);
        }
        for (TransformFieldData transform : data.transforms()) {
            loadClassDataAt(codeBuilder, CD_FUNCTION, transform.dataIndex())
                    .putstatic(classDesc, transform.name(), CD_FUNCTION);
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
        final EmitContext context = new EmitContext(classDesc, data, directSlot, new IdentityHashMap<>());
        if (program.initialSource() != null) {
            context.locals().put(program.initialSource(), objectSlot);
        }
        emitProgram(codeBuilder, context, program);
        codeBuilder.return_();
    }

    private static void buildRead(CodeBuilder codeBuilder, ClassDesc classDesc, IrClassData data, ProgramIr program) {
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        emitDirectBuffer(codeBuilder, directSlot);
        final EmitContext context = new EmitContext(classDesc, data, directSlot, new IdentityHashMap<>());
        emitProgram(codeBuilder, context, program);
    }

    private static void emitProgram(CodeBuilder codeBuilder, EmitContext context, ProgramIr program) {
        for (Op programOp : program.ops()) {
            emitOp(codeBuilder, context, programOp);
        }
    }

    private static void emitOp(CodeBuilder codeBuilder, EmitContext context, Op op) {
        switch (op) {
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
            case Op.StringToBytes stringToBytes -> {
                emitLoadLocal(codeBuilder, context, stringToBytes.in());
                codeBuilder.checkcast(CD_STRING);
                codeBuilder.getstatic(CD_STANDARD_CHARSETS, "UTF_8", CD_CHARSET);
                codeBuilder.invokevirtual(CD_STRING, "getBytes", MethodTypeDesc.of(CD_BYTE_ARRAY, CD_CHARSET));
                emitStoreLocal(codeBuilder, context, stringToBytes.out());
            }
            case Op.BytesToString bytesToString -> {
                codeBuilder.new_(CD_STRING);
                codeBuilder.dup();
                emitLoadLocal(codeBuilder, context, bytesToString.in());
                codeBuilder.checkcast(CD_BYTE_ARRAY);
                codeBuilder.getstatic(CD_STANDARD_CHARSETS, "UTF_8", CD_CHARSET);
                codeBuilder.invokespecial(CD_STRING, ConstantDescs.INIT_NAME, MethodTypeDesc.of(CD_VOID, CD_BYTE_ARRAY, CD_CHARSET));
                emitStoreLocal(codeBuilder, context, bytesToString.out());
            }
            case Op.EitherLeft eitherLeft -> {
                emitLoadLocal(codeBuilder, context, eitherLeft.in());
                codeBuilder.invokestatic(CD_EITHER, "left", MethodTypeDesc.of(CD_EITHER, CD_OBJECT), true);
                emitStoreLocal(codeBuilder, context, eitherLeft.out());
            }
            case Op.EitherRight eitherRight -> {
                emitLoadLocal(codeBuilder, context, eitherRight.in());
                codeBuilder.invokestatic(CD_EITHER, "right", MethodTypeDesc.of(CD_EITHER, CD_OBJECT), true);
                emitStoreLocal(codeBuilder, context, eitherRight.out());
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
            case Op.WritePrimitive writePrimitive -> {
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                codeBuilder.aload(context.directSlot())
                        .loadConstant((long) writePrimitive.kind().storeKind().byteSize())
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                        .lstore(indexSlot);
                codeBuilder.aload(context.directSlot())
                        .lload(indexSlot);
                emitValue(codeBuilder, context, writePrimitive.value());
                emitStoreKindWrite(codeBuilder, writePrimitive.kind().storeKind(), writePrimitive.value());
            }
            case Op.ReadPrimitive readPrimitive -> {
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                codeBuilder.aload(context.directSlot())
                        .loadConstant((long) readPrimitive.kind().storeKind().byteSize())
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveRead", MT_RESERVE)
                        .lstore(indexSlot);
                codeBuilder.aload(context.directSlot())
                        .lload(indexSlot);
                emitStoreKindRead(codeBuilder, readPrimitive.kind().storeKind(), readPrimitive.out());
                emitStoreLocal(codeBuilder, context, readPrimitive.out());
            }
            case Op.ReadVarInt readVarInt -> {
                codeBuilder.aload(context.directSlot())
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "readVarInt", MT_READ_VAR_INT, true);
                emitStoreLocal(codeBuilder, context, readVarInt.out());
            }
            case Op.WriteVarInt writeVarInt -> {
                final int intSlot = codeBuilder.allocateLocal(TypeKind.INT);
                final int sizeSlot = codeBuilder.allocateLocal(TypeKind.INT);
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                emitIntValue(codeBuilder, context, writeVarInt.value());
                codeBuilder.istore(intSlot)
                        .iload(intSlot)
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varIntSize", MT_VAR_INT_SIZE, true)
                        .istore(sizeSlot)
                        .aload(context.directSlot())
                        .iload(sizeSlot)
                        .i2l()
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                        .lstore(indexSlot)
                        .aload(context.directSlot())
                        .lload(indexSlot)
                        .iload(intSlot)
                        .invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarIntUnchecked", MT_WRITE_VAR_INT_UNCHECKED, true);
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
            case Op.WriteFixedBytes writeFixedBytes -> {
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                final int arraySlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
                final int lengthSlot = codeBuilder.allocateLocal(TypeKind.INT);
                emitValue(codeBuilder, context, writeFixedBytes.value());
                codeBuilder.checkcast(CD_BYTE_ARRAY)
                        .astore(arraySlot)
                        .aload(arraySlot)
                        .arraylength()
                        .istore(lengthSlot)
                        .aload(context.directSlot())
                        .iload(lengthSlot)
                        .i2l()
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                        .lstore(indexSlot)
                        .aload(context.directSlot())
                        .lload(indexSlot)
                        .aload(arraySlot)
                        .iconst_0()
                        .iload(lengthSlot)
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putBytesUnchecked", MT_PUT_BYTES);
            }
            case Op.ReadFixedBytes readFixedBytes -> {
                final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
                final int lengthSlot = codeBuilder.allocateLocal(TypeKind.INT);
                emitIntValue(codeBuilder, context, readFixedBytes.length());
                codeBuilder.istore(lengthSlot)
                        .aload(context.directSlot())
                        .iload(lengthSlot)
                        .i2l()
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveRead", MT_RESERVE)
                        .lstore(indexSlot)
                        .iload(lengthSlot)
                        .newarray(TypeKind.BYTE);
                emitStoreLocal(codeBuilder, context, readFixedBytes.out());
                codeBuilder.aload(context.directSlot())
                        .lload(indexSlot);
                emitLoadLocal(codeBuilder, context, readFixedBytes.out());
                codeBuilder.iconst_0()
                        .iload(lengthSlot)
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getBytesUnchecked", MT_GET_BYTES);
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
            case Op.ArrayCreate arrayCreate -> {
                emitIntValue(codeBuilder, context, arrayCreate.size());
                codeBuilder.anewarray(CD_OBJECT);
                emitStoreLocal(codeBuilder, context, arrayCreate.out());
            }
            case Op.ArraySet arraySet -> {
                emitLoadLocal(codeBuilder, context, arraySet.array());
                codeBuilder.checkcast(CD_OBJECT_ARRAY);
                emitIntValue(codeBuilder, context, arraySet.index());
                emitValue(codeBuilder, context, arraySet.value());
                codeBuilder.aastore();
            }
            case Op.ListFinish listFinish -> {
                emitLoadLocal(codeBuilder, context, listFinish.array());
                codeBuilder.checkcast(CD_OBJECT_ARRAY);
                codeBuilder.invokestatic(CD_LIST, "of", MethodTypeDesc.of(CD_LIST, CD_OBJECT_ARRAY), true);
                emitStoreLocal(codeBuilder, context, listFinish.out());
            }
            case Op.MapFinish mapFinish -> {
                emitLoadLocal(codeBuilder, context, mapFinish.keys());
                codeBuilder.checkcast(CD_OBJECT_ARRAY);
                emitLoadLocal(codeBuilder, context, mapFinish.values());
                codeBuilder.checkcast(CD_OBJECT_ARRAY);
                emitIntValue(codeBuilder, context, mapFinish.size());
                codeBuilder.invokestatic(CD_ARRAY_UTILS, "toMap", MT_TO_MAP);
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
                final ClassDesc constructorType = constructorInterface(construct.args().size());
                codeBuilder.getstatic(context.classDesc(), ctorFieldName(context.data(), construct.factory()), constructorType);
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
            default -> throw new IllegalStateException("Unexpected value: " + op);
        }
    }

    private static void emitRunItemWriteAt(CodeBuilder codeBuilder, EmitContext context, int addressSlot, RunItem item) {
        codeBuilder.aload(context.directSlot());
        codeBuilder.lload(addressSlot);
        switch (item) {
            case RunItem.Put put -> {
                emitValue(codeBuilder, context, put.value());
                emitStoreKindWrite(codeBuilder, put.kind(), put.value());
                codeBuilder.lload(addressSlot);
                codeBuilder.loadConstant((long) put.kind().byteSize());
                codeBuilder.ladd();
                codeBuilder.lstore(addressSlot);
            }
            case RunItem.PutVarInt putVarInt -> {
                emitIntValue(codeBuilder, context, putVarInt.value());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarIntUnchecked", MT_WRITE_VAR_INT_UNCHECKED, true);
                codeBuilder.lstore(addressSlot);
            }
            case RunItem.PutVarLong putVarLong -> {
                emitLongValue(codeBuilder, context, putVarLong.value());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarLongUnchecked", MT_WRITE_VAR_LONG_UNCHECKED, true);
                codeBuilder.lstore(addressSlot);
            }
            case RunItem.PutBytes putBytes -> {
                emitValue(codeBuilder, context, putBytes.byteArray());
                codeBuilder.iconst_0();
                emitIntValue(codeBuilder, context, putBytes.length());
                codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putBytesUnchecked", MT_PUT_BYTES);
                codeBuilder.lload(addressSlot);
                emitLongValue(codeBuilder, context, putBytes.length());
                codeBuilder.ladd();
                codeBuilder.lstore(addressSlot);
            }
            default -> throw new UnsupportedOperationException("Unsupported run item for at-address emission: " + item);
        }
    }

    private static void emitRunItemReadAt(CodeBuilder codeBuilder, EmitContext context, int addressSlot, RunItem item) {
        switch (item) {
            case RunItem.Get get -> {
                codeBuilder.aload(context.directSlot());
                codeBuilder.lload(addressSlot);
                emitStoreKindRead(codeBuilder, get.kind(), get.out());
                emitStoreLocal(codeBuilder, context, get.out());
                codeBuilder.lload(addressSlot);
                codeBuilder.loadConstant((long) get.kind().byteSize());
                codeBuilder.ladd();
                codeBuilder.lstore(addressSlot);
            }
            case RunItem.GetBytes getBytes -> {
                final int lengthSlot = codeBuilder.allocateLocal(TypeKind.INT);
                emitIntValue(codeBuilder, context, getBytes.length());
                codeBuilder.dup()
                        .istore(lengthSlot)
                        .newarray(TypeKind.BYTE);
                emitStoreLocal(codeBuilder, context, getBytes.byteArray());

                codeBuilder.aload(context.directSlot());
                codeBuilder.lload(addressSlot);
                emitLoadLocal(codeBuilder, context, getBytes.byteArray());
                codeBuilder.iconst_0()
                        .iload(lengthSlot)
                        .invokevirtual(CD_NETWORK_BUFFER_IMPL, "_getBytesUnchecked", MT_GET_BYTES);

                codeBuilder.lload(addressSlot);
                codeBuilder.iload(lengthSlot);
                codeBuilder.i2l();
                codeBuilder.ladd();
                codeBuilder.lstore(addressSlot);
            }
            default -> throw new UnsupportedOperationException("Unsupported run item for at-address emission: " + item);
        }
    }

    private static Value itemOffset(RunItem item) {
        return switch (item) {
            case RunItem.Put put -> put.offset();
            case RunItem.Get get -> get.offset();
            case RunItem.PutVarInt putVarInt -> putVarInt.offset();
            case RunItem.PutVarLong putVarLong -> putVarLong.offset();
            case RunItem.PutBytes putBytes -> putBytes.offset();
            case RunItem.GetBytes getBytes -> getBytes.offset();
            case RunItem.ForIndex _ -> new Value.Const(0L);
        };
    }

    private static Value itemSize(RunItem item) {
        return switch (item) {
            case RunItem.Put put -> new Value.Const((long) put.kind().byteSize());
            case RunItem.Get get -> new Value.Const((long) get.kind().byteSize());
            case RunItem.PutVarInt putVarInt -> putVarInt.encodedSize();
            case RunItem.PutVarLong putVarLong -> putVarLong.encodedSize();
            case RunItem.PutBytes putBytes -> putBytes.length();
            case RunItem.GetBytes getBytes -> getBytes.length();
            case RunItem.ForIndex _ -> new Value.Const(0L);
        };
    }

    private static Value addValues(Value left, Value right) {
        if (left instanceof Value.Const(Object lv) && right instanceof Value.Const(Object rv)) {
            if (lv instanceof Number l && rv instanceof Number r) return new Value.Const(l.longValue() + r.longValue());
        }
        if (left instanceof Value.Const(Object lv) && lv instanceof Number n && n.longValue() == 0) return right;
        if (right instanceof Value.Const(Object rv) && rv instanceof Number n && n.longValue() == 0) return left;

        // Normalize: ensure left-leaning tree
        if (right instanceof Value.Add rightAdd) {
            return addValues(addValues(left, rightAdd.left()), rightAdd.right());
        }
        return new Value.Add(left, right);
    }

    private static void emitWriteRun(CodeBuilder codeBuilder, EmitContext context, RunIr run) {
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        codeBuilder.aload(context.directSlot());
        emitLongValue(codeBuilder, context, run.size());
        codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveWrite", MT_RESERVE)
                .lstore(indexSlot);

        int currentSlot = indexSlot;
        if (run.items().size() > 1) {
            currentSlot = codeBuilder.allocateLocal(TypeKind.LONG);
            codeBuilder.lload(indexSlot);
            codeBuilder.lstore(currentSlot);
        }
        Value currentOffset = new Value.Const(0L);

        for (RunItem item : run.items()) {
            if (item instanceof RunItem.ForIndex loop) {
                // For now, treat loops as barriers and use absolute offset from indexSlot
                emitLoopWrite(codeBuilder, context, indexSlot, loop);
                currentSlot = indexSlot;
                currentOffset = new Value.Const(0L);
                continue;
            }

            Value offset = itemOffset(item);
            Value delta = subtract(offset, currentOffset);
            if (delta == null) {
                codeBuilder.lload(indexSlot);
                if (!isZero(offset)) {
                    emitLongValue(codeBuilder, context, offset);
                    codeBuilder.ladd();
                }
                codeBuilder.lstore(currentSlot);
            } else if (!isZero(delta)) {
                codeBuilder.lload(currentSlot);
                emitLongValue(codeBuilder, context, delta);
                codeBuilder.ladd();
                codeBuilder.lstore(currentSlot);
            }

            emitRunItemWriteAt(codeBuilder, context, currentSlot, item);
            currentOffset = addValues(offset, itemSize(item));
        }
    }

    private static void emitLoopWrite(CodeBuilder codeBuilder, EmitContext context, int indexSlot, RunItem.ForIndex loop) {
        final Label startLabel = codeBuilder.newLabel();
        final Label endLabel = codeBuilder.newLabel();
        final int loopIndexSlot = localSlot(codeBuilder, context, loop.index());

        emitIntValue(codeBuilder, context, loop.start());
        codeBuilder.istore(loopIndexSlot);

        codeBuilder.labelBinding(startLabel);
        codeBuilder.iload(loopIndexSlot);
        emitIntValue(codeBuilder, context, loop.end());
        codeBuilder.if_icmpge(endLabel);

        for (RunStep step : loop.body()) {
            switch (step) {
                case RunStep.ElementAt elementAt -> {
                    emitValue(codeBuilder, context, elementAt.source());
                    codeBuilder.checkcast(CD_LIST);
                    emitIntValue(codeBuilder, context, elementAt.index());
                    codeBuilder.invokeinterface(CD_LIST, "get", MethodTypeDesc.of(CD_OBJECT, CD_INT));
                    emitStoreLocal(codeBuilder, context, elementAt.out());
                }
                case RunStep.Apply apply -> {
                    codeBuilder.getstatic(context.classDesc(), transformFunctionName(context.data(), apply.function()), CD_FUNCTION);
                    emitLoadLocal(codeBuilder, context, apply.in());
                    codeBuilder.invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
                    emitStoreLocal(codeBuilder, context, apply.out());
                }
                case RunStep.Cast cast -> {
                    emitLoadLocal(codeBuilder, context, cast.in());
                    codeBuilder.checkcast(classDesc(cast.targetClass()));
                    emitStoreLocal(codeBuilder, context, cast.out());
                }
                case RunStep.Unbox unbox -> {
                    emitLoadLocal(codeBuilder, context, unbox.in());
                    final ClassDesc wrapper = unbox.kind().wrapperClass().describeConstable().orElseThrow();
                    codeBuilder.checkcast(wrapper);
                    emitUnboxedTypeIrValue(codeBuilder, unbox.kind());
                    emitStoreLocal(codeBuilder, context, unbox.out());
                }
                case RunStep.Box box -> {
                    emitLoadLocal(codeBuilder, context, box.in());
                    emitBoxedTypeIrValue(codeBuilder, box.kind());
                    emitStoreLocal(codeBuilder, context, box.out());
                }
                case RunStep.Put put -> {
                    codeBuilder.aload(context.directSlot());
                    emitOffsetValue(codeBuilder, context, indexSlot, put.offset());
                    emitValue(codeBuilder, context, put.value());
                    emitStoreKindWrite(codeBuilder, put.kind(), put.value());
                }
                case RunStep.PutVarInt putVarInt -> {
                    codeBuilder.aload(context.directSlot());
                    emitOffsetValue(codeBuilder, context, indexSlot, putVarInt.offset());
                    emitIntValue(codeBuilder, context, putVarInt.value());
                    codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarIntUnchecked", MT_WRITE_VAR_INT_UNCHECKED, true);
                }
                case RunStep.PutVarLong putVarLong -> {
                    codeBuilder.aload(context.directSlot());
                    emitOffsetValue(codeBuilder, context, indexSlot, putVarLong.offset());
                    emitLongValue(codeBuilder, context, putVarLong.value());
                    codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "writeVarLongUnchecked", MT_WRITE_VAR_LONG_UNCHECKED, true);
                }
                case RunStep.PutBytes putBytes -> {
                    codeBuilder.aload(context.directSlot());
                    emitOffsetValue(codeBuilder, context, indexSlot, putBytes.offset());
                    emitValue(codeBuilder, context, putBytes.byteArray());
                    codeBuilder.iconst_0();
                    emitIntValue(codeBuilder, context, putBytes.length());
                    codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "_putBytesUnchecked", MT_PUT_BYTES);
                }
                default -> throw new UnsupportedOperationException("Unsupported run step: " + step);
            }
        }

        codeBuilder.iinc(loopIndexSlot, 1);
        codeBuilder.goto_(startLabel);
        codeBuilder.labelBinding(endLabel);
    }

    private static void emitReadRun(CodeBuilder codeBuilder, EmitContext context, RunIr run) {
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        codeBuilder.aload(context.directSlot());
        emitLongValue(codeBuilder, context, run.size());
        codeBuilder.invokevirtual(CD_NETWORK_BUFFER_IMPL, "reserveRead", MT_RESERVE)
                .lstore(indexSlot);

        int currentSlot = indexSlot;
        if (run.items().size() > 1) {
            currentSlot = codeBuilder.allocateLocal(TypeKind.LONG);
            codeBuilder.lload(indexSlot);
            codeBuilder.lstore(currentSlot);
        }
        Value currentOffset = new Value.Const(0L);

        for (RunItem item : run.items()) {
            if (item instanceof RunItem.ForIndex loop) {
                // For now, treat loops as barriers and use absolute offset from indexSlot
                emitLoopRead(codeBuilder, context, indexSlot, loop);
                currentSlot = indexSlot;
                currentOffset = new Value.Const(0L);
                continue;
            }

            Value offset = itemOffset(item);
            Value delta = subtract(offset, currentOffset);
            if (delta == null) {
                codeBuilder.lload(indexSlot);
                if (!isZero(offset)) {
                    emitLongValue(codeBuilder, context, offset);
                    codeBuilder.ladd();
                }
                codeBuilder.lstore(currentSlot);
            } else if (!isZero(delta)) {
                codeBuilder.lload(currentSlot);
                emitLongValue(codeBuilder, context, delta);
                codeBuilder.ladd();
                codeBuilder.lstore(currentSlot);
            }

            emitRunItemReadAt(codeBuilder, context, currentSlot, item);
            currentOffset = addValues(offset, itemSize(item));
        }
    }

    private static void emitLoopRead(CodeBuilder codeBuilder, EmitContext context, int indexSlot, RunItem.ForIndex loop) {
        final Label startLabel = codeBuilder.newLabel();
        final Label endLabel = codeBuilder.newLabel();
        final int loopIndexSlot = localSlot(codeBuilder, context, loop.index());

        emitIntValue(codeBuilder, context, loop.start());
        codeBuilder.istore(loopIndexSlot);

        codeBuilder.labelBinding(startLabel);
        codeBuilder.iload(loopIndexSlot);
        emitIntValue(codeBuilder, context, loop.end());
        codeBuilder.if_icmpge(endLabel);

        for (RunStep step : loop.body()) {
            switch (step) {
                case RunStep.Get get -> {
                    codeBuilder.aload(context.directSlot());
                    emitOffsetValue(codeBuilder, context, indexSlot, get.offset());
                    emitStoreKindRead(codeBuilder, get.kind(), get.out());
                    emitStoreLocal(codeBuilder, context, get.out());
                }
                case RunStep.GetBytes getBytes -> {
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
                case RunStep.Apply apply -> {
                    codeBuilder.getstatic(context.classDesc(), transformFunctionName(context.data(), apply.function()), CD_FUNCTION);
                    emitLoadLocal(codeBuilder, context, apply.in());
                    codeBuilder.invokeinterface(CD_FUNCTION, "apply", MT_FUNCTION_APPLY);
                    emitStoreLocal(codeBuilder, context, apply.out());
                }
                case RunStep.Cast cast -> {
                    emitLoadLocal(codeBuilder, context, cast.in());
                    codeBuilder.checkcast(classDesc(cast.targetClass()));
                    emitStoreLocal(codeBuilder, context, cast.out());
                }
                case RunStep.Unbox unbox -> {
                    emitLoadLocal(codeBuilder, context, unbox.in());
                    final ClassDesc wrapper = unbox.kind().wrapperClass().describeConstable().orElseThrow();
                    codeBuilder.checkcast(wrapper);
                    emitUnboxedTypeIrValue(codeBuilder, unbox.kind());
                    emitStoreLocal(codeBuilder, context, unbox.out());
                }
                case RunStep.Box box -> {
                    emitLoadLocal(codeBuilder, context, box.in());
                    emitBoxedTypeIrValue(codeBuilder, box.kind());
                    emitStoreLocal(codeBuilder, context, box.out());
                }
                case RunStep.ArraySet arraySet -> {
                    emitLoadLocal(codeBuilder, context, arraySet.array());
                    codeBuilder.checkcast(CD_OBJECT_ARRAY);
                    emitIntValue(codeBuilder, context, arraySet.index());
                    emitValue(codeBuilder, context, arraySet.value());
                    codeBuilder.aastore();
                }
                case RunStep.Construct construct -> {
                    final ClassDesc constructorType = constructorInterface(construct.args().size());
                    codeBuilder.getstatic(context.classDesc(), ctorFieldName(context.data(), construct.factory()), constructorType);
                    for (Value arg : construct.args()) {
                        emitValue(codeBuilder, context, arg);
                    }
                    codeBuilder.invokeinterface(constructorType, "apply", constructorMethodType(construct.args().size()));
                    emitStoreLocal(codeBuilder, context, construct.out());
                }
                default -> throw new UnsupportedOperationException("Unsupported run step: " + step);
            }
        }

        codeBuilder.iinc(loopIndexSlot, 1);
        codeBuilder.goto_(startLabel);
        codeBuilder.labelBinding(endLabel);
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
            case Value.IsLeft isLeft -> {
                emitValue(codeBuilder, context, isLeft.value());
                codeBuilder.instanceOf(CD_EITHER_LEFT);
            }
            case Value.EitherLeft eitherLeft -> {
                emitValue(codeBuilder, context, eitherLeft.value());
                codeBuilder.checkcast(CD_EITHER_LEFT);
                codeBuilder.invokevirtual(CD_EITHER_LEFT, "value", MethodTypeDesc.of(CD_OBJECT));
            }
            case Value.EitherRight eitherRight -> {
                emitValue(codeBuilder, context, eitherRight.value());
                codeBuilder.checkcast(CD_EITHER_RIGHT);
                codeBuilder.invokevirtual(CD_EITHER_RIGHT, "value", MethodTypeDesc.of(CD_OBJECT));
            }
            case Value.BoolByte boolByte -> emitValue(codeBuilder, context, boolByte.booleanValue());
            case Value.UnsignedByte unsignedByte -> {
                emitValue(codeBuilder, context, unsignedByte.byteValue());
                codeBuilder.sipush(0xFF)
                        .iand();
            }
            case Value.LessThanOrEqual lessThanOrEqual -> {
                if (isIntCompatible(lessThanOrEqual.left()) && isIntCompatible(lessThanOrEqual.right())) {
                    emitIntValue(codeBuilder, context, lessThanOrEqual.left());
                    emitIntValue(codeBuilder, context, lessThanOrEqual.right());
                    final Label elseLabel = codeBuilder.newLabel();
                    final Label endLabel = codeBuilder.newLabel();
                    codeBuilder.if_icmpgt(elseLabel)
                            .iconst_1()
                            .goto_(endLabel)
                            .labelBinding(elseLabel)
                            .iconst_0()
                            .labelBinding(endLabel);
                } else {
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
            }
            case Value.GreaterThan greaterThan -> {
                if (isIntCompatible(greaterThan.left()) && isIntCompatible(greaterThan.right())) {
                    emitIntValue(codeBuilder, context, greaterThan.left());
                    emitIntValue(codeBuilder, context, greaterThan.right());
                    final Label elseLabel = codeBuilder.newLabel();
                    final Label endLabel = codeBuilder.newLabel();
                    codeBuilder.if_icmple(elseLabel)
                            .iconst_1()
                            .goto_(endLabel)
                            .labelBinding(elseLabel)
                            .iconst_0()
                            .labelBinding(endLabel);
                } else {
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
                codeBuilder.invokevirtual(CD_STRING, "getBytes", MethodTypeDesc.of(CD_BYTE_ARRAY, CD_CHARSET));
                codeBuilder.arraylength();
            }
            case Value.VarIntSize varIntSize -> {
                emitIntValue(codeBuilder, context, varIntSize.intValue());
                codeBuilder.invokestatic(CD_NETWORK_BUFFER_TYPE_IMPL, "varIntSize", MT_VAR_INT_SIZE, true);
            }
            case Value.Add add -> {
                if (isIntCompatible(add.left()) && isIntCompatible(add.right())) {
                    emitIntValue(codeBuilder, context, add.left());
                    emitIntValue(codeBuilder, context, add.right());
                    codeBuilder.iadd();
                } else {
                    emitLongValue(codeBuilder, context, add.left());
                    emitLongValue(codeBuilder, context, add.right());
                    codeBuilder.ladd();
                }
            }
            case Value.Mul mul -> {
                if (isIntCompatible(mul.left()) && isIntCompatible(mul.right())) {
                    emitIntValue(codeBuilder, context, mul.left());
                    emitIntValue(codeBuilder, context, mul.right());
                    codeBuilder.imul();
                } else {
                    emitLongValue(codeBuilder, context, mul.left());
                    emitLongValue(codeBuilder, context, mul.right());
                    codeBuilder.lmul();
                }
            }
            default -> throw new UnsupportedOperationException("Unsupported IR value: " + value);
        }
    }

    private static boolean isIntCompatible(Value value) {
        return switch (value) {
            case Value.LocalValue localValue -> localValue.local().type() instanceof LocalType.Kind(TypeKind kind) &&
                    kind != TypeKind.LONG && kind != TypeKind.DOUBLE && kind != TypeKind.FLOAT;
            case Value.Const constant -> constant.value() instanceof Integer || constant.value() instanceof Short ||
                    constant.value() instanceof Byte || constant.value() instanceof Boolean;
            case Value.ArrayLength _, Value.CollectionSize _, Value.MapSize _, Value.VarIntSize _ -> true;
            case Value.Add add -> isIntCompatible(add.left()) && isIntCompatible(add.right());
            case Value.Mul mul -> isIntCompatible(mul.left()) && isIntCompatible(mul.right());
            default -> false;
        };
    }

    private static void emitIntValue(CodeBuilder codeBuilder, EmitContext context, Value value) {
        switch (value) {
            case Value.LocalValue localValue -> {
                emitLoadLocal(codeBuilder, context, localValue.local());
                if (localValue.local().type() instanceof LocalType.Kind(TypeKind kind) && kind == TypeKind.LONG) {
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
                if (localValue.local().type() instanceof LocalType.Kind(TypeKind kind) && kind != TypeKind.LONG) {
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
        if (offset instanceof Value.Const(Object c) && ((c instanceof Long l && l == 0L) || (c instanceof Integer i && i == 0))) {
            codeBuilder.lload(indexSlot);
            return;
        }
        codeBuilder.lload(indexSlot);
        emitLongValue(codeBuilder, context, offset);
        codeBuilder.ladd();
    }

    private static void emitConstant(CodeBuilder codeBuilder, @Nullable Object value) {
        if (value == null) {
            codeBuilder.aconst_null();
        } else if (value instanceof ConstantDesc constantDesc) {
            codeBuilder.loadConstant(constantDesc);
        } else if (value instanceof Boolean booleanValue) {
            if (booleanValue) codeBuilder.iconst_1();
            else codeBuilder.iconst_0();
        } else if (value == Unit.INSTANCE) {
            codeBuilder.getstatic(CD_UNIT, "INSTANCE", CD_UNIT);
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
            case Value.LocalValue localValue -> localValue.local().type() instanceof LocalType.Kind(TypeKind kind) &&
                    kind == TypeKind.LONG;
            case Value.Const constant -> constant.value() instanceof Long;
            case Value.Add _, Value.Mul _ -> true;
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

    private static String typeFieldName(IrClassData data, NetworkBuffer.Type<?> type) {
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

    private static String ctorFieldName(IrClassData data, Object factory) {
        for (Map.Entry<String, IrCtorData> entry : data.constructorIrs().entrySet()) {
            if (entry.getValue().factory() == factory) return entry.getKey();
        }
        throw new IllegalStateException("Missing constructor field");
    }

    private static MethodTypeDesc constructorMethodType(int fieldCount) {
        final ClassDesc[] params = new ClassDesc[fieldCount];
        Arrays.fill(params, CD_OBJECT);
        return MethodTypeDesc.of(CD_OBJECT, params);
    }

    private static ClassDesc classDesc(Class<?> type) {
        return type.describeConstable().orElseThrow();
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

    private static void emitDirectBuffer(CodeBuilder codeBuilder, int directSlot) {
        codeBuilder.aload(1)
                .checkcast(CD_NETWORK_BUFFER_IMPL)
                .astore(directSlot);
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

    private static boolean isZero(Value value) {
        return value instanceof Value.Const(Object c) && c instanceof Number n && n.longValue() == 0;
    }

    private static @Nullable Value subtract(Value offset, Value lastOffset) {
        if (offset.equals(lastOffset)) return new Value.Const(0L);
        if (offset instanceof Value.Add add) {
            if (add.left().equals(lastOffset)) return add.right();
            // Try recursing left (standard for left-leaning trees)
            Value sub = subtract(add.left(), lastOffset);
            if (sub != null) return addValues(sub, add.right());
        }
        return null;
    }

    private static CodeBuilder loadClassDataAt(CodeBuilder codeBuilder, ClassDesc type, int index) {
        return codeBuilder.aload(0) // assumes lookup is at slot 0
                .ldc("_")
                .ldc(type)
                .loadConstant(index)
                .invokestatic(CD_METHOD_HANDLES, "classDataAt", MT_CLASS_DATA_AT)
                .checkcast(type);
    }

    private static MethodTypeDesc constructorApplyType(int fieldCount) {
        final ClassDesc[] params = new ClassDesc[fieldCount];
        Arrays.fill(params, CD_OBJECT);
        return MethodTypeDesc.of(CD_OBJECT, params);
    }

    record EmitContext(ClassDesc classDesc, IrClassData data, int directSlot, Map<Local, Integer> locals) {
    }
}
