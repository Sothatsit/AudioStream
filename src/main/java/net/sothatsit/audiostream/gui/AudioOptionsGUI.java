package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioUtils;
import net.sothatsit.audiostream.util.Exceptions;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The GUI to select an audio mixer and an audio format.
 *
 * @author Paddy Lamont
 */
public class AudioOptionsGUI extends JPanel {

    private final AudioType audioType;
    private final List<JComponent> settingComponents;

    private final JLabel mixerLabel;
    private final JLabel audioFormatLabel;
    private final GuiUtils.WrappedComboBox<Mixer.Info> mixerCombo;
    private final GuiUtils.WrappedComboBox<AudioFormat.Encoding> encodingCombo;
    private final GuiUtils.WrappedComboBox<Boolean> bigEndianCombo;
    private final GuiUtils.WrappedComboBox<Float> sampleRateCombo;
    private final GuiUtils.WrappedComboBox<Integer> sampleSizeCombo;
    private final GuiUtils.WrappedComboBox<Integer> channelsCombo;

    public AudioOptionsGUI(AudioType audioType) {
        this.audioType = audioType;
        this.settingComponents = new ArrayList<>();

        setLayout(new GridBagLayout());

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.HORIZONTAL)
                .insets(5, 5, 5, 5);

        { // Mixer selection
            Mixer.Info[] mixerOptions;
            switch (audioType) {
                case INPUT:
                    mixerOptions = AudioUtils.getInputMixers();
                    break;
                case OUTPUT:
                    mixerOptions = AudioUtils.getOutputMixers();
                    break;
                default:
                    throw new IllegalStateException("Unknown AudioType " + audioType);
            }
            mixerLabel = new JLabel("Audio " + audioType);
            mixerCombo = GuiUtils.createComboBox(mixerOptions, Mixer.Info::getName);

            add(mixerLabel, constraints.build());
            add(mixerCombo, constraints.weightX(1.0).build(4));
            settingComponents.add(mixerCombo);
            constraints.nextRow();
        }

        { // Audio format selection
            encodingCombo = GuiUtils.createComboBox(
                    new AudioFormat.Encoding[] {
                            AudioFormat.Encoding.PCM_SIGNED,
                            AudioFormat.Encoding.PCM_UNSIGNED
                    },
                    AudioFormat.Encoding.PCM_SIGNED,
                    encoding -> {
                        if (encoding == AudioFormat.Encoding.PCM_SIGNED)
                            return "PCM Signed";
                        if (encoding == AudioFormat.Encoding.PCM_UNSIGNED)
                            return "PCM Unsigned";
                        if (encoding == AudioFormat.Encoding.PCM_FLOAT)
                            return "PCM Floating Point";
                        if (encoding == AudioFormat.Encoding.ULAW)
                            return "U-Law";
                        if (encoding == AudioFormat.Encoding.ALAW)
                            return "A-Law";
                        return encoding.toString();
                    }
            );

            bigEndianCombo = GuiUtils.createComboBox(
                    new Boolean[] {true, false}, true,
                    value -> (value ? "Big Endian" : "Little Endian")
            );

            sampleRateCombo = GuiUtils.createComboBox(
                    new Float[] {8000f, 44100f, 48000f}, 48000f,
                    value -> value + " Hz"
            );

            sampleSizeCombo = GuiUtils.createComboBox(
                    new Integer[] {8, 16, 24}, 24,
                    value -> value + " bit"
            );

            channelsCombo = GuiUtils.createComboBox(
                    new Integer[] {1, 2, 64}, 2,
                    value -> value + " channels"
            );

            settingComponents.addAll(Arrays.asList(
                    encodingCombo, bigEndianCombo,
                    sampleRateCombo, sampleSizeCombo,
                    channelsCombo
            ));

            audioFormatLabel = new JLabel("Audio Format");
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

        // Start update loop
        new Timer(100, event -> update()).start();
    }

    private void add(String string, GridBagConstraints constraints) {
        add(new JLabel(string), constraints);
    }

    private void add(String string, Color foreground, GridBagConstraints constraints) {
        JLabel label = new JLabel(string);
        label.setForeground(foreground);
        add(label, constraints);
    }

    public void update() {
        if (getSelectedMixer() != null) {
            mixerLabel.setForeground(Color.BLACK);
        } else {
            mixerLabel.setForeground(Color.RED);
        }

        if (isSelectedAudioFormatSupported()) {
            audioFormatLabel.setForeground(Color.BLACK);
        } else {
            audioFormatLabel.setForeground(Color.RED);
        }

        Mixer.Info[] mixerOptions;
        switch (audioType) {
            case INPUT:
                mixerOptions = AudioUtils.getInputMixers();
                break;
            case OUTPUT:
                mixerOptions = AudioUtils.getOutputMixers();
                break;
            default:
                throw new IllegalStateException("Unknown AudioType " + audioType);
        }
        mixerCombo.setAvailableValues(mixerOptions);
    }

    public Mixer.Info getSelectedMixer() {
        return mixerCombo.getSelectedValue();
    }

    public AudioFormat getSelectedAudioFormat() {
        AudioFormat.Encoding encoding = encodingCombo.getSelectedValue();
        if (encoding == null)
            throw new Exceptions.ValidationException("Please specify the Audio Encoding");

        Boolean bigEndian = bigEndianCombo.getSelectedValue();
        if (bigEndian == null)
            throw new Exceptions.ValidationException("Please specify the Audio Endianness");

        Float sampleRate = sampleRateCombo.getSelectedValue();
        if (sampleRate == null)
            throw new Exceptions.ValidationException("Please specify the Audio Sample Rate");

        Integer sampleSizeInBits = sampleSizeCombo.getSelectedValue();
        if (sampleSizeInBits == null)
            throw new Exceptions.ValidationException("Please specify the Audio Sample Size");

        Integer channels = channelsCombo.getSelectedValue();
        if (channels == null)
            throw new Exceptions.ValidationException("Please specify the Audio Channels");

        boolean signed = (encoding == AudioFormat.Encoding.PCM_SIGNED);
        if (!signed && encoding != AudioFormat.Encoding.PCM_UNSIGNED)
            throw new Exceptions.ValidationException("Unsupported Audio Encoding " + encoding);

        return new AudioFormat(
                sampleRate,
                sampleSizeInBits,
                channels,
                signed,
                bigEndian
        );
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        GuiUtils.setEnabledAll(settingComponents, enabled);
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

    public boolean isSelectedAudioFormatSupported() {
        Mixer.Info mixerInfo = getSelectedMixer();
        if (mixerInfo == null)
            return false;

        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        if (mixer == null)
            return false;

        AudioFormat format = getSelectedAudioFormat();
        if (format == null)
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
