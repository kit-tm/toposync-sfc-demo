package thesiscode.common.nfv.traffic;

import java.util.HashMap;
import java.util.Map;

// TODO as classes?
public class NprNfvTypes {

    public enum Type {
        TRANSCODER,
        FIREWALL,
        INTRUSION_DETECTION
    }

    public static Map<NprResources, Integer> getRequirements(NprNfvTypes.Type type) {
        Map<NprResources, Integer> requirements = new HashMap<>();
        switch (type) {
            case TRANSCODER:
                requirements.put(NprResources.CPU_CORES, 4);
                requirements.put(NprResources.RAM_IN_GB, 2);
                break;
            case INTRUSION_DETECTION:
                requirements.put(NprResources.CPU_CORES, 6);
                requirements.put(NprResources.RAM_IN_GB, 1);
                break;
            case FIREWALL:
                requirements.put(NprResources.CPU_CORES, 4);
                requirements.put(NprResources.RAM_IN_GB, 1);
                break;

            default:
                throw new IllegalStateException();

        }
        return requirements;
    }

    public static int getBaseDelay(NprNfvTypes.Type type) {
        return 40;
    }

}
