package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;

import java.util.List;
import java.util.function.Function;

public sealed interface RunItem {
    record Put(StoreKind kind, Value offset, Value value) implements RunItem {
    }

    record Get(StoreKind kind, Value offset, Local out) implements RunItem {
    }

    record PutVarInt(Value offset, Value value, Value encodedSize) implements RunItem {
    }

    record GetVarInt(Local out) implements RunItem {
    }

    record PutVarLong(Value offset, Value value, Value encodedSize) implements RunItem {
    }

    record GetVarLong(Local out) implements RunItem {
    }

    record PutBytes(Value offset, Value byteArray, Value length) implements RunItem {
    }

    record GetBytes(Value offset, Local byteArray, Value length) implements RunItem {
    }

    record If(Value condition, List<RunIr> thenRuns, List<RunIr> elseRuns) implements RunItem {
        public If {
            thenRuns = List.copyOf(thenRuns);
            elseRuns = List.copyOf(elseRuns);
        }
    }

    record ForEach(Value source, Local element, List<RunIr> body) implements RunItem {
        public ForEach {
            body = List.copyOf(body);
        }
    }

    record ForIndex(Local index, Value start, Value end, List<RunIr> body) implements RunItem {
        public ForIndex {
            body = List.copyOf(body);
        }
    }

    record Apply(Function<?, ?> function, Local in, Local out) implements RunItem {
    }

    record Cast(Local in, Class<?> targetClass, Local out) implements RunItem {
    }

    record Unbox(PrimitiveKind kind, Local in, Local out) implements RunItem {
    }

    record Box(PrimitiveKind kind, Local in, Local out) implements RunItem {
    }

    record StringToBytes(Local in, Local out) implements RunItem {
    }

    record BytesToString(Local in, Local out) implements RunItem {
    }

    record EitherLeft(Local in, Local out) implements RunItem {
    }

    record EitherRight(Local in, Local out) implements RunItem {
    }

    record Store(Value value, Local out) implements RunItem {
    }

    record Check(Value condition, String message) implements RunItem {
    }

    record WriteExternal(NetworkBuffer.Type<?> type, Value value) implements RunItem {
    }

    record ReadExternal(NetworkBuffer.Type<?> type, Local out) implements RunItem {
    }

    record ElementAt(Value source, Value index, Local out) implements RunItem {
    }

    record MapEntrySet(Value map, Local out) implements RunItem {
    }

    record MapEntryKey(Local entry, Local out) implements RunItem {
    }

    record MapEntryValue(Local entry, Local out) implements RunItem {
    }

    record ResultElementSet(Value result, Value index, Value value) implements RunItem {
    }

    record ArrayCreate(Value size, Local out) implements RunItem {
    }

    record ArraySet(Local array, Value index, Value value) implements RunItem {
    }

    record ListFinish(Local array, Local out) implements RunItem {
    }

    record MapFinish(Local keys, Local values, Value size, Local out) implements RunItem {
    }

    record Construct(Object factory, List<Value> args, Local out) implements RunItem {
        public Construct {
            args = List.copyOf(args);
        }
    }

    record Return(Value value) implements RunItem {
    }
}
