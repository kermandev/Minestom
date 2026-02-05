package net.minestom.server.codec.stream;

import java.nio.ByteOrder;

/// Flips endianness from LE to BE or BE to LE
public record FlippingStreamTranscoder(StreamTranscoder delegate) implements StreamTranscoderProxy {
    @Override
    public ByteOrder order() {
        return StreamTranscoderProxy.super.order() == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

    @Override
    public short takeShort() {
        short value = StreamTranscoderProxy.super.takeShort();
        value = Short.reverseBytes(value);
        return value;
    }

    @Override
    public int takeInt() {
        int value = StreamTranscoderProxy.super.takeInt();
        value = Integer.reverseBytes(value);
        return value;
    }

    @Override
    public long takeLong() {
        long value = StreamTranscoderProxy.super.takeLong();
        value = Long.reverseBytes(value);
        return value;
    }

    @Override
    public float takeFloat() {
        float value = StreamTranscoderProxy.super.takeFloat();
        value = reverseFloat(value);
        return value;
    }

    @Override
    public double takeDouble() {
        double value = StreamTranscoderProxy.super.takeDouble();
        value = reverseDouble(value);
        return value;
    }

    @Override
    public void writeShort(short value) {
        value = Short.reverseBytes(value);
        StreamTranscoderProxy.super.writeShort(value);
    }

    @Override
    public void writeInt(int value) {
        value = Integer.reverseBytes(value);
        StreamTranscoderProxy.super.writeInt(value);
    }

    @Override
    public void writeLong(long value) {
        value = Long.reverseBytes(value);
        StreamTranscoderProxy.super.writeLong(value);
    }

    @Override
    public void writeFloat(float value) {
        int bytes = Float.floatToIntBits(value);
        bytes = Integer.reverseBytes(bytes);
        value = Float.intBitsToFloat(bytes);
        StreamTranscoderProxy.super.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) {
        long bytes = Double.doubleToLongBits(value);
        bytes = Long.reverseBytes(bytes);
        value = Double.longBitsToDouble(bytes);
        StreamTranscoderProxy.super.writeDouble(value);
    }

    @Override
    public void writeShorts(short[] value) {
        for (int i = 0; i < value.length; i++) {
            value[i] = Short.reverseBytes(value[i]);
        }
        StreamTranscoderProxy.super.writeShorts(value);
    }

    @Override
    public void writeInts(int[] value) {
        for (int i = 0; i < value.length; i++) {
            value[i] = Integer.reverseBytes(value[i]);
        }
        StreamTranscoderProxy.super.writeInts(value);
    }

    @Override
    public void writeLongs(long[] value) {
        for (int i = 0; i < value.length; i++) {
            value[i] = Long.reverseBytes(value[i]);
        }
        StreamTranscoderProxy.super.writeLongs(value);
    }

    @Override
    public void writeFloats(float[] value) {
        for (int i = 0; i < value.length; i++) {
            value[i] = reverseFloat(value[i]);
        }
        StreamTranscoderProxy.super.writeFloats(value);
    }

    @Override
    public void writeFloats(float[] value, int offset, int length) {
        int offsetLength = offset + length;
        assert offsetLength <= value.length;
        for (int i = offset; i < offsetLength; i++) {
            value[i] = reverseFloat(value[i]);
        }
        StreamTranscoderProxy.super.writeFloats(value, offset, length);
    }

    @Override
    public void writeDoubles(double[] value) {
        for (int i = 0; i < value.length; i++) {
            value[i] = reverseDouble(value[i]);
        }
        StreamTranscoderProxy.super.writeDoubles(value);
    }

    @Override
    public void writeDoubles(double[] value, int offset, int length) {
        int offsetLength = offset + length;
        assert offsetLength <= value.length;
        for (int i = offset; i < offsetLength; i++) {
            value[i] = reverseDouble(value[i]);
        }
        StreamTranscoderProxy.super.writeDoubles(value, offset, length);
    }

    private static float reverseFloat(float value) {
        int bytes = Float.floatToRawIntBits(value);
        bytes = Integer.reverseBytes(bytes);
        value = Float.intBitsToFloat(bytes);
        return value;
    }

    private static double reverseDouble(double value) {
        long bytes = Double.doubleToRawLongBits(value);
        bytes = Long.reverseBytes(bytes);
        value = Double.longBitsToDouble(bytes);
        return value;
    }
}
