package net.sothatsit.audiostream;

/**
 * Monitors the data transferred within a stream.
 *
 * @author Paddy Lamont
 */
public class StreamMonitor {

    private final double reportIntervalSecs;
    private final int bytesPerSample;

    private long lastTime;
    private int bytesRead;
    private int zerosRead;
    private String status;

    public StreamMonitor(double reportIntervalSecs, int bytesPerSample) {
        this.reportIntervalSecs = reportIntervalSecs;
        this.bytesPerSample = bytesPerSample;
        this.lastTime = -1;
        this.bytesRead = 0;
        this.zerosRead = 0;
        this.status = "";
    }

    private int countZeroSampleBytes(byte[] bytes, int offset, int length) {
        int zeros = 0;

        for (int sampleStart = 0; sampleStart < length; sampleStart += bytesPerSample) {
            boolean allZeroes = true;
            for (int i=0; i < bytesPerSample; ++i) {
                if (bytes[offset + sampleStart + i] != 0) {
                    allZeroes = false;
                }
            }
            if (allZeroes) {
                zeros += bytesPerSample;
            }
        }

        return zeros;
    }

    private double getSecondsSinceLastStatus() {
        return (System.nanoTime() - lastTime) * 1e-9d;
    }

    private void createNextStatus() {
        double bytesPerSec = bytesRead / getSecondsSinceLastStatus();
        double zerosPercent;
        if (bytesRead > 0) {
            zerosPercent = 100.0 * (double) zerosRead / (double) bytesRead;
        } else {
            zerosPercent = 0;
        }

        bytesRead = 0;
        zerosRead = 0;
        lastTime = System.nanoTime();

        double kbsPerSec = bytesPerSec / 1024.0;
        kbsPerSec = Math.round(kbsPerSec * 10) / 10.0;

        status = kbsPerSec + " KB / sec, " + ((int) zerosPercent) + "% zeros";
    }

    public String update(byte[] bytes, int offset, int length) {
        bytesRead += length;
        zerosRead += countZeroSampleBytes(bytes, offset, length);

        if (lastTime < 0) {
            lastTime = System.nanoTime();
        }

        if(getSecondsSinceLastStatus() > reportIntervalSecs) {
            createNextStatus();
        }

        return status;
    }
}
