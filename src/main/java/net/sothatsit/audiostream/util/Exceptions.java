package net.sothatsit.audiostream.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

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

    /**
     * Close many resources, ensuring that all close methods are called even if some of them throw Exceptions.
     */
    public static void closeMany(AutoCloseable... closeables) throws Exception {
        closeMany(Arrays.asList(closeables));
    }

    /**
     * Close many resources, ensuring that all close methods are called even if some of them throw Exceptions.
     */
    public static void closeMany(List<AutoCloseable> closeables) throws Exception {
        Exception thrown = null;
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception exception) {
                if (thrown == null) {
                    thrown = exception;
                } else {
                    thrown.addSuppressed(exception);
                }
            }
        }

        if (thrown != null) {
            throw thrown;
        }
    }

    /**
     * Close many resources, ensuring that all close methods are called even if some of them throw IOExceptions.
     */
    public static void closeManyIO(AutoCloseable... closeables) throws IOException {
        closeManyIO(Arrays.asList(closeables));
    }

    /**
     * Close many resources, ensuring that all close methods are called even if some of them throw IOExceptions.
     */
    public static void closeManyIO(List<AutoCloseable> closeables) throws IOException {
        boolean throwRuntime = false;
        IOException thrownIO = null;
        RuntimeException thrownRuntime = null;

        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch(IOException exception) {
                // If we catch an IOException, we want to update the cases for throwing a Runtime or IO Exception.

                if (thrownIO == null) {
                    thrownIO = exception;
                } else {
                    thrownIO.addSuppressed(exception);
                }

                if (thrownRuntime == null) {
                    thrownRuntime = new RuntimeException(exception);
                } else {
                    thrownRuntime.addSuppressed(exception);
                }
            } catch (Exception exception) {
                //
                if (thrownRuntime == null) {
                    throwRuntime = true;
                    thrownRuntime = new RuntimeException(exception);
                } else {
                    thrownRuntime.addSuppressed(exception);
                }
            }
        }

        // If there have been no exceptions
        if (thrownRuntime == null)
            return;

        // Throw the relevant exception
        if (throwRuntime) {
            throw thrownRuntime;
        } else {
            throw thrownIO;
        }
    }
}
