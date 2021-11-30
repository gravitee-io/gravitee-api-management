/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.loadbalancer;

import io.gravitee.gateway.api.endpoint.Endpoint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class WeightedLoadBalancer extends LoadBalancer {

    transient int lastIndex;

    private List<WeightRatio> runtimeRatios;

    WeightedLoadBalancer(Collection<Endpoint> endpoints) {
        super(endpoints);
    }

    protected void refresh() {
        this.loadRuntimeRatios();
    }

    private void loadRuntimeRatios() {
        if (runtimeRatios == null) {
            runtimeRatios = new ArrayList<>(endpoints.size());
        } else {
            runtimeRatios.clear();
        }

        int position = 0;

        for (Endpoint endpoint : endpoints) {
            runtimeRatios.add(new WeightRatio(position++, endpoint.weight()));
        }
    }

    boolean isRuntimeRatiosZeroed() {
        boolean cleared = true;

        for (WeightRatio runtimeRatio : runtimeRatios) {
            if (runtimeRatio.getRuntime() > 0) {
                cleared = false;
            }
        }
        return cleared;
    }

    void resetRuntimeRatios() {
        for (WeightRatio runtimeRatio : runtimeRatios) {
            runtimeRatio.setRuntime(runtimeRatio.getDistribution());
        }
    }

    List<WeightRatio> getRuntimeRatios() {
        return runtimeRatios;
    }

    @Override
    public boolean postAdd(Endpoint object) {
        super.postAdd(object);
        this.refresh();
        return false;
    }

    @Override
    public boolean postRemove(Endpoint object) {
        super.postRemove(object);
        this.refresh();
        return false;
    }
}
