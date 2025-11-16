package net.minestom.codegen.value;

import com.palantir.javapoet.CodeBlock;
import net.minestom.codegen.CodegenRegistry;
import net.minestom.codegen.CodegenValue;

public class AttributeValueGenerator extends RegistryValueGenerator {

    @Override
    public CodeBlock generateValue(CodegenRegistry registry, CodegenValue value, Static codeEntry) {
        String translationKey = codeEntry.value().get("translationKey").getAsString();
        double defaultValue = codeEntry.value().get("defaultValue").getAsDouble();
        boolean clientSync = codeEntry.value().get("clientSync").getAsBoolean();
        double maxValue = codeEntry.value().get("maxValue").getAsDouble();
        double minValue = codeEntry.value().get("minValue").getAsDouble();

        return CodeBlock.builder().add(
                "new $T($L, $L, $S, $L, $L, $L, $L)",
                codeEntry.implType(),
                codeEntry.key(),
                codeEntry.id(),
                translationKey,
                defaultValue,
                clientSync,
                maxValue,
                minValue

        ).build();
    }
}
