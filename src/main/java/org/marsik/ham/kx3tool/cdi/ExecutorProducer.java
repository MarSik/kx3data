package org.marsik.ham.kx3tool.cdi;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ExecutorProducer {
    @Produces @Singleton @JobExecutor
    public ExecutorService getExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Produces @Singleton @Timer
    public ScheduledExecutorService getScheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
