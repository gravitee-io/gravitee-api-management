package io.gravitee.gateway.core.http.loadbalancer;

import io.gravitee.definition.model.Api;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class WeightedLoadBalancer extends LoadBalancerSupport {

    transient int lastIndex;

    private List<Integer> distributionRatios = new ArrayList<>();
    private List<WeightRatio> runtimeRatios = new ArrayList<WeightRatio>();

    public WeightedLoadBalancer(Api api) {
        super(api);
        // TODO: how to get endpoint ratio ?
        loadRuntimeRatios(null);
    }

    protected void loadRuntimeRatios(List<Integer> distributionRatios) {
        int position = 0;

        for (Integer value : distributionRatios) {
            runtimeRatios.add(new WeightRatio(position++, value.intValue()));
        }
    }

    protected boolean isRuntimeRatiosZeroed() {
        boolean cleared = true;

        for (WeightRatio runtimeRatio : runtimeRatios) {
            if (runtimeRatio.getRuntime() > 0) {
                cleared = false;
            }
        }
        return cleared;
    }

    protected void resetRuntimeRatios() {
        for (WeightRatio runtimeRatio : runtimeRatios) {
            runtimeRatio.setRuntime(runtimeRatio.getDistribution());
        }
    }
}
