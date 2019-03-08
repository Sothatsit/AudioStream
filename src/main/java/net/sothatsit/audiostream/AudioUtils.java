package net.sothatsit.audiostream;

import javax.sound.sampled.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Some useful audio functions.
 *
 * @author Paddy Lamont
 */
public class AudioUtils {

    public static final Map<String, AudioFormat.Encoding> audioFormatEncodings = new HashMap<>();
    static {
        Class<AudioFormat.Encoding> formatClass = AudioFormat.Encoding.class;
        for (Field field : formatClass.getDeclaredFields()) {
            if (!formatClass.equals(field.getType()) || !Modifier.isStatic(field.getModifiers()))
                continue;

            AudioFormat.Encoding encoding;
            try {
                field.setAccessible(true);
                encoding = (AudioFormat.Encoding) field.get(null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            audioFormatEncodings.put(encoding.toString(), encoding);
        }
    }

    public static String getAudioFormatEncodingHumanString(AudioFormat.Encoding encoding) {
        if (encoding == AudioFormat.Encoding.PCM_SIGNED)
            return "Signed";
        if (encoding == AudioFormat.Encoding.PCM_UNSIGNED)
            return "Unsigned";
        if (encoding == AudioFormat.Encoding.PCM_FLOAT)
            return "Floating Point";
        if (encoding == AudioFormat.Encoding.ULAW)
            return "U-Law";
        if (encoding == AudioFormat.Encoding.ALAW)
            return "A-Law";
        return encoding.toString();
    }

    public static String getAudioFormatChannelsHumanString(int channels) {
        if (channels == 1)
            return "Mono";
        if (channels == 2)
            return "Stereo";
        if (channels == AudioSystem.NOT_SPECIFIED)
            return "Unknown Channel";
        return channels + " Channel";
    }

    public static Mixer.Info getMixer(String name) {
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(name))
                return info;
        }
        return null;
    }

    public static Mixer.Info[] getOutputMixers() {
        List<Mixer.Info> mixers = new ArrayList<>();

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            if (getOutputFormats(mixerInfo).length == 0)
                continue;

            mixers.add(mixerInfo);
        }

        return mixers.toArray(new Mixer.Info[mixers.size()]);
    }

    public static AudioFormat[] getOutputFormats(Mixer.Info mixerInfo) {
        Mixer mixer = AudioSystem.getMixer(mixerInfo);

        List<AudioFormat> formats = new ArrayList<>();
        for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
            if (!(lineInfo instanceof DataLine.Info))
                continue;

            AudioFormat[] lineFormats = ((DataLine.Info) lineInfo).getFormats();
            formats.addAll(Arrays.asList(lineFormats));
        }

        return formats.toArray(new AudioFormat[formats.size()]);
    }

    public static Mixer.Info[] getInputMixers() {
        List<Mixer.Info> mixers = new ArrayList<>();

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            if (getInputFormats(mixerInfo).length == 0)
                continue;

            mixers.add(mixerInfo);
        }

        return mixers.toArray(new Mixer.Info[mixers.size()]);
    }

    public static AudioFormat[] getInputFormats(Mixer.Info mixerInfo) {
        Mixer mixer = AudioSystem.getMixer(mixerInfo);

        List<AudioFormat> formats = new ArrayList<>();
        for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
            if (!(lineInfo instanceof DataLine.Info))
                continue;

            AudioFormat[] lineFormats = ((DataLine.Info) lineInfo).getFormats();
            formats.addAll(Arrays.asList(lineFormats));
        }

        return formats.toArray(new AudioFormat[formats.size()]);
    }

    public static void displayMixerInfo() {
        Mixer.Info [] mixersInfo = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixersInfo)
        {
            System.out.println("Mixer: " + mixerInfo.getName());

            Mixer mixer = AudioSystem.getMixer(mixerInfo);

            Line.Info [] sourceLineInfo = mixer.getSourceLineInfo();
            for (Line.Info info : sourceLineInfo) {
                System.out.println("  source");
                showLineInfo(info);
            }

            Line.Info [] targetLineInfo = mixer.getTargetLineInfo();
            for (Line.Info info : targetLineInfo) {
                System.out.println("  target");
                showLineInfo(info);
            }
        }
    }

    private static void showLineInfo(final Line.Info lineInfo) {
        System.out.println("  " + lineInfo.toString());

        if (lineInfo instanceof DataLine.Info)
        {
            DataLine.Info dataLineInfo = (DataLine.Info)lineInfo;

            AudioFormat[] formats = dataLineInfo.getFormats();
            for (final AudioFormat format : formats)
                System.out.println("    " + format.toString());
        }
    }
}
