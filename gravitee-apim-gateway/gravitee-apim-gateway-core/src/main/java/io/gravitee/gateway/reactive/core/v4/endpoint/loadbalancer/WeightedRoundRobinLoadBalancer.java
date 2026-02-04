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
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * This loadbalancer will choose one endpoint after another and verify that its remaining weight is higher than 0, otherwise it will pick the next one.
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
@Slf4j
public class WeightedRoundRobinLoadBalancer extends WeightedLoadBalancer {

    final AtomicInteger counter = new AtomicInteger(0);

    public WeightedRoundRobinLoadBalancer(final List<ManagedEndpoint> endpoints) {
        super(endpoints);
        refresh();
    }

    @Override
    public void refresh() {
        super.refresh();
        counter.set(0);
    }

    @Override
    protected synchronized ManagedEndpoint getManagedEndpoint() {
        WeightDistributions currentWeightDistributions = weightDistributions.get();
        if (endpoints.size() != currentWeightDistributions.getDistributions().size()) {
            refresh();
            currentWeightDistributions = weightDistributions.get();
        }

        List<WeightDistributions.WeightDistribution> distributions = currentWeightDistributions.getDistributions();
        int maxAttempts = distributions.size() + 1;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (currentWeightDistributions.tryResetRemaining()) {
                counter.set(0);
            }
            int position = Math.floorMod(counter.getAndIncrement(), distributions.size());
            WeightDistributions.WeightDistribution weightDistribution = distributions.get(position);
            if (weightDistribution.getRemaining() > 0) {
                weightDistribution.setRemaining(weightDistribution.getRemaining() - 1);
                currentWeightDistributions.decreaseRemaining();
                return endpoints.get(weightDistribution.getPosition());
            }
        }

        // Safety fallback: force reset all distributions and return first endpoint
        log.warn("Weighted round-robin load balancer exceeded max attempts, forcing distribution reset");
        distributions.forEach(WeightDistributions.WeightDistribution::reset);
        currentWeightDistributions.setRemainingSum(currentWeightDistributions.getWeightSum());
        counter.set(0);

        WeightDistributions.WeightDistribution first = distributions.get(0);
        first.setRemaining(first.getRemaining() - 1);
        currentWeightDistributions.decreaseRemaining();
        return endpoints.get(first.getPosition());
    }
}
