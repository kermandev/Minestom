package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class NetworkTemplater {
    private NetworkTemplater() {}

    public static <T extends @UnknownNullability Object> NetworkBuffer.Type<T> template(Object... values) {
        Objects.requireNonNull(values, "values");
        Check.argCondition(values.length % 2 == 0, "Expected an odd number of values, got: {0}", values.length);
        Check.argCondition(values.length < 3, "Expected at least three values ([type, getter], ctor), got: {0}", values.length);
        final int fieldCount = values.length / 2;
        Check.argCondition(fieldCount > 20, "Templates only support up to 20 fields, got: {0}", fieldCount);
        for (int i = 0; i < fieldCount; i++) {
            Objects.requireNonNull(values[i * 2], "type " + i);
            Objects.requireNonNull(values[i * 2 + 1], "getter " + i);
        }
        Objects.requireNonNull(values[values.length - 1], "ctor");

        NetworkBuffer.Type<T> unoptimized = new TemplateType<>(values, fieldCount);
        return IrCompiler.compile(unoptimized);
    }

    private static final class TemplateType<T extends @UnknownNullability Object> implements NetworkBuffer.Type<T> {
        private final Object[] values;
        private final int fieldCount;
        private final Object constructor;

        TemplateType(Object[] values, int fieldCount) {
            this.values = values;
            this.fieldCount = fieldCount;
            this.constructor = values[values.length - 1];
        }

        @Override
        public void lowerWrite(IrWriteBuilder builder) {
            for (int i = 0; i < fieldCount; i++) {
                final NetworkBuffer.Type<?> type = (NetworkBuffer.Type<?>) values[i * 2];
                final Function<?, ?> getter = (Function<?, ?>) values[i * 2 + 1];
                final Local nested = IrLowering.referenceLocal();
                builder.push(new Op.Apply(getter, builder.source(), nested));
                builder.pushSource(nested);
                builder.lower(type, new Value.LocalValue(nested));
                builder.popSource();
            }
        }

        @Override
        public Value lowerRead(IrReadBuilder builder) {
            final List<Value> args = new ArrayList<>(fieldCount);
            for (int i = 0; i < fieldCount; i++) {
                final NetworkBuffer.Type<?> type = (NetworkBuffer.Type<?>) values[i * 2];
                args.add(builder.lower(type));
            }
            final Local result = IrLowering.referenceLocal();
            builder.push(new Op.Construct(constructor, args, result));
            return new Value.LocalValue(result);
        }

        @Override
        public void write(NetworkBuffer buffer, T value) {
            throw new UnsupportedOperationException("TemplateType should be compiled");
        }

        @Override
        public T read(NetworkBuffer buffer) {
            throw new UnsupportedOperationException("TemplateType should be compiled");
        }
    }
}
