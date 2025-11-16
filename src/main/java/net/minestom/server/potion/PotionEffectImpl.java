package net.minestom.server.potion;

import net.kyori.adventure.key.Key;
import net.minestom.server.registry.BuiltinRegistries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.UnknownNullability;

record PotionEffectImpl(Key key, int id, String translationKey, int color, boolean instantaneous) implements PotionEffect {
    static final Registry<PotionEffect> REGISTRY = PotionEffectValues.load(BuiltinRegistries.POTION_EFFECT);

    static @UnknownNullability PotionEffect get(RegistryKey<PotionEffect> key) {
        return REGISTRY.get(key);
    }

    @Override
    public String toString() {
        return name();
    }
}
