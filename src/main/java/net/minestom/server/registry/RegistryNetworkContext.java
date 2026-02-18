package net.minestom.server.registry;

import net.minestom.server.network.NetworkContext;

import java.util.Objects;

public interface RegistryNetworkContext extends NetworkContext, Registries.Provider {
    static RegistryNetworkContext of(Registries registries) {
        Objects.requireNonNull(registries, "registries");
        return () -> registries;
    }
}
