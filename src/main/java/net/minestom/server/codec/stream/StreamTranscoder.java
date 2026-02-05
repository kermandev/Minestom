package net.minestom.server.codec.stream;

import org.jetbrains.annotations.ApiStatus;

import java.nio.ByteOrder;

public interface StreamTranscoder extends StreamWriter, StreamReader {

    @ApiStatus.Experimental
    StreamTranscoder order(ByteOrder order);

    @Override
    void voidBytes(long length);

    interface Proxy extends StreamTranscoder {
        StreamTranscoder delegate();

        @Override
        default StreamTranscoder order(ByteOrder order) {
            return delegate().order(order);
        }

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
            return delegate().takeByte();
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
        default void writeVarInt(int value) {
            delegate().writeVarInt(value);
        }

        @Override
        default void writeVarLong(long value) {
            delegate().writeVarLong(value);
        }

        @Override
        default void voidBytes(long length) {
            delegate().voidBytes(length);
        }
    }
}
