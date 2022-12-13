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
package io.gravitee.gateway.jupiter.core.v4.endpoint.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
public class WeightDistributions {

    private int weightSum;
    private int remainingSum;

    private List<WeightDistribution> distributions = new ArrayList<>();

    public WeightDistributions(final List<WeightDistribution> newDistributions) {
        this.distributions = newDistributions;
        int sum = 0;
        for (WeightDistribution weightDistribution : this.distributions) {
            sum += weightDistribution.getWeight();
        }
        weightSum = sum;
        remainingSum = weightSum;
    }

    public void decreaseRemaining() {
        remainingSum--;
    }

    public boolean tryResetRemaining() {
        if (remainingSum <= 0) {
            distributions.forEach(WeightDistribution::reset);
            remainingSum = weightSum;
            return true;
        }
        return false;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class WeightDistribution {

        private int position;
        private int weight;
        private int remaining;

        public WeightDistribution(final int position, final int weight) {
            this(position, weight, weight);
        }

        public void reset() {
            this.remaining = this.weight;
        }
    }
}
