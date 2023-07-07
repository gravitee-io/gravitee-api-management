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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
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
        ManagedEndpoint next = cut.next();
        assertThat(next).isNull();
    }

    @Test
    void should_return_endpoints_in_order() {
        List<ManagedEndpoint> endpoints = new ArrayList<>();
        Endpoint endpoint1 = new Endpoint();
        endpoint1.setWeight(1);
        ManagedEndpoint managedEndpoint1 = new DefaultManagedEndpoint(
            endpoint1,
            new DefaultManagedEndpointGroup(new EndpointGroup()),
            mock(EndpointConnector.class)
        );
        endpoints.add(managedEndpoint1);
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setWeight(5);
        ManagedEndpoint managedEndpoint2 = new DefaultManagedEndpoint(
            endpoint2,
            new DefaultManagedEndpointGroup(new EndpointGroup()),
            mock(EndpointConnector.class)
        );
        endpoints.add(managedEndpoint2);
        Endpoint endpoint3 = new Endpoint();
        endpoint3.setWeight(3);
        ManagedEndpoint managedEndpoint3 = new DefaultManagedEndpoint(
            endpoint3,
            new DefaultManagedEndpointGroup(new EndpointGroup()),
            mock(EndpointConnector.class)
        );
        endpoints.add(managedEndpoint3);

        WeightedRoundRobinLoadBalancer cut = new WeightedRoundRobinLoadBalancer(endpoints);
        // 1
        ManagedEndpoint next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint1); // 1 > 0
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2); // 5 > 4
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint3); // 3 > 2

        // 2
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2); // 4 > 3
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint3); // 2 > 1

        // 3
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2); // 3 > 2
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint3); // 1 > 0

        // 4
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2); // 2 > 1
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2); // 1 > 0

        // 5
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint1);
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint2);
        next = cut.next();
        assertThat(next).isEqualTo(managedEndpoint3);
    }
}
