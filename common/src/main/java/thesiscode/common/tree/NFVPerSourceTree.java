package thesiscode.common.tree;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.slf4j.LoggerFactory;
import thesiscode.common.group.AbstractMulticastGroup;
import thesiscode.common.group.IGroupMember;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NFVPerSourceTree implements IPerLogicalEdgeTree {
    private IGroupMember src;
    private Set<IGroupMember> dsts;
    private List<Set<Link>> linksPerLogical;
    private List<Set<ConnectPoint>> vnfConnectPoints;
    private AbstractMulticastGroup group;
    private org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());

    public NFVPerSourceTree(IGroupMember source, List<Set<Link>> links, Set<IGroupMember> receivers, List<Set<ConnectPoint>> vnfConnectPoints, AbstractMulticastGroup group) {
        this.vnfConnectPoints = vnfConnectPoints;
        this.src = source;
        this.dsts = receivers;
        this.linksPerLogical = links;
        this.group = group;
    }


    public List<Set<ConnectPoint>> getVnfConnectPoints() {
        return vnfConnectPoints;
    }


    @Override
    public IGroupMember getSource() {
        return src;
    }

    @Override
    public List<Set<Link>> getLinks() {
        return linksPerLogical;
    }

    @Override
    public TrafficSelector getSelector() {
        TrafficSelector.Builder selBuilder = DefaultTrafficSelector.builder();

        Set<Criterion> treeCriterionSet = new HashSet<>();

        // match group
        if (group != null) {
            treeCriterionSet.addAll(group.getMatchCriterionSet());
        } else {
            log.warn("group of tree is null");
        }

        // match source
        treeCriterionSet.addAll(src.getMatchCriterionSet(true));

        // add criterions to builder
        for (Criterion criterion : treeCriterionSet) {
            selBuilder.add(criterion);
        }

        // build
        return selBuilder.build();
    }

    @Override
    public Set<IGroupMember> getReceivers() {
        return dsts;
    }
}
