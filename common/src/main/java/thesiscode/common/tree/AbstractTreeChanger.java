package thesiscode.common.tree;


import thesiscode.common.AbstractChanger;

/**
 * Abstraction of a tree changer. Causes {@link TreeChangeEvent}s and notifies {@link ITreeChangeListener}s.
 */
public abstract class AbstractTreeChanger extends AbstractChanger<ITreeChangeListener, TreeChangeEvent> {

}
