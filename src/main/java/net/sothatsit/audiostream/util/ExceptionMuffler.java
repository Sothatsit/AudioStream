package net.sothatsit.audiostream.util;

import java.util.Arrays;

/**
 * Can be used to subdue excessive logging of repeated exceptions.
 *
 * @author Paddy Lamont
 */
public class ExceptionMuffler {

    private static final double DEFAULT_INITIAL_IGNORE_TIME_MS = 1000;
    private static final double DEFAULT_IGNORE_TIME_MULTIPLIER = 2;

    private final double initialIgnoreTimeMs;
    private final double ignoreTimeMultiplier;

    private double ignoreTimeMs;
    private Exception lastException = null;
    private long lastExceptionTime = -1;
    private long lastReportTime = -1;
    private int exceptionRepeats = 0;

    public ExceptionMuffler() {
        this(DEFAULT_INITIAL_IGNORE_TIME_MS, DEFAULT_IGNORE_TIME_MULTIPLIER);
    }

    public ExceptionMuffler(double initialIgnoreTimeMs, double ignoreTimeMultiplier) {
        this.initialIgnoreTimeMs = initialIgnoreTimeMs;
        this.ignoreTimeMultiplier = ignoreTimeMultiplier;
        this.ignoreTimeMs = initialIgnoreTimeMs;
    }

    public Exception getLastException() {
        return lastException;
    }

    public long getLastExceptionTime() {
        return lastExceptionTime;
    }

    /**
     * A response dictating how or whether to report an exception.
     */
    public static class Response {

        public final Exception exception;
        public final int repeats;
        public final boolean muffled;

        private Response(Exception exception, int repeats, boolean muffled) {
            this.exception = exception;
            this.repeats = repeats;
            this.muffled = muffled;
        }
    }

    /**
     * Report a new exception to this exception muffler to get a response of how to deal with it.
     *
     * @return A response object that can be used to decide how to report the exception.
     */
    public Response reportException(Exception exception) {
        Exception lastException = this.lastException;
        this.lastException = exception;
        this.lastExceptionTime = System.currentTimeMillis();

        // If we've already seen the same exception, print a smaller report
        if (areExceptionsEqual(exception, lastException)) {
            long timeSince = System.currentTimeMillis() - lastReportTime;

            exceptionRepeats += 1;
            if (timeSince < ignoreTimeMs)
                return new Response(exception, exceptionRepeats, true);

            int repeats = exceptionRepeats + 1;
            exceptionRepeats = 0;
            lastReportTime = System.currentTimeMillis();
            ignoreTimeMs *= ignoreTimeMultiplier;

            return new Response(exception, repeats, false);
        }

        ignoreTimeMs = initialIgnoreTimeMs;
        lastReportTime = System.currentTimeMillis();
        return new Response(exception, 0, false);
    }

    /**
     * @return Whether {@param e1} and {@param e2} are exceptions with the same
     *         class, message, stack trace, causes, and suppressed exceptions.
     */
    private static boolean areExceptionsEqual(Throwable e1, Throwable e2) {
        if (e1 == null || e2 == null)
            return e1 == null && e2 == null;
        if (!e1.getClass().equals(e2.getClass()))
            return false;
        if (!e1.getMessage().equals(e2.getMessage()))
            return false;
        if (!Arrays.equals(e1.getStackTrace(), e2.getStackTrace()))
            return false;
        if (!areExceptionsEqual(e1.getCause(), e2.getCause()))
            return false;

        Throwable[] suppressed1 = e1.getSuppressed();
        Throwable[] suppressed2 = e2.getSuppressed();

        if (suppressed1.length != suppressed2.length)
            return false;

        for (int index = 0; index < suppressed1.length; ++index) {
            if (!areExceptionsEqual(suppressed1[index], suppressed2[index]))
                return false;
        }

        return true;
    }
}
