package net.minestom.codegen.value;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.palantir.javapoet.*;
import net.minestom.codegen.CodegenRegistry;
import net.minestom.codegen.CodegenValue;
import net.minestom.codegen.RegistryGenerator;

import javax.lang.model.element.Modifier;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;

public abstract non-sealed class RegistryValueGenerator extends RegistryGenerator {
    protected static final ClassName REGISTRY_DATA = ClassName.get("net.minestom.server.registry", "RegistryData"); // TODO move these types
    protected static final ClassName KEY = ClassName.get("net.kyori.adventure.key", "Key");
    private static final ClassName REGISTRY = ClassName.get("net.minestom.server.registry", "Registry");
    private static final ClassName REGISTRY_KEY = ClassName.get("net.minestom.server.registry", "RegistryKey");
    private static final ClassName REGISTRY_BUILDER = REGISTRY.nestedClass("Builder");

    @Override
    public void generate(Path outputFolder, CodegenRegistry registry, CodegenValue value) {
        if (value.registryType() != CodegenValue.Type.STATIC)
            throw new IllegalStateException("Only static code generators are supported");
        super.generate(outputFolder, registry, value);
        generateValues(outputFolder, registry, value);
    }

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

        ParameterizedTypeName registryType = ParameterizedTypeName.get(REGISTRY, typeClass);
        ParameterizedTypeName argumentType = ParameterizedTypeName.get(REGISTRY_KEY, registryType);

        MethodSpec.Builder builder = MethodSpec.methodBuilder("load")
                .returns(registryType)
                .addParameter(ParameterSpec.builder(argumentType, "key", Modifier.FINAL).build())
                .addModifiers(Modifier.STATIC).addModifiers(Modifier.PUBLIC);

        Set<Map.Entry<String, JsonElement>> staticEntries = json.asMap().entrySet();
        Set<Map.Entry<String, JsonElement>> tagEntries = tagJson != null ? tagJson.asMap().entrySet() : Set.of();

        final ParameterizedTypeName builderType = ParameterizedTypeName.get(REGISTRY_BUILDER, typeClass);
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

    public abstract CodeBlock generateValue(CodegenRegistry registry, CodegenValue value, Static codeEntry);

    public CodeBlock generateTagValue(CodegenRegistry registry, CodegenValue value, Tag tag) {
        List<CodeBlock> declarations = new ArrayList<>();
        for (var entry: tag.value().getAsJsonArray("values")) {
            var tagNamespace = entry.getAsString();
            if (tagNamespace.startsWith("#")) {
                final String newNamespace = tagNamespace.substring(1);
                final JsonObject newObject = tag.lookupObject().getAsJsonObject(newNamespace);
                declarations.add(generateTagValue(registry, value, new Tag(tag.lookupObject(), tag.keysTypes(), newObject)));
            } else {
                declarations.add(CodeBlock.builder()
                        .add("$T.$L", tag.keysTypes(), toConstant(tagNamespace)).build()
                );
            }
        }

        return CodeBlock.join(declarations, ", ");
    }

    public record Static(ClassName type, ClassName implType, ClassName keysTypes, String namespace, String constantName,
                         int id, JsonObject value) {
        public Static {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(implType, "implType");
            Objects.requireNonNull(keysTypes, "keysTypes");
            Objects.requireNonNull(namespace, "namespace");
            Objects.requireNonNull(constantName, "constantName");
            if (id < 0) throw new IllegalArgumentException("id is negative");
            Objects.requireNonNull(value, "value");
        }

        public CodeBlock key() { //TODO support registry keys
            return CodeBlock.builder().add("$T.$L.key()", keysTypes, constantName).build();
        }
    }

    public record Tag(JsonObject lookupObject, ClassName keysTypes, JsonObject value) {
        public Tag {
            Objects.requireNonNull(lookupObject, "lookupObject");
            Objects.requireNonNull(keysTypes, "keysTypes");
            Objects.requireNonNull(value, "value");
        }
    }
}
