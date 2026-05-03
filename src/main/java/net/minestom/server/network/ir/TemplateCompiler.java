package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.lang.constant.ClassDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static net.minestom.server.network.ir.IrMetadata.*;

public final class TemplateCompiler {
    private TemplateCompiler() {}

    public static final String PACKAGE = "net.minestom.server.network";
    public static final String TYPE_PREFIX = "t";
    public static final String GETTER_PREFIX = "g";
    public static final String TRANSFORM_TO_PREFIX = "to";
    public static final String TRANSFORM_FROM_PREFIX = "from";
    public static final String FACTORY_PREFIX = "fac";
    public static final String CTOR_NAME = "ctor";
    public static final String IR_FIELD_NAME = "networkIr";
    public static final String IR = "ir";
    public static final String READ = "read";
    public static final String WRITE = "write";

    public static final boolean DEBUG = true;
    public static final Path DUMP_ROOT = Path.of("build", "generated", "network-templates");
    public static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static final Function<String, byte[]> STRING_TO_BYTES = s -> s.getBytes(StandardCharsets.UTF_8);
    public static final Function<byte[], String> BYTES_TO_STRING = b -> new String(b, StandardCharsets.UTF_8);

    @SuppressWarnings("unchecked")
    public static <T extends @UnknownNullability Object> NetworkBuffer.Type<T> template(Object... values) {
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
            final NetworkIr<T> ir = IrLowering.networkIr("NetworkTemplate", values, fieldCount, constructorIr);
            final IrClassData irData = IrLowering.collectIrClassData(classData, ir);
            final int irIndex = addClassData(classData, ir);

            final byte[] bytes = IrEmitter.buildClass(classDesc, irData, irIndex);
            if (DEBUG) dump(bytes, fieldCount);

            final Lookup lookup = net.minestom.server.network.NetworkBufferTemplate.lookup().defineHiddenClassWithClassData(bytes, List.copyOf(classData), true);
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
                    return declaringClass != TemplateCompiler.class && declaringClass != NetworkBufferTemplate.class;
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

    public static String typeName(int index) {
        return typeName(Integer.toString(index + 1));
    }

    public static String getterName(int index) {
        return getterName(Integer.toString(index + 1));
    }

    public static String typeName(String path) {
        return TYPE_PREFIX + path;
    }

    public static String getterName(String path) {
        return GETTER_PREFIX + path;
    }

    public static String transformToName(String path, int level) {
        return TRANSFORM_TO_PREFIX + path + "_" + (level + 1);
    }

    public static String transformFromName(String path, int level) {
        return TRANSFORM_FROM_PREFIX + path + "_" + (level + 1);
    }

    public static String factoryName(String path) {
        return FACTORY_PREFIX + path;
    }

    public static String ctorName(String path) {
        return path.isEmpty() ? CTOR_NAME : CTOR_NAME + path;
    }

    public static ClassDesc constructorInterface(int fieldCount) {
        return ClassDesc.of("net.minestom.server.network", "NetworkBufferTemplate$F" + fieldCount);
    }

    private static int addClassData(List<Object> classData, Object value) {
        final int index = classData.size();
        classData.add(value);
        return index;
    }
}
