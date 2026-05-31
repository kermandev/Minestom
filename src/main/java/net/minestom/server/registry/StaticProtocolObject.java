package net.minestom.server.registry;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface StaticProtocolObject<T> extends Keyed {

    @Contract(pure = true)
    default String name() {
        return key().asString();
    }

    @Override
    @Contract(pure = true)
    Key key();

    default RegistryKey<T> registryKey() {
        return RegistryKey.unsafeOf(key());
    }

    @Contract(pure = true)
    int id();

    default @Nullable Object registry() {
        return null;
    }
}
