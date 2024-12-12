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
package io.gravitee.gateway.core.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultManagedEndpoint;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultManagedEndpointGroup;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WeightedRoundRobinLoadBalancerTest {

    @Test
    void should_return_null_with_empty_endpoints() {
        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(List.of());
        Endpoint next = cut.next();
        assertThat(next).isNull();
    }

    @Test
    void should_return_endpoint_even_with_invalid_configured_weight() {
        List<Endpoint> endpoints = new ArrayList<>();
        Endpoint endpoint1 = endpoint(0);
        endpoints.add(endpoint1);
        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(endpoints);
        // 1
        Endpoint next = cut.next();
        assertThat(next).isEqualTo(endpoint1); // 1 > 0
    }

    @Test
    void should_return_endpoints_in_order() {
        List<Endpoint> endpoints = new ArrayList<>();
        Endpoint endpoint1 = endpoint(1);
        endpoints.add(endpoint1);
        Endpoint endpoint2 = endpoint(5);
        endpoints.add(endpoint2);
        Endpoint endpoint3 = endpoint(3);
        endpoints.add(endpoint3);

        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(endpoints);
        // 1
        Endpoint next = cut.next();
        assertThat(next).isEqualTo(endpoint1); // 1 > 0
        next = cut.next();
        assertThat(next).isEqualTo(endpoint2); // 5 > 4
        next = cut.next();
        assertThat(next).isEqualTo(endpoint3); // 3 > 2

        // 2
        next = cut.next();
        assertThat(next).isEqualTo(endpoint2); // 4 > 3
        next = cut.next();
        assertThat(next).isEqualTo(endpoint3); // 2 > 1

        // 3
        next = cut.next();
        assertThat(next).isEqualTo(endpoint2); // 3 > 2
        next = cut.next();
        assertThat(next).isEqualTo(endpoint3); // 1 > 0

        // 4
        next = cut.next();
        assertThat(next).isEqualTo(endpoint2); // 2 > 1
        next = cut.next();
        assertThat(next).isEqualTo(endpoint2); // 1 > 0

        // 5
        next = cut.next();
        assertThat(next).isEqualTo(endpoint1);
        next = cut.next();
        assertThat(next).isEqualTo(endpoint2);
        next = cut.next();
        assertThat(next).isEqualTo(endpoint3);
    }

    private static Endpoint endpoint(int weight) {
        Endpoint endpoint = mock(Endpoint.class, "endpoint with %s".formatted(weight));
        when(endpoint.weight()).thenReturn(weight);
        when(endpoint.primary()).thenReturn(true);
        return endpoint;
    }
}
