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

import static io.gravitee.gateway.debug.utils.Stubs.getADebugApiDefinition;
import static io.gravitee.gateway.debug.utils.Stubs.getAReactorEvent;
import static io.gravitee.gateway.debug.utils.Stubs.getAnEvent;
import static io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.gateway.debug.definition.DebugApi;
import io.gravitee.gateway.debug.vertx.VertxDebugHttpClientConfiguration;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.impl.ReactableWrapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.MultiMap;
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
class DebugReactorEventListenerTest {

    private static final String EVENT_ID = "evt-id";

    private static final String PAYLOAD = "Debugged Api payload";

    @Captor
    ArgumentCaptor<Event> eventCaptor;

    @Mock
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private Vertx vertx;

    @Mock
    private DataEncryptor dataEncryptor;

    private VertxDebugHttpClientConfiguration debugHttpClientConfiguration;
    private DebugReactorEventListener debugReactorEventListener;
    private EventManagerImpl eventManager;

    @BeforeEach
    public void beforeEach() {
        eventManager = new EventManagerImpl();
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
                    organizationManager,
                    dataEncryptor
                )
            );
    }

    @Test
    public void shouldRegisterReactorEventListener() throws Exception {
        debugReactorEventListener.start();

        eventManager.publishEvent(new SimpleEvent<>(ReactorEvent.DEPLOY, ""));
        verify(debugReactorEventListener).onEvent(any());
    }

    @Test
    public void shouldDebugApiSuccessfully() throws TechnicalException, JsonProcessingException, GeneralSecurityException {
        io.gravitee.definition.model.debug.DebugApi debugApiModel = getADebugApiDefinition();

        Properties properties = new Properties();
        properties.setProperties(List.of(new Property("key", "encrypted", true)));
        debugApiModel.setProperties(properties);
        when(dataEncryptor.decrypt(any())).thenReturn("decrypted");

        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod("GET");
        httpRequest.setPath("/path1");
        httpRequest.setBody("request body");
        debugApiModel.setRequest(httpRequest);
        when(objectMapper.readValue(anyString(), any(DebugApi.class.getClass()))).thenReturn(debugApiModel);

        Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
        final ReactableWrapper<Event> reactableWrapper = new ReactableWrapper(anEvent);

        when(reactorHandlerRegistry.contains(any(DebugApi.class))).thenReturn(false);

        final HttpClient mockHttpClient = mock(HttpClient.class);
        when(vertx.createHttpClient(any())).thenReturn(mockHttpClient);

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
        verify(reactorHandlerRegistry, times(1)).contains(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).create(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).remove(any(DebugApi.class));
        verify(eventRepository, times(1)).update(eventCaptor.capture());

        final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
        assertThat(events.get(0).getProperties()).containsKey(API_DEBUG_STATUS.getValue());
        assertThat(events.get(0).getProperties().get(API_DEBUG_STATUS.getValue())).isEqualTo(ApiDebugStatus.DEBUGGING.name());
        assertThat(events.get(0).getPayload()).isEqualTo(PAYLOAD);
    }

    @Test
    public void shouldDebugApiSuccessfullyNullBody() throws TechnicalException, JsonProcessingException {
        io.gravitee.definition.model.debug.DebugApi debugApiModel = getADebugApiDefinition();
        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod("GET");
        httpRequest.setPath("/path1");
        httpRequest.setBody(null);
        debugApiModel.setRequest(httpRequest);
        when(objectMapper.readValue(anyString(), any(DebugApi.class.getClass()))).thenReturn(debugApiModel);

        Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
        final ReactableWrapper<io.gravitee.repository.management.model.Event> reactableWrapper = new ReactableWrapper(anEvent);

        when(reactorHandlerRegistry.contains(any(DebugApi.class))).thenReturn(false);
        final HttpClient mockHttpClient = mock(HttpClient.class);
        when(vertx.createHttpClient(any())).thenReturn(mockHttpClient);

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

        verify(reactorHandlerRegistry, times(1)).create(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).contains(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).remove(any(DebugApi.class));
        verify(eventRepository, times(1)).update(eventCaptor.capture());

        final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
        assertThat(events.get(0).getProperties()).containsKey(API_DEBUG_STATUS.getValue());
        assertThat(events.get(0).getProperties().get(API_DEBUG_STATUS.getValue())).isEqualTo(ApiDebugStatus.DEBUGGING.name());
        assertThat(events.get(0).getPayload()).isEqualTo(PAYLOAD);
    }

    @Test
    public void shouldRemoveReactorHandlerIfBodyDoNotCompleteSuccessfully() throws TechnicalException, JsonProcessingException {
        io.gravitee.definition.model.debug.DebugApi debugApiModel = getADebugApiDefinition();
        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod("GET");
        httpRequest.setPath("/path1");
        httpRequest.setBody("request body");
        debugApiModel.setRequest(httpRequest);
        when(objectMapper.readValue(anyString(), any(DebugApi.class.getClass()))).thenReturn(debugApiModel);

        Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
        final ReactableWrapper<io.gravitee.repository.management.model.Event> reactableWrapper = new ReactableWrapper(anEvent);

        when(reactorHandlerRegistry.contains(any(DebugApi.class))).thenReturn(false);
        final HttpClient mockHttpClient = mock(HttpClient.class);
        when(vertx.createHttpClient(any())).thenReturn(mockHttpClient);

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

        verify(reactorHandlerRegistry, times(1)).create(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).contains(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).remove(any(DebugApi.class));

        verify(eventRepository, times(2)).update(eventCaptor.capture());

        final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
        assertThat(events.get(1).getProperties()).containsKey(API_DEBUG_STATUS.getValue());
        assertThat(events.get(1).getProperties().get(API_DEBUG_STATUS.getValue())).isEqualTo(ApiDebugStatus.ERROR.name());
    }

    @Test
    public void shouldRemoveReactorHandlerIfResponseDoNotCompleteSuccessfully() throws TechnicalException, JsonProcessingException {
        io.gravitee.definition.model.debug.DebugApi debugApiModel = getADebugApiDefinition();
        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod("GET");
        httpRequest.setPath("/path1");
        httpRequest.setBody("request body");
        debugApiModel.setRequest(httpRequest);
        when(objectMapper.readValue(anyString(), any(DebugApi.class.getClass()))).thenReturn(debugApiModel);

        Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
        final ReactableWrapper<io.gravitee.repository.management.model.Event> reactableWrapper = new ReactableWrapper(anEvent);

        when(reactorHandlerRegistry.contains(any(DebugApi.class))).thenReturn(false);
        final HttpClient mockHttpClient = mock(HttpClient.class);
        when(vertx.createHttpClient(any())).thenReturn(mockHttpClient);

        // Mock failed HttpClientRequest
        final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
        when(mockHttpClient.rxRequest(any())).thenReturn(Single.just(httpClientRequest));
        when(httpClientRequest.setChunked(true)).thenReturn(httpClientRequest);
        when(httpClientRequest.rxSend()).thenReturn(Single.error(new RuntimeException()));

        debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

        verify(reactorHandlerRegistry, times(1)).create(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).contains(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).remove(any(DebugApi.class));

        verify(eventRepository, times(2)).update(eventCaptor.capture());

        final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
        assertThat(events.get(1).getProperties()).containsKey(API_DEBUG_STATUS.getValue());
        assertThat(events.get(1).getProperties().get(API_DEBUG_STATUS.getValue())).isEqualTo(ApiDebugStatus.ERROR.name());
    }

    @Test
    public void shouldRemoveReactorHandlerIfRequestDoNotCompleteSuccessfully() throws TechnicalException, JsonProcessingException {
        io.gravitee.definition.model.debug.DebugApi debugApiModel = getADebugApiDefinition();
        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod("GET");
        httpRequest.setPath("/path1");
        httpRequest.setBody("request body");
        debugApiModel.setRequest(httpRequest);
        when(objectMapper.readValue(anyString(), any(DebugApi.class.getClass()))).thenReturn(debugApiModel);

        Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
        final ReactableWrapper<io.gravitee.repository.management.model.Event> reactableWrapper = new ReactableWrapper(anEvent);

        when(reactorHandlerRegistry.contains(any(DebugApi.class))).thenReturn(false);

        final HttpClient mockHttpClient = mock(HttpClient.class);
        when(vertx.createHttpClient(any())).thenReturn(mockHttpClient);

        // Mock failed HttpClientRequest
        final HttpClientRequest httpClientRequest = mock(HttpClientRequest.class);
        when(mockHttpClient.rxRequest(any())).thenReturn(Single.error(new RuntimeException()));

        debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

        verify(reactorHandlerRegistry, times(1)).create(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).contains(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).remove(any(DebugApi.class));

        verify(eventRepository, times(2)).update(eventCaptor.capture());

        final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
        assertThat(events.get(1).getProperties()).containsKey(API_DEBUG_STATUS.getValue());
        assertThat(events.get(1).getProperties().get(API_DEBUG_STATUS.getValue())).isEqualTo(ApiDebugStatus.ERROR.name());
    }

    @Test
    public void shouldRemoveReactorHandlerIfTechnicalExceptionDuringEventUpdating() throws TechnicalException, JsonProcessingException {
        io.gravitee.definition.model.debug.DebugApi debugApiModel = getADebugApiDefinition();
        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod("GET");
        httpRequest.setPath("/path1");
        httpRequest.setBody("request body");
        debugApiModel.setRequest(httpRequest);
        when(objectMapper.readValue(anyString(), any(DebugApi.class.getClass()))).thenReturn(debugApiModel);

        Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
        final ReactableWrapper<io.gravitee.repository.management.model.Event> reactableWrapper = new ReactableWrapper(anEvent);

        when(reactorHandlerRegistry.contains(any(DebugApi.class))).thenReturn(false);
        when(eventRepository.update(any())).thenThrow(TechnicalException.class);

        debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

        verify(reactorHandlerRegistry, times(1)).create(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).contains(any(DebugApi.class));
        verify(reactorHandlerRegistry, times(1)).remove(any(DebugApi.class));

        verify(eventRepository, times(2)).update(eventCaptor.capture());

        final List<io.gravitee.repository.management.model.Event> events = eventCaptor.getAllValues();
        assertThat(events.get(1).getProperties()).containsKey(API_DEBUG_STATUS.getValue());
        assertThat(events.get(1).getProperties().get(API_DEBUG_STATUS.getValue())).isEqualTo(ApiDebugStatus.ERROR.name());
    }

    @Test
    public void shouldDoNothingWhenReactableAlreadyDebugging() throws JsonProcessingException, TechnicalException {
        io.gravitee.definition.model.debug.DebugApi debugApiModel = getADebugApiDefinition();
        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod("GET");
        httpRequest.setPath("/path1");
        httpRequest.setBody("request body");
        debugApiModel.setRequest(httpRequest);
        when(objectMapper.readValue(anyString(), any(DebugApi.class.getClass()))).thenReturn(debugApiModel);

        Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
        final ReactableWrapper<io.gravitee.repository.management.model.Event> reactableWrapper = new ReactableWrapper(anEvent);

        when(reactorHandlerRegistry.contains(any(DebugApi.class))).thenReturn(true);

        debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEBUG, reactableWrapper));

        verify(reactorHandlerRegistry, times(0)).create(any());
        verify(reactorHandlerRegistry, times(1)).contains(any());
        verify(reactorHandlerRegistry, times(0)).remove(any());
    }

    @Test
    public void shouldDoNothing() {
        debugReactorEventListener.onEvent(getAReactorEvent(ReactorEvent.DEPLOY, null));

        verify(reactorHandlerRegistry, times(0)).create(any());
        verify(reactorHandlerRegistry, times(0)).contains(any());
        verify(reactorHandlerRegistry, times(0)).remove(any());
    }

    @Test
    public void shouldConvertSimpleMapHeadersToMultiMapHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("transfer-encoding", singletonList("chunked"));
        headers.put("X-Gravitee-Transaction-Id", singletonList("transaction-id"));
        headers.put("content-type", singletonList("application/json"));
        headers.put("X-Gravitee-Request-Id", singletonList("request-id"));
        headers.put("accept-encoding", List.of("deflate", "gzip", "compress"));

        final MultiMap result = debugReactorEventListener.convertHeaders(headers);

        assertThat(result.get("transfer-encoding")).contains("chunked");
        assertThat(result.get("X-Gravitee-Transaction-Id")).contains("transaction-id");
        assertThat(result.get("content-type")).contains("application/json");
        assertThat(result.get("X-Gravitee-Request-Id")).contains("request-id");
        assertThat(result.getAll("accept-encoding")).containsAll(List.of("deflate", "gzip", "compress"));
        // Implementation returned by convertHeaders(headers) will return the first value when using get(key)
        assertThat(result.get("accept-encoding")).contains("deflate");
    }

    @Test
    public void shouldDebugApiAndFilterClosedPlan() throws TechnicalException, JsonProcessingException {
        io.gravitee.definition.model.debug.DebugApi debugApiModel = getADebugApiDefinition();
        Plan keylessPlan = new Plan();
        keylessPlan.setId("keyless-plan");
        keylessPlan.setSecurity("KEY_LESS");
        keylessPlan.setStatus("CLOSED");

        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod("GET");
        httpRequest.setPath("/path1");
        httpRequest.setBody("request body");
        debugApiModel.setRequest(httpRequest);
        when(objectMapper.readValue(anyString(), any(DebugApi.class.getClass()))).thenReturn(debugApiModel);

        Event anEvent = getAnEvent(EVENT_ID, PAYLOAD);
        final ReactableWrapper<Event> reactableWrapper = new ReactableWrapper(anEvent);

        when(reactorHandlerRegistry.contains(any(DebugApi.class))).thenReturn(false);

        final HttpClient mockHttpClient = mock(HttpClient.class);
        when(vertx.createHttpClient(any())).thenReturn(mockHttpClient);

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

        verify(reactorHandlerRegistry, times(1))
            .contains(
                argThat(debugApi ->
                    ((DebugApi) debugApi).getDefinition().getPlans().stream().noneMatch(plan -> plan.getStatus().equals("CLOSED"))
                )
            );
    }
}
