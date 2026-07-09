package net.minestom.server.monitoring;

public record TickMonitor(long tickNanos, long acquisitionNanos) {

    @Deprecated
    public double getTickTime() {
        return tickNanos / 1e6D;
    }

    @Deprecated
    public double getAcquisitionTime() {
        return acquisitionNanos / 1e6D;
    }
}
