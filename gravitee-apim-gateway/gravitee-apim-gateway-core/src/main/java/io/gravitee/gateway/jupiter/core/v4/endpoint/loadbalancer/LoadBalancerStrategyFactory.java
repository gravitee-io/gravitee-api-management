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

import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType;
import io.gravitee.gateway.jupiter.core.v4.endpoint.ManagedEndpoint;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoadBalancerStrategyFactory {

    public static LoadBalancerStrategy create(LoadBalancerType type, List<ManagedEndpoint> endpoints) {
        switch (type) {
            case RANDOM:
                return new RandomLoadBalancer(endpoints);
            case WEIGHTED_ROUND_ROBIN:
                return new WeightedRoundRobinLoadBalancer(endpoints);
            case WEIGHTED_RANDOM:
                return new WeightedRandomLoadBalancer(endpoints);
            default:
            case ROUND_ROBIN:
                return new RoundRobinLoadBalancer(endpoints);
        }
    }
}
