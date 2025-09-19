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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class RoundRobinLoadBalancerTest {

    @Test
    void shouldReturnNullWithEmptyEndpoints() {
        RoundRobinLoadBalancer cut = new RoundRobinLoadBalancer(List.of());
        ManagedEndpoint next = cut.next();
        assertThat(next).isNull();
    }

    @Test
    void shouldReturnTheNextEndpointInTheListAtEachCall() {
        int totalEndpoints = 5;
        List<ManagedEndpoint> endpoints = IntStream.range(0, totalEndpoints)
            .mapToObj(i ->
                new DefaultManagedEndpoint(
                    new Endpoint(),
                    new DefaultManagedEndpointGroup(new EndpointGroup()),
                    mock(EndpointConnector.class)
                )
            )
            .collect(Collectors.toList());
        RoundRobinLoadBalancer cut = new RoundRobinLoadBalancer(endpoints);
        ManagedEndpoint prevEndpoint = null;
        for (int i = 0; i < totalEndpoints; i++) {
            ManagedEndpoint next = cut.next();
            assertThat(next).isNotNull();
            assertThat(next).isNotEqualTo(prevEndpoint);
            assertThat(endpoints.indexOf(next)).isEqualTo(i);
            prevEndpoint = next;
        }
    }
}
