package thesiscode.common.flow;

import thesiscode.common.tree.ISharedTree;

public interface ISharedTreeFlowPusher extends ITreeFlowPusher {
    void pushTree(ISharedTree tree);
}
