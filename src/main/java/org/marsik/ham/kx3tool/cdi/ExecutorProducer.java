package org.marsik.ham.kx3tool.cdi;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExecutorProducer {
    @Produces @Singleton
    public Executor getExecutor() {
        return Executors.newCachedThreadPool();
    }
}
