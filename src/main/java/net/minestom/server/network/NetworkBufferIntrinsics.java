package net.minestom.server.network;

import net.minestom.server.utils.Unit;

import static net.minestom.server.network.NetworkBufferImpl.impl;

final class NetworkBufferIntrinsics {
    private NetworkBufferIntrinsics() {}

    static void writeUnit(NetworkBuffer buffer, long index, Object value) {
    }

    static Object readUnit(NetworkBuffer buffer, long index) {
        return Unit.INSTANCE;
    }

    static void writeBoolean(NetworkBuffer buffer, long index, Object value) {
        impl(buffer)._putByte(index, (Boolean) value ? (byte) 1 : (byte) 0);
    }

    static Object readBoolean(NetworkBuffer buffer, long index) {
        return impl(buffer)._getByte(index) == 1;
    }

    static void writeByte(NetworkBuffer buffer, long index, Object value) {
        impl(buffer)._putByte(index, (Byte) value);
    }

    static Object readByte(NetworkBuffer buffer, long index) {
        return impl(buffer)._getByte(index);
    }

    static void writeUnsignedByte(NetworkBuffer buffer, long index, Object value) {
        impl(buffer)._putByte(index, (byte) ((Short) value & 0xFF));
    }

    static Object readUnsignedByte(NetworkBuffer buffer, long index) {
        return (short) (impl(buffer)._getByte(index) & 0xFF);
    }

    static void writeShort(NetworkBuffer buffer, long index, Object value) {
        impl(buffer)._putShort(index, (Short) value);
    }

    static Object readShort(NetworkBuffer buffer, long index) {
        return impl(buffer)._getShort(index);
    }

    static void writeUnsignedShort(NetworkBuffer buffer, long index, Object value) {
        impl(buffer)._putShort(index, (short) ((Integer) value & 0xFFFF));
    }

    static Object readUnsignedShort(NetworkBuffer buffer, long index) {
        return impl(buffer)._getShort(index) & 0xFFFF;
    }

    static void writeInt(NetworkBuffer buffer, long index, Object value) {
        impl(buffer)._putInt(index, (Integer) value);
    }

    static Object readInt(NetworkBuffer buffer, long index) {
        return impl(buffer)._getInt(index);
    }

    static void writeUnsignedInt(NetworkBuffer buffer, long index, Object value) {
        impl(buffer)._putInt(index, (int) ((Long) value & 0xFFFFFFFFL));
    }

    static Object readUnsignedInt(NetworkBuffer buffer, long index) {
        return impl(buffer)._getInt(index) & 0xFFFFFFFFL;
    }

    static void writeLong(NetworkBuffer buffer, long index, Object value) {
        impl(buffer)._putLong(index, (Long) value);
    }

    static Object readLong(NetworkBuffer buffer, long index) {
        return impl(buffer)._getLong(index);
    }

    static void writeFloat(NetworkBuffer buffer, long index, Object value) {
        impl(buffer)._putFloat(index, (Float) value);
    }

    static Object readFloat(NetworkBuffer buffer, long index) {
        return impl(buffer)._getFloat(index);
    }

    static void writeDouble(NetworkBuffer buffer, long index, Object value) {
        impl(buffer)._putDouble(index, (Double) value);
    }

    static Object readDouble(NetworkBuffer buffer, long index) {
        return impl(buffer)._getDouble(index);
    }
}
