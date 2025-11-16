package net.minestom.codegen.value;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import net.minestom.codegen.CodegenRegistry;
import net.minestom.codegen.CodegenValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

public class MaterialValueGenerator extends RegistryValueGenerator {

    @Override
    public CodeBlock generateValue(CodegenRegistry registry, CodegenValue value, Static codeEntry) {
        String translationKey = codeEntry.value().get("translationKey").getAsString();
        final CodeBlock accompanyingBlock;
        if (codeEntry.value().get("correspondingBlock") != null) {
            String correspondingBlock = codeEntry.value().get("correspondingBlock").getAsString();
            CodegenValue blockValue = registry.get("block");
            ClassName blockKey = ClassName.get(blockValue.packageName(), blockValue.typeName());
            accompanyingBlock = CodeBlock.of("$T.$L", blockKey, blockValue.generator().get().toConstant(correspondingBlock));
        } else {
            accompanyingBlock = CodeBlock.of("null");
        }

        final JsonElement componentElement = codeEntry.value().get("components");
        CodeBlock generatedComponents = generateJsonValue(componentElement);

        return CodeBlock.builder().add(
                "new $T($L, $L, $S, $L, new $T($L))",
                codeEntry.implType(),
                codeEntry.key(),
                codeEntry.id(),
                translationKey,
                accompanyingBlock,
                codeEntry.implType().nestedClass("Record"), generatedComponents
        ).build();
    }

    @Override
    protected Collection<String> requirements() {
        return Set.of("block");
    }

    // Helper to turn any json element into its correct elements.
    public static CodeBlock generateJsonValue(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.isEmpty()) {
                return CodeBlock.of("$T.of()", Map.class);
            }
            if (obj.size() <= 10) {
                return CodeBlock.builder()
                        .add("$T.of(", Map.class)
                        .add(obj.entrySet().stream()
                                .map(entry -> CodeBlock.of("$S, $L", entry.getKey(), generateJsonValue(entry.getValue())))
                                .collect(CodeBlock.joining(", ")))
                        .add(")")
                        .build();
            } else {
                return CodeBlock.builder()
                        .add("$T.ofEntries(", Map.class)
                        .add(obj.entrySet().stream()
                                .map(entry -> CodeBlock.of("$T.entry($S, $L)", Map.class, entry.getKey(), generateJsonValue(entry.getValue())))
                                .collect(CodeBlock.joining(", ")))
                        .add(")")
                        .build();
            }
        } else if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            if (array.isEmpty()) {
                return CodeBlock.of("$T.of()", List.class);
            }
            return CodeBlock.builder()
                    .add("$T.of(", List.class)
                    .add(StreamSupport.stream(array.spliterator(), false)
                            .map(MaterialValueGenerator::generateJsonValue)
                            .collect(CodeBlock.joining(", ")))
                    .add(")")
                    .build();
        } else if (element.isJsonPrimitive()) {
            var primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return CodeBlock.of("$S", primitive.getAsString());
            } else if (primitive.isNumber()) {
                return CodeBlock.of("$L", primitive.getAsNumber());
            } else if (primitive.isBoolean()) {
                return CodeBlock.of("$L", primitive.getAsBoolean());
            }
        }
        return CodeBlock.of("null");
    }



}
