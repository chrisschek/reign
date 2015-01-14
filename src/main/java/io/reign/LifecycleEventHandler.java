package io.reign;

/**
 *
 * @author ypai
 *
 */
public interface LifecycleEventHandler {

    /**
     * Invoked right before Reign is started.
     */
    public void starting();

    /**
     * Invoked after Reign has initialized.
     */
    public void started(ReignContext context);

    /**
     * Invoked right before Reign is shut down.
     */
    public void stopping(ReignContext context);

    /**
     * Invoked after Reign is stopped.
     */
    public void stopped();

}
