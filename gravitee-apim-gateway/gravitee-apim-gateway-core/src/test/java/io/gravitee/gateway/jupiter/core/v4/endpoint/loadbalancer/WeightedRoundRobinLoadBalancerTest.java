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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.core.v4.endpoint.ManagedEndpoint;
import io.gravitee.gateway.jupiter.core.v4.endpoint.ManagedEndpointGroup;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class WeightedRoundRobinLoadBalancerTest {

    @Test
    void shouldReturnNullWithEmptyEndpoints() {
        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(List.of());
        ManagedEndpoint next = cut.next();
        assertThat(next).isNull();
    }

    @Test
    void shouldReturn2timesSameEndpointThenNextOne() {
        List<ManagedEndpoint> endpoints = new ArrayList<>();
        Endpoint endpoint1 = new Endpoint();
        endpoint1.setWeight(2);
        ManagedEndpoint managedEndpoint1 = new ManagedEndpoint(
            endpoint1,
            new ManagedEndpointGroup(new EndpointGroup()),
            mock(EndpointConnector.class)
        );
        endpoints.add(managedEndpoint1);
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setWeight(1);
        ManagedEndpoint managedEndpoint2 = new ManagedEndpoint(
            endpoint2,
            new ManagedEndpointGroup(new EndpointGroup()),
            mock(EndpointConnector.class)
        );
        endpoints.add(managedEndpoint2);

        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(endpoints);
        ManagedEndpoint next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint1);
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2);
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint1);
    }
}
