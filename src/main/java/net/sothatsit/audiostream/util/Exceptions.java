package net.sothatsit.audiostream.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Useful exceptions and methods relating to exceptions.
 *
 * @author Paddy Lamont
 */
public class Exceptions {

    /**
     * Print {@param exception} into a String, and return it.
     *
     * @param exception the exception to be printed
     * @return the printed exception
     */
    public static String exceptionToString(Exception exception) {
        StringWriter errorWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(errorWriter);
        exception.printStackTrace(printWriter);
        printWriter.flush();
        errorWriter.flush();

        return errorWriter.toString();
    }
}
