package net.sothatsit.audiostream.util;

/**
 * Allows easily timing sections of code.
 *
 * @author Paddy Lamont
 */
public class Timer {

    private final long start;

    public Timer() {
        this.start = System.nanoTime();
    }

    public long getDurationNs() {
        return System.nanoTime() - start;
    }

    public double getDurationMs() {
        return getDurationNs() / 1.0e6;
    }

    public double getDurationS() {
        return getDurationNs() / 1.0e9;
    }

    @Override
    public String toString() {
        long durationNs = getDurationNs();

        double durationMs = durationNs / 1.0e6;
        if (durationMs < 0.01)
            return String.format("%d nanos", durationNs);

        double durationS = durationNs / 1.0e9;
        if (durationS < 1)
            return String.format("%.2f ms", durationMs);

        return String.format("%.2f s", durationS);
    }

    public static Timer start() {
        return new Timer();
    }
}
