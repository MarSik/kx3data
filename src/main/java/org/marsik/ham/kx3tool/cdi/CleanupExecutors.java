package org.marsik.ham.kx3tool.cdi;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

public class CleanupExecutors implements CleanupPhase {
    @Inject @JobExecutor
    ExecutorService jobService;

    @Inject @Timer
    ExecutorService timerService;

    @Override
    public void close() {
        jobService.shutdownNow();
        timerService.shutdownNow();
    }
}
