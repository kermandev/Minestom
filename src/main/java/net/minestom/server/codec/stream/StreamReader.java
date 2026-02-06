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

    /// Takes the remaining bytes, if any, or no ops.
    byte[] takeBytes();

    void takeBytes(byte[] buffer);

    default byte[] takeBytes(int length) {
        byte[] bytes = new byte[length];
        takeBytes(bytes);
        return bytes;
    }

    void takeShorts(short[] buffer);

    default short[] takeShorts(int length) {
        short[] shorts = new short[length];
        takeShorts(shorts);
        return shorts;
    }

    void takeInts(int[] buffer);

    default int[] takeInts(int length) {
        int[] ints = new int[length];
        takeInts(ints);
        return ints;
    }

    void takeLongs(long[] buffer);

    default long[] takeLongs(int length) {
        long[] longs = new long[length];
        takeLongs(longs);
        return longs;
    }

    void takeFloats(float[] buffer);

    default float[] takeFloats(int length) {
        float[] floats = new float[length];
        takeFloats(floats);
        return floats;
    }

    void takeDoubles(double[] buffer);

    default double[] takeDoubles(int length) {
        double[] doubles = new double[length];
        takeDoubles(doubles);
        return doubles;
    }

    void voidBytes(long length);
}
