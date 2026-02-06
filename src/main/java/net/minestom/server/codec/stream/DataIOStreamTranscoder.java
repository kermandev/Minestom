package net.minestom.server.codec.stream;

import java.io.*;
import java.util.Arrays;
import java.util.Objects;

/// Assume the sink will provide a BE layout, if not, {@link EndianStreamTranscoder}
public interface DataIOStreamTranscoder extends StreamTranscoderProxy, DataInput, DataOutput {
    
    @Override
    default void readFully(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        takeBytes(bytes);
    }

    @Override
    default void readFully(byte[] bytes, int off, int len) {
        Objects.requireNonNull(bytes, "bytes");
        //TODO determine how bad this will be
        byte[] buffer = new byte[len];
        takeBytes(buffer);
        System.arraycopy(buffer, 0, bytes, off, len);
    }

    @Override
    default int skipBytes(int n) {
        // TODO, hmmm skipBytes should likely tell you how many you skipped?
        //  - No clue why that would matter to be fair, as its streaming, you cant go back.
        skipBytes((long) n);
        return n;
    }

    @Override
    default boolean readBoolean() {
        return takeBoolean();
    }

    @Override
    default byte readByte() {
        return takeByte();
    }

    @Override
    default int readUnsignedByte() {
        return (takeByte() & 0xFF);
    }

    @Override
    default short readShort() {
        return takeShort();
    }

    @Override
    default int readUnsignedShort() {
        return (takeShort() & 0xFFFF);
    }

    @Override
    default char readChar() {
        return (char) readUnsignedShort();
    }

    @Override
    default int readInt() {
        return takeInt();
    }

    @Override
    default long readLong() {
        return takeLong();
    }

    @Override
    default float readFloat() {
        return takeFloat();
    }

    @Override
    default double readDouble() {
        return takeDouble();
    }

    @Override
    default String readLine() throws IOException {
        throw new UnsupportedEncodingException("unsupported");
    }

    @Override
    default String readUTF() {
        return takeString();
    }

    @Override
    default void write(int lower) {
        this.writeByte(lower);
    }

    @Override
    default void write(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        StreamTranscoderProxy.super.writeBytes(bytes);
    }

    @Override
    default void write(byte[] bytes, int off, int len) {
        Objects.requireNonNull(bytes, "bytes");
        StreamTranscoderProxy.super.writeBytes(Arrays.copyOfRange(bytes, off, off + len));
    }

    @Override
    default void writeBoolean(boolean value) {
        StreamTranscoderProxy.super.writeBoolean(value);
    }

    @Override
    default void writeByte(int value) {
        StreamTranscoderProxy.super.writeByte((byte) (value & 0xFF));
    }

    @Override
    default void writeShort(int value) {
        StreamTranscoderProxy.super.writeShort((short) (value & 0xFFFF));
    }

    @Override
    default void writeChar(int value) {
        this.writeShort(value);
    }

    @Override
    default void writeInt(int value) {
        StreamTranscoderProxy.super.writeInt(value);
    }

    @Override
    default void writeLong(long value) {
        StreamTranscoderProxy.super.writeLong(value);
    }

    @Override
    default void writeFloat(float value) {
        StreamTranscoderProxy.super.writeFloat(value);
    }

    @Override
    default void writeDouble(double value) {
        StreamTranscoderProxy.super.writeDouble(value);
    }

    @Override
    default void writeBytes(String value) {
        Objects.requireNonNull(value, "value");
        for (int i = 0; i < value.length(); i++) {
            writeByte((byte) value.charAt(i)); // Low byte only
        }
    }

    @Override
    default void writeChars(String value) {
        Objects.requireNonNull(value, "value");
        for (int i = 0; i < value.length(); i++) {
            writeShort(value.charAt(i));
        }
    }

    @Override
    default void writeUTF(String value) {
        Objects.requireNonNull(value, "value");
        writeString(value);
    }

    static final int MAX_BYTE_LEN = 65535;

    // Super special string and read string methods that do NOT pass it through, instead override the existing.
    // Follows java.io.DataOutputStream#writeUTF(DataOutput, String) for JDK 25, not default sadly.
    @SuppressWarnings("deprecation")
    @Override
    default void writeString(String value) {
        final int strlen = value.length();
        int utflen = strlen; // optimized for ASCII
        int copyableBytes = 0;

        for (int i = 0; i < strlen; i++) {
            int c = value.charAt(i);
            if (c >= 0x80 || c == 0)
                utflen += (c >= 0x800) ? 2 : 1;
            if (strlen == utflen)
                copyableBytes++; // We have no access to JLA for this.
        }

        if (utflen > MAX_BYTE_LEN || /* overflow */ utflen < strlen)
            throw new RuntimeException("UTF-8 string too long");
        writeShort(utflen);
        if (copyableBytes > 0) { // write if we have any copyableBytes
            byte[] ascii = new byte[copyableBytes];
            value.getBytes(0, copyableBytes, ascii, 0);
            writeBytes(ascii);
        }

        for (int i = copyableBytes; i < strlen; i++) { // Excerpt from ModifiedUtf#putChar
            int c = value.charAt(i);
            if (c != 0 && c < 0x80) {
                writeByte((byte) c);
            } else if (c >= 0x800) {
                writeByte((byte) (0xE0 | c >> 12 & 0x0F));
                writeByte((byte) (0x80 | c >> 6 & 0x3F));
                writeByte((byte) (0x80 | c & 0x3F));
            } else {
                writeByte((byte) (0xC0 | c >> 6 & 0x1F));
                writeByte((byte) (0x80 | c & 0x3F));
            }
        }
    }

    @Override
    default String takeString() {
        try { // DataInputStream only has readUTF sadly.
            return DataInputStream.readUTF(this);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read string", e);
        }
    }
}
