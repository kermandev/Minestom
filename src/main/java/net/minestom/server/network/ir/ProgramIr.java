package net.minestom.server.network.ir;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public record ProgramIr(List<Op> ops, @Nullable Local initialSource) {
    public static final ProgramIr EMPTY = new ProgramIr(List.of(), null);

    public ProgramIr(List<Op> ops) {
        this(ops, null);
    }

    public ProgramIr {
        ops = List.copyOf(ops);
    }
}
