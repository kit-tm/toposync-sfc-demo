package thesiscode.common.flow;

import org.onosproject.net.Link;
import thesiscode.common.group.WrappedHost;

import java.util.Set;

public interface IPathFlowPusher extends IFlowPusher {
    void pushPath(WrappedHost source, Set<Link> path, WrappedHost destination);
}
