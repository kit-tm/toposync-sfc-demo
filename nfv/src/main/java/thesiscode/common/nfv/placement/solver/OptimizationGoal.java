package thesiscode.common.nfv.placement.solver;

public enum OptimizationGoal {
    /**
     * load balancing goal. minimize max link utilization
     */
    LOAD_BALANCING,
    /**
     * load reduction goal. minimize absolute link utilization
     */
    LOAD_REDUCTION,
    /**
     * multi-objective goal: first LOAD_BALANCING, then LOAD_REDUCTION
     */
    BALANCE_THEN_REDUCTION,
    /**
     * multi-objective goal: first LOAD_REDUCTION, then LOAD_BALANCING
     */
    REDUCTION_THEN_BALANCE,
    /**
     * minimize delay by minimizing sum_{t in T} sum_{d in D} delay(d)
     */
    DELAY_REDUCTION_PER_DST_SUM,
    /**
     * with sum of maxDelay-minDelay as lower-prioritized objective
     */
    DELAY_REDUCTION_PER_DST_SUM_MULTI,
    /**
     * multi-objective:
     * 1.minimize sum of max deviations per flow
     * 2.minimize sum of delay deviations
     */
    MIN_MAX_DELAYSUM_THEN_DEVIATION,
    /**
     * shortest path tree (shortest delay tree)
     */
    SPT
}
