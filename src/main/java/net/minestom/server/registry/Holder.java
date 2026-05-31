package net.minestom.server.registry;

import net.minestom.server.codec.Codec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.Either;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * <p>Represents either a reference to a registry entry {@link RegistryKey} or a direct registry value.</p>
 *
 * <p>Whether registry values implement this type depends on client support for direct values.</p>
 *
 * @param <T> the type of the registry entry
 */
public sealed interface Holder<T> {

    static <T> NetworkBuffer.Type<Holder<T>> networkType(
            Registries.Selector<T> selector,
            NetworkBuffer.Type<T> registryNetworkType
    ) {
        return new RegistryNetworkTypes.HolderNetworkTypeImpl<>(selector, registryNetworkType);
    }

    static <T> Codec<Holder<T>> codec(
            Registries.Selector<T> selector,
            Codec<T> registryCodec
    ) {
        return new RegistryCodecs.HolderCodec<>(selector, registryCodec);
    }

    static <T> Direct<T> direct(T value) {
        return new Direct<>(value);
    }

    static <T> Reference<T> reference(RegistryKey<T> value) {
        return new Reference<>(value);
    }

    record Direct<T>(T value) implements Holder<T> {
        public Direct {
            Objects.requireNonNull(value, "value");
        }
    }

    record Reference<T>(RegistryKey<T> key) implements Holder<T> {
        public Reference {
            Objects.requireNonNull(key, "key");
        }
    }

    default boolean isDirect() {
        return !(this instanceof Direct<T>);
    }

    default @Nullable RegistryKey<T> asKey() {
        return this instanceof Reference<T>(RegistryKey<T> key) ? key : null;
    }

    default @Nullable T asValue() {
        return this instanceof Direct<T>(T value) ? value : null;
    }

    default Either<RegistryKey<T>, T> unwrap() {
        return switch (this) {
            case Reference(RegistryKey<T> key) -> Either.left(key);
            case Direct(T value) -> Either.right(value);
        };
    }

    default @Nullable T resolve(DynamicRegistry<T> registry) {
        return switch (this) {
            case Reference(RegistryKey<T> key) -> registry.get(key);
            case Direct(T value) -> value;
        };
    }

}
