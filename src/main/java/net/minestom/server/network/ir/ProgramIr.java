package net.minestom.server.network.ir;

import java.util.List;

public record ProgramIr(List<Op> ops) {
    public static final ProgramIr EMPTY = new ProgramIr(List.of());

    public ProgramIr {
        ops = List.copyOf(ops);
    }
}
