package thesiscode.common.group;

import thesiscode.common.AbstractChanger;

/**
 * Abstraction for classes which are causing {@link GroupChangeEvent} and notify {@link IGroupChangeListener}.
 */
public abstract class AbstractGroupChanger extends AbstractChanger<IGroupChangeListener, GroupChangeEvent> {

}
