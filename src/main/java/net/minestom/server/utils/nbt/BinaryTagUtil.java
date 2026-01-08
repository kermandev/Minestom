package net.minestom.server.utils.nbt;

import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class BinaryTagUtil {
    private BinaryTagUtil() {
    }

    public static BinaryTagType<?> nbtTypeFromId(byte id) {
        return switch (id) {
            case 0 -> BinaryTagTypes.END;
            case 1 -> BinaryTagTypes.BYTE;
            case 2 -> BinaryTagTypes.SHORT;
            case 3 -> BinaryTagTypes.INT;
            case 4 -> BinaryTagTypes.LONG;
            case 5 -> BinaryTagTypes.FLOAT;
            case 6 -> BinaryTagTypes.DOUBLE;
            case 7 -> BinaryTagTypes.BYTE_ARRAY;
            case 8 -> BinaryTagTypes.STRING;
            case 9 -> BinaryTagTypes.LIST;
            case 10 -> BinaryTagTypes.COMPOUND;
            case 11 -> BinaryTagTypes.INT_ARRAY;
            case 12 -> BinaryTagTypes.LONG_ARRAY;
            default -> throw new UnsupportedOperationException("Unsupported NBT type: " + id);
        };
    }

    public static Object nbtValueFromTag(BinaryTag tag) {
        return switch (tag) {
            case ByteBinaryTag byteTag -> byteTag.value();
            case ShortBinaryTag shortTag -> shortTag.value();
            case IntBinaryTag intTag -> intTag.value();
            case LongBinaryTag longTag -> longTag.value();
            case FloatBinaryTag floatTag -> floatTag.value();
            case DoubleBinaryTag doubleTag -> doubleTag.value();
            case ByteArrayBinaryTag byteArrayTag -> byteArrayTag.value();
            case StringBinaryTag stringTag -> stringTag.value();
            case IntArrayBinaryTag intArrayTag -> intArrayTag.value();
            case LongArrayBinaryTag longArrayTag -> longArrayTag.value();
            default -> throw new UnsupportedOperationException("Unsupported NBT type: " + tag.getClass());
        };
    }
}
