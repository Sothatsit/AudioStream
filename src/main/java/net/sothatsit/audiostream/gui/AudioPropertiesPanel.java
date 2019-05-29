package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioUtils;
import net.sothatsit.property.ListProperty;
import net.sothatsit.property.Property;
import net.sothatsit.property.Either;
import net.sothatsit.property.gui.GBCBuilder;
import net.sothatsit.property.gui.PropertyComboBox;
import net.sothatsit.property.gui.PropertyLabel;
import net.sothatsit.property.gui.PropertyPanel;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * An interface to select an audio mixer and an audio format.
 *
 * @author Paddy Lamont
 */
public class AudioPropertiesPanel extends PropertyPanel {

    private final AudioType audioType;
    private final ListProperty<Mixer.Info> availableMixers;

    public AudioPropertiesPanel(AudioType audioType, AudioProperties properties) {
        this(audioType, properties, true);
    }

    public AudioPropertiesPanel(AudioType audioType, AudioProperties properties, boolean showFormatSelectors) {
        super(new JPanel());

        this.audioType = audioType;

        Property<Boolean> isAudioFormatSupported = Property.map(
                "isSupportedAudioFormat",
                properties.mixer, properties.audioFormat,
                this::isAudioFormatSupported
        );

        setLayout(new GridBagLayout());

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.HORIZONTAL)
                .insets(5, 5, 5, 5);

        { // Mixer selection
            this.availableMixers = new ListProperty<>("availableMixers", audioType.findAvailableMixers());
            properties.mixer.compareAndSet(null, availableMixers.get(0));

            PropertyComboBox<Mixer.Info> mixerCombo = new PropertyComboBox<>(
                    availableMixers, properties.mixer, Mixer.Info::getName
            );
            mixerCombo.setEnabled(isEnabled());

            add(audioType.toString(), constraints.build());
            add(mixerCombo, constraints.weightX(1.0).build(4));
            constraints.nextRow();
        }

        // Audio format selection
        if (showFormatSelectors) {
            PropertyComboBox<AudioFormat.Encoding> encodingCombo = new PropertyComboBox<>(
                    new AudioFormat.Encoding[] {
                            AudioFormat.Encoding.PCM_SIGNED,
                            AudioFormat.Encoding.PCM_UNSIGNED
                    },
                    properties.encoding,
                    AudioUtils::toHumanString
            );

            PropertyComboBox<Boolean> bigEndianCombo = new PropertyComboBox<>(
                    new Boolean[] {true, false},
                    properties.isBigEndian,
                    value -> (value ? "Big Endian" : "Little Endian")
            );

            PropertyComboBox<Float> sampleRateCombo = new PropertyComboBox<>(
                    new Float[] {8000f, 44100f, 48000f},
                    properties.sampleRate,
                    value -> value + " Hz"
            );

            PropertyComboBox<Integer> sampleSizeCombo = new PropertyComboBox<>(
                    new Integer[] {8, 16, 24},
                    properties.sampleSize,
                    value -> value + " bit"
            );

            PropertyComboBox<Integer> channelsCombo = new PropertyComboBox<>(
                    new Integer[] {1, 2, 64},
                    properties.channels,
                    value -> value + " channels"
            );

            encodingCombo.setEnabled(isEnabled());
            bigEndianCombo.setEnabled(isEnabled());
            sampleRateCombo.setEnabled(isEnabled());
            sampleSizeCombo.setEnabled(isEnabled());
            channelsCombo.setEnabled(isEnabled());

            PropertyLabel audioFormatLabel = new PropertyLabel("Audio Format");
            audioFormatLabel.setForeground(
                    Property.ifCond("audioFormatLabelFgColor", isAudioFormatSupported, Color.BLACK, Color.RED)
            );

            add(audioFormatLabel, constraints.build());
            add("Encoding", Color.DARK_GRAY, constraints.build());
            add(encodingCombo, constraints.weightX(1.0).build());
            add("Endian", Color.DARK_GRAY, constraints.build());
            add(bigEndianCombo, constraints.weightX(1.0).build());
            constraints.nextRow();

            constraints.nextColumn();
            add("Sample Rate", Color.DARK_GRAY, constraints.build());
            add(sampleRateCombo, constraints.weightX(1.0).build());
            add("Sample Size", Color.DARK_GRAY, constraints.build());
            add(sampleSizeCombo, constraints.weightX(1.0).build());
            constraints.nextRow();

            constraints.nextColumn();
            add("Channels", Color.DARK_GRAY, constraints.build());
            add(channelsCombo, constraints.weightX(1.0).build());
            constraints.nextRow();
        }

        // Periodically update the available mixers
        new Timer(1000, event -> updateAvailableMixers()).start();
    }

    private void add(String string, Color foreground, GridBagConstraints constraints) {
        JLabel label = new JLabel(string);
        label.setForeground(foreground);
        add(label, constraints);
    }

    /**
     * Allows program to pick up on mixers as they are added or removed (e.g. connecting/disconnecting headphones).
     */
    private void updateAvailableMixers() {
        availableMixers.set(Arrays.asList(audioType.findAvailableMixers()));
    }

    /**
     * @return Whether the AudioFormat within {@param audioFormatEither} is supported on the Mixer {@param mixerInfo}.
     *         If {@param audioFormatEither} does not contain a valid AudioFormat, or if a valid Mixer could not be
     *         found for {@param mixerInfo}, then false will be returned.
     */
    private boolean isAudioFormatSupported(Mixer.Info mixerInfo, Either<AudioFormat, String> audioFormatEither) {
        if (audioFormatEither.isRight())
            return false;

        AudioFormat format = audioFormatEither.getLeft();
        if (mixerInfo == null || format == null)
            return false;

        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        if (mixer == null)
            return false;

        return audioType.findAvailableLines(mixer, format).length > 0;
    }

    /**
     * Contains logic for dealing with audio sources or sinks.
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
}
