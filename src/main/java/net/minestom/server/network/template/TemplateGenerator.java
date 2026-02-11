package net.minestom.server.network.template;

import net.minestom.server.network.NetworkBuffer;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.ClassFile;
import java.lang.constant.*;
import java.lang.invoke.MethodHandle;

@SuppressWarnings("ClassCanBeRecord") // Not necessary currently.
final class TemplateGenerator {
    private static final ClassDesc CD_OBJECT = ConstantDescs.CD_Object;
    private static final ClassDesc CD_VOID = ConstantDescs.CD_void;
    private static final ClassDesc CD_INT = ConstantDescs.CD_int;
    private static final ClassDesc CD_NETWORK_BUFFER = NetworkBuffer.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_TYPE = NetworkBuffer.Type.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_PRIMITIVE_TYPE = PrimitiveType.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_METHOD_HANDLE = ConstantDescs.CD_MethodHandle;
    private static final ClassDesc CD_NETWORK_TEMPLATE = NetworkTemplate.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_METHOD_HANDLES = ClassDesc.of("java.lang.invoke.MethodHandles");
    private static final ClassDesc CD_LOOKUP = ClassDesc.of("java.lang.invoke.MethodHandles$Lookup");

    private static final DirectMethodHandleDesc BSM_CLASS_DATA_AT = MethodHandleDesc.ofMethod(
            DirectMethodHandleDesc.Kind.STATIC,
            CD_METHOD_HANDLES,
            "classDataAt",
            MethodTypeDesc.of(CD_OBJECT, CD_LOOKUP, ConstantDescs.CD_String, ConstantDescs.CD_Class, CD_INT)
    );

