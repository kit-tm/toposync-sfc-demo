package thesiscode.common.tree;

import org.onosproject.net.Link;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.group.AbstractMulticastGroup;
import thesiscode.common.group.IGroupMember;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple implementation of a per-source tree.
 */
public class DefaultPerSourceTree implements IPerSourceTree {
    private Logger log = LoggerFactory.getLogger(getClass());

    private Set<Link> links;
    private IGroupMember source;
    private Set<IGroupMember> receivers;
    private AbstractMulticastGroup group;


    public DefaultPerSourceTree(IGroupMember source, Set<Link> links, Set<IGroupMember> receivers) {
        this.links = links;
        this.source = source;
        this.receivers = receivers;
    }

    @Override
    public Set<Link> getLinks() {
        return links;
    }

    @Override
    public Set<IGroupMember> getReceivers() {
        return receivers;
    }

    @Override
    public TrafficSelector getSelector() {
        TrafficSelector.Builder selBuilder = DefaultTrafficSelector.builder();

        Set<Criterion> treeCriterionSet = new HashSet<>();

        // match group
        if (group != null) {
            treeCriterionSet.addAll(group.getMatchCriterionSet());
        } else {
            log.warn("group of tree with links {} is null", links);
        }

        // match source
        treeCriterionSet.addAll(source.getMatchCriterionSet(true));

        // add criterions to builder
        for (Criterion criterion : treeCriterionSet) {
            selBuilder.add(criterion);
        }

        // build
        return selBuilder.build();
    }


    @Override
    public void setGroup(AbstractMulticastGroup associatedGroup) {
        this.group = associatedGroup;
    }

    @Override
    public IGroupMember getSource() {
        return source;
    }

    public AbstractMulticastGroup getGroup() {
        return group;
    }
}
