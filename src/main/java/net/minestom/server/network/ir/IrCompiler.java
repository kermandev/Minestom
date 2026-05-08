package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.lang.constant.ClassDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class IrCompiler {
    private IrCompiler() {}

    public static final String PACKAGE = "net.minestom.server.network";
    public static final String TEMPLATE_CLASS_NAME = "NetworkTemplate";

    public static final boolean DEBUG = true;/*Boolean.getBoolean("minestom.network.ir.dump");*/
    public static final Path DUMP_ROOT = Path.of("build", "generated", "network-templates");
    public static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @SuppressWarnings("unchecked")
    public static <T extends @UnknownNullability Object> NetworkBuffer.Type<T> compile(NetworkBuffer.Type<T> type) {
        try {
            final ClassDesc classDesc = ClassDesc.of(PACKAGE, TEMPLATE_CLASS_NAME);
            final List<Object> classData = new ArrayList<>();

            IrLowering.WriteBuilderImpl writeBuilder = new IrLowering.WriteBuilderImpl(new Local(new LocalType.Reference(Object.class)));
            writeBuilder.lower(type, new Value.LocalValue(writeBuilder.source()));
            List<RunIr> writeRuns = RunLowering.lower(writeBuilder.result());
            ProgramIr write = new ProgramIr(IrOptimizer.optimize(writeRuns), writeBuilder.source());
            IrVerifier.verifyWrite(write);

            IrLowering.ReadBuilderImpl readBuilder = new IrLowering.ReadBuilderImpl();
            Value readValue = readBuilder.lower(type);
            Local result = new Local(new LocalType.Reference(Object.class));
            readBuilder.push(new Op.Store(readValue, result));
            readBuilder.push(new Op.Return(new Value.LocalValue(result)));
            List<RunIr> readRuns = RunLowering.lower(readBuilder.result());
            ProgramIr read = new ProgramIr(IrOptimizer.optimize(readRuns));
            IrVerifier.verifyRead(read);

            IrClassData irClassData = IrClassData.collect(classData, write, read);
            final byte[] bytes = IrEmitter.emit(classDesc, irClassData);
            if (DEBUG) dump(bytes);

            final Lookup lookup = NetworkBufferTemplate.lookup().defineHiddenClassWithClassData(bytes, List.copyOf(classData), true);
            final MethodHandle constructor = lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class));
            return (NetworkBuffer.Type<T>) constructor.invoke();
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to generate network type template", throwable);
        }
    }

    private static void dump(byte[] bytes) throws IOException {
        final StackWalker.StackFrame caller = STACK_WALKER.walk(frames -> frames
                .filter(frame -> {
                    final Class<?> declaringClass = frame.getDeclaringClass();
                    return declaringClass != IrCompiler.class && declaringClass != NetworkBufferTemplate.class && declaringClass != NetworkTemplater.class;
                })
                .findFirst()
                .orElseThrow());
        final Path directory = DUMP_ROOT
                .resolve(sanitize(caller.getClassName()))
                .resolve(sanitize(caller.getMethodName()))
                .resolve("line%s-bci%s".formatted(caller.getLineNumber(), caller.getByteCodeIndex()));
        Files.createDirectories(directory);
        Files.write(directory.resolve("NetworkTemplate-%s.class".formatted(caller.getDeclaringClass().getSimpleName())), bytes);
    }

    private static String sanitize(String value) {
        final StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            builder.append(Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_' ? c : '_');
        }
        return builder.toString();
    }

    public static ClassDesc constructorInterface(int fieldCount) {
        return ClassDesc.of("net.minestom.server.network", "NetworkBufferTemplate$F" + fieldCount);
    }
}
