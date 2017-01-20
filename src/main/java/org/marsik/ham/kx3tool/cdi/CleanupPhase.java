package org.marsik.ham.kx3tool.cdi;

public interface CleanupPhase extends AutoCloseable {
    void close();
}
