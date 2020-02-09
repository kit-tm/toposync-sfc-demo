package thesiscode.common.flow;

import thesiscode.common.tree.IPerSourceTree;

public interface IPerSourceTreeFlowPusher extends ITreeFlowPusher {
    void pushTree(IPerSourceTree tree);
}