    private static final MethodTypeDesc MT_READ_OBJECT = MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER);
    private static final MethodTypeDesc MT_WRITE_OBJECT = MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_OBJECT);

    private static final int METHOD_FLAGS = ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;

    private static final String READ = "read";
    private static final String WRITE = "write";

    final ClassDesc thisClass;
    final MethodHandle ctor;
    final TemplateReflection.FieldAnalysis[] fields;
    final TemplateReflection.FieldAnalysis[] uniqueFields;
    final int[] fieldToUniqueIndex;

    public TemplateGenerator(ClassDesc classDesc, MethodHandle ctor, TemplateReflection.FieldAnalysis[] fields, TemplateReflection.FieldAnalysis[] uniqueFields, int[] fieldToUniqueIndex) {
        this.thisClass = classDesc;
        this.ctor = ctor;
        this.fields = fields;
        this.uniqueFields = uniqueFields;
        this.fieldToUniqueIndex = fieldToUniqueIndex;
        super();
    }

    public byte[] build() {
        return ClassFile.of().build(thisClass, classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_FINAL | ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER | ClassFile.ACC_SYNTHETIC);
            classBuilder.withInterfaceSymbols(CD_NETWORK_TEMPLATE);

            buildStaticFields(classBuilder);
            buildStaticInitializer(classBuilder);
            buildConstructor(classBuilder);
            buildWriteMethod(classBuilder);
            buildReadMethod(classBuilder);
        });
    }

    private void buildStaticFields(ClassBuilder cb) {
        // Types
        for (int i = 0; i < uniqueFields.length; i++) {
            boolean isPrimitive = uniqueFields[i].physicalType() instanceof PrimitiveType;
            cb.withField("T_" + i, isPrimitive ? CD_PRIMITIVE_TYPE : CD_TYPE, ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL);
        }
        // Getters
        for (int i = 0; i < fields.length; i++) {
            cb.withField("G_" + i, CD_METHOD_HANDLE, ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL);
        }
        // Ctor
        cb.withField("C", CD_METHOD_HANDLE, ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL);
    }

    private void buildStaticInitializer(ClassBuilder cb) {
        cb.withMethodBody(ConstantDescs.CLASS_INIT_NAME, MethodTypeDesc.of(CD_VOID), ClassFile.ACC_STATIC, code -> {
            // Types
            for (int i = 0; i < uniqueFields.length; i++) {
                boolean isPrimitive = uniqueFields[i].physicalType() instanceof PrimitiveType;
                ClassDesc type = isPrimitive ? CD_PRIMITIVE_TYPE : CD_TYPE;
                loadConstant(code, i, type);
                code.putstatic(thisClass, "T_" + i, type);
            }
            // Getters
            for (int i = 0; i < fields.length; i++) {
                loadConstant(code, uniqueFields.length + i, CD_METHOD_HANDLE);
                code.putstatic(thisClass, "G_" + i, CD_METHOD_HANDLE);
            }
            // Ctor
            loadConstant(code, uniqueFields.length + fields.length, CD_METHOD_HANDLE);
            code.putstatic(thisClass, "C", CD_METHOD_HANDLE);

            code.return_();
        });
    }

    private void loadConstant(CodeBuilder code, int index, ClassDesc type) {
        code.ldc(DynamicConstantDesc.ofNamed(
                BSM_CLASS_DATA_AT,
                "_",
                type,
                index
        ));
    }

    public void buildConstructor(ClassBuilder cb) {
        cb.withMethodBody(ConstantDescs.INIT_NAME, MethodTypeDesc.of(CD_VOID), ClassFile.ACC_PUBLIC | ClassFile.ACC_SYNTHETIC, code -> {
            code.aload(0);
            code.invokespecial(CD_OBJECT, ConstantDescs.INIT_NAME, MethodTypeDesc.of(CD_VOID));
            code.return_();
        });
    }

    public void buildWriteMethod(ClassBuilder cb) {
        cb.withMethodBody(WRITE, MT_WRITE_OBJECT, METHOD_FLAGS, code -> {
            for (int i = 0; i < fields.length; i++) {
                TemplateReflection.FieldAnalysis fa = fields[i];
                int uniqueIndex = fieldToUniqueIndex[i];
                boolean isPrimitive = fa.physicalType() instanceof PrimitiveType;
                ClassDesc fieldDesc = isPrimitive ? CD_PRIMITIVE_TYPE : CD_TYPE;

                // Load Type
                code.getstatic(thisClass, "T_" + uniqueIndex, fieldDesc);
                code.aload(1); // Buffer

                // Load Getter
                code.getstatic(thisClass, "G_" + i, CD_METHOD_HANDLE);
                code.aload(2); // Record Instance (Object)

                // Checkcast the instance to the type the getter expects
                Class<?> receiverType = fa.getter().type().parameterType(0);
                if (receiverType != Object.class) {
                    code.checkcast(receiverType.describeConstable().orElseThrow());
                }

                // invokeExact: (Receiver) -> Value
                code.invokevirtual(CD_METHOD_HANDLE, "invokeExact", MethodTypeDesc.ofDescriptor(fa.getter().type().toMethodDescriptorString()));

                if (isPrimitive) {
                    PrimitiveType<?> pt = (PrimitiveType<?>) fa.physicalType();
                    code.invokeinterface(CD_PRIMITIVE_TYPE, pt.writeMethodName(), MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, fa.physicalDesc()));
                } else {
                    code.invokeinterface(CD_TYPE, WRITE, MethodTypeDesc.of(CD_VOID, CD_NETWORK_BUFFER, CD_OBJECT));
                }
            }
            code.return_();
        });
    }

    public void buildReadMethod(ClassBuilder cb) {
        cb.withMethodBody(READ, MT_READ_OBJECT, METHOD_FLAGS, code -> {
            // Load Ctor
            code.getstatic(thisClass, "C", CD_METHOD_HANDLE);

            for (int i = 0; i < fields.length; i++) {
                TemplateReflection.FieldAnalysis fa = fields[i];
                int uniqueIndex = fieldToUniqueIndex[i];
                boolean isPrimitive = fa.physicalType() instanceof PrimitiveType;
                ClassDesc fieldDesc = isPrimitive ? CD_PRIMITIVE_TYPE : CD_TYPE;

                // Load Type
                code.getstatic(thisClass, "T_" + uniqueIndex, fieldDesc);
                code.aload(1); // Buffer

                if (isPrimitive) {
                    PrimitiveType<?> pt = (PrimitiveType<?>) fa.physicalType();
                    // Read raw primitive (Z, B, I, F, etc.)
                    code.invokeinterface(CD_PRIMITIVE_TYPE, pt.readMethodName(), MethodTypeDesc.of(fa.physicalDesc(), CD_NETWORK_BUFFER));
                } else {
                    // Read Object
                    code.invokeinterface(CD_TYPE, READ, MethodTypeDesc.of(CD_OBJECT, CD_NETWORK_BUFFER));

                    // Checkcast the object to what the ctor expects
                    // The ctor has been conformed in TemoplateReflection to expect exactly what we read.
                    Class<?> expected = ctor.type().parameterType(i);
                    if (expected != Object.class && !expected.isPrimitive()) {
                        code.checkcast(expected.describeConstable().orElseThrow());
                    }
                }
            }

            // invokeExact using the ctor's own MethodType
            code.invokevirtual(CD_METHOD_HANDLE, "invokeExact", MethodTypeDesc.ofDescriptor(ctor.type().toMethodDescriptorString()));

            // Emit a checkcast the return type, just to ensure it throws on mismatch
            Class<?> returnType = ctor.type().returnType();
            if (returnType != Object.class) {
                code.checkcast(returnType.describeConstable().orElseThrow());
            }

            code.areturn();
        });
    }
}