package io.reign;

/**
 * 
 * @author ypai
 *
 */
public interface LifecycleEventHandler {

    /**
     * Invoked after Reign has initialized.
     */
    public void onStart(ReignContext context);

    /**
     * Invoked right before Reign is shut down.
     */
    public void onStop(ReignContext context);

}
