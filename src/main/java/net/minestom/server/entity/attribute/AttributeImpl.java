package net.minestom.server.entity.attribute;

import net.kyori.adventure.key.Key;
import net.minestom.server.registry.BuiltinRegistries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.UnknownNullability;

record AttributeImpl(Key key, int id,
                     String translationKey, double defaultValue, boolean clientSync,
                     double maxValue, double minValue) implements Attribute {
    static final Registry<Attribute> REGISTRY = AttributeValues.load(BuiltinRegistries.ATTRIBUTE);

    static @UnknownNullability Attribute get(RegistryKey<Attribute> namespace) {
        return REGISTRY.get(namespace);
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public boolean isSynced() {
        return clientSync();
    }
}
