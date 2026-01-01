package org.leeminkan.redis.jmx;

import org.leeminkan.redis.KanProtocol;
import org.leeminkan.redis.KanRedisServer;
import org.leeminkan.redis.KanStore;

public class KanMonitor implements KanMonitorMBean {

    private final KanStore store;

    public KanMonitor(KanStore store) {
        this.store = store;
    }

    @Override
    public int getConnectedClients() {
        return KanRedisServer.connectedClients.get();
    }

    @Override
    public long getTotalCommandsProcessed() {
        return KanProtocol.totalCommands.get();
    }

    @Override
    public long getOffHeapMemoryUsage() {
        return store.getUsedMemory();
    }

    @Override
    public String getStatus() {
        return "Healthy";
    }
}