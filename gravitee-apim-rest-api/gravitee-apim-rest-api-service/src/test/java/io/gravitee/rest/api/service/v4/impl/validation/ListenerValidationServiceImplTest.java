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
package io.gravitee.rest.api.service.v4.impl.validation;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.domain_service.ApiHostValidatorDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiHostsDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.exception.InvalidHostException;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
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
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.nativeapi.NativeEntrypoint;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointDuplicatedException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointInvalidDlqException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointInvalidQosException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointMissingException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointMissingTypeException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointUnsupportedDlqException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointUnsupportedListenerTypeException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointUnsupportedQosException;
import io.gravitee.rest.api.service.v4.exception.ListenersDuplicatedException;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListenerValidationServiceImplTest {

    @Mock
    InstallationAccessQueryService installationAccessQueryService;

    @Mock
    CorsValidationService corsValidationService;

    @Mock
    EntrypointConnectorPluginService entrypointService;

    @Mock
    EndpointConnectorPluginService endpointService;

    ListenerValidationServiceImpl listenerValidationService;

    @Mock
    ApiQueryService apiQueryService;

    @Mock
    ApiHostValidatorDomainService apiHostValidatorDomainService;

    @Mock
    VerifyApiHostsDomainService verifyApiHostsDomainService;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(installationAccessQueryService.getGatewayRestrictedDomains(any())).thenReturn(List.of());
        lenient()
            .when(entrypointService.validateConnectorConfiguration(any(String.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        listenerValidationService =
            new ListenerValidationServiceImpl(
                new VerifyApiPathDomainService(apiQueryService, installationAccessQueryService, apiHostValidatorDomainService),
                entrypointService,
                endpointService,
                corsValidationService,
                verifyApiHostsDomainService
            );
    }

    @Nested
    class ValidateAndSanitizeHttpV4 {

        @Test
        void should_ignore_empty_list() {
            List<Listener> emptyListeners = List.of();
            List<Listener> validatedListeners = listenerValidationService.validateAndSanitizeHttpV4(
                GraviteeContext.getExecutionContext(),
                null,
                emptyListeners,
                emptyList()
            );
            assertThat(validatedListeners).isEmpty();
        }

        @Test
        void should_throw_duplicated_exception_with_duplicated_type() {
            // Given
            Listener httpListener1 = new HttpListener();
            Entrypoint e1 = new Entrypoint();
            e1.setType("e1");
            httpListener1.setEntrypoints(List.of(e1));
            Listener httpListener2 = new HttpListener();
            Entrypoint e2 = new Entrypoint();
            e2.setType("e2");
            httpListener1.setEntrypoints(List.of(e2));
            List<Listener> listeners = List.of(httpListener1, httpListener2);
            // When
            assertThatExceptionOfType(ListenersDuplicatedException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(GraviteeContext.getExecutionContext(), null, listeners, emptyList())
                );
        }

        @Test
        void should_return_validated_listeners() {
            // Given
            HttpListener httpListener = new HttpListener();
            httpListener.setPaths(List.of(new Path("path")));
            Entrypoint entrypoint = new Entrypoint();
            entrypoint.setType("type");
            httpListener.setEntrypoints(List.of(entrypoint));
            List<Listener> listeners = List.of(httpListener);
            when(entrypointService.findById("type")).thenReturn(mock(ConnectorPluginEntity.class));
            // When
            List<Listener> validatedListeners = listenerValidationService.validateAndSanitizeHttpV4(
                GraviteeContext.getExecutionContext(),
                null,
                listeners,
                emptyList()
            );
            // Then
            assertThat(validatedListeners).hasSize(1);
            Listener actual = validatedListeners.get(0);
            assertThat(actual).isInstanceOf(HttpListener.class);
            HttpListener actualHttpListener = (HttpListener) actual;
            assertThat(actualHttpListener.getPaths()).hasSize(1);
            Path path = actualHttpListener.getPaths().get(0);
            assertThat(path.getHost()).isNull();
            assertThat(path.getPath()).isEqualTo("/path/");
        }

        @Test
        void should_return_validated_listeners_with_qos_validation() {
            // Given
            HttpListener httpListener = new HttpListener();
            httpListener.setPaths(List.of(new Path("path")));
            httpListener.setType(ListenerType.HTTP);
            Entrypoint entrypoint = new Entrypoint();
            entrypoint.setType("type");
            httpListener.setEntrypoints(List.of(entrypoint));
            List<Listener> listeners = List.of(httpListener);
            ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
            when(connectorPluginEntity.getSupportedApiType()).thenReturn(ApiType.MESSAGE);
            when(connectorPluginEntity.getSupportedQos()).thenReturn(Set.of(Qos.AUTO));
            when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);
            // When
            List<Listener> validatedListeners = listenerValidationService.validateAndSanitizeHttpV4(
                GraviteeContext.getExecutionContext(),
                null,
                listeners,
                emptyList()
            );
            // Then
            assertThat(validatedListeners).hasSize(1);
            Listener actual = validatedListeners.get(0);
            assertThat(actual).isInstanceOf(HttpListener.class);
            HttpListener actualHttpListener = (HttpListener) actual;
            assertThat(actualHttpListener.getPaths()).hasSize(1);
            Path path = actualHttpListener.getPaths().get(0);
            assertThat(path.getHost()).isNull();
            assertThat(path.getPath()).isEqualTo("/path/");
        }

        @Test
        void should_throw_missing_path_exception_without_path() {
            // Given
            HttpListener httpListener = new HttpListener();
            // When
            assertThatExceptionOfType(InvalidPathsException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_missing_entrypoint_exception_without_entrypoints() {
            // Given
            HttpListener httpListener = new HttpListener();
            httpListener.setPaths(List.of(new Path("/path")));
            // When
            assertThatExceptionOfType(ListenerEntrypointMissingException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_missing_entrypoint_type_exception_with_no_type() {
            // Given
            HttpListener httpListener = new HttpListener();
            httpListener.setPaths(List.of(new Path("/path")));
            httpListener.setEntrypoints(List.of(new Entrypoint()));
            // When
            assertThatExceptionOfType(ListenerEntrypointMissingTypeException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_duplicated_entrypoints_exception_with_duplicated() {
            // Given
            HttpListener httpListener = new HttpListener();
            httpListener.setPaths(List.of(new Path("/path")));
            Entrypoint entrypoint = new Entrypoint();
            entrypoint.setType("type");
            httpListener.setEntrypoints(List.of(entrypoint, entrypoint));
            // When
            assertThatExceptionOfType(ListenerEntrypointDuplicatedException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_return_validated_subscription_listeners() {
            // Given
            SubscriptionListener subscriptionListener = new SubscriptionListener();
            subscriptionListener.setType(ListenerType.SUBSCRIPTION);
            Entrypoint entrypoint = new Entrypoint();
            entrypoint.setType("type");
            subscriptionListener.setEntrypoints(List.of(entrypoint));
            List<Listener> listeners = List.of(subscriptionListener);

            when(entrypointService.findById("type")).thenReturn(mock(ConnectorPluginEntity.class));
            // When
            List<Listener> validatedListeners = listenerValidationService.validateAndSanitizeHttpV4(
                GraviteeContext.getExecutionContext(),
                null,
                listeners,
                emptyList()
            );
            // Then
            assertThat(validatedListeners).hasSize(1);
            Listener actual = validatedListeners.get(0);
            assertThat(actual).isInstanceOf(SubscriptionListener.class);
        }

        @Test
        void should_throw_missing_entrypoint_exception_without_entrypoints_on_subscription_listener() {
            // Given
            SubscriptionListener subscriptionListener = new SubscriptionListener();
            // When
            assertThatExceptionOfType(ListenerEntrypointMissingException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(subscriptionListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_missing_entrypoint_type_exception_with_not_type_on_subscription_listener() {
            // Given
            SubscriptionListener subscriptionListener = new SubscriptionListener();
            Entrypoint entrypoint = new Entrypoint();
            subscriptionListener.setEntrypoints(List.of(entrypoint));

            // When
            assertThatExceptionOfType(ListenerEntrypointMissingTypeException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(subscriptionListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_duplicated_entrypoints_exception_with_duplicated_on_subscription_listener() {
            // Given
            SubscriptionListener subscriptionListener = new SubscriptionListener();
            Entrypoint entrypoint = new Entrypoint();
            entrypoint.setType("type");
            subscriptionListener.setEntrypoints(List.of(entrypoint, entrypoint));

            // When
            assertThatExceptionOfType(ListenerEntrypointDuplicatedException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(subscriptionListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_unsupported_qos_exception_with_wrong_qos() {
            // Given
            HttpListener httpListener = new HttpListener();
            httpListener.setPaths(List.of(new Path("path")));
            Entrypoint entrypoint = new Entrypoint();
            entrypoint.setType("type");
            entrypoint.setQos(Qos.AT_LEAST_ONCE);
            httpListener.setEntrypoints(List.of(entrypoint));
            ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
            when(connectorPluginEntity.getSupportedApiType()).thenReturn(ApiType.MESSAGE);
            when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);
            // When
            assertThatExceptionOfType(ListenerEntrypointUnsupportedQosException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_invalid_qos_exception_when_target_endpoint_does_not_support_at_least_once_qos() {
            HttpListener httpListener = new HttpListener();
            httpListener.setPaths(List.of(new Path("path")));
            Entrypoint entrypoint = new Entrypoint();
            entrypoint.setType("type");
            entrypoint.setQos(Qos.AT_LEAST_ONCE);
            httpListener.setEntrypoints(List.of(entrypoint));
            ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
            when(connectorPluginEntity.getSupportedApiType()).thenReturn(ApiType.MESSAGE);
            when(connectorPluginEntity.getSupportedQos()).thenReturn(Set.of(Qos.AT_LEAST_ONCE, Qos.AUTO));
            when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);

            ConnectorPluginEntity endpointConnectorPluginEntity = mock(ConnectorPluginEntity.class);
            when(endpointConnectorPluginEntity.getSupportedApiType()).thenReturn(ApiType.MESSAGE);
            when(endpointConnectorPluginEntity.getSupportedQos()).thenReturn(Set.of(Qos.AUTO));
            when(endpointService.findById("endpoint-test")).thenReturn(endpointConnectorPluginEntity);

            final EndpointGroup endpointGroup = buildEndpointGroup();

            assertThatExceptionOfType(ListenerEntrypointInvalidQosException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        List.of(endpointGroup)
                    )
                );
        }

        @Test
        void should_throw_invalid_qos_exception_when_target_endpoint_does_not_have_connector() {
            HttpListener httpListener = new HttpListener();
            httpListener.setPaths(List.of(new Path("path")));
            httpListener.setType(ListenerType.HTTP);
            Entrypoint entrypoint = new Entrypoint();
            entrypoint.setType("type");
            entrypoint.setQos(Qos.AT_LEAST_ONCE);
            httpListener.setEntrypoints(List.of(entrypoint));
            ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
            when(connectorPluginEntity.getSupportedApiType()).thenReturn(ApiType.MESSAGE);
            when(connectorPluginEntity.getSupportedQos()).thenReturn(Set.of(Qos.AT_LEAST_ONCE, Qos.AUTO));
            when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);
            when(endpointService.findById("endpoint-test")).thenReturn(null);

            final EndpointGroup endpointGroup = buildEndpointGroup();

            assertThatExceptionOfType(ListenerEntrypointInvalidQosException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        List.of(endpointGroup)
                    )
                );
        }

        @Test
        void should_validate_qos_config_when_targeting_endpoint() {
            HttpListener httpListener = new HttpListener();
            httpListener.setPaths(List.of(new Path("path")));
            Entrypoint entrypoint = new Entrypoint();
            entrypoint.setType("type");
            entrypoint.setQos(Qos.AT_LEAST_ONCE);
            httpListener.setEntrypoints(List.of(entrypoint));
            ConnectorPluginEntity connectorPluginEntity = mock(ConnectorPluginEntity.class);
            when(connectorPluginEntity.getSupportedApiType()).thenReturn(ApiType.MESSAGE);
            when(connectorPluginEntity.getSupportedQos()).thenReturn(Set.of(Qos.AT_LEAST_ONCE, Qos.AUTO));
            when(entrypointService.findById("type")).thenReturn(connectorPluginEntity);

            ConnectorPluginEntity endpointConnectorPluginEntity = mock(ConnectorPluginEntity.class);
            when(endpointConnectorPluginEntity.getSupportedApiType()).thenReturn(ApiType.MESSAGE);
            when(endpointConnectorPluginEntity.getSupportedQos()).thenReturn(Set.of(Qos.AT_LEAST_ONCE));
            when(endpointService.findById("endpoint-test")).thenReturn(endpointConnectorPluginEntity);

            final EndpointGroup endpointGroup = buildEndpointGroup();

            listenerValidationService.validateAndSanitizeHttpV4(
                GraviteeContext.getExecutionContext(),
                null,
                List.of(httpListener),
                List.of(endpointGroup)
            );
        }

        @Test
        void should_throw_unsupported_dlq_exception_when_connector_does_not_support_dlq_feature() {
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
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_invalid_dlq_exception_when_null_target_endpoint() {
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
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_unsupported_dlq_exception_when_unknown_target_endpoint() {
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
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_invalid_dlq_exception_when_target_endpoint_does_not_support_publish() {
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
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        List.of(endpointGroup)
                    )
                );
        }

        @Test
        void should_throw_invalid_dlq_exception_when_target_endpoint_does_not_have_connector() {
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
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        List.of(endpointGroup)
                    )
                );
        }

        @Test
        void should_validate_dlq_config_when_targeting_endpoint() {
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

            listenerValidationService.validateAndSanitizeHttpV4(
                GraviteeContext.getExecutionContext(),
                null,
                List.of(httpListener),
                List.of(endpointGroup)
            );
        }

        @Test
        void should_validate_dlq_config_when_targeting_endpoint_group() {
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

            listenerValidationService.validateAndSanitizeHttpV4(
                GraviteeContext.getExecutionContext(),
                null,
                List.of(httpListener),
                List.of(endpointGroup)
            );
        }

        @Test
        void should_throw_listener_entrypoint_unsupported_listener_type_exception_when_target_endpoint_does_not_match_listener_type() {
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
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(httpListener),
                        List.of(endpointGroup)
                    )
                );
        }

        @Test
        void should_throw_error_when_tcp_listener_hosts_are_not_valid() {
            when(verifyApiHostsDomainService.checkApiHosts(any(), any(), any())).thenThrow(new InvalidHostException("invalid hosts"));

            assertThatExceptionOfType(InvalidHostException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeHttpV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(TcpListener.builder().hosts(List.of("")).build()),
                        emptyList()
                    )
                );
        }
    }

    @Nested
    class ValidateAndSanitizeNativeV4 {

        @Test
        void should_ignore_empty_list() {
            List<NativeListener> emptyListeners = List.of();
            List<NativeListener> validatedListeners = listenerValidationService.validateAndSanitizeNativeV4(
                GraviteeContext.getExecutionContext(),
                null,
                emptyListeners,
                emptyList()
            );
            assertThat(validatedListeners).isEmpty();
        }

        @Test
        void should_throw_duplicated_exception_with_duplicated_type() {
            // Given
            NativeListener kafkaListener1 = new KafkaListener();
            NativeEntrypoint e1 = new NativeEntrypoint();
            e1.setType("e1");
            kafkaListener1.setEntrypoints(List.of(e1));
            NativeListener kafkaListener2 = new KafkaListener();
            NativeEntrypoint e2 = new NativeEntrypoint();
            e2.setType("e2");
            kafkaListener1.setEntrypoints(List.of(e2));
            List<NativeListener> listeners = List.of(kafkaListener1, kafkaListener2);
            // When
            assertThatExceptionOfType(ListenersDuplicatedException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeNativeV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        listeners,
                        emptyList()
                    )
                );
        }

        @Test
        void should_return_validated_kafka_listeners() {
            // Given
            NativeEntrypoint entrypoint = new NativeEntrypoint();
            entrypoint.setType("type");

            KafkaListener kafkaListener = new KafkaListener();
            kafkaListener.setHost("kafka-host");
            kafkaListener.setPort(9000);
            kafkaListener.setEntrypoints(List.of(entrypoint));

            List<NativeListener> listeners = List.of(kafkaListener);
            when(entrypointService.findById("type")).thenReturn(mock(ConnectorPluginEntity.class));
            // When
            List<NativeListener> validatedListeners = listenerValidationService.validateAndSanitizeNativeV4(
                GraviteeContext.getExecutionContext(),
                null,
                listeners,
                emptyList()
            );
            // Then
            assertThat(validatedListeners).hasSize(1);
            NativeListener actual = validatedListeners.get(0);
            assertThat(actual).isInstanceOf(KafkaListener.class);
            KafkaListener actualHttpListener = (KafkaListener) actual;
            assertThat(actualHttpListener.getHost()).isEqualTo(kafkaListener.getHost());
            assertThat(actualHttpListener.getPort()).isEqualTo(kafkaListener.getPort());
        }

        @Test
        void should_throw_error_when_kafka_listener_host_is_not_valid() {
            when(verifyApiHostsDomainService.checkApiHosts(any(), any(), any())).thenThrow(new InvalidHostException("invalid hosts"));

            assertThatExceptionOfType(InvalidHostException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeNativeV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(KafkaListener.builder().host("").build()),
                        emptyList()
                    )
                );
        }

        @Test
        void should_throw_duplicated_entrypoints_exception_with_duplicated() {
            // Given
            KafkaListener kafkaListener = new KafkaListener();

            NativeEntrypoint entrypoint = new NativeEntrypoint();
            entrypoint.setType("type");
            kafkaListener.setEntrypoints(List.of(entrypoint, entrypoint));

            // When
            assertThatExceptionOfType(ListenerEntrypointDuplicatedException.class)
                .isThrownBy(() ->
                    listenerValidationService.validateAndSanitizeNativeV4(
                        GraviteeContext.getExecutionContext(),
                        null,
                        List.of(kafkaListener),
                        emptyList()
                    )
                );
        }
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
