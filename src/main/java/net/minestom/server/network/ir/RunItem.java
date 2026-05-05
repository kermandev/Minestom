package net.minestom.server.network.ir;

import java.util.List;

public sealed interface RunItem {
    record Put(StoreKind kind, Value offset, Value value) implements RunItem {
    }

    record Get(StoreKind kind, Value offset, Local out) implements RunItem {
    }

    record PutVarInt(Value offset, Value value, Value encodedSize) implements RunItem {
    }

    record PutVarLong(Value offset, Value value, Value encodedSize) implements RunItem {
    }

    record PutBytes(Value offset, Value byteArray, Value length) implements RunItem {
    }

    record GetBytes(Value offset, Local byteArray, Value length) implements RunItem {
    }

    record ForIndex(Local index, Value start, Value end, List<RunStep> body) implements RunItem {
        public ForIndex {
            body = List.copyOf(body);
        }
    }
}
