package net.minestom.codegen.value;

import com.palantir.javapoet.CodeBlock;
import net.minestom.codegen.CodegenRegistry;
import net.minestom.codegen.CodegenValue;

public final class GameEventValueGenerator extends RegistryValueGenerator {

    @Override
    public CodeBlock generateValue(CodegenRegistry registry, CodegenValue value, Static staticEntry) {
        int notificationRadius = staticEntry.value().get("notificationRadius").getAsInt();

        return CodeBlock.builder().add(
                "new $T($L, $L, $L)",
                staticEntry.implType(),
                staticEntry.key(),
                staticEntry.id(),
                notificationRadius
        ).build();
    }
}
