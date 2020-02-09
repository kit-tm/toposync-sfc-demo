package thesiscode.common.util;

import thesiscode.common.nfv.traffic.NprTraffic;

import java.util.List;

public interface IDemandGenerator {
    List<NprTraffic> generateDemand();
}
