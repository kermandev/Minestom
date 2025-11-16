package net.minestom.codegen.value;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import net.minestom.codegen.CodegenRegistry;
import net.minestom.codegen.CodegenValue;

import java.util.*;

public class EntityValueGenerator extends RegistryValueGenerator {
    private static final ClassName BOUNDING_BOX = ClassName.get("net.minestom.server.collision", "BoundingBox");

    @Override
    public CodeBlock generateValue(CodegenRegistry registry, CodegenValue value, Static codeEntry) {
        String translationKey = codeEntry.value().get("translationKey").getAsString();

        double drag = codeEntry.value().get("drag") != null ? codeEntry.value().get("drag").getAsDouble() : 0.02;
        double acceleration = codeEntry.value().get("acceleration") != null ? codeEntry.value().get("acceleration").getAsDouble() : 0.08;

        String packetType = codeEntry.value().get("packetType").getAsString().toUpperCase(Locale.ROOT);
        boolean isLiving = "LIVING".equals(packetType) || "PLAYER".equals(packetType);

        boolean fireImmune = codeEntry.value().get("fireImmune") != null && codeEntry.value().get("fireImmune").getAsBoolean();
        int clientTrackingRange = codeEntry.value().get("clientTrackingRange").getAsInt();

        // Dimensions
        double width = codeEntry.value().get("width").getAsDouble();
        double height = codeEntry.value().get("height").getAsDouble();
        double eyeHeight = codeEntry.value().get("eyeHeight").getAsDouble();
        CodeBlock boundingBox = CodeBlock.of("new $T($L, $L, $L)", BOUNDING_BOX, width, height, width);

        // Attachments
        Map<String, CodeBlock> entityOffsets = new HashMap<>();
        JsonElement attachments = codeEntry.value().get("attachments");
        if (attachments != null) {
            var allAttachments = attachments.getAsJsonObject().asMap().keySet();
            for (String key : allAttachments) {
                JsonArray offset = attachments.getAsJsonObject().getAsJsonArray(key).get(0).getAsJsonArray();
                CodeBlock codeBlock = CodeBlock.of("$T.of($L, $L, $L)", ClassName.get(List.class), offset.get(0).getAsDouble(), offset.get(1).getAsDouble(), offset.get(2).getAsDouble());
                entityOffsets.put(key, codeBlock);
            }
        }

        List<CodeBlock> entityCodeblocks = new ArrayList<>();
        for (String key : entityOffsets.keySet()) {
            entityCodeblocks.add(CodeBlock.of("$S, $L", key, entityOffsets.get(key)));
        }
        CodeBlock entityCodeblock = CodeBlock.of("$T.of($L)", ClassName.get(Map.class), CodeBlock.join(entityCodeblocks, ", "));

        return CodeBlock.builder().add(
                "new $T($L, $L, $S, $L, $L, $L, $L, $L, $L, $L, $L, $L, $L)",
                codeEntry.implType(),
                codeEntry.key(),
                codeEntry.id(),
                translationKey,
                drag,
                acceleration,
                isLiving,
                width,
                height,
                eyeHeight,
                clientTrackingRange,
                fireImmune,
                entityCodeblock,
                boundingBox
        ).build();
    }
}
