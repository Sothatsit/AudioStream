package net.sothatsit.audiostream.util;

import com.apple.eawt.AppEventListener;
import com.apple.eawt.Application;

/**
 * Allows the tracking of an Apple ApplicationListener.
 *
 * @author Paddy Lamont
 */
public class AppleAppEventListener implements RemovableListener {

    private final AppEventListener listener;

    public AppleAppEventListener(AppEventListener listener) {
        this.listener = listener;
    }

    public void add() {
        Application application = Application.getApplication();
        application.addAppEventListener(listener);
    }

    @Override
    public void remove() {
        Application application = Application.getApplication();
        application.removeAppEventListener(listener);
    }
}
