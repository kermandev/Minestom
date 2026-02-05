package net.minestom.server.codec.stream;

import java.util.Arrays;

public interface StreamWriter {
    void writeBoolean(boolean value);

    void writeByte(byte value);

    void writeShort(short value);

    void writeInt(int value);

    void writeLong(long value);

    void writeFloat(float value);

    void writeDouble(double value);

    void writeString(String value);

    void writeBytes(byte[] value);

    default void writeBytes(byte[] value, int offset, int length) {
        writeBytes(Arrays.copyOfRange(value, offset, offset + length));
    }

    void writeVarInt(int value);

    void writeVarLong(long value);

    void voidBytes(long length);
}
