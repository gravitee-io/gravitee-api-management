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
package io.gravitee.gateway.http.core.loadbalancer;

import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.api.Request;

import java.util.List;
import java.util.Random;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class WeightedRandomLoadBalancerStrategy extends WeightedLoadBalancerStrategy {

    private final Random rnd = new Random();
    private final int distributionRatioSum;
    private int runtimeRatioSum;

    public WeightedRandomLoadBalancerStrategy(List<Endpoint> endpoints) {
        super(endpoints);
        int sum = 0;
        for (Endpoint endpoint : endpoints) {
            sum += endpoint.getWeight();
        }
        distributionRatioSum = sum;
        runtimeRatioSum = distributionRatioSum;
    }

    @Override
    public synchronized String chooseEndpoint(Request request) {
        List<Endpoint> endpoints = endpoints();
        if (endpoints.isEmpty()) {
            return null;
        }

        int index = selectProcessIndex();
        lastIndex = index;
        return endpoints.get(index).getTarget();
    }

    private int selectProcessIndex() {
        if (runtimeRatioSum == 0) {
            for (WeightRatio distributionRatio : getRuntimeRatios()) {
                int weight = distributionRatio.getDistribution();
                distributionRatio.setRuntime(weight);
            }
            runtimeRatioSum = distributionRatioSum;
        }

        WeightRatio selected = null;
        int randomWeight = rnd.nextInt(runtimeRatioSum);
        int choiceWeight = 0;
        for (WeightRatio distributionRatio : getRuntimeRatios()) {
            choiceWeight += distributionRatio.getRuntime();
            if (randomWeight < choiceWeight) {
                selected = distributionRatio;
                break;
            }
        }

        selected.setRuntime(selected.getRuntime() - 1);
        runtimeRatioSum--;

        return selected.getPosition();
    }

    @Override
    public String toString() {
        return "WeightedRandomLoadBalancer";
    }
}
