package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.Unit;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.*;
import java.util.function.Function;

import static net.minestom.server.network.ir.IrCompiler.constructorInterface;

final class IrEmitter {
    static final ClassDesc CD_OBJECT = ConstantDescs.CD_Object;
    static final ClassDesc CD_OBJECT_ARRAY = CD_OBJECT.arrayType();
    static final ClassDesc CD_STRING = ConstantDescs.CD_String;
    static final ClassDesc CD_INT = ConstantDescs.CD_int;
    static final ClassDesc CD_LONG = ConstantDescs.CD_long;
    static final ClassDesc CD_FLOAT = ConstantDescs.CD_float;
    static final ClassDesc CD_DOUBLE = ConstantDescs.CD_double;
    static final ClassDesc CD_SHORT = ConstantDescs.CD_short;
    static final ClassDesc CD_BYTE = ConstantDescs.CD_byte;
    static final ClassDesc CD_BOOLEAN = ConstantDescs.CD_boolean;
    static final ClassDesc CD_BYTE_ARRAY = CD_BYTE.arrayType();
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
    static final ClassDesc CD_NETWORK_BUFFER_IMPL = ClassDesc.of("net.minestom.server.network", "NetworkBufferImpl");
    static final ClassDesc CD_NETWORK_BUFFER_TYPE_IMPL = ClassDesc.of("net.minestom.server.network", "NetworkBufferTypeImpl");
    static final ClassDesc CD_TYPE = CD_NETWORK_BUFFER.nested("Type");
    static final ClassDesc CD_FUNCTION = Function.class.describeConstable().orElseThrow();
    static final ClassDesc CD_UNIT = Unit.class.describeConstable().orElseThrow();
    static final ClassDesc CD_STANDARD_CHARSETS = ClassDesc.of("java.nio.charset", "StandardCharsets");
    static final ClassDesc CD_CHARSET = ClassDesc.of("java.nio.charset", "Charset");
    static final ClassDesc CD_COLLECTION = ClassDesc.of("java.util", "Collection");
    static final ClassDesc CD_LIST = ClassDesc.of("java.util", "List");
    static final ClassDesc CD_ARRAY_UTILS = ClassDesc.of("net.minestom.server.utils", "ArrayUtils");
    static final ClassDesc CD_ITERABLE = ClassDesc.of("java.lang", "Iterable");
    static final ClassDesc CD_ITERATOR = ClassDesc.of("java.util", "Iterator");
    static final ClassDesc CD_MAP = ClassDesc.of("java.util", "Map");
    static final ClassDesc CD_MAP_ENTRY = CD_MAP.nested("Entry");
    static final ClassDesc CD_SET = ClassDesc.of("java.util", "Set");
    static final ClassDesc CD_EITHER = ClassDesc.of("net.minestom.server.utils", "Either");
    static final ClassDesc CD_EITHER_LEFT = CD_EITHER.nested("Left");
    static final ClassDesc CD_EITHER_RIGHT = CD_EITHER.nested("Right");

    static final MethodTypeDesc MT_VOID = MethodTypeDesc.of(CD_VOID);
    static final MethodTypeDesc MT_LOOKUP = MethodTypeDesc.of(CD_METHOD_HANDLES_LOOKUP);
    static final MethodTypeDesc MT_CLASS_DATA_AT = MethodTypeDesc.of(CD_OBJECT, CD_METHOD_HANDLES_LOOKUP, CD_STRING, ConstantDescs.CD_Class, CD_INT);
    static final MethodTypeDesc MT_READ_OBJECT = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER);
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
    static final MethodTypeDesc MT_TO_MAP = MethodTypeDesc.of(CD_MAP, CD_OBJECT_ARRAY, CD_OBJECT_ARRAY, CD_INT);

    static final int FIELD_FLAGS = ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    static final int METHOD_FLAGS = ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    static final int CLASS_FLAGS = ClassFile.ACC_FINAL | ClassFile.ACC_SUPER | ClassFile.ACC_SYNTHETIC;

    static final String READ = "read";
    static final String WRITE = "write";

    private IrEmitter() {
    }

    static byte[] emit(ClassDesc classDesc, IrClassData data) {
        return buildClass(classDesc, data);
    }

    private static byte[] buildClass(ClassDesc classDesc, IrClassData data) {
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
        for (IrClassData.ExternalTypeFieldData external : data.externalTypes()) {
            classBuilder.withField(external.name(), CD_TYPE, FIELD_FLAGS);
        }
        for (IrClassData.TransformFieldData transform : data.transforms()) {
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
        for (IrClassData.ExternalTypeFieldData external : data.externalTypes()) {
            loadClassDataAt(codeBuilder, CD_TYPE, external.dataIndex())
                    .putstatic(classDesc, external.name(), CD_TYPE);
        }
        for (IrClassData.TransformFieldData transform : data.transforms()) {
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
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        emitDirectBuffer(codeBuilder, directSlot);
        final EmitContext context = new EmitContext(classDesc, data, directSlot, indexSlot, new IdentityHashMap<>(), true);
        if (program.initialSource() != null) {
            context.locals().put(program.initialSource(), objectSlot);
        }
        IrRunEmitter.emitProgram(codeBuilder, context, program);
        codeBuilder.return_();
    }

    private static void buildRead(CodeBuilder codeBuilder, ClassDesc classDesc, IrClassData data, ProgramIr program) {
        final int directSlot = codeBuilder.allocateLocal(TypeKind.REFERENCE);
        final int indexSlot = codeBuilder.allocateLocal(TypeKind.LONG);
        emitDirectBuffer(codeBuilder, directSlot);
        final EmitContext context = new EmitContext(classDesc, data, directSlot, indexSlot, new IdentityHashMap<>(), false);
        IrRunEmitter.emitProgram(codeBuilder, context, program);
    }

    static ClassDesc classDesc(Class<?> type) {
        return type.describeConstable().orElseThrow();
    }

    private static void emitDirectBuffer(CodeBuilder codeBuilder, int directSlot) {
        codeBuilder.aload(1)
                .checkcast(CD_NETWORK_BUFFER_IMPL)
                .astore(directSlot);
    }

    private static CodeBuilder loadClassDataAt(CodeBuilder codeBuilder, ClassDesc type, int index) {
        return codeBuilder.aload(0) // assumes lookup is at slot 0
                .ldc("_")
                .ldc(type)
                .loadConstant(index)
                .invokestatic(CD_METHOD_HANDLES, "classDataAt", MT_CLASS_DATA_AT)
                .checkcast(type);
    }

    static MethodTypeDesc constructorApplyType(int fieldCount) {
        final ClassDesc[] params = new ClassDesc[fieldCount];
        Arrays.fill(params, CD_OBJECT);
        return MethodTypeDesc.of(CD_OBJECT, params);
    }

    record EmitContext(ClassDesc classDesc, IrClassData data, int directSlot, int indexSlot, Map<Local, Integer> locals,
                       boolean write) {
    }
}
