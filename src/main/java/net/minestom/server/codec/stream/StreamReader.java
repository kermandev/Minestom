package net.minestom.server.codec.stream;

public interface StreamReader {
    boolean takeBoolean();

    byte takeByte();

    short takeShort();

    int takeInt();

    int takeVarInt();

    long takeLong();

    long takeVarLong();

    float takeFloat();

    double takeDouble();

    String takeString();

    byte[] takeBytes();

    void takeBytes(byte[] buffer);

    default byte[] takeBytes(int length) {
        byte[] bytes = new byte[length];
        takeBytes(bytes);
        return bytes;
    }

    void voidBytes(long length);
}
