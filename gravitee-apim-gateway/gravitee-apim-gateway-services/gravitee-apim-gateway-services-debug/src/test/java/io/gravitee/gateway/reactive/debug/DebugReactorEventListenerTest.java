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
package io.gravitee.gateway.reactive.debug;

import static io.gravitee.gateway.debug.utils.Stubs.aDebugApiV4Definition;
import static io.gravitee.gateway.debug.utils.Stubs.getADebugApiDefinition;
import static io.gravitee.gateway.debug.utils.Stubs.getAReactorEvent;
import static io.gravitee.gateway.debug.utils.Stubs.getAnEvent;
import static io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.definition.model.debug.DebugApiV4;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.debug.definition.DebugApiV2;
import io.gravitee.gateway.debug.definition.ReactableDebugApi;
import io.gravitee.gateway.debug.vertx.VertxDebugHttpClientConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.impl.ReactableEvent;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DebugReactorEventListenerTest {

    private static final String EVENT_ID = "evt-id";

    private static final String PAYLOAD = "Debugged Api payload";

    @Captor
    ArgumentCaptor<Event> eventCaptor;

    @Captor
    ArgumentCaptor<SecretDiscoveryEvent> secretDiscoveryEventCaptor;

    @Mock
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AccessPointManager accessPointManager;

    @Mock
    private Vertx vertx;

    @Mock
    private DataEncryptor dataEncryptor;

    @Mock
    private EventManager eventManager;

    private VertxDebugHttpClientConfiguration debugHttpClientConfiguration;
    private DebugReactorEventListener debugReactorEventListener;

    @BeforeEach
    public void beforeEach() {
        debugHttpClientConfiguration = VertxDebugHttpClientConfiguration.builder().build();
        debugReactorEventListener =
            spy(
                new DebugReactorEventListener(
                    vertx,
                    eventManager,
                    eventRepository,
                    objectMapper,
                    debugHttpClientConfiguration,
                    reactorHandlerRegistry,
                    accessPointManager,
                    dataEncryptor
                )
            );
    }

    @Test
    void should_register_reactor_event_listener() throws Exception {
        eventManager = new EventManagerImpl();
        debugReactorEventListener =
            spy(
                new DebugReactorEventListener(
                    vertx,
                    eventManager,
                    eventRepository,
                    objectMapper,
                    debugHttpClientConfiguration,
                    reactorHandlerRegistry,
                    accessPointManager,
                    dataEncryptor
                )
            );

        debugReactorEventListener.start();

        eventManager.publishEvent(new SimpleEvent<>(ReactorEvent.DEPLOY, ""));
        verify(debugReactorEventListener).onEvent(any());
    }

    @Test
    void should_do_nothing() {
        debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEPLOY, null));

        verify(reactorHandlerRegistry, times(0)).create(any());
        verify(reactorHandlerRegistry, times(0)).contains(any());
        verify(reactorHandlerRegistry, after(500).times(0)).remove(any());
    }

    @Nested
    class DebugApiV2Tests {

        @Test
        void should_debug_api_successfully() throws TechnicalException, JsonProcessingException, GeneralSecurityException {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();

            Properties properties = new Properties();
            properties.setProperties(List.of(new Property("key", "encrypted", true)));
            debugApiModel.setProperties(properties);
            when(dataEncryptor.decrypt(any())).thenReturn("decrypted");

            final HttpRequest httpRequest = new HttpRequest("/path1", "GET", "request body");
            debugApiModel.setRequest(httpRequest);
            when(objectMapper.readValue(anyString(), any(DebugApiV2.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(DebugApiV2.class))).thenReturn(false);

            final HttpClient mockHttpClient = mock(HttpClient.class);
            when(vertx.createHttpClient(any(HttpClientOptions.class))).thenReturn(mockHttpClient);

            // Mock successful Buffer body in HttpClientResponse
            final HttpClientResponse httpClientResponse = mock(HttpClientResponse.class);
            when(httpClientResponse.statusCode()).thenReturn(200);
            final Buffer bodyBuffer = Buffer.buffer("response body");
            when(httpClientResponse.rxBody()).thenReturn(Single.just(bodyBuffer));

            // Mock successful HttpClientRequest
            final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
            when(mockHttpClient.rxRequest(any())).thenReturn(Single.just(httpClientRequest));
            when(httpClientRequest.setChunked(true)).thenReturn(httpClientRequest);
            when(httpClientRequest.rxSend(any(String.class))).thenReturn(Single.just(httpClientResponse));

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(dataEncryptor, times(1)).decrypt("encrypted");
            verify(reactorHandlerRegistry, times(1)).contains(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, times(1)).create(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, timeout(10000).times(1)).remove(any(DebugApiV2.class));
            verify(eventRepository, times(1)).update(eventCaptor.capture());

            final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
            assertThat(events.getFirst().getProperties()).containsEntry(API_DEBUG_STATUS.getValue(), ApiDebugStatus.DEBUGGING.name());
            assertThat(events.getFirst().getPayload()).isEqualTo(PAYLOAD);

            verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), secretDiscoveryEventCaptor.capture());

            final SecretDiscoveryEvent capturedEvent = secretDiscoveryEventCaptor.getValue();
            assertThat(capturedEvent).isNotNull();
            assertThat(capturedEvent.envId()).isEqualTo(reactableWrapper.getEnvironmentId());
            assertThat(capturedEvent.definition()).isEqualTo(debugApiModel);
            assertThat(capturedEvent.metadata()).isNotNull();

            verify(eventManager, timeout(10000)).publishEvent(eq(SecretDiscoveryEventType.REVOKE), secretDiscoveryEventCaptor.capture());

            final SecretDiscoveryEvent capturedRevokeEvent = secretDiscoveryEventCaptor.getValue();
            assertThat(capturedRevokeEvent).isNotNull();
            assertThat(capturedRevokeEvent.envId()).isEqualTo(reactableWrapper.getEnvironmentId());
            assertThat(capturedRevokeEvent.definition()).isEqualTo(debugApiModel);
            assertThat(capturedRevokeEvent.metadata()).isNotNull();
        }

        @Test
        void should_debug_api_successfully_null_body() throws TechnicalException, JsonProcessingException {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            final HttpRequest httpRequest = new HttpRequest("/path1", "GET");
            debugApiModel.setRequest(httpRequest);
            when(objectMapper.readValue(anyString(), any(DebugApiV2.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(DebugApiV2.class))).thenReturn(false);
            final HttpClient mockHttpClient = mock(HttpClient.class);
            when(vertx.createHttpClient(any(HttpClientOptions.class))).thenReturn(mockHttpClient);

            // Mock successful Buffer body in HttpClientResponse
            final HttpClientResponse httpClientResponse = mock(HttpClientResponse.class);
            when(httpClientResponse.statusCode()).thenReturn(200);
            final Buffer bodyBuffer = Buffer.buffer("response body");
            when(httpClientResponse.rxBody()).thenReturn(Single.just(bodyBuffer));

            // Mock successful HttpClientRequest
            final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
            when(mockHttpClient.rxRequest(any())).thenReturn(Single.just(httpClientRequest));
            when(httpClientRequest.setChunked(true)).thenReturn(httpClientRequest);
            when(httpClientRequest.rxSend()).thenReturn(Single.just(httpClientResponse));

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(reactorHandlerRegistry, times(1)).create(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, times(1)).contains(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, timeout(10000).times(1)).remove(any(DebugApiV2.class));
            verify(eventRepository, times(1)).update(eventCaptor.capture());

            final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
            assertThat(events.getFirst().getProperties()).containsEntry(API_DEBUG_STATUS.getValue(), ApiDebugStatus.DEBUGGING.name());
            assertThat(events.getFirst().getPayload()).isEqualTo(PAYLOAD);
        }

        @Test
        void should_remove_reactor_handler_if_body_do_not_complete_successfully() throws TechnicalException, JsonProcessingException {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            final HttpRequest httpRequest = new HttpRequest("/path1", "GET", "request body");
            debugApiModel.setRequest(httpRequest);
            when(objectMapper.readValue(anyString(), any(DebugApiV2.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(DebugApiV2.class))).thenReturn(false);
            final HttpClient mockHttpClient = mock(HttpClient.class);
            when(vertx.createHttpClient(any(HttpClientOptions.class))).thenReturn(mockHttpClient);

            // Mock failing Buffer body in HttpClientResponse future
            final HttpClientResponse httpClientResponse = mock(HttpClientResponse.class);
            when(httpClientResponse.statusCode()).thenReturn(200);
            when(httpClientResponse.rxBody()).thenReturn(Single.error(new RuntimeException()));

            // Mock successful HttpClientRequest
            final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
            when(mockHttpClient.rxRequest(any())).thenReturn(Single.just(httpClientRequest));
            when(httpClientRequest.setChunked(true)).thenReturn(httpClientRequest);
            when(httpClientRequest.rxSend(any(String.class))).thenReturn(Single.just(httpClientResponse));

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(reactorHandlerRegistry, times(1)).create(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, times(1)).contains(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, timeout(10000).times(1)).remove(any(DebugApiV2.class));

            verify(eventRepository, times(2)).update(eventCaptor.capture());

            final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
            assertThat(events.get(1).getProperties()).containsEntry(API_DEBUG_STATUS.getValue(), ApiDebugStatus.ERROR.name());

            verify(eventManager, timeout(10000)).publishEvent(eq(SecretDiscoveryEventType.REVOKE), secretDiscoveryEventCaptor.capture());

            final SecretDiscoveryEvent capturedRevokeEvent = secretDiscoveryEventCaptor.getValue();
            assertThat(capturedRevokeEvent).isNotNull();
            assertThat(capturedRevokeEvent.envId()).isEqualTo(reactableWrapper.getEnvironmentId());
            assertThat(capturedRevokeEvent.definition()).isEqualTo(debugApiModel);
            assertThat(capturedRevokeEvent.metadata()).isNotNull();
        }

        @Test
        void should_remove_reactor_handler_if_response_do_not_complete_successfully() throws TechnicalException, JsonProcessingException {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            final HttpRequest httpRequest = new HttpRequest("/path1", "GET", "request body");
            debugApiModel.setRequest(httpRequest);
            when(objectMapper.readValue(anyString(), any(DebugApiV2.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(DebugApiV2.class))).thenReturn(false);
            final HttpClient mockHttpClient = mock(HttpClient.class);
            when(vertx.createHttpClient(any(HttpClientOptions.class))).thenReturn(mockHttpClient);

            // Mock failed HttpClientRequest
            final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
            when(mockHttpClient.rxRequest(any())).thenReturn(Single.just(httpClientRequest));
            when(httpClientRequest.setChunked(true)).thenReturn(httpClientRequest);
            when(httpClientRequest.rxSend()).thenReturn(Single.error(new RuntimeException()));

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(reactorHandlerRegistry, times(1)).create(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, times(1)).contains(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, timeout(10000).times(1)).remove(any(DebugApiV2.class));

            verify(eventRepository, times(2)).update(eventCaptor.capture());

            final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
            assertThat(events.get(1).getProperties()).containsEntry(API_DEBUG_STATUS.getValue(), ApiDebugStatus.ERROR.name());

            verify(eventManager, timeout(10000)).publishEvent(eq(SecretDiscoveryEventType.REVOKE), secretDiscoveryEventCaptor.capture());

            final SecretDiscoveryEvent capturedRevokeEvent = secretDiscoveryEventCaptor.getValue();
            assertThat(capturedRevokeEvent).isNotNull();
            assertThat(capturedRevokeEvent.envId()).isEqualTo(reactableWrapper.getEnvironmentId());
            assertThat(capturedRevokeEvent.definition()).isEqualTo(debugApiModel);
            assertThat(capturedRevokeEvent.metadata()).isNotNull();
        }

        @Test
        void should_remove_reactor_handler_if_request_do_not_complete_successfully() throws TechnicalException, JsonProcessingException {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            final HttpRequest httpRequest = new HttpRequest("/path1", "GET", "request body");
            debugApiModel.setRequest(httpRequest);
            when(objectMapper.readValue(anyString(), any(DebugApiV2.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(DebugApiV2.class))).thenReturn(false);

            final HttpClient mockHttpClient = mock(HttpClient.class);
            when(vertx.createHttpClient(any(HttpClientOptions.class))).thenReturn(mockHttpClient);

            // Mock failed HttpClientRequest
            final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
            when(mockHttpClient.rxRequest(any())).thenReturn(Single.error(new RuntimeException()));

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(reactorHandlerRegistry, times(1)).create(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, times(1)).contains(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, timeout(10000).times(1)).remove(any(DebugApiV2.class));

            verify(eventRepository, times(2)).update(eventCaptor.capture());

            final List<Event> events = eventCaptor.getAllValues();
            assertThat(events.get(1).getProperties())
                .containsKey(API_DEBUG_STATUS.getValue())
                .containsEntry(API_DEBUG_STATUS.getValue(), ApiDebugStatus.ERROR.name());

            verify(eventManager, timeout(10000)).publishEvent(eq(SecretDiscoveryEventType.REVOKE), secretDiscoveryEventCaptor.capture());

            final SecretDiscoveryEvent capturedRevokeEvent = secretDiscoveryEventCaptor.getValue();
            assertThat(capturedRevokeEvent).isNotNull();
            assertThat(capturedRevokeEvent.envId()).isEqualTo(reactableWrapper.getEnvironmentId());
            assertThat(capturedRevokeEvent.definition()).isEqualTo(debugApiModel);
            assertThat(capturedRevokeEvent.metadata()).isNotNull();
        }

        @Test
        void should_remove_reactor_handler_if_technical_exception_during_event_updating()
            throws TechnicalException, JsonProcessingException {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            final HttpRequest httpRequest = new HttpRequest("/path1", "GET", "request body");
            debugApiModel.setRequest(httpRequest);
            when(objectMapper.readValue(anyString(), any(DebugApiV2.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(DebugApiV2.class))).thenReturn(false);
            when(eventRepository.update(any())).thenThrow(TechnicalException.class);

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(reactorHandlerRegistry, times(1)).create(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, times(1)).contains(any(DebugApiV2.class));
            verify(reactorHandlerRegistry, timeout(10000).times(1)).remove(any(DebugApiV2.class));

            verify(eventRepository, times(2)).update(eventCaptor.capture());

            final List<Event> events = eventCaptor.getAllValues();
            assertThat(events.get(1).getProperties())
                .containsKey(API_DEBUG_STATUS.getValue())
                .containsEntry(API_DEBUG_STATUS.getValue(), ApiDebugStatus.ERROR.name());
        }

        @Test
        void should_do_nothing_when_reactable_already_debugging() throws JsonProcessingException, TechnicalException {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            final HttpRequest httpRequest = new HttpRequest("/path1", "GET", "request body");
            debugApiModel.setRequest(httpRequest);
            when(objectMapper.readValue(anyString(), any(DebugApiV2.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(DebugApiV2.class))).thenReturn(true);

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(reactorHandlerRegistry, times(0)).create(any());
            verify(reactorHandlerRegistry, times(1)).contains(any());
            verify(reactorHandlerRegistry, after(500).times(0)).remove(any());
            verify(eventManager, times(0)).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), any(SecretDiscoveryEvent.class));
        }

        @Test
        void should_debug_api_and_filter_closed_plan() throws JsonProcessingException {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            debugApiModel.setPlans(
                List.of(
                    Plan.builder().id("plan1").security("KEY_LESS").status("PUBLISHED").build(),
                    Plan.builder().id("closed-plan").security("KEY_LESS").status("CLOSED").build()
                )
            );

            final HttpRequest httpRequest = new HttpRequest("/path1", "GET", "request body");
            debugApiModel.setRequest(httpRequest);
            when(objectMapper.readValue(anyString(), any(DebugApiV2.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(DebugApiV2.class))).thenReturn(false);

            final HttpClient mockHttpClient = mock(HttpClient.class);
            when(vertx.createHttpClient(any(HttpClientOptions.class))).thenReturn(mockHttpClient);

            // Mock successful HttpClientRequest
            final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
            when(mockHttpClient.rxRequest(any())).thenReturn(Single.just(httpClientRequest));

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(reactorHandlerRegistry, times(1))
                .contains(
                    argThat(debugApi ->
                        ((DebugApiV2) debugApi).getDefinition().getPlans().stream().noneMatch(plan -> plan.getStatus().equals("CLOSED"))
                    )
                );
        }
    }

    @Nested
    class DebugApiV4Tests {

        @Test
        void should_debug_api_successfully() throws TechnicalException, JsonProcessingException, GeneralSecurityException {
            io.gravitee.definition.model.debug.DebugApiV4 debugApiModel = aDebugApiV4Definition(
                new HttpRequest("/path1", "GET", "request body")
            );
            debugApiModel
                .getApiDefinition()
                .setProperties(List.of(new io.gravitee.definition.model.v4.property.Property("key", "encrypted", true)));
            when(dataEncryptor.decrypt(eq("encrypted"))).thenReturn("decrypted");
            when(objectMapper.readValue(anyString(), any(DebugApiV4.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD, new HashMap<>(Map.of("definition_version", "V4")));
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(ReactableDebugApi.class))).thenReturn(false);

            final HttpClient mockHttpClient = mock(HttpClient.class);
            when(vertx.createHttpClient(any(HttpClientOptions.class))).thenReturn(mockHttpClient);

            // Mock successful Buffer body in HttpClientResponse
            final HttpClientResponse httpClientResponse = mock(HttpClientResponse.class);
            when(httpClientResponse.statusCode()).thenReturn(200);
            final Buffer bodyBuffer = Buffer.buffer("response body");
            when(httpClientResponse.rxBody()).thenReturn(Single.just(bodyBuffer));

            // Mock successful HttpClientRequest
            final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
            when(mockHttpClient.rxRequest(any())).thenReturn(Single.just(httpClientRequest));
            when(httpClientRequest.setChunked(true)).thenReturn(httpClientRequest);
            when(httpClientRequest.rxSend(any(String.class))).thenReturn(Single.just(httpClientResponse));

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(dataEncryptor, times(1)).decrypt("encrypted");
            verify(reactorHandlerRegistry, times(1)).contains(any(io.gravitee.gateway.debug.definition.DebugApiV4.class));
            verify(reactorHandlerRegistry, times(1)).create(any(io.gravitee.gateway.debug.definition.DebugApiV4.class));
            verify(reactorHandlerRegistry, timeout(10000).times(1)).remove(any(io.gravitee.gateway.debug.definition.DebugApiV4.class));
            verify(eventRepository, times(1)).update(eventCaptor.capture());

            final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
            assertThat(events.getFirst().getProperties()).containsEntry(API_DEBUG_STATUS.getValue(), ApiDebugStatus.DEBUGGING.name());
            assertThat(events.getFirst().getPayload()).isEqualTo(PAYLOAD);

            verify(eventManager).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), secretDiscoveryEventCaptor.capture());

            final SecretDiscoveryEvent capturedEvent = secretDiscoveryEventCaptor.getValue();
            assertThat(capturedEvent).isNotNull();
            assertThat(capturedEvent.envId()).isEqualTo(reactableWrapper.getEnvironmentId());
            assertThat(capturedEvent.definition()).isEqualTo(debugApiModel.getApiDefinition());
            assertThat(capturedEvent.metadata()).isNotNull();

            verify(eventManager, timeout(10000)).publishEvent(eq(SecretDiscoveryEventType.REVOKE), secretDiscoveryEventCaptor.capture());

            final SecretDiscoveryEvent capturedRevokeEvent = secretDiscoveryEventCaptor.getValue();
            assertThat(capturedRevokeEvent).isNotNull();
            assertThat(capturedRevokeEvent.envId()).isEqualTo(reactableWrapper.getEnvironmentId());
            assertThat(capturedRevokeEvent.definition()).isEqualTo(debugApiModel.getApiDefinition());
            assertThat(capturedRevokeEvent.metadata()).isNotNull();
        }

        @Test
        void should_remove_reactor_handler_if_body_do_not_complete_successfully() throws TechnicalException, JsonProcessingException {
            io.gravitee.definition.model.debug.DebugApiV4 debugApiModel = aDebugApiV4Definition(
                new HttpRequest("/path1", "GET", "request body")
            );
            when(objectMapper.readValue(anyString(), any(DebugApiV4.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD, new HashMap<>(Map.of("definition_version", "V4")));
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(ReactableDebugApi.class))).thenReturn(false);

            final HttpClient mockHttpClient = mock(HttpClient.class);
            when(vertx.createHttpClient(any(HttpClientOptions.class))).thenReturn(mockHttpClient);

            // Mock failing Buffer body in HttpClientResponse future
            final HttpClientResponse httpClientResponse = mock(HttpClientResponse.class);
            when(httpClientResponse.statusCode()).thenReturn(200);
            when(httpClientResponse.rxBody()).thenReturn(Single.error(new RuntimeException()));

            // Mock successful HttpClientRequest
            final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
            when(mockHttpClient.rxRequest(any())).thenReturn(Single.just(httpClientRequest));
            when(httpClientRequest.setChunked(true)).thenReturn(httpClientRequest);
            when(httpClientRequest.rxSend(any(String.class))).thenReturn(Single.just(httpClientResponse));

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(reactorHandlerRegistry, times(1)).create(any(io.gravitee.gateway.debug.definition.DebugApiV4.class));
            verify(reactorHandlerRegistry, times(1)).contains(any(io.gravitee.gateway.debug.definition.DebugApiV4.class));
            verify(reactorHandlerRegistry, timeout(10000).times(1)).remove(any(io.gravitee.gateway.debug.definition.DebugApiV4.class));

            verify(eventRepository, times(2)).update(eventCaptor.capture());

            final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
            assertThat(events.get(1).getProperties()).containsEntry(API_DEBUG_STATUS.getValue(), ApiDebugStatus.ERROR.name());

            verify(eventManager, timeout(10000)).publishEvent(eq(SecretDiscoveryEventType.REVOKE), secretDiscoveryEventCaptor.capture());

            final SecretDiscoveryEvent capturedRevokeEvent = secretDiscoveryEventCaptor.getValue();
            assertThat(capturedRevokeEvent).isNotNull();
            assertThat(capturedRevokeEvent.envId()).isEqualTo(reactableWrapper.getEnvironmentId());
            assertThat(capturedRevokeEvent.definition()).isEqualTo(debugApiModel.getApiDefinition());
            assertThat(capturedRevokeEvent.metadata()).isNotNull();
        }

        @Test
        void should_do_nothing_when_reactable_already_debugging() throws JsonProcessingException {
            io.gravitee.definition.model.debug.DebugApiV4 debugApiModel = aDebugApiV4Definition(
                new HttpRequest("/path1", "GET", "request body")
            );
            when(objectMapper.readValue(anyString(), any(DebugApiV4.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD, new HashMap<>(Map.of("definition_version", "V4")));
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(ReactableDebugApi.class))).thenReturn(true);

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(reactorHandlerRegistry, times(0)).create(any());
            verify(reactorHandlerRegistry, times(1)).contains(any());
            verify(reactorHandlerRegistry, after(500).times(0)).remove(any());
            verify(eventManager, times(0)).publishEvent(eq(SecretDiscoveryEventType.DISCOVER), any(SecretDiscoveryEvent.class));
        }

        @Test
        void should_debug_api_and_filter_closed_plan() throws JsonProcessingException {
            io.gravitee.definition.model.debug.DebugApiV4 debugApiModel = aDebugApiV4Definition(
                new HttpRequest("/path1", "GET", "request body")
            );
            debugApiModel
                .getApiDefinition()
                .setPlans(
                    List.of(
                        io.gravitee.definition.model.v4.plan.Plan
                            .builder()
                            .id("plan1")
                            .security(PlanSecurity.builder().type("KEYLESS").build())
                            .status(PlanStatus.PUBLISHED)
                            .build(),
                        io.gravitee.definition.model.v4.plan.Plan
                            .builder()
                            .id("closed-plan")
                            .security(PlanSecurity.builder().type("KEYLESS").build())
                            .status(PlanStatus.CLOSED)
                            .build()
                    )
                );

            when(objectMapper.readValue(anyString(), any(DebugApiV4.class.getClass()))).thenReturn(debugApiModel);

            Event anEvent = getAnEvent(EVENT_ID, PAYLOAD, new HashMap<>(Map.of("definition_version", "V4")));
            final ReactableEvent<Event> reactableWrapper = new ReactableEvent<>(anEvent.getId(), anEvent);

            when(reactorHandlerRegistry.contains(any(ReactableDebugApi.class))).thenReturn(false);

            final HttpClient mockHttpClient = mock(HttpClient.class);
            when(vertx.createHttpClient(any(HttpClientOptions.class))).thenReturn(mockHttpClient);

            // Mock successful HttpClientRequest
            final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
            when(mockHttpClient.rxRequest(any())).thenReturn(Single.just(httpClientRequest));

            debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

            verify(reactorHandlerRegistry, times(1))
                .contains(
                    argThat(debugApi ->
                        ((io.gravitee.gateway.debug.definition.DebugApiV4) debugApi).getDefinition()
                            .getPlans()
                            .stream()
                            .noneMatch(plan -> plan.getStatus().equals(PlanStatus.CLOSED))
                    )
                );
        }
    }

    @Nested
    class BuildHeadersTest {

        @Test
        void should_copy_request_headers() {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            final HttpRequest httpRequest = new HttpRequest(
                "/path1",
                "GET",
                "request body",
                Map.ofEntries(
                    Map.entry("transfer-encoding", singletonList("chunked")),
                    Map.entry("X-Gravitee-Transaction-Id", singletonList("transaction-id")),
                    Map.entry("content-type", singletonList("application/json")),
                    Map.entry("X-Gravitee-Request-Id", singletonList("request-id")),
                    Map.entry("accept-encoding", List.of("deflate", "gzip", "compress"))
                )
            );
            debugApiModel.setRequest(httpRequest);

            final MultiMap result = debugReactorEventListener.buildHeaders(new DebugApiV2("eventId", debugApiModel), httpRequest);

            assertThat(result.get("transfer-encoding")).contains("chunked");
            assertThat(result.get("X-Gravitee-Transaction-Id")).contains("transaction-id");
            assertThat(result.get("content-type")).contains("application/json");
            assertThat(result.get("X-Gravitee-Request-Id")).contains("request-id");
            assertThat(result.getAll("accept-encoding")).containsAll(List.of("deflate", "gzip", "compress"));
            // Implementation returned by convertHeaders(headers) will return the first value when using get(key)
            assertThat(result.get("accept-encoding")).contains("deflate");
        }

        @Test
        void should_enforce_host_headers_from_virtual_host() {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            debugApiModel.getProxy().getVirtualHosts().getFirst().setHost("custom_host");
            final HttpRequest httpRequest = new HttpRequest("/path1", "GET", "request body");
            debugApiModel.setRequest(httpRequest);
            final MultiMap result = debugReactorEventListener.buildHeaders(new DebugApiV2("eventId", debugApiModel), httpRequest);
            assertThat(result.get("host")).contains("custom_host");
        }

        @Test
        void should_enforce_host_headers_from_access_points() {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            when(accessPointManager.getByEnvironmentId(any()))
                .thenReturn(List.of(ReactableAccessPoint.builder().host("custom_host").build()));
            final HttpRequest httpRequest = new HttpRequest("/path1", "GET", "request body");
            debugApiModel.setRequest(httpRequest);
            final MultiMap result = debugReactorEventListener.buildHeaders(new DebugApiV2("eventId", debugApiModel), httpRequest);
            assertThat(result.get("host")).contains("custom_host");
        }

        @Test
        void should_not_enforce_host_headers_from_neither_empty_virtual_host_or_empty_access_points() {
            io.gravitee.definition.model.debug.DebugApiV2 debugApiModel = getADebugApiDefinition();
            when(accessPointManager.getByEnvironmentId(any())).thenReturn(List.of());
            final HttpRequest httpRequest = new HttpRequest("/path1", "GET", "request body");
            debugApiModel.setRequest(httpRequest);
            final MultiMap result = debugReactorEventListener.buildHeaders(new DebugApiV2("eventId", debugApiModel), httpRequest);
            assertThat(result.get("host")).isNull();
        }
    }
}
