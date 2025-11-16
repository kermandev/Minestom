package net.minestom.server.entity;

import net.kyori.adventure.key.Key;
import net.minestom.server.registry.BuiltinRegistries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public record VillagerProfessionImpl(Key key, int id, @Nullable SoundEvent workSound) implements VillagerProfession {
    static final Registry<VillagerProfession> REGISTRY = VillagerProfessionValues.load(BuiltinRegistries.VILLAGER_PROFESSION);

    static @UnknownNullability VillagerProfession get(RegistryKey<VillagerProfession> key) {
        return REGISTRY.get(key);
    }

    @Override
    public String toString() {
        return name();
    }
}
