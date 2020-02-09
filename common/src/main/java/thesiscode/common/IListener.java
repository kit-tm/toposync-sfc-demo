package thesiscode.common;

/**
 * An interface for listeners.
 *
 * @param <E> the class the listener will be notified about
 */
public interface IListener<E extends IEvent> {
    /**
     * Updates the listener with the event.
     *
     * @param e the event the listener learns about
     */
    void update(E e);
}
