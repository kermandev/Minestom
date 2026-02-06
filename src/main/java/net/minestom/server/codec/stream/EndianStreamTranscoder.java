package net.minestom.server.codec.stream;

import java.util.Objects;

/// Flips endianness from LE to BE or BE to LE
/// Depends on the context this is attached too.
public interface EndianStreamTranscoder extends StreamTranscoderProxy {
    static StreamTranscoder of(StreamTranscoder transcoder) {
        return of(transcoder, false);
    }

    static StreamTranscoder of(StreamTranscoder transcoder, boolean trusted) {
        Objects.requireNonNull(transcoder, "transcoder");
        if (transcoder instanceof EndianStreamTranscoder endianStreamTranscoder) // Flipping twice in a row is a noop
            return StreamTranscoderProxy.extractDelegate(endianStreamTranscoder);
        return new EndianStreamTranscoder() {
            @Override
            public boolean trusted() {
                return trusted;
            }

            @Override
            public StreamTranscoder delegate() {
                return transcoder;
            }
        };
    }

    /// Return true to be considered trusted.
    ///
    /// Trusted means copies will not be preformed for writing arrays, instead they will be modified in place.
    boolean trusted();

    @Override
    default short takeShort() {
        short value = StreamTranscoderProxy.super.takeShort();
        value = Short.reverseBytes(value);
        return value;
    }

    @Override
    default int takeInt() {
        int value = StreamTranscoderProxy.super.takeInt();
        value = Integer.reverseBytes(value);
        return value;
    }

    @Override
    default long takeLong() {
        long value = StreamTranscoderProxy.super.takeLong();
        value = Long.reverseBytes(value);
        return value;
    }

    @Override
    default float takeFloat() {
        float value = StreamTranscoderProxy.super.takeFloat();
        value = reverseFloat(value);
        return value;
    }

    @Override
    default double takeDouble() {
        double value = StreamTranscoderProxy.super.takeDouble();
        value = reverseDouble(value);
        return value;
    }

    @Override
    default void takeShorts(short[] buffer) {
        StreamTranscoderProxy.super.takeShorts(buffer);
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Short.reverseBytes(buffer[i]);
        }
    }

    @Override
    default void takeInts(int[] buffer) {
        StreamTranscoderProxy.super.takeInts(buffer);
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Integer.reverseBytes(buffer[i]);
        }
    }

    @Override
    default void takeLongs(long[] buffer) {
        StreamTranscoderProxy.super.takeLongs(buffer);
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Long.reverseBytes(buffer[i]);
        }
    }

    @Override
    default void takeFloats(float[] buffer) {
        StreamTranscoderProxy.super.takeFloats(buffer);
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = reverseFloat(buffer[i]);
        }
    }

    @Override
    default void takeDoubles(double[] buffer) {
        StreamTranscoderProxy.super.takeDoubles(buffer);
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = reverseDouble(buffer[i]);
        }
    }

    @Override
    default void writeShort(short value) {
        value = Short.reverseBytes(value);
        StreamTranscoderProxy.super.writeShort(value);
    }

    @Override
    default void writeInt(int value) {
        value = Integer.reverseBytes(value);
        StreamTranscoderProxy.super.writeInt(value);
    }

    @Override
    default void writeLong(long value) {
        value = Long.reverseBytes(value);
        StreamTranscoderProxy.super.writeLong(value);
    }

    @Override
    default void writeFloat(float value) {
        int bytes = Float.floatToIntBits(value);
        bytes = Integer.reverseBytes(bytes);
        value = Float.intBitsToFloat(bytes);
        StreamTranscoderProxy.super.writeFloat(value);
    }

    @Override
    default void writeDouble(double value) {
        long bytes = Double.doubleToLongBits(value);
        bytes = Long.reverseBytes(bytes);
        value = Double.longBitsToDouble(bytes);
        StreamTranscoderProxy.super.writeDouble(value);
    }

    @Override
    default void writeShorts(short[] value) {
        short[] buffer = trusted() ? value : new short[value.length];
        for (int i = 0; i < buffer.length; i++)
            buffer[i] = Short.reverseBytes(value[i]);
        StreamTranscoderProxy.super.writeShorts(buffer);
    }

    @Override
    default void writeInts(int[] value) {
        int[] buffer = trusted() ? value : new int[value.length];
        for (int i = 0; i < buffer.length; i++)
            buffer[i] = Integer.reverseBytes(value[i]);
        StreamTranscoderProxy.super.writeInts(buffer);
    }

    @Override
    default void writeLongs(long[] value) {
        long[] buffer = trusted() ? value : new long[value.length];
        for (int i = 0; i < value.length; i++) {
            value[i] = Long.reverseBytes(value[i]);
        }
        StreamTranscoderProxy.super.writeLongs(buffer);
    }

    @Override
    default void writeFloats(float[] value) {
        float[] buffer = trusted() ? new float[value.length] : value;
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = reverseFloat(value[i]);
        }
        StreamTranscoderProxy.super.writeFloats(buffer);
    }

    @Override
    default void writeDoubles(double[] value) {
        double[] buffer = trusted() ? new double[value.length] : value;
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = reverseDouble(value[i]);
        }
        StreamTranscoderProxy.super.writeDoubles(buffer);
    }

    static float reverseFloat(float value) {
        int bytes = Float.floatToRawIntBits(value);
        bytes = Integer.reverseBytes(bytes);
        value = Float.intBitsToFloat(bytes);
        return value;
    }

    static double reverseDouble(double value) {
        long bytes = Double.doubleToRawLongBits(value);
        bytes = Long.reverseBytes(bytes);
        value = Double.longBitsToDouble(bytes);
        return value;
    }
}
