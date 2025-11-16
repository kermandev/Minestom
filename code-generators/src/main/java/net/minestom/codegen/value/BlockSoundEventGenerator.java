package net.minestom.codegen.value;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import net.minestom.codegen.CodegenRegistry;
import net.minestom.codegen.CodegenValue;
import net.minestom.codegen.MinestomCodeGenerator;

import java.util.Collection;
import java.util.Set;

public class BlockSoundEventGenerator extends RegistryValueGenerator {

    // These dont have ids since they arent a true registry.
    @Override
    public CodeBlock generateValue(CodegenRegistry registry, CodegenValue value, Static codeEntry) {
        CodegenValue sound = registry.get("sound_event");
        float volume = codeEntry.value().get("volume").getAsFloat();
        float pitch = codeEntry.value().get("pitch").getAsFloat();
        ClassName soundType = ClassName.get(sound.packageName(), sound.typeName());
        MinestomCodeGenerator generator = sound.generator().get();

        return CodeBlock.builder().add(
                "new $T($L, $Lf, $Lf, $T.$L, $T.$L, $T.$L, $T.$L, $T.$L)",
                codeEntry.implType(),
                codeEntry.key(),
                volume,
                pitch,
                soundType, generator.toConstant(codeEntry.value().get("breakSound").getAsString()),
                soundType, generator.toConstant(codeEntry.value().get("hitSound").getAsString()),
                soundType, generator.toConstant(codeEntry.value().get("fallSound").getAsString()),
                soundType, generator.toConstant(codeEntry.value().get("placeSound").getAsString()),
                soundType, generator.toConstant(codeEntry.value().get("stepSound").getAsString())

        ).build();
    }


    @Override
    protected Collection<String> requirements() {
        return Set.of("sound_event");
    }
}
