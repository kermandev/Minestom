package net.minestom.server.network.ir;

import java.util.List;

public record RunIr(Value size, List<RunItem> items, boolean reserve) {
    public RunIr(Value size, List<RunItem> items) {
        this(size, items, true);
    }

    public RunIr {
        items = List.copyOf(items);
    }
}
