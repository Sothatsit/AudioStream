package net.sothatsit.audiostream.util;

import com.apple.eawt.AppReOpenedListener;
import com.apple.eawt.Application;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.*;

/**
 * A class to hold any Apple specific functionality.
 *
 * @author Paddy Lamont
 */
public class Apple {

    private static final ScriptEngineManager engineManager;
    private static final ScriptEngine appleScriptEngine;
    static {
        engineManager = new ScriptEngineManager();

        // Can have different names on different systems
        ScriptEngine possibleEngine = engineManager.getEngineByName("AppleScript");
        if (possibleEngine == null) {
            possibleEngine = engineManager.getEngineByName("AppleScriptEngine");
        }

        appleScriptEngine = possibleEngine;
    }

    public static boolean isAppleScriptAvailable() {
        return appleScriptEngine != null;
    }

    public static String invokeAppleScript(String script) {
        if (script == null)
            throw new IllegalArgumentException("script cannot be null");
        if (appleScriptEngine == null)
            throw new IllegalStateException("AppleScript is not available on the current system");

        try {
            return (String) appleScriptEngine.eval(script);
        } catch (ScriptException e) {
            throw new RuntimeException("Exception while invoking the AppleScript: \"" + script + "\"", e);
        }
    }

    private static boolean isApplicationAvailable() {
        try {
            Class.forName("com.apple.eawt.Application");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isDockListenerAvailable() {
        return isApplicationAvailable();
    }

    public static RemovableListener addDockListener(Runnable listener) {
        AppReOpenedListener dockListener = event -> listener.run();
        AppleAppEventListener appleListener = new AppleAppEventListener(dockListener);

        appleListener.add();

        return appleListener;
    }

    public static boolean isDockIconAvailable() {
        return isApplicationAvailable();
    }

    public static void setDockIcon(Image image) {
        Application application = Application.getApplication();
        application.setDockIconImage(image);
    }
}
