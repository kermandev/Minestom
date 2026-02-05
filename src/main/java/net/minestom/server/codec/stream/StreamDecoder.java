package net.minestom.server.codec.stream;

import org.jetbrains.annotations.UnknownNullability;

@FunctionalInterface
public interface StreamDecoder<T extends @UnknownNullability Object> {
    T decode(StreamReader stream) throws RuntimeException;
}
