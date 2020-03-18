package thesiscode.common.topo;

import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.TopologyVertex;

public class WrappedVertex implements TopologyVertex {
    private TopologyVertex wrapped = null;


    public WrappedVertex(TopologyVertex wrapped) {
        this.wrapped = wrapped;
    }


    @Override
    public DeviceId deviceId() {
        return wrapped.deviceId();
    }


    @Override
    public String toString() {
        return wrapped.toString();
    }

}
