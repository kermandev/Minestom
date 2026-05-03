package net.minestom.server.network.ir;
import net.minestom.server.network.NetworkBuffer;

import java.util.List;
import java.util.function.Function;

public sealed interface Op
        permits Op.GetField, Op.Apply, Op.Cast, Op.Unbox, Op.Box, Op.Store, Op.Check, Op.WriteExternal, Op.ReadExternal,
        Op.ReadVarInt, Op.WriteVarLong, Op.ReadVarLong, Op.WriteRun, Op.ReadRun, Op.If, Op.ForEach,
        Op.ForIndex, Op.ElementAt, Op.MapEntrySet, Op.MapEntryKey, Op.MapEntryValue, Op.ResultElementSet,
        Op.ArrayCreate, Op.ArraySet, Op.ListFinish, Op.MapFinish, Op.Construct, Op.Return {
    record GetField(FieldIr<?, ?> field, String path, Local source, Local out) implements Op {
    }

    record Apply(Function<?, ?> function, Local in, Local out) implements Op {
    }

    record Cast(Local in, Class<?> targetClass, Local out) implements Op {
    }

    record Unbox(PrimitiveKind kind, Local in, Local out) implements Op {
    }

    record Box(PrimitiveKind kind, Local in, Local out) implements Op {
    }

    record Store(Value value, Local out) implements Op {
    }

    record Check(Value condition, String message) implements Op {
    }

    record WriteExternal(NetworkBuffer.Type<?> type, Value value) implements Op {
    }

    record ReadExternal(NetworkBuffer.Type<?> type, Local out) implements Op {
    }

    record ReadVarInt(Local out) implements Op {
    }

    record WriteVarLong(Value value) implements Op {
    }

    record ReadVarLong(Local out) implements Op {
    }

    record WriteRun(RunIr run) implements Op {
    }

    record ReadRun(RunIr run) implements Op {
    }

    record If(Value condition, List<Op> thenOps, List<Op> elseOps) implements Op {
        public If {
            thenOps = List.copyOf(thenOps);
            elseOps = List.copyOf(elseOps);
        }
    }

    record ForEach(Value source, Local element, List<Op> body) implements Op {
        public ForEach {
            body = List.copyOf(body);
        }
    }

    record ForIndex(Local index, Value start, Value end, List<Op> body) implements Op {
        public ForIndex {
            body = List.copyOf(body);
        }
    }

    record ElementAt(Value source, Value index, Local out) implements Op {
    }

    record MapEntrySet(Value map, Local out) implements Op {
    }

    record MapEntryKey(Local entry, Local out) implements Op {
    }

    record MapEntryValue(Local entry, Local out) implements Op {
    }

    record ResultElementSet(Value result, Value index, Value value) implements Op {
    }

    record ArrayCreate(Value size, Local out) implements Op {
    }

    record ArraySet(Local array, Value index, Value value) implements Op {
    }

    record ListFinish(Local array, Local out) implements Op {
    }

    record MapFinish(Local keys, Local values, Value size, Local out) implements Op {
    }

    record Construct(ConstructorIr<?> constructor, String path, List<Value> args, Local out) implements Op {
        public Construct {
            args = List.copyOf(args);
        }
    }

    record Return(Value value) implements Op {
    }
}
