package net.minestom.server.network.ir;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public record ProgramIr(List<RunIr> runs, @Nullable Local initialSource) {
    public static final ProgramIr EMPTY = new ProgramIr(List.of(), null);

    public ProgramIr(List<RunIr> runs) {
        this(runs, null);
    }

    public ProgramIr {
        runs = List.copyOf(runs);
    }
}
