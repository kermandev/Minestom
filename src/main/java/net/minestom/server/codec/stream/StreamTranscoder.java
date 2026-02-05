package net.minestom.server.codec.stream;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.nio.ByteOrder;

public interface StreamTranscoder extends StreamWriter, StreamReader {

    ByteOrder order();

    @Contract("_ -> new")
    @ApiStatus.Experimental
    StreamTranscoder order(ByteOrder order);

    @Override
    void voidBytes(long length);
}
