package net.minestom.server.instance.fluid;

import net.kyori.adventure.key.Key;
import net.minestom.server.registry.BuiltinRegistries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryData;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.UnknownNullability;

record FluidImpl(Key key, int id) implements Fluid {
    static final Registry<Fluid> REGISTRY = FluidValues.load(BuiltinRegistries.FLUID);

    static @UnknownNullability Fluid get(RegistryKey<Fluid> key) {
        return REGISTRY.get(key);
    }
}
