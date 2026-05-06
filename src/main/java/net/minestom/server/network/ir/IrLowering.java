package net.minestom.server.network.ir;

import net.minestom.server.network.NetworkBuffer;

import java.lang.classfile.TypeKind;
import java.util.*;
import java.util.function.Function;

final class IrLowering {
    private IrLowering() {
    }

    static final class WriteBuilderImpl implements IrWriteBuilder {
        private final Deque<List<Op>> opStack = new ArrayDeque<>();
        private final Deque<Local> sources = new ArrayDeque<>();

        WriteBuilderImpl(Local initialSource) {
            opStack.push(new ArrayList<>());
            sources.push(initialSource);
        }

        @Override
        public void push(Op op) {
            opStack.peek().add(op);
        }

        @Override
        public Local source() {
            return sources.peek();
        }

        @Override
        public void pushSource(Local source) {
            sources.push(source);
        }

        @Override
        public void popSource() {
            sources.pop();
        }

        @Override
        public void lower(NetworkBuffer.Type<?> type, Value value) {
            if (value instanceof Value.LocalValue(Local local)) {
                pushSource(local);
            } else {
                Local temp = new Local(new LocalType.Kind(TypeKind.REFERENCE));
                push(new Op.Store(value, temp));
                pushSource(temp);
            }
            type.lowerWrite(this);
            popSource();
        }

        @Override
        public List<Op> buildNested(Runnable action) {
            opStack.push(new ArrayList<>());
            action.run();
            return opStack.pop();
        }

        List<Op> result() {
            return opStack.peek();
        }
    }

    static final class ReadBuilderImpl implements IrReadBuilder {
        private final Deque<List<Op>> opStack = new ArrayDeque<>();

        ReadBuilderImpl() {
            opStack.push(new ArrayList<>());
        }

        @Override
        public void push(Op op) {
            opStack.peek().add(op);
        }

        @Override
        public Value lower(NetworkBuffer.Type<?> type) {
            return type.lowerRead(this);
        }

        @Override
        public List<Op> buildNested(Runnable action) {
            opStack.push(new ArrayList<>());
            action.run();
            return opStack.pop();
        }

        List<Op> result() {
            return opStack.peek();
        }
    }
}
