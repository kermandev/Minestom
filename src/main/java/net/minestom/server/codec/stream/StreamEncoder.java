package net.minestom.server.codec.stream;

import org.jetbrains.annotations.UnknownNullability;

@FunctionalInterface
public interface StreamEncoder<T extends @UnknownNullability Object> {
    void encode(StreamWriter stream, T value) throws RuntimeException;
}
