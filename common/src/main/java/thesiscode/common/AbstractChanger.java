package thesiscode.common;

import java.util.Set;

/**
 * "Subject" of the observer pattern. Changes things and notifies listeners about it.
 *
 * @param <L> the listener class whose instances are able to listen to this changer
 * @param <E> the event which is caused by this changer and which the listeners are notified about
 */
public abstract class AbstractChanger<L extends IListener<E>, E extends IEvent> {
    /**
     * Notifies all listeners about the event
     *
     * @param event the event to push to the listeners
     */
    public void notifyListeners(E event) {
        for (L l : getListeners()) {
            l.update(event);
        }
    }

    /**
     * Returns all listeners.
     *
     * @return the listeners
     */
    public abstract Set<L> getListeners();

    /**
     * Adds a listener to this changer.
     *
     * @param listener the listener to add
     */
    public abstract void addListener(L listener);

    /**
     * Removes a listener from this changer.
     *
     * @param listener the listener to remove
     */
    public abstract void removeListener(L listener);
}
