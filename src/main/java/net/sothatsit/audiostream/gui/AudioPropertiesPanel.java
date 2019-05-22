package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioUtils;
import net.sothatsit.audiostream.gui.util.*;
import net.sothatsit.audiostream.property.ListProperty;
import net.sothatsit.audiostream.property.Property;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * The GUI to select an audio mixer and an audio format.
 *
 * @author Paddy Lamont
 */
public class AudioPropertiesPanel extends PropertyPanel {

    private final AudioType audioType;
    private final AudioProperties properties;
    private final Property<Boolean> isSupportedAudioFormat;

    private final ListProperty<Mixer.Info> availableMixers;
    private final PropertyComboBox<Mixer.Info> mixerCombo;
    private final PropertyComboBox<AudioFormat.Encoding> encodingCombo;
    private final PropertyComboBox<Boolean> bigEndianCombo;
    private final PropertyComboBox<Float> sampleRateCombo;
    private final PropertyComboBox<Integer> sampleSizeCombo;
    private final PropertyComboBox<Integer> channelsCombo;

    public AudioPropertiesPanel(AudioType audioType, AudioProperties properties) {
        this(audioType, properties, true);
    }

    public AudioPropertiesPanel(AudioType audioType, AudioProperties properties, boolean showFormatSelectors) {
        super(new JPanel());

        this.audioType = audioType;
        this.properties = properties;
        this.isSupportedAudioFormat = Property.map(
                "isSupportedAudioFormat",
                properties.mixer, properties.audioFormat,
                (mixerInfo, either) -> {
                    if (either.isRight())
                        return false;

                    AudioFormat format = either.getLeft();
                    return isAudioFormatSupported(audioType, mixerInfo, format);
                }
        );

        setLayout(new GridBagLayout());

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.HORIZONTAL)
                .insets(5, 5, 5, 5);

        { // Mixer selection
            this.availableMixers = new ListProperty<>("availableMixers", findMixers(audioType));
            properties.mixer.compareAndSet(null, availableMixers.get(0));

            mixerCombo = new PropertyComboBox<>(
                    availableMixers, properties.mixer, Mixer.Info::getName
            );

            add("Mixer", constraints.build());
            add(mixerCombo, constraints.weightX(1.0).build(4));
            constraints.nextRow();
        }

        { // Audio format selection
            encodingCombo = new PropertyComboBox<>(
                    new AudioFormat.Encoding[] {
                            AudioFormat.Encoding.PCM_SIGNED,
                            AudioFormat.Encoding.PCM_UNSIGNED
                    },
                    properties.encoding,
                    AudioUtils::getAudioFormatEncodingHumanString
            );

            bigEndianCombo = new PropertyComboBox<>(
                    new Boolean[] {true, false},
                    properties.isBigEndian,
                    value -> (value ? "Big Endian" : "Little Endian")
            );

            sampleRateCombo = new PropertyComboBox<>(
                    new Float[] {8000f, 44100f, 48000f},
                    properties.sampleRate,
                    value -> value + " Hz"
            );

            sampleSizeCombo = new PropertyComboBox<>(
                    new Integer[] {8, 16, 24},
                    properties.sampleSize,
                    value -> value + " bit"
            );

            channelsCombo = new PropertyComboBox<>(
                    new Integer[] {1, 2, 64},
                    properties.channels,
                    value -> value + " channels"
            );

            if (showFormatSelectors) {
                PropertyLabel audioFormatLabel = new PropertyLabel("Audio Format");
                audioFormatLabel.setForeground(
                        Property.ifCond("audioFormatLabelFgColor", isSupportedAudioFormat, Color.BLACK, Color.RED)
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
        }

        new Timer(1000, event -> updateAvailableMixers()).start();
    }

    /**
     * Allows program to pick up on mixers as they are added or removed (e.g. connecting/disconnecting headphones).
     */
    private void updateAvailableMixers() {
        availableMixers.set(Arrays.asList(findMixers(audioType)));
    }

    private void add(String string, GridBagConstraints constraints) {
        add(new JLabel(string), constraints);
    }

    private void add(String string, Color foreground, GridBagConstraints constraints) {
        JLabel label = new JLabel(string);
        label.setForeground(foreground);
        add(label, constraints);
    }

    public void setModificationEnabled(Property<Boolean> enabled) {
        mixerCombo.setEnabled(enabled);
        encodingCombo.setEnabled(enabled);
        bigEndianCombo.setEnabled(enabled);
        sampleRateCombo.setEnabled(enabled);
        sampleSizeCombo.setEnabled(enabled);
        channelsCombo.setEnabled(enabled);
    }

    public enum AudioType {
        INPUT("Input"),
        OUTPUT("Output");

        private final String name;

        AudioType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static Mixer.Info[] findMixers(AudioType audioType) {
        switch (audioType) {
            case INPUT:
                return AudioUtils.getInputMixers();
            case OUTPUT:
                return AudioUtils.getOutputMixers();
            default:
                throw new IllegalStateException("Unknown AudioType " + audioType);
        }
    }

    private static boolean isAudioFormatSupported(AudioType audioType, Mixer.Info mixerInfo, AudioFormat format) {
        if (mixerInfo == null || format == null)
            return false;

        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        if (mixer == null)
            return false;

        Line.Info[] availableLines;
        switch (audioType) {
            case INPUT:
                DataLine.Info target = new DataLine.Info(TargetDataLine.class, format);
                availableLines = mixer.getTargetLineInfo(target);
                break;
            case OUTPUT:
                DataLine.Info source = new DataLine.Info(SourceDataLine.class, format);
                availableLines = mixer.getSourceLineInfo(source);
                break;
            default:
                throw new IllegalStateException("Unknown AudioType " + audioType);
        }

        return availableLines.length > 0;
    }
}
