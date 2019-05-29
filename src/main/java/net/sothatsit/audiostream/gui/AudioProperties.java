package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.AudioStream;
import net.sothatsit.audiostream.AudioUtils;
import net.sothatsit.audiostream.config.SerializableMap;
import net.sothatsit.property.MappedProperty;
import net.sothatsit.property.NonNullProperty;
import net.sothatsit.property.Property;
import net.sothatsit.property.Either;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;

/**
 * Properties that describe an audio source or sink.
 *
 * @author Paddy Lamont
 */
public class AudioProperties extends SerializableMap {

    public final Property<Mixer.Info> mixer;
    public final NonNullProperty<AudioFormat.Encoding> encoding;
    public final NonNullProperty<Boolean> isBigEndian;
    public final NonNullProperty<Float> sampleRate;
    public final NonNullProperty<Integer> sampleSize;
    public final NonNullProperty<Integer> channels;
    public final NonNullProperty<Integer> bufferSize;

    public final Property<Either<AudioFormat, String>> audioFormat;
    public final Property<Boolean> isValidAudioFormat;

    public AudioProperties() {
        this.mixer = createProperty("mixer", null, AudioUtils.MIXER_SERIALIZER);
        this.encoding = createNonNullProperty(
                "encoding",
                AudioFormat.Encoding.PCM_SIGNED,
                AudioUtils.AUDIO_FORMAT_ENCODING_SERIALIZER
        );
        this.isBigEndian = createNonNullProperty("isBigEndian", true);
        this.sampleRate = createNonNullProperty("sampleRate", 48000f);
        this.sampleSize = createNonNullProperty("sampleSize", 24);
        this.channels = createNonNullProperty("channels", 2);
        this.bufferSize = createNonNullProperty("bufferSize", AudioStream.DEFAULT_BUFFER_SIZE);

        this.audioFormat = MappedProperty.mapMany(
                "audioFormat", this::generateAudioFormat,
                mixer, encoding, isBigEndian, sampleRate, sampleSize, channels
        );
        this.isValidAudioFormat = audioFormat.map("isValidAudioFormat", Either::isLeft);
    }

    private Either<AudioFormat, String> generateAudioFormat() {
        AudioFormat.Encoding encoding = this.encoding.get();
        Boolean isBigEndian = this.isBigEndian.get();
        Float sampleRate = this.sampleRate.get();
        Integer sampleSize = this.sampleSize.get();
        Integer channels = this.channels.get();

        if (encoding == null)
            return Either.right("Please select the Audio Encoding");
        if (isBigEndian == null)
            return Either.right("Please select whether Audio is Big Endian");
        if (sampleRate == null)
            return Either.right("Please select the Audio Sample Rate");
        if (sampleSize == null)
            return Either.right("Please select the Audio Sample Size");
        if (channels == null)
            return Either.right("Please select the Audio Channel Count");


        boolean signed = (encoding == AudioFormat.Encoding.PCM_SIGNED);
        if (!signed && encoding != AudioFormat.Encoding.PCM_UNSIGNED)
            return Either.right("Unsupported Audio Encoding " + encoding);

        return Either.left(new AudioFormat(
                sampleRate,
                sampleSize,
                channels,
                signed,
                isBigEndian
        ));
    }
}
