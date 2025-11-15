package net.minestom.server;

import net.kyori.adventure.key.Key;
import net.minestom.server.registry.BuiltinRegistries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.UnknownNullability;

record FeatureFlagImpl(Key key, int id) implements FeatureFlag {
    static final Registry<FeatureFlag> REGISTRY = FeatureFlagValues.load(BuiltinRegistries.FEATURE_FLAG);

    static @UnknownNullability FeatureFlag get(RegistryKey<FeatureFlag> key) {
        return REGISTRY.get(key);
    }
}
