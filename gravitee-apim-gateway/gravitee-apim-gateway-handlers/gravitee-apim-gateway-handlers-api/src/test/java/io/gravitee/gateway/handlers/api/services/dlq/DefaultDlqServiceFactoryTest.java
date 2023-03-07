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
package io.gravitee.gateway.handlers.api.services.dlq;

import static io.gravitee.gateway.reactive.api.ConnectorMode.PUBLISH;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.entrypoint.Dlq;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.reactive.api.service.dlq.DlqService;
import io.gravitee.gateway.reactive.core.v4.endpoint.DefaultManagedEndpoint;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultDlqServiceFactoryTest {

    private static final String ENDPOINT_TYPE = "test";
    private static final String ENDPOINT_GROUP_CONFIG = "{ \"groupSharedConfig\": \"something\"}";
    private static final String ENDPOINT_CONFIG = "{ \"config\": \"something esle\"}";
    public static final String ENTRYPOINT_TYPE = "gio";
    public static final String OTHER_ENTRYPOINT_TYPE = "other";

    @Mock
    private EndpointManager endpointManager;

    @Mock
    private EntrypointConnector entrypointConnector;

    @Test
    void shouldCreateDlqService() {
        final Api api = buildApi();

        final Endpoint endpoint = api.getEndpointGroups().get(0).getEndpoints().get(1);
        final String endpointTarget = endpoint.getName();

        final DefaultDlqServiceFactory cut = new DefaultDlqServiceFactory(api, endpointManager);

        when(endpointManager.next(any())).thenReturn(mock(DefaultManagedEndpoint.class));
        when(entrypointConnector.id()).thenReturn(ENTRYPOINT_TYPE);
        final DlqService dlqService = cut.create(entrypointConnector);

        assertThat(dlqService).isNotNull().isExactlyInstanceOf(DefaultDlqService.class);
        verify(endpointManager)
            .next(
                argThat(
                    criteria ->
                        criteria.getName().equals(endpointTarget) &&
                        criteria.getModes().equals(Set.of(PUBLISH)) &&
                        criteria.getApiType() == ApiType.MESSAGE
                )
            );
    }

    @Test
    void shouldNotCreateDlqServiceWhenNoEndpointConnectorFound() {
        final Api api = buildApi();
        final DefaultDlqServiceFactory cut = new DefaultDlqServiceFactory(api, endpointManager);

        when(endpointManager.next(any())).thenReturn(null);
        when(entrypointConnector.id()).thenReturn(ENTRYPOINT_TYPE);
        final DlqService dlqService = cut.create(entrypointConnector);

        assertThat(dlqService).isNull();
    }

    @Test
    void shouldNotCreateDlqServiceWhenNoDlqServiceIsConfigured() {
        final Api api = buildApi();
        final DefaultDlqServiceFactory cut = new DefaultDlqServiceFactory(api, endpointManager);

        when(entrypointConnector.id()).thenReturn(OTHER_ENTRYPOINT_TYPE);
        final DlqService dlqService = cut.create(entrypointConnector);

        assertThat(dlqService).isNull();
    }

    private Api buildApi() {
        final Api api = new Api();
        final ArrayList<EndpointGroup> endpointGroups = new ArrayList<>();
        api.setEndpointGroups(endpointGroups);

        endpointGroups.add(buildEndpointGroup());
        endpointGroups.add(buildEndpointGroup());

        final Listener listener = buildListener(endpointGroups.get(0).getEndpoints().get(1).getName());

        api.setListeners(List.of(listener));
        return api;
    }

    private Listener buildListener(final String dlqEndpointTarget) {
        final HttpListener httpListener = new HttpListener();
        final Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType(ENTRYPOINT_TYPE);

        final Dlq dlq = new Dlq();
        dlq.setEndpoint(dlqEndpointTarget);
        entrypoint.setDlq(dlq);

        final Entrypoint entrypointNoDlq = new Entrypoint();
        entrypointNoDlq.setType(OTHER_ENTRYPOINT_TYPE);

        httpListener.setEntrypoints(List.of(entrypoint));

        return httpListener;
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
}
