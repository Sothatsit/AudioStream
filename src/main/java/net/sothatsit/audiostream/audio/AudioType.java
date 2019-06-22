package net.sothatsit.audiostream.audio;

import javax.sound.sampled.*;

/**
 * Contains logic for dealing with audio sources and sinks.
 *
 * @author Paddy Lamont
 */
public enum AudioType {
    INPUT("Input") {
        @Override
        public Mixer.Info[] findAvailableMixers() {
            return AudioUtils.getInputMixers();
        }

        @Override
        public Line.Info[] findAvailableLines(Mixer mixer, AudioFormat format) {
            DataLine.Info target = new DataLine.Info(TargetDataLine.class, format);
            return mixer.getTargetLineInfo(target);
        }
    },

    OUTPUT("Output") {
        @Override
        public Mixer.Info[] findAvailableMixers() {
            return AudioUtils.getOutputMixers();
        }

        @Override
        public Line.Info[] findAvailableLines(Mixer mixer, AudioFormat format) {
            DataLine.Info target = new DataLine.Info(SourceDataLine.class, format);
            return mixer.getTargetLineInfo(target);
        }
    };

    private final String name;

    private AudioType(String name) {
        this.name = name;
    }

    /**
     * @return All available mixers of this AudioType.
     */
    public abstract Mixer.Info[] findAvailableMixers();

    /**
     * @return All available lines on the Mixer {@param mixer} with the AudioFormat {@param format}.
     */
    public abstract Line.Info[] findAvailableLines(Mixer mixer, AudioFormat format);

    @Override
    public String toString() {
        return name;
    }
}