package net.minestom.server.codec.stream;

import java.io.Flushable;

/// A stream writer writes to a sink, remember to flush.
public interface StreamWriter extends Flushable {
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

    void writeBytes(byte[] value);

    void writeShorts(short[] value);

    void writeInts(int[] value);

    void writeLongs(long[] value);

    void writeFloats(float[] value);

    void writeDoubles(double[] value);

    void skipBytes(long length);

    default void flush() {
    }
}
