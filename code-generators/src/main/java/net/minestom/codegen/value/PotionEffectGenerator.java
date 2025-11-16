package net.minestom.codegen.value;

import com.palantir.javapoet.CodeBlock;
import net.minestom.codegen.CodegenRegistry;
import net.minestom.codegen.CodegenValue;

public class PotionEffectGenerator extends RegistryValueGenerator {

    @Override
    public CodeBlock generateValue(CodegenRegistry registry, CodegenValue value, Static codeEntry) {
        String translationKey = codeEntry.value().get("translationKey").getAsString();
        int color = codeEntry.value().get("color").getAsInt();
        boolean instantaneous = codeEntry.value().get("instantaneous").getAsBoolean();

        return CodeBlock.builder().add(
                "new $T($L, $L, $S, $L, $L)",
                codeEntry.implType(),
                codeEntry.key(),
                codeEntry.id(),
                translationKey,
                color,
                instantaneous
        ).build();
    }
}
