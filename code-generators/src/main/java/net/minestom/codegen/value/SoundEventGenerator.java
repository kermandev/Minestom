package net.minestom.codegen.value;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.palantir.javapoet.*;
import net.minestom.codegen.CodegenRegistry;
import net.minestom.codegen.CodegenValue;

import javax.lang.model.element.Modifier;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class SoundEventGenerator extends RegistryValueGenerator {

    @Override // TODO not do this.
    protected void generateValues(Path outputFolder, CodegenRegistry registry, CodegenValue value) {
        InputStreamReader resourceFile = registry.resource(value.resource());
        InputStreamReader gsonFile = registry.optionalResource(value.tagResource());
        ClassName typeClass = ClassName.get(value.packageName(), value.typeName());
        ClassName implType = ClassName.get(value.packageName(), value.loaderName());
        ClassName tagsClass = ClassName.get(value.packageName(), value.tagsName());
        ClassName keyClass = ClassName.get(value.packageName(), value.keysName());
        JsonObject json = GSON.fromJson(resourceFile, JsonObject.class);
        JsonObject tagJson = gsonFile != null ? GSON.fromJson(gsonFile, JsonObject.class) : null;
        ClassName generatedCN = ClassName.get(value.packageName(), value.valuesName());
        // BlockConstants class
        TypeSpec.Builder blockConstantsClass = TypeSpec.classBuilder(generatedCN)
                // Add @SuppressWarnings("unused")
                .addModifiers(Modifier.FINAL)
                .addAnnotation(SUPPRESS_ANNOTATION)
                .addJavadoc(generateJavadoc(typeClass));

        ParameterizedTypeName registryType = ParameterizedTypeName.get(REGISTRY, implType);
        ParameterizedTypeName argumentType = ParameterizedTypeName.get(REGISTRY_KEY, registryType);

        MethodSpec.Builder builder = MethodSpec.methodBuilder("load")
                .returns(registryType)
                .addParameter(ParameterSpec.builder(argumentType, "key", Modifier.FINAL).build())
                .addModifiers(Modifier.STATIC).addModifiers(Modifier.PUBLIC);

        Set<Map.Entry<String, JsonElement>> staticEntries = json.asMap().entrySet();
        Set<Map.Entry<String, JsonElement>> tagEntries = tagJson != null ? tagJson.asMap().entrySet() : Set.of();

        final ParameterizedTypeName builderType = ParameterizedTypeName.get(REGISTRY_BUILDER, implType);
        builder.addCode(CodeBlock.builder()
                .addStatement("final $T builder = $T.builder(key, $L, $L)", builderType, REGISTRY_BUILDER, staticEntries.size(), 0).build()
        );

        for (var entry : staticEntries) {
            final String namespace = entry.getKey();
            final JsonObject element = entry.getValue().getAsJsonObject();
            final int id = element.get("id").getAsInt();
            final String constantName = toConstant(namespace);
            final Static codeEntry = new Static(typeClass, implType, keyClass, namespace, constantName, id, element);
            builder.addCode( // register
                    CodeBlock.builder().addStatement(
                            "builder.register($L)", generateValue(registry, value, codeEntry)
                    ).build()
            );
        }

        for (var entry: tagEntries) {
            final String namespace = entry.getKey();
            final JsonObject element = entry.getValue().getAsJsonObject();
            final String constantName = toConstant(namespace);
            builder.addCode(
                    CodeBlock.builder()
                            .addStatement("builder.registerTag($T.$L, $L)", tagsClass, constantName, generateTagValue(
                                    registry, value, new Tag(tagJson, keyClass, element)
                            ))
                            .build()
            );
        }

        builder.addCode(CodeBlock.builder()
                .addStatement("return builder.build()").build());

        blockConstantsClass.addMethod(builder.build());

        writeFiles(outputFolder, JavaFile.builder(value.packageName(), blockConstantsClass.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build()
        );
    }
}
