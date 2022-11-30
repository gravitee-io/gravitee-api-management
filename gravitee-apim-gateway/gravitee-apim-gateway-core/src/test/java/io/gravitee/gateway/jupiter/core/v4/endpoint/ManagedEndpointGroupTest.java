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
package io.gravitee.gateway.jupiter.core.v4.endpoint;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import java.util.ArrayList;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class ManagedEndpointGroupTest {

    private static final String ENDPOINT_TYPE = "test";
    private static final String ENDPOINT_GROUP_CONFIG = "{ \"groupSharedConfig\": \"something\"}";
    private static final String ENDPOINT_CONFIG = "{ \"config\": \"something\"}";

    @Test
    void shouldReturnNullEndpointWhenNoEndpointInTheGroup() {
        final EndpointGroup endpointGroup = buildEndpointGroup();
        final ManagedEndpointGroup cut = new ManagedEndpointGroup(endpointGroup);

        final ManagedEndpoint endpoint = cut.next();

        assertThat(endpoint).isNull();
    }

    @Test
    void shouldAddManagedEndpoint() {
        final EndpointGroup endpointGroup = buildEndpointGroup();
        final ManagedEndpointGroup cut = new ManagedEndpointGroup(endpointGroup);

        final ManagedEndpoint managedEndpoint = mockManagedEndpoint(endpointGroup.getEndpoints().get(0), cut);
        cut.addManagedEndpoint(managedEndpoint);

        assertThat(cut.next()).isSameAs(managedEndpoint);
    }

    @Test
    void shouldAddSecondaryManagedEndpoint() {
        final EndpointGroup endpointGroup = buildEndpointGroup();
        final ManagedEndpointGroup cut = new ManagedEndpointGroup(endpointGroup);

        final Endpoint endpoint = endpointGroup.getEndpoints().get(0);
        endpoint.setSecondary(true);
        final ManagedEndpoint managedEndpoint = mockManagedEndpoint(endpoint, cut);
        cut.addManagedEndpoint(managedEndpoint);

        assertThat(cut.next()).isSameAs(managedEndpoint);
    }

    @Test
    void shouldReturnPrimaryManagedEndpoint() {
        final EndpointGroup endpointGroup = buildEndpointGroup();
        final ManagedEndpointGroup cut = new ManagedEndpointGroup(endpointGroup);

        final Endpoint primary = endpointGroup.getEndpoints().get(0);
        final Endpoint secondary = endpointGroup.getEndpoints().get(1);
        secondary.setSecondary(true);

        final ManagedEndpoint secondaryManagedEndpoint = mockManagedEndpoint(secondary, cut);
        cut.addManagedEndpoint(secondaryManagedEndpoint);

        final ManagedEndpoint primaryManagedEndpoint = mockManagedEndpoint(primary, cut);
        cut.addManagedEndpoint(primaryManagedEndpoint);

        assertThat(cut.next()).isSameAs(primaryManagedEndpoint);
    }

    @Test
    void shouldRemoveManagedEndpoints() {
        final EndpointGroup endpointGroup = buildEndpointGroup();
        final ManagedEndpointGroup cut = new ManagedEndpointGroup(endpointGroup);

        final Endpoint primary = endpointGroup.getEndpoints().get(0);
        final Endpoint secondary = endpointGroup.getEndpoints().get(1);
        secondary.setSecondary(true);

        final ManagedEndpoint secondaryManagedEndpoint = mockManagedEndpoint(secondary, cut);
        cut.addManagedEndpoint(secondaryManagedEndpoint);

        final ManagedEndpoint primaryManagedEndpoint = mockManagedEndpoint(primary, cut);
        cut.addManagedEndpoint(primaryManagedEndpoint);

        assertThat(cut.next()).isSameAs(primaryManagedEndpoint);

        cut.removeManagedEndpoint(primary.getName());
        assertThat(cut.next()).isSameAs(secondaryManagedEndpoint);

        cut.removeManagedEndpoint(secondaryManagedEndpoint);
        assertThat(cut.next()).isNull();
    }

    private EndpointGroup buildEndpointGroup() {
        final EndpointGroup endpointGroup = new EndpointGroup();
        final ArrayList<Endpoint> endpoints = new ArrayList<>();

        endpointGroup.setName(randomUUID().toString());
        endpointGroup.setType(ENDPOINT_TYPE);
        endpointGroup.setEndpoints(endpoints);
        endpointGroup.setSharedConfiguration(ENDPOINT_GROUP_CONFIG);

        endpoints.add(buildEndpoint());
        endpoints.add(buildEndpoint());

        return endpointGroup;
    }

    private Endpoint buildEndpoint() {
        final Endpoint endpoint = new Endpoint();
        endpoint.setName(randomUUID().toString());
        endpoint.setType(ENDPOINT_TYPE);
        endpoint.setConfiguration(ENDPOINT_CONFIG);
        return endpoint;
    }

    private ManagedEndpoint mockManagedEndpoint(final Endpoint endpoint, final ManagedEndpointGroup group) {
        return new ManagedEndpoint(endpoint, group, mock(EndpointConnector.class));
    }
}
