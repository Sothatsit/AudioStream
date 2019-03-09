package net.sothatsit.audiostream.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A class that manages a thread that repeatedly calls a function.
 *
 * @author Paddy Lamont
 */
public class LoopedThread {

    private final String name;
    private final Consumer<AtomicBoolean> task;
    private final long delay;
    private final boolean daemon;

    private Thread thread;
    private AtomicBoolean enabled;
    private Exception exception;

    public LoopedThread(String name, Runnable task) {
        this(name, task, 0);
    }

    public LoopedThread(String name, Runnable task, long delay) {
        this(name, task, delay, true);
    }

    public LoopedThread(String name, Runnable task, long delay, boolean daemon) {
        this(name, running -> task.run(), delay, daemon);
    }

    public LoopedThread(String name, Consumer<AtomicBoolean> task) {
        this(name, task, 0);
    }

    public LoopedThread(String name, Consumer<AtomicBoolean> task, long delay) {
        this(name, task, delay, true);
    }

    public LoopedThread(String name, Consumer<AtomicBoolean> task, long delay, boolean daemon) {
        if (task == null)
            throw new IllegalArgumentException("task cannot be null");
        if (delay < 0)
            throw new IllegalArgumentException("delay cannot be negative");

        this.name = name;
        this.task = task;
        this.delay = delay;
        this.daemon = daemon;
        this.enabled = new AtomicBoolean(false);
    }

    /**
     * The state of this thread.
     */
    public enum ThreadState {
        STOPPED,
        RUNNING,
        ERRORED
    }

    public ThreadState getState() {
        if (exception != null)
            return ThreadState.ERRORED;
        if (thread == null || !thread.isAlive())
            return ThreadState.STOPPED;
        return ThreadState.RUNNING;
    }

    public boolean isRunning() {
        return getState() == ThreadState.RUNNING;
    }

    public Exception getException() {
        return exception;
    }

    /**
     * Start the thread to loop calling the given task.
     */
    public synchronized void start() {
        if (thread != null && thread.isAlive())
            throw new IllegalStateException("LoopedThread is already running");

        thread = new Thread(this::runLoop);
        thread.setDaemon(daemon);

        enabled.set(true);
        exception = null;
        thread.start();
    }

    /**
     * Alias for stopGracefully(interruptTimeout, forcefulTimeout) with
     * defaults of 3 seconds for the interrupt and forceful timeouts.
     */
    public void stopGracefully() {
        stopGracefully(3 * 1000, 3 * 1000);
    }

    /**
     * Gracefully stop the given thread, first disabling it and waiting
     * interruptTimeout milliseconds for it to stop on its own.
     *
     * If interruptTimeout milliseconds pass without the thread stopping,
     * the thread will then be interrupted and forcefulTimeout milliseconds
     * will be waited before forcefully stopping the thread.
     */
    public synchronized void stopGracefully(long interruptTimeout, long forcefulTimeout) {
        if (thread == null || !thread.isAlive())
            return;

        enabled.set(false);

        long start = System.currentTimeMillis();

        try {
            thread.join(interruptTimeout);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for thread to finish", e);
        } finally {
            if (thread.isAlive()) {
                if (interruptTimeout > 0 && System.currentTimeMillis() - start >= interruptTimeout) {
                    System.err.println("Forcefully stopping thread " + this + " as interrupt timeout was exceeded");
                } else {
                    System.err.println("Forcefully stopping thread " + this + " due to exception");
                }

                stopForcefully(forcefulTimeout);
            }

            thread = null;
        }
    }

    /**
     * Alias for stopForcefully(forcefulTimeout) with a default 3 second timeout.
     */
    public void stopForcefully() {
        stopForcefully(3 * 1000);
    }

    /**
     * Interrupt the thread and wait forcefulTimeout milliseconds for
     * it to terminate before forcefully stopping the thread.
     */
    public synchronized void stopForcefully(long forcefulTimeout) {
        if (thread == null || !thread.isAlive())
            return;

        long start = System.currentTimeMillis();

        try {
            thread.interrupt();
            thread.join(forcefulTimeout);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for LoopedThread " + name + "to finish", e);
        } finally {
            if (thread.isAlive()) {
                if (forcefulTimeout > 0 && System.currentTimeMillis() - start >= forcefulTimeout) {
                    System.err.println("Terminating LoopedThread " + name + " as forceful timeout was exceeded");
                } else {
                    System.err.println("Terminating LoopedThread " + name + " due to exception");
                }

                thread.stop();
            }

            thread = null;
        }
    }

    private void runLoop() {
        while (enabled.get()) {
            try {
                task.accept(enabled);
            } catch (Exception e) {
                new RuntimeException("Exception in LoopedThread " + name, e).printStackTrace();
                exception = e;
                break;
            }

            if (!enabled.get())
                break;
            if (delay == 0)
                continue;

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                System.err.println("Interrupted executing LoopedThread " + name);
            }
        }
    }
}
