package net.minestom.server.network;

import java.util.List;

public record RunIr(Value size, List<RunItem> items) {
    public RunIr {
        items = List.copyOf(items);
    }
}
