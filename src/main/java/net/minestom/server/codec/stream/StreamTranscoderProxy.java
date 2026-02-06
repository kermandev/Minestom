package net.minestom.server.codec.stream;

public interface StreamTranscoderProxy extends StreamTranscoder {
    static StreamTranscoder extractDelegate(StreamTranscoder transcoder) {
        if (transcoder instanceof StreamTranscoderProxy proxy) {
            return extractDelegate(proxy.delegate());
        }
        return transcoder;
    }

    StreamTranscoder delegate();

    @Override
    default boolean takeBoolean() {
        return delegate().takeBoolean();
    }

    @Override
    default byte takeByte() {
        return delegate().takeByte();
    }

    @Override
    default short takeShort() {
        return delegate().takeShort();
    }

    @Override
    default int takeInt() {
        return delegate().takeInt();
    }

    @Override
    default int takeVarInt() {
        return delegate().takeVarInt();
    }

    @Override
    default long takeLong() {
        return delegate().takeLong();
    }

    @Override
    default long takeVarLong() {
        return delegate().takeVarLong();
    }

    @Override
    default float takeFloat() {
        return delegate().takeFloat();
    }

    @Override
    default double takeDouble() {
        return delegate().takeDouble();
    }

    @Override
    default String takeString() {
        return delegate().takeString();
    }

    @Override
    default byte[] takeBytes() {
        return delegate().takeBytes();
    }

    @Override
    default void takeBytes(byte[] buffer) {
        delegate().takeBytes(buffer);
    }

    @Override
    default void takeShorts(short[] buffer) {
        delegate().takeShorts(buffer);
    }

    @Override
    default void takeInts(int[] buffer) {
        delegate().takeInts(buffer);
    }

    @Override
    default void takeLongs(long[] buffer) {
        delegate().takeLongs(buffer);
    }

    @Override
    default void takeFloats(float[] buffer) {
        delegate().takeFloats(buffer);
    }

    @Override
    default void takeDoubles(double[] buffer) {
        delegate().takeDoubles(buffer);
    }

    @Override
    default void voidBytes(long length) {
        delegate().voidBytes(length);
    }

    @Override
    default void writeBoolean(boolean value) {
        delegate().writeBoolean(value);
    }

    @Override
    default void writeByte(byte value) {
        delegate().writeByte(value);
    }

    @Override
    default void writeShort(short value) {
        delegate().writeShort(value);
    }

    @Override
    default void writeInt(int value) {
        delegate().writeInt(value);
    }

    @Override
    default void writeLong(long value) {
        delegate().writeLong(value);
    }

    @Override
    default void writeFloat(float value) {
        delegate().writeFloat(value);
    }

    @Override
    default void writeDouble(double value) {
        delegate().writeDouble(value);
    }

    @Override
    default void writeString(String value) {
        delegate().writeString(value);
    }

    @Override
    default void writeBytes(byte[] value) {
        delegate().writeBytes(value);
    }

    @Override
    default void writeShorts(short[] value) {
        delegate().writeShorts(value);
    }

    @Override
    default void writeInts(int[] value) {
        delegate().writeInts(value);
    }

    @Override
    default void writeLongs(long[] value) {
        delegate().writeLongs(value);
    }

    @Override
    default void writeFloats(float[] value) {
        delegate().writeFloats(value);
    }

    @Override
    default void writeDoubles(double[] value) {
        delegate().writeDoubles(value);
    }

    @Override
    default void skipBytes(long length) {
        delegate().skipBytes(length);
    }

    @Override
    default void writeVarInt(int value) {
        delegate().writeVarInt(value);
    }

    @Override
    default void writeVarLong(long value) {
        delegate().writeVarLong(value);
    }

    @Override
    default void flush() {
        delegate().flush();
    }
}
