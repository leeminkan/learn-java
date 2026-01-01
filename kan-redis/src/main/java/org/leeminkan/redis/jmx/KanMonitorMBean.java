package org.leeminkan.redis.jmx;

public interface KanMonitorMBean {
    int getConnectedClients();
    long getTotalCommandsProcessed();
    long getOffHeapMemoryUsage();
    String getStatus(); // Just for fun, e.g., "Running"
}