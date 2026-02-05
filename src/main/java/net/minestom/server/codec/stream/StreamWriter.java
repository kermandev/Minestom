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

    void writeVarInt(int value);

    void writeVarLong(long value);

    void voidBytes(long length);

    void writeBytes(byte[] value);

    default void writeBytes(byte[] value, int offset, int length) {
        writeBytes(Arrays.copyOfRange(value, offset, offset + length));
    }

    void writeShorts(short[] value);

    default void writeShorts(short[] value, int offset, int length) {
        writeShorts(Arrays.copyOfRange(value, offset, offset + length));
    }

    void writeInts(int[] value);

    default void writeInts(int[] value, int offset, int length) {
        writeInts(Arrays.copyOfRange(value, offset, length));
    }

    void writeLongs(long[] value);

    default void writeLongs(long[] value, int offset, int length) {
        writeLongs(Arrays.copyOfRange(value, offset, offset + length));
    }

    void writeFloats(float[] value);

    default void writeFloats(float[] value, int offset, int length) {
        writeFloats(Arrays.copyOfRange(value, offset, offset + length));
    }

    void writeDoubles(double[] value);

    default void writeDoubles(double[] value, int offset, int length) {
        writeDoubles(Arrays.copyOfRange(value, offset, length));
    }
}
