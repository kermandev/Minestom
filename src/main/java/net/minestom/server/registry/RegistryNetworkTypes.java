package net.minestom.server.registry;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkContext;
import net.minestom.server.utils.Either;
import net.minestom.server.utils.validate.Check;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class RegistryNetworkTypes {

    record RegistryKeyImpl<T>(Registries.Selector<T> selector) implements NetworkBuffer.Type<RegistryKey<T>, RegistryNetworkContext> {
        @Override
        public void write(NetworkBuffer buffer, RegistryKey<T> value, RegistryNetworkContext context) {
            final var registry = selector.select(context.registries());
            final int id = registry.getId(value);
            Check.stateCondition(id == -1, "Key {0} is not registered in registry {1}", value, registry.key());
            buffer.write(NetworkBuffer.VAR_INT, id, context);
        }

        @Override
        public RegistryKey<T> read(NetworkBuffer buffer, RegistryNetworkContext context) {
            final var registry = selector.select(context.registries());
            final int id = buffer.read(NetworkBuffer.VAR_INT, context);
            final var key = registry.getKey(id);
            Check.stateCondition(key == null, "Unknown id {0} for registry {1}", id - 1, registry.key());
            return key;
        }
    }

    record HolderNetworkTypeImpl<T, C extends NetworkContext & Registries.Provider>(
            Registries.Selector<T> selector,
            NetworkBuffer.Type<T, ? super C> registryNetworkType
    ) implements NetworkBuffer.Type<Holder<T>, C> {
        @Override
        public void write(NetworkBuffer buffer, Holder<T> value, C context) {
            final var registry = selector.select(context.registries());
            switch (value.unwrap()) {
                case Either.Left(RegistryKey<T> key) -> {
                    final int id = registry.getId(key);
                    Check.stateCondition(id == -1, "Key {0} is not registered in registry {1}", key, registry.key());
                    buffer.write(NetworkBuffer.VAR_INT, id + 1, context);
                }
                case Either.Right(T direct) -> {
                    buffer.write(NetworkBuffer.VAR_INT, 0, context);
                    buffer.write(registryNetworkType, direct, context);
                }
            }
        }

        @Override
        public Holder<T> read(NetworkBuffer buffer, C context) {
            final var registry = selector.select(context.registries());
            final int id = buffer.read(NetworkBuffer.VAR_INT, context);
            if (id == 0) //noinspection unchecked
                return (Holder<T>) buffer.read(registryNetworkType, context);

            final var key = registry.getKey(id - 1);
            Check.stateCondition(key == null, "Unknown id {0} for registry {1}", id - 1, registry.key());
            return key;
        }
    }

    record RegistryTagImpl<T>(Registries.Selector<T> selector) implements NetworkBuffer.Type<RegistryTag<T>, RegistryNetworkContext> {
        @Override
        public void write(NetworkBuffer buffer, RegistryTag<T> value, RegistryNetworkContext context) {
            switch (value) {
                case net.minestom.server.registry.RegistryTagImpl.Backed<T> backed -> {
                    buffer.write(NetworkBuffer.VAR_INT, 0);
                    buffer.write(NetworkBuffer.KEY, backed.key().key());
                }
                case net.minestom.server.registry.RegistryTagImpl.Empty() -> buffer.write(NetworkBuffer.VAR_INT, 1);
                case net.minestom.server.registry.RegistryTagImpl.Direct(var entries) -> {
                    final var registry = selector.select(context.registries());
                    buffer.write(NetworkBuffer.VAR_INT, entries.size() + 1);
                    for (RegistryKey<T> key : entries) {
                        final int id = registry.getId(key);
                        Check.stateCondition(id == -1, "Key {0} is not registered in registry {1}", key, registry.key());
                        buffer.write(NetworkBuffer.VAR_INT, id);
                    }
                }
            }
        }

        @Override
        public RegistryTag<T> read(NetworkBuffer buffer, RegistryNetworkContext context) {
            final var registry = selector.select(context.registries());
            int count = buffer.read(NetworkBuffer.VAR_INT) - 1;
            if (count < 0) {
                final var key = buffer.read(NetworkBuffer.KEY);
                final var tag = registry.getTag(key);
                Check.stateCondition(tag == null, "No such tag {0} for registry {1}", key, registry.key());
                return tag;
            } else if (count == 0) {
                return RegistryTag.empty();
            } else {
                final List<RegistryKey<T>> keys = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    final int id = buffer.read(NetworkBuffer.VAR_INT);
                    final var key = registry.getKey(id);
                    Check.stateCondition(key == null, "Unknown id {0} for registry {1}", id, registry.key());
                    keys.add(key);
                }
                return new net.minestom.server.registry.RegistryTagImpl.Direct<>(keys);
            }
        }
    }

}
