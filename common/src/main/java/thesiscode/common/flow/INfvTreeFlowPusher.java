package thesiscode.common.flow;

import thesiscode.common.tree.NFVPerSourceTree;

public interface INfvTreeFlowPusher extends IFlowPusher {
    void pushTree(NFVPerSourceTree tree);
}
