package net.minestom.server.potion;

import net.kyori.adventure.key.Key;
import net.minestom.server.registry.BuiltinRegistries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.UnknownNullability;

record PotionTypeImpl(Key key, int id) implements PotionType {
    static final Registry<PotionType> REGISTRY = PotionTypeValues.load(BuiltinRegistries.POTION_TYPE);

    static @UnknownNullability PotionType get(RegistryKey<PotionType> key) {
        return REGISTRY.get(key);
    }

    @Override
    public String toString() {
        return name();
    }
}
