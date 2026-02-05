package net.minestom.server.utils.nbt;

import net.minestom.server.codec.stream.StreamTranscoder;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteOrder;

public record DataIOStreamTranscoder(StreamTranscoder delegate) implements StreamTranscoder.Proxy, DataInput, DataOutput {
    public DataIOStreamTranscoder {
        delegate = delegate.order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public void readFully(byte[] b) throws IOException {

    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {

    }

    @Override
    public int skipBytes(int n) throws IOException {
        delegate.takeBytes()
    }

    @Override
    public boolean readBoolean() throws IOException {
        return false;
    }

    @Override
    public byte readByte() throws IOException {
        return 0;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return 0;
    }

    @Override
    public short readShort() throws IOException {
        return 0;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return 0;
    }

    @Override
    public char readChar() throws IOException {
        return 0;
    }

    @Override
    public int readInt() throws IOException {
        return 0;
    }

    @Override
    public long readLong() throws IOException {
        return 0;
    }

    @Override
    public float readFloat() throws IOException {
        return 0;
    }

    @Override
    public double readDouble() throws IOException {
        return 0;
    }

    @Override
    public String readLine() throws IOException {
        return "";
    }

    @Override
    public @NonNull String readUTF() throws IOException {
        return "";
    }

    @Override
    public void write(int b) throws IOException {

    }

    @Override
    public void write(@NonNull byte[] b) throws IOException {

    }

    @Override
    public void write(@NonNull byte[] b, int off, int len) throws IOException {

    }

    @Override
    public void writeByte(int v) throws IOException {

    }

    @Override
    public void writeShort(int v) throws IOException {

    }

    @Override
    public void writeChar(int v) throws IOException {

    }

    @Override
    public void writeBytes(@NonNull String s) throws IOException {

    }

    @Override
    public void writeChars(@NonNull String s) throws IOException {

    }

    @Override
    public void writeUTF(@NonNull String s) throws IOException {

    }

    @Override
    public void voidBytes(long length) {

    }
}
