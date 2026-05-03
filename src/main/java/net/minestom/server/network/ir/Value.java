package net.minestom.server.network.ir;
public sealed interface Value
        permits Value.LocalValue, Value.Const, Value.IsNull, Value.IsNotNull, Value.Not, Value.Add, Value.Mul,
        Value.And, Value.Or, Value.LessThanOrEqual, Value.GreaterThan, Value.ShiftLeft, Value.ShiftRightUnsigned,
        Value.BoolByte, Value.UnsignedByte, Value.VarIntSize, Value.ArrayLength, Value.CollectionSize, Value.MapSize,
        Value.StringUtf8Bytes {
    record LocalValue(Local local) implements Value {
    }

    record Const(Object value) implements Value {
    }

    record IsNull(Value value) implements Value {
    }

    record IsNotNull(Value value) implements Value {
    }

    record Not(Value value) implements Value {
    }

    record Add(Value left, Value right) implements Value {
    }

    record Mul(Value left, Value right) implements Value {
    }

    record And(Value left, Value right) implements Value {
    }

    record Or(Value left, Value right) implements Value {
    }

    record LessThanOrEqual(Value left, Value right) implements Value {
    }

    record GreaterThan(Value left, Value right) implements Value {
    }

    record ShiftLeft(Value value, int amount) implements Value {
    }

    record ShiftRightUnsigned(Value value, int amount) implements Value {
    }

    record BoolByte(Value booleanValue) implements Value {
    }

    record UnsignedByte(Value byteValue) implements Value {
    }

    record VarIntSize(Value intValue) implements Value {
    }

    record ArrayLength(Value array) implements Value {
    }

    record CollectionSize(Value collection) implements Value {
    }

    record MapSize(Value map) implements Value {
    }

    record StringUtf8Bytes(Value string) implements Value {
    }
}
