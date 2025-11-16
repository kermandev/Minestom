package net.minestom.server.instance.block;

import net.kyori.adventure.key.Key;
import net.minestom.server.registry.BuiltinRegistries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.UnknownNullability;

record BlockSoundImpl(Key key, float volume, float pitch,
                      SoundEvent breakSound, SoundEvent hitSound, SoundEvent fallSound,
                      SoundEvent placeSound, SoundEvent stepSound) implements BlockSoundType {
    static final Registry<BlockSoundType> REGISTRY = BlockSoundTypeValues.load(BuiltinRegistries.BLOCK_SOUND_TYPE);

    static @UnknownNullability BlockSoundType get(RegistryKey<BlockSoundType> key) {
        return REGISTRY.get(key);
    }
}
