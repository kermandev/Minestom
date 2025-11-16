package net.minestom.codegen.value;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import net.minestom.codegen.CodegenRegistry;
import net.minestom.codegen.CodegenValue;

import java.util.Collection;
import java.util.Set;

public class VillagerProfessionGenerator extends RegistryValueGenerator {

    @Override
    public CodeBlock generateValue(CodegenRegistry registry, CodegenValue value, Static codeEntry) {
        final CodeBlock workSound;
        var entry = codeEntry.value().get("workSound");
        if (entry != null) {
            String namespace = entry.getAsString();
            var soundEventValue = registry.get("sound_event");
            var constantName = soundEventValue.generator().get().toConstant(namespace);
            workSound = CodeBlock.of(
                    "$T.$L", ClassName.get(soundEventValue.packageName(), soundEventValue.typeName()), constantName
            );
        } else {
            workSound = CodeBlock.of("null");
        }

        return CodeBlock.builder().add(
                "new $T($L, $L, $L)",
                codeEntry.implType(),
                codeEntry.key(),
                codeEntry.id(),
                workSound
        ).build();
    }

    @Override
    protected Collection<String> requirements() {
        return Set.of("sound_event");
    }
}
