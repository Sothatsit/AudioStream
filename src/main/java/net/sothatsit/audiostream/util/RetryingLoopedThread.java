package net.sothatsit.audiostream.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A LoopedThread that retries on exceptions instead of halting.
 *
 * @author Paddy Lamont
 */
public class RetryingLoopedThread extends LoopedThread {

    /**
     * The number of milliseconds to wait in between reporting
     * repeated exceptions from a RetryingLoopedThread. The total
     * time waited will double each time the same exception is reported.
     */
    private static final long INITIAL_IGNORE_REPEATED_EXCEPTION_MS = 10 * 1000;

    private final ExceptionMuffler exceptionMuffler = new ExceptionMuffler();

    public RetryingLoopedThread(String name, Runnable task) {
        super(name, task);
    }

    public RetryingLoopedThread(String name, Runnable task, long delay) {
        super(name, task, delay);
    }

    public RetryingLoopedThread(String name, Runnable task, long delay, boolean daemon) {
        super(name, task, delay, daemon);
    }

    public RetryingLoopedThread(String name, Consumer<AtomicBoolean> task) {
        super(name, task);
    }

    public RetryingLoopedThread(String name, Consumer<AtomicBoolean> task, long delay) {
        super(name, task, delay);
    }

    public RetryingLoopedThread(String name, Consumer<AtomicBoolean> task, long delay, boolean daemon) {
        super(name, task, delay, daemon);
    }

    @Override
    protected boolean runTask() {
        try {
            task.accept(enabled);
            exception = null;
            return true;
        } catch (Exception e) {
            return reportException(e);
        }
    }

    @Override
    protected boolean reportException(Exception exception) {
        this.exception = exception;

        ExceptionMuffler.Response response = exceptionMuffler.reportException(exception);

        if (response.muffled)
            return true;

        if (response.repeats <= 0) {
            super.reportException(exception);
            return true;
        }

        StringBuilder message = new StringBuilder("Repeated exception");
        if (response.repeats > 1) {
            message.append('s');
        }
        message.append(" in LoopedThread ");
        message.append(name);
        if (response.repeats > 1) {
            message.append(" (").append(response.repeats).append("x)");
        }
        message.append(": ");
        message.append(exception.getMessage());

        System.err.println(message);

        return true;
    }
}
