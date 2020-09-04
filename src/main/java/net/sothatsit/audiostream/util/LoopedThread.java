package net.sothatsit.audiostream.util;

import net.sothatsit.property.Property;

import java.util.function.Consumer;

/**
 * A class that manages a thread that repeatedly calls a function.
 *
 * @author Paddy Lamont
 */
public class LoopedThread implements AutoCloseable {

    protected final String name;
    protected final Consumer<Property<Boolean>> task;
    private final long delay;
    private final boolean daemon;
    private InterruptStrategy interruptStrategy;

    private final Property<Boolean> enabled;
    private final ServiceState.StateProperty state;
    private Thread thread;

    public LoopedThread(String name, Runnable task) {
        this(name, task, 0);
    }

    public LoopedThread(String name, Runnable task, long delay) {
        this(name, task, delay, true);
    }

    public LoopedThread(String name, Runnable task, long delay, boolean daemon) {
        this(name, running -> task.run(), delay, daemon);
    }

    public LoopedThread(String name, Consumer<Property<Boolean>> task) {
        this(name, task, 0);
    }

    public LoopedThread(String name, Consumer<Property<Boolean>> task, long delay) {
        this(name, task, delay, true);
    }

    public LoopedThread(String name, Consumer<Property<Boolean>> task, long delay, boolean daemon) {
        if (task == null)
            throw new IllegalArgumentException("task cannot be null");
        if (delay < 0)
            throw new IllegalArgumentException("delay cannot be negative");

        this.name = name;
        this.task = task;
        this.delay = delay;
        this.daemon = daemon;
        this.interruptStrategy = InterruptStrategy.THROW;
        this.enabled = Property.create("enabled", false);
        this.state = new ServiceState.StateProperty("state");
    }

    public Property<ServiceState> getState() {
        return state.readOnly();
    }

    public InterruptStrategy getInterruptStrategy() {
        return interruptStrategy;
    }

    public void setInterruptStrategy(InterruptStrategy interruptStrategy) {
        this.interruptStrategy = interruptStrategy;
    }

    /**
     * Start the thread to loop calling the given task.
     */
    public synchronized void start() {
        if (thread != null && thread.isAlive())
            throw new IllegalStateException("LoopedThread is already alive");

        state.setToStarting("Starting thread", false);

        thread = new Thread(null, this::runLoop, "LoopedThread-" + name);
        thread.setDaemon(daemon);

        enabled.set(true);
        thread.start();
    }

    /**
     * See {@link #stop()}.
     */
    @Override
    public void close() {
        stop();
    }

    /**
     * Will stop this LoopedThread the next time an iteration is completed.
     *
     * Could potentially never stop the LoopedThread if this thread's task never finishes.
     */
    public void stopNextLoop() {
        if (thread == null || !thread.isAlive())
            throw new IllegalStateException("LoopedThread is not alive");

        state.setToStopping("Stopping next loop");
        enabled.set(false);
    }

    /**
     * Alias for stopForcefully(forcefulTimeout) with a default 3 second timeout.
     */
    public void stop() {
        stop(3 * 1000);
    }

    /**
     * Interrupt the thread and wait forcefulTimeout milliseconds for
     * it to terminate before forcefully stopping the thread.
     */
    @SuppressWarnings("deprecation")
    public synchronized void stop(long timeout) {
        if (thread == null || !thread.isAlive())
            return;

        state.setToStopping("Stopping within " + timeout + " ms");
        enabled.set(false);

        long start = System.currentTimeMillis();

        try {
            long timeoutPerJoin = (timeout + 1) / 2;
            thread.join(timeoutPerJoin);
            thread.interrupt();
            thread.join(timeoutPerJoin);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for LoopedThread " + name + " to finish", e);
        } finally {
            if (thread.isAlive()) {
                String error;

                // The 15 ms is used as an epsilon due to the inaccuracy of currentTimeMillis
                if (timeout > 0 && System.currentTimeMillis() - start >= timeout - 15) {
                    error = "Forcefully terminating LoopedThread " + name + " as forceful timeout was exceeded";
                } else {
                    error = "Forcefully terminating LoopedThread " + name + " due to exception";
                }

                new RuntimeException(error).printStackTrace();
                thread.stop();
            }

            thread = null;
            state.setToStopped("Stopped");
        }
    }

    /**
     * Trigger an interrupt in this thread.
     */
    public void interrupt() {
        if (thread == null || !thread.isAlive())
            throw new IllegalStateException("LoopedThread is not running");

        thread.interrupt();
    }

    /**
     * Trigger an interrupt in this thread if it is running.
     */
    public void interruptIfRunning() {
        if (thread == null || !thread.isAlive())
            return;

        thread.interrupt();
    }

    /**
     * Continuously repeat the running of the task.
     */
    private void runLoop() {
        try {
            state.setToRunning("Running task loop");
            while (enabled.get()) {
                if (!runTask() || !enabled.get())
                    break;

                sleepForDelay();
            }
        } catch (Throwable error) {
            state.setToStopping("Error running task loop: " + error.getMessage(), error);
            throw error;
        } finally {
            enabled.set(false);
            state.setToStopped("Stopped");
        }
    }

    /**
     * Sleep for the delay between task runs.
     */
    private void sleepForDelay() {
        if (delay <= 0)
            return;

        long sleepUntil = System.nanoTime() + delay * 1000000;

        // Sleep until
        sleepLoop: while (System.nanoTime() < sleepUntil) {
            try {
                long delay = sleepUntil - System.nanoTime();
                if (delay <= 0)
                    break;

                Thread.sleep(delay / 1000000, (int) (delay % 1000000));
            } catch (InterruptedException e) {
                // If this thread has been stopped, just exit
                if (!enabled.get())
                    return;

                switch (interruptStrategy) {
                    case SKIP_WAIT:
                        break sleepLoop;

                    case REPORT:
                        System.err.println("Interrupted between executions of LoopedThread " + name);
                        break;

                    case THROW:
                        throw new RuntimeException("Interrupted between executions of LoopedThread " + name, e);

                    default:
                        throw new IllegalStateException("Unknown InterruptStrategy " + interruptStrategy);
                }
            }
        }
    }

    /**
     * @return Whether to continue running.
     */
    private boolean runTask() {
        try {
            task.accept(enabled.readOnly());
            state.clearRunningError();
            return true;
        } catch (Exception exception) {
            boolean continueRunning = reportException(exception);

            if (continueRunning) {
                state.setToRunning("Error running task: " + exception.getMessage(), false, exception);
            } else {
                state.setToStopping("Error running task: " + exception.getMessage(), false, exception);
            }

            return continueRunning;
        }
    }

    /**
     * @return Whether to continue running despite the exception.
     */
    protected boolean reportException(Exception exception) {
        new IllegalStateException("Exception in LoopedThread " + name, exception).printStackTrace();
        return false;
    }

    /**
     * Strategy to take when the thread is interrupted during waits between task executions.
     */
    public enum InterruptStrategy {
        /**
         * Execute the next iteration of the looped thread straight away.
         */
        SKIP_WAIT,

        /**
         * Report the interrupt and then continue on as normal.
         */
        REPORT,

        /**
         * Throw the interrupt exception further up the chain.
         */
        THROW
    }
}
