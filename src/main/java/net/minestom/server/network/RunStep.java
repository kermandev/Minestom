package net.minestom.server.network;

import java.util.List;
import java.util.function.Function;

public sealed interface RunStep
        permits RunStep.ElementAt, RunStep.GetField, RunStep.Apply, RunStep.Cast, RunStep.Unbox, RunStep.Box,
        RunStep.Put, RunStep.Get, RunStep.PutVarInt, RunStep.PutBytes, RunStep.GetBytes, RunStep.ResultElementSet,
        RunStep.CollectionAdd, RunStep.MapPut, RunStep.Construct {
    record ElementAt(Value source, Value index, Local out) implements RunStep {
    }

    record GetField(FieldIr<?, ?> field, String path, Local source, Local out) implements RunStep {
    }

    record Apply(Function<?, ?> function, Local in, Local out) implements RunStep {
    }

    record Cast(Local in, Class<?> targetClass, Local out) implements RunStep {
    }

    record Unbox(PrimitiveKind kind, Local in, Local out) implements RunStep {
    }

    record Box(PrimitiveKind kind, Local in, Local out) implements RunStep {
    }

    record Put(StoreKind kind, Value offset, Value value) implements RunStep {
    }

    record Get(StoreKind kind, Value offset, Local out) implements RunStep {
    }

    record PutVarInt(Value offset, Value value, Value encodedSize) implements RunStep {
    }

    record PutBytes(Value offset, Value byteArray, Value length) implements RunStep {
    }

    record GetBytes(Value offset, Local byteArray, Value length) implements RunStep {
    }

    record ResultElementSet(Value result, Value index, Value value) implements RunStep {
    }

    record CollectionAdd(CollectionFactory<?, ?> factory, String path, Local collection, Value value) implements RunStep {
    }

    record MapPut(MapFactory<?, ?, ?> factory, String path, Local map, Value key, Value value) implements RunStep {
    }

    record Construct(ConstructorIr<?> constructor, String path, List<Value> args, Local out) implements RunStep {
        public Construct {
            args = List.copyOf(args);
        }
    }
}
