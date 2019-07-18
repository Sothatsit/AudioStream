package net.sothatsit.audiostream.util;

import net.sothatsit.property.Property;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A LoopedThread that retries on exceptions instead of halting.
 *
 * @author Paddy Lamont
 */
public class RetryingLoopedThread extends LoopedThread {

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

    public RetryingLoopedThread(String name, Consumer<Property<Boolean>> task) {
        super(name, task);
    }

    public RetryingLoopedThread(String name, Consumer<Property<Boolean>> task, long delay) {
        super(name, task, delay);
    }

    public RetryingLoopedThread(String name, Consumer<Property<Boolean>> task, long delay, boolean daemon) {
        super(name, task, delay, daemon);
    }

    @Override
    protected boolean reportException(Exception exception) {
        ExceptionMuffler.Response response = exceptionMuffler.reportException(exception);

        if (response.muffled)
            return true;

        if (response.repeats == 0) {
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
