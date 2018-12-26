package net.sothatsit.audiostream.util;

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
    private String lastStatus;

    public StreamMonitor(double reportIntervalSecs, int bytesPerSample) {
        this.reportIntervalSecs = reportIntervalSecs;
        this.bytesPerSample = bytesPerSample;
        this.lastTime = -1;
        this.bytesRead = 0;
        this.zerosRead = 0;
        this.lastStatus = "";
    }

    public String update(byte[] bytes, int offset, int length) {
        bytesRead += length;
        for (int sampleStart = 0; sampleStart < length; sampleStart += bytesPerSample) {
            boolean allZeroes = true;
            for (int i=0; i < bytesPerSample; ++i) {
                if (bytes[offset + sampleStart + i] != 0) {
                    allZeroes = false;
                }
            }
            if (allZeroes) {
                zerosRead += bytesPerSample;
            }
        }

        if (lastTime < 0) {
            lastTime = System.nanoTime();
        }

        long time = System.nanoTime();
        double timeElapsed = (time - lastTime) * 1e-9d;

        if(timeElapsed > reportIntervalSecs) {
            double bytesPerSec = bytesRead / timeElapsed;
            double zerosPercent = 100.0 * (double) zerosRead / (double) bytesRead;

            bytesRead = 0;
            zerosRead = 0;
            lastTime = time;

            double kbsPerSec = bytesPerSec / 1024.0;
            kbsPerSec = Math.round(kbsPerSec * 10) / 10.0;

            lastStatus = kbsPerSec + " KB / sec, " + ((int) zerosPercent) + "% zeros";
        }

        return lastStatus;
    }
}
