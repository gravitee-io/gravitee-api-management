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
package io.gravitee.rest.api.service.v4.impl.validation;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorFeature;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Dlq;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.*;
import io.gravitee.rest.api.service.v4.validation.AnalyticsValidationService;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ListenerValidationServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private CorsValidationService corsValidationService;

    @Mock
    private EntrypointConnectorPluginService entrypointService;

    @Mock
    private EndpointConnectorPluginService endpointService;

    private ListenerValidationServiceImpl listenerValidationService;

    @Before
    public void setUp() throws Exception {
        when(environmentService.findById(any())).thenReturn(new EnvironmentEntity());
        lenient().when(entrypointService.validateConnectorConfiguration(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        listenerValidationService =
            new ListenerValidationServiceImpl(
                new PathValidationServiceImpl(apiRepository, objectMapper, environmentService),
                entrypointService,
                endpointService,
                corsValidationService
            );
    }

    @Test
    public void shouldIgnoreEmptyList() {
        List<Listener> emptyListeners = List.of();
        List<Listener> validatedListeners = listenerValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            null,
            emptyListeners,
            emptyList()
        );
        Assertions.assertThat(validatedListeners).isEmpty();
        Assertions.assertThat(validatedListeners).isEqualTo(emptyListeners);
    }

    @Test
    public void shouldThrowDuplicatedExceptionWithDuplicatedType() {
        // Given
        Listener httpListener1 = new HttpListener();
        Listener httpListener2 = new HttpListener();
        List<Listener> listeners = List.of(httpListener1, httpListener2);
        // When
        assertThatExceptionOfType(ListenersDuplicatedException.class)
            .isThrownBy(
                () -> listenerValidationService.validateAndSanitize(GraviteeContext.getExecutionContext(), null, listeners, emptyList())
            );
    }

    @Test
    public void shouldReturnValidatedListeners() {
        // Given
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        httpListener.setEntrypoints(List.of(entrypoint));
        List<Listener> listeners = List.of(httpListener);
        when(entrypointService.findById("type")).thenReturn(mock(ConnectorPluginEntity.class));
        // When
        List<Listener> validatedListeners = listenerValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            null,
            listeners,
            emptyList()
        );
        // Then
        assertThat(validatedListeners).isNotNull();
        assertThat(validatedListeners.size()).isEqualTo(1);
        Listener actual = validatedListeners.get(0);
        assertThat(actual).isInstanceOf(HttpListener.class);
        HttpListener actualHttpListener = (HttpListener) actual;
        assertThat(actualHttpListener.getPaths().size()).isEqualTo(1);
        Path path = actualHttpListener.getPaths().get(0);
        assertThat(path.getHost()).isNull();
        assertThat(path.getPath()).isEqualTo("/path/");
    }

    @Test
    public void shouldReturnValidatedListenersWithQosValidation() {
        // Given
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        httpListener.setType(ListenerType.HTTP);
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        httpListener.setEntrypoints(List.of(entrypoint));
        List<Listener> listeners = List.of(httpListener);
        ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(connectorPluginEntity.getSupportedApiType()).thenReturn(ApiType.ASYNC);
        when(connectorPluginEntity.getSupportedQos()).thenReturn(Set.of(Qos.AUTO));
        when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);
        // When
        List<Listener> validatedListeners = listenerValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            null,
            listeners,
            emptyList()
        );
        // Then
        assertThat(validatedListeners).isNotNull();
        assertThat(validatedListeners.size()).isEqualTo(1);
        Listener actual = validatedListeners.get(0);
        assertThat(actual).isInstanceOf(HttpListener.class);
        HttpListener actualHttpListener = (HttpListener) actual;
        assertThat(actualHttpListener.getPaths().size()).isEqualTo(1);
        Path path = actualHttpListener.getPaths().get(0);
        assertThat(path.getHost()).isNull();
        assertThat(path.getPath()).isEqualTo("/path/");
    }

    @Test
    public void shouldThrowMissingPathExceptionWithoutPath() {
        // Given
        HttpListener httpListener = new HttpListener();
        // When
        assertThatExceptionOfType(HttpListenerPathMissingException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldThrowMissingEntrypointExceptionWithoutEntrypoints() {
        // Given
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("/path")));
        // When
        assertThatExceptionOfType(ListenerEntrypointMissingException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldThrowMissingEntrypointTypeExceptionWithNoType() {
        // Given
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("/path")));
        httpListener.setEntrypoints(List.of(new Entrypoint()));
        // When
        assertThatExceptionOfType(ListenerEntrypointMissingTypeException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldThrowDuplicatedEntrypointsExceptionWithDuplicated() {
        // Given
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("/path")));
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        httpListener.setEntrypoints(List.of(entrypoint, entrypoint));
        // When
        assertThatExceptionOfType(ListenerEntrypointDuplicatedException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldReturnValidatedSubscriptionListeners() {
        // Given
        SubscriptionListener subscriptionListener = new SubscriptionListener();
        subscriptionListener.setType(ListenerType.SUBSCRIPTION);
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        subscriptionListener.setEntrypoints(List.of(entrypoint));
        List<Listener> listeners = List.of(subscriptionListener);

        when(entrypointService.findById("type")).thenReturn(mock(ConnectorPluginEntity.class));
        // When
        List<Listener> validatedListeners = listenerValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            null,
            listeners,
            emptyList()
        );
        // Then
        assertThat(validatedListeners).isNotNull();
        assertThat(validatedListeners.size()).isEqualTo(1);
        Listener actual = validatedListeners.get(0);
        assertThat(actual).isInstanceOf(SubscriptionListener.class);
    }

    @Test
    public void shouldThrowMissingEntrypointExceptionWithoutEntrypointsOnSubscriptionListener() {
        // Given
        SubscriptionListener subscriptionListener = new SubscriptionListener();
        // When
        assertThatExceptionOfType(ListenerEntrypointMissingException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(subscriptionListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldThrowMissingEntrypointTypeExceptionWithNotTypeOnSubscriptionListener() {
        // Given
        SubscriptionListener subscriptionListener = new SubscriptionListener();
        Entrypoint entrypoint = new Entrypoint();
        subscriptionListener.setEntrypoints(List.of(entrypoint));

        // When
        assertThatExceptionOfType(ListenerEntrypointMissingTypeException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(subscriptionListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldThrowDuplicatedEntrypointsExceptionWithDuplicatedOnSubscriptionListener() {
        // Given
        SubscriptionListener subscriptionListener = new SubscriptionListener();
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        subscriptionListener.setEntrypoints(List.of(entrypoint, entrypoint));

        // When
        assertThatExceptionOfType(ListenerEntrypointDuplicatedException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(subscriptionListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldThrowUnsupportedQosExceptionWithWrongQos() {
        // Given
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        entrypoint.setQos(Qos.AT_LEAST_ONCE);
        httpListener.setEntrypoints(List.of(entrypoint));
        ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(connectorPluginEntity.getSupportedApiType()).thenReturn(ApiType.ASYNC);
        when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);
        // When
        assertThatExceptionOfType(ListenerEntrypointUnsupportedQosException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldThrowUnsupportedDlqExceptionWhenConnectorDoesNotSupportDlqFeature() {
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        httpListener.setType(ListenerType.HTTP);
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        entrypoint.setDlq(new Dlq("target"));
        httpListener.setEntrypoints(List.of(entrypoint));
        ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(connectorPluginEntity.getAvailableFeatures()).thenReturn(emptySet());
        when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);

        assertThatExceptionOfType(ListenerEntrypointUnsupportedDlqException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldThrowInvalidDlqExceptionWhenNullTargetEndpoint() {
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        entrypoint.setDlq(new Dlq(null));
        httpListener.setEntrypoints(List.of(entrypoint));
        ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);
        when(connectorPluginEntity.getAvailableFeatures()).thenReturn(Set.of(ConnectorFeature.DLQ));

        assertThatExceptionOfType(ListenerEntrypointInvalidDlqException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldThrowUnsupportedDlqExceptionWhenUnknownTargetEndpoint() {
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        httpListener.setType(ListenerType.HTTP);
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        entrypoint.setDlq(new Dlq("unknown"));
        httpListener.setEntrypoints(List.of(entrypoint));
        ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);
        when(connectorPluginEntity.getAvailableFeatures()).thenReturn(Set.of(ConnectorFeature.DLQ));

        assertThatExceptionOfType(ListenerEntrypointInvalidDlqException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
            );
    }

    @Test
    public void shouldThrowInvalidDlqExceptionWhenTargetEndpointDoesNotSupportPublish() {
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        httpListener.setType(ListenerType.HTTP);
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        entrypoint.setDlq(new Dlq("endpoint2"));
        httpListener.setEntrypoints(List.of(entrypoint));
        ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(connectorPluginEntity.getAvailableFeatures()).thenReturn(Set.of(ConnectorFeature.DLQ));
        when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);

        ConnectorPluginEntity endpointConnectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(endpointConnectorPluginEntity.getSupportedModes()).thenReturn(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointService.findById("endpoint-test")).thenReturn(endpointConnectorPluginEntity);

        final EndpointGroup endpointGroup = buildEndpointGroup();

        assertThatExceptionOfType(ListenerEntrypointInvalidDlqException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        List.of(endpointGroup)
                    )
            );
    }

    @Test
    public void shouldThrowInvalidDlqExceptionWhenTargetEndpointDoesNotHaveConnector() {
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        httpListener.setType(ListenerType.HTTP);
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        entrypoint.setDlq(new Dlq("endpoint2"));
        httpListener.setEntrypoints(List.of(entrypoint));
        ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(connectorPluginEntity.getAvailableFeatures()).thenReturn(Set.of(ConnectorFeature.DLQ));
        when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);
        when(endpointService.findById("endpoint-test")).thenReturn(null);

        final EndpointGroup endpointGroup = buildEndpointGroup();

        assertThatExceptionOfType(ListenerEntrypointInvalidDlqException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        List.of(endpointGroup)
                    )
            );
    }

    @Test
    public void shouldValidateDlqConfigWhenTargetingEndpoint() {
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        entrypoint.setDlq(new Dlq("endpoint2"));
        httpListener.setEntrypoints(List.of(entrypoint));
        ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(connectorPluginEntity.getAvailableFeatures()).thenReturn(Set.of(ConnectorFeature.DLQ));
        when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);

        ConnectorPluginEntity endpointConnectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(endpointConnectorPluginEntity.getSupportedModes()).thenReturn(Set.of(ConnectorMode.PUBLISH));
        when(endpointService.findById("endpoint-test")).thenReturn(endpointConnectorPluginEntity);

        final EndpointGroup endpointGroup = buildEndpointGroup();

        listenerValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            null,
            List.of(httpListener),
            List.of(endpointGroup)
        );
    }

    @Test
    public void shouldValidateDlqConfigWhenTargetingEndpointGroup() {
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        httpListener.setType(ListenerType.HTTP);
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("type");
        entrypoint.setDlq(new Dlq("endpoint-group"));
        httpListener.setEntrypoints(List.of(entrypoint));
        ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(connectorPluginEntity.getAvailableFeatures()).thenReturn(Set.of(ConnectorFeature.DLQ));
        when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);

        ConnectorPluginEntity endpointConnectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(endpointConnectorPluginEntity.getSupportedModes()).thenReturn(Set.of(ConnectorMode.PUBLISH));
        when(endpointService.findById("endpoint-test")).thenReturn(endpointConnectorPluginEntity);

        final EndpointGroup endpointGroup = buildEndpointGroup();

        listenerValidationService.validateAndSanitize(
            GraviteeContext.getExecutionContext(),
            null,
            List.of(httpListener),
            List.of(endpointGroup)
        );
    }

    @Test
    public void should_throw_ListenerEntrypointUnsupportedListenerTypeException_when_target_endpoint_does_not_match_ListenerType() {
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("path")));
        httpListener.setType(ListenerType.HTTP);
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("webhook");
        httpListener.setEntrypoints(List.of(entrypoint));

        ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
        when(connectorPluginEntity.getSupportedListenerType()).thenReturn(ListenerType.SUBSCRIPTION);
        when(entrypointService.findById("webhook")).thenReturn(connectorPluginEntity);

        final EndpointGroup endpointGroup = buildEndpointGroup();

        assertThatExceptionOfType(ListenerEntrypointUnsupportedListenerTypeException.class)
            .isThrownBy(
                () ->
                    listenerValidationService.validateAndSanitize(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        List.of(endpointGroup)
                    )
            );
    }

    private io.gravitee.definition.model.v4.endpointgroup.EndpointGroup buildEndpointGroup() {
        final io.gravitee.definition.model.v4.endpointgroup.EndpointGroup endpointGroup = new EndpointGroup();
        final ArrayList<Endpoint> endpoints = new ArrayList<>();

        endpointGroup.setName("endpoint-group");
        endpointGroup.setType("endpoint-test");
        endpointGroup.setEndpoints(endpoints);

        endpoints.add(buildEndpoint("endpoint1"));
        endpoints.add(buildEndpoint("endpoint2"));

        return endpointGroup;
    }

    private Endpoint buildEndpoint(String name) {
        final Endpoint endpoint = new Endpoint();
        endpoint.setName(name);
        endpoint.setType("endpoint-test");
        return endpoint;
    }
}
