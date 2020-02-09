package thesiscode.common.tree;

import thesiscode.common.group.IGroupMember;

import java.util.Set;

/**
 * Abstraction of an algorithm, which calculates a per-source tree.
 */
public abstract class AbstractPerSourceTreeAlgorithm extends AbstractTreeAlgorithm {
    abstract IPerSourceTree computeTree(IGroupMember source, Set<IGroupMember> receivers);
}
