package net.minestom.codegen.value;

import com.palantir.javapoet.CodeBlock;
import net.minestom.codegen.CodegenRegistry;
import net.minestom.codegen.CodegenValue;

public class FeatureFlagGenerator extends RegistryValueGenerator {
    @Override
    public CodeBlock generateValue(CodegenRegistry registry, CodegenValue value, Static staticEntry) {
        return CodeBlock.builder().add(
                "new $T($L, $L)",
                staticEntry.implType(),
                staticEntry.key(),
                staticEntry.id()
        ).build();
    }
}
