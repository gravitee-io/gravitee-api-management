/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.core.v4.endpoint.loadbalancer;

import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import java.util.List;
import java.util.Random;

/**
 * This loadbalancer will randomly choose one endpoint based on total remaining weight and verify that its remaining weight is higher than 0, otherwise it will pick the next one.
 * After a selection is made, the total remaining weight is decreased.
 *
 * For example, if you have the following two endpoints:
 * <ul>
 * <li>Endpoint 1 with a weight of 9</li>
 * <li>Endpoint 2 with a weight of 1</li>
 * </ul>
 * Endpoint 1 is selected 9 times out of 10, whereas Endpoint 2 is selected only 1 time out of 10.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WeightedRandomLoadBalancer extends WeightedLoadBalancer {

    private static final Random RANDOM = new Random(); // NOSONAR the random value is not used for security purposes here

    public WeightedRandomLoadBalancer(final List<ManagedEndpoint> endpoints) {
        super(endpoints);
        refresh();
    }

    @Override
    protected synchronized ManagedEndpoint getManagedEndpoint() {
        if (endpoints.size() != weightDistributions.get().getDistributions().size()) {
            refresh();
        }

        int index = computeNextIndex();
        return endpoints.get(index);
    }

    private int computeNextIndex() {
        WeightDistributions currentWeightDistributions = weightDistributions.get();
        currentWeightDistributions.tryResetRemaining();

        WeightDistributions.WeightDistribution selected = new WeightDistributions.WeightDistribution(0, 0);
        int randomWeight = RANDOM.nextInt(currentWeightDistributions.getRemainingSum());
        int choiceWeight = 0;
        for (WeightDistributions.WeightDistribution distributionRatio : currentWeightDistributions.getDistributions()) {
            choiceWeight += distributionRatio.getRemaining();
            if (randomWeight < choiceWeight) {
                selected = distributionRatio;
                break;
            }
        }
        selected.setRemaining(selected.getRemaining() - 1);
        currentWeightDistributions.decreaseRemaining();
        return selected.getPosition();
    }
}
