package net.minestom.server.network.ir;
import net.minestom.server.network.NetworkBuffer;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public sealed interface Op
        permits Op.GetField, Op.Apply, Op.Cast, Op.Unbox, Op.Box, Op.Store, Op.Check, Op.StringToBytes, Op.BytesToString, Op.EitherLeft, Op.EitherRight, Op.WriteExternal, Op.ReadExternal, Op.WritePrimitive, Op.ReadPrimitive, Op.WriteVarInt, Op.ReadVarInt, Op.WriteVarLong, Op.ReadVarLong, Op.WriteFixedBytes, Op.ReadFixedBytes, Op.WriteRun, Op.ReadRun, Op.If, Op.ForEach, Op.ForIndex, Op.ReserveWrite, Op.ReserveRead, Op.AdvanceWriteIndex, Op.AdvanceReadIndex, Op.Return {
    record GetField(FieldIr<?, ?> field, String path, Local source, Local out) implements Op {
        public GetField {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(out, "out");
        }
    }

    record Apply(Function<?, ?> function, String path, Local in, Local out) implements Op {
        public Apply {
            Objects.requireNonNull(function, "function");
            Objects.requireNonNull(in, "in");
            Objects.requireNonNull(out, "out");
        }
    }

    record Cast(Local in, Class<?> targetClass, Local out) implements Op {
        public Cast {
            Objects.requireNonNull(in, "in");
            Objects.requireNonNull(targetClass, "targetClass");
            Objects.requireNonNull(out, "out");
        }
    }

    record Unbox(PrimitiveKind kind, Local in, Local out) implements Op {
        public Unbox {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(in, "in");
            Objects.requireNonNull(out, "out");
        }
    }

    record Box(PrimitiveKind kind, Local in, Local out) implements Op {
        public Box {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(in, "in");
            Objects.requireNonNull(out, "out");
        }
    }

    record Store(Value value, Local out) implements Op {
        public Store {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(out, "out");
        }
    }

    record Check(Value condition, String message) implements Op {
        public Check {
            Objects.requireNonNull(condition, "condition");
            Objects.requireNonNull(message, "message");
        }
    }

    record StringToBytes(Local in, Local out) implements Op {
        public StringToBytes {
            Objects.requireNonNull(in, "in");
            Objects.requireNonNull(out, "out");
        }
    }

    record BytesToString(Local in, Local out) implements Op {
        public BytesToString {
            Objects.requireNonNull(in, "in");
            Objects.requireNonNull(out, "out");
        }
    }

    record EitherLeft(Local in, Local out) implements Op {
        public EitherLeft {
            Objects.requireNonNull(in, "in");
            Objects.requireNonNull(out, "out");
        }
    }

    record EitherRight(Local in, Local out) implements Op {
        public EitherRight {
            Objects.requireNonNull(in, "in");
            Objects.requireNonNull(out, "out");
        }
    }

    record WriteExternal(NetworkBuffer.Type<?> type, Value value) implements Op {
        public WriteExternal {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(value, "value");
        }
    }

    record ReadExternal(NetworkBuffer.Type<?> type, Local out) implements Op {
        public ReadExternal {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(out, "out");
        }
    }

    record WritePrimitive(PrimitiveKind kind, Value value, Value address) implements Op {
        public WritePrimitive {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(value, "value");
        }
    }

    record ReadPrimitive(PrimitiveKind kind, Value address, Local out) implements Op {
        public ReadPrimitive {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(out, "out");
        }
    }

    record WriteVarInt(Value value, Value address) implements Op {
        public WriteVarInt {
            Objects.requireNonNull(value, "value");
        }
    }

    record ReadVarInt(Value address, Local out) implements Op {
        public ReadVarInt {
            Objects.requireNonNull(out, "out");
        }
    }

    record WriteVarLong(Value value, Value address) implements Op {
        public WriteVarLong {
            Objects.requireNonNull(value, "value");
        }
    }

    record ReadVarLong(Value address, Local out) implements Op {
        public ReadVarLong {
            Objects.requireNonNull(out, "out");
        }
    }

    record WriteFixedBytes(Value value, Value address) implements Op {
        public WriteFixedBytes {
            Objects.requireNonNull(value, "value");
        }
    }

    record ReadFixedBytes(Value length, Value address, Local out) implements Op {
        public ReadFixedBytes {
            Objects.requireNonNull(length, "length");
            Objects.requireNonNull(out, "out");
        }
    }

    record WriteRun(Value address, RunIr run) implements Op {
        public WriteRun {
            Objects.requireNonNull(run, "run");
        }
    }

    record ReadRun(Value address, RunIr run) implements Op {
        public ReadRun {
            Objects.requireNonNull(run, "run");
        }
    }

    record If(Value condition, List<Op> thenOps, List<Op> elseOps) implements Op {
        public If {
            Objects.requireNonNull(condition, "condition");
            thenOps = List.copyOf(thenOps);
            elseOps = List.copyOf(elseOps);
        }
    }

    record ForEach(Value source, Local element, List<Op> body) implements Op {
        public ForEach {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(element, "element");
            body = List.copyOf(body);
        }
    }

    record ForIndex(Local index, Value start, Value end, List<Op> body) implements Op {
        public ForIndex {
            Objects.requireNonNull(index, "index");
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            body = List.copyOf(body);
        }
    }

    record ReserveWrite(Value size, Local addressOut) implements Op {
        public ReserveWrite {
            Objects.requireNonNull(size, "size");
            Objects.requireNonNull(addressOut, "addressOut");
        }
    }

    record ReserveRead(Value size, Local addressOut) implements Op {
        public ReserveRead {
            Objects.requireNonNull(size, "size");
            Objects.requireNonNull(addressOut, "addressOut");
        }
    }

    record AdvanceWriteIndex(Value amount) implements Op {
        public AdvanceWriteIndex {
            Objects.requireNonNull(amount, "amount");
        }
    }

    record AdvanceReadIndex(Value amount) implements Op {
        public AdvanceReadIndex {
            Objects.requireNonNull(amount, "amount");
        }
    }

    record Construct(ConstructorIr<?> constructor, String path, List<Value> args, Local out) implements Op {
        public Construct {
            Objects.requireNonNull(constructor, "constructor");
            Objects.requireNonNull(out, "out");
            args = List.copyOf(args);
        }
    }

    record Return(Value value) implements Op {
        public Return {
            Objects.requireNonNull(value, "value");
        }
    }
}
