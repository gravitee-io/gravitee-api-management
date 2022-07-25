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
package io.gravitee.gateway.debug.reactor.processor;

import static io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.PolicyScope;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.debug.core.invoker.InvokerResponse;
import io.gravitee.gateway.debug.definition.DebugApi;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContext;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugRequestStep;
import io.gravitee.gateway.debug.vertx.VertxHttpServerResponseDebugDecorator;
import io.gravitee.gateway.http.vertx.VertxHttpServerResponse;
import io.gravitee.gateway.jupiter.debug.vertx.TimeoutServerResponseDebugDecorator;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DebugEventCompletionProcessorTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private DebugExecutionContext debugExecutionContext;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DebugApi debugApi;

    @Mock
    private Request request;

    @Mock
    private PolicyMetadata policyMetadata;

    private DebugEventCompletionProcessor cut;

    @BeforeEach
    public void setUp() {
        cut = new DebugEventCompletionProcessor(eventRepository, objectMapper);
        cut.handler(__ -> {});
    }

    @ParameterizedTest
    @EnumSource(ResponseType.class)
    void shouldUpdateEventWithData(ResponseType responseType) throws TechnicalException, JsonProcessingException {
        when(debugApi.getEventId()).thenReturn("event-id");
        when(debugExecutionContext.getComponent(Api.class)).thenReturn(debugApi);
        when(debugExecutionContext.request()).thenReturn(request);

        DebugRequestStep step1 = new DebugRequestStep(
            "policy-id",
            StreamType.ON_REQUEST,
            "16415346-a549-4673-871c-3f8227e9bcfa",
            PolicyScope.ON_REQUEST,
            policyMetadata
        );

        DebugRequestStep step2 = new DebugRequestStep(
            "policy-id-2",
            StreamType.ON_RESPONSE,
            "bdcccd71-f7ef-436e-9ac1-4ab3bff45668",
            PolicyScope.ON_RESPONSE_CONTENT,
            policyMetadata
        );

        when(debugExecutionContext.getDebugSteps()).thenReturn(List.of(step1, step2));
        if (ResponseType.REGULAR_RESPONSE.equals(responseType)) {
            VertxHttpServerResponse debugResponse = getDebugResponse();
            when(debugExecutionContext.response()).thenReturn(debugResponse);
        } else {
            TimeoutServerResponseDebugDecorator timeout = getTimeoutDebugResponse();
            when(debugExecutionContext.response()).thenReturn(timeout);
        }

        when(debugExecutionContext.getInvokerResponse()).thenReturn(getInvokerResponse());

        Event event = new Event();
        event.setId("event-id");
        event.setProperties(new HashMap<>());
        event.setType(EventType.DEBUG_API);

        when(eventRepository.findById("event-id")).thenReturn(Optional.of(event));
        when(objectMapper.writeValueAsString(any())).thenReturn("PAYLOAD_CONTENT");

        cut.handle(debugExecutionContext);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).update(captor.capture());
        Event updatedEvent = captor.getValue();
        assertThat(updatedEvent.getId()).isEqualTo("event-id");
        assertThat(updatedEvent.getProperties()).isEqualTo(Map.of(API_DEBUG_STATUS.getValue(), ApiDebugStatus.SUCCESS.name()));
        assertThat(updatedEvent.getPayload()).isEqualTo("PAYLOAD_CONTENT");
    }

    @ParameterizedTest
    @EnumSource(ResponseType.class)
    void shouldSetEventInErrorWhenEventUpdateThrows(ResponseType responseType) throws TechnicalException, JsonProcessingException {
        when(debugApi.getEventId()).thenReturn("event-id");
        when(debugExecutionContext.getComponent(Api.class)).thenReturn(debugApi);
        when(debugExecutionContext.request()).thenReturn(request);
        if (ResponseType.REGULAR_RESPONSE.equals(responseType)) {
            VertxHttpServerResponse debugResponse = getDebugResponse();
            when(debugExecutionContext.response()).thenReturn(debugResponse);
        } else {
            TimeoutServerResponseDebugDecorator timeout = getTimeoutDebugResponse();
            when(debugExecutionContext.response()).thenReturn(timeout);
        }

        when(debugExecutionContext.getInvokerResponse()).thenReturn(getInvokerResponse());

        Event event = new Event();
        event.setId("event-id");
        event.setProperties(new HashMap<>());
        event.setType(EventType.DEBUG_API);

        when(eventRepository.findById("event-id")).thenReturn(Optional.of(event));
        when(objectMapper.writeValueAsString(any())).thenReturn("");
        when(eventRepository.update(any())).thenThrow(new TechnicalException(""));

        cut.handle(debugExecutionContext);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(2)).update(captor.capture());
        assertThat(captor.getAllValues().get(1).getId()).isEqualTo("event-id");
        assertThat(captor.getAllValues().get(1).getProperties())
            .isEqualTo(Map.of(API_DEBUG_STATUS.getValue(), ApiDebugStatus.ERROR.name()));
    }

    @ParameterizedTest
    @EnumSource(ResponseType.class)
    void shouldSetEventInErrorWhenWriteValueAsStringThrows(ResponseType responseType) throws TechnicalException, JsonProcessingException {
        when(debugApi.getEventId()).thenReturn("event-id");
        when(debugExecutionContext.getComponent(Api.class)).thenReturn(debugApi);
        when(debugExecutionContext.request()).thenReturn(request);
        if (ResponseType.REGULAR_RESPONSE.equals(responseType)) {
            VertxHttpServerResponse debugResponse = getDebugResponse();
            when(debugExecutionContext.response()).thenReturn(debugResponse);
        } else {
            TimeoutServerResponseDebugDecorator timeout = getTimeoutDebugResponse();
            when(debugExecutionContext.response()).thenReturn(timeout);
        }

        when(debugExecutionContext.getInvokerResponse()).thenReturn(getInvokerResponse());

        Event event = new Event();
        event.setId("event-id");
        event.setProperties(new HashMap<>());
        event.setType(EventType.DEBUG_API);

        when(eventRepository.findById("event-id")).thenReturn(Optional.of(event));
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonMappingException(null, "JsonMappingException"));

        cut.handle(debugExecutionContext);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("event-id");
        assertThat(captor.getValue().getProperties()).isEqualTo(Map.of(API_DEBUG_STATUS.getValue(), ApiDebugStatus.ERROR.name()));
    }

    @Test
    void shouldHandleDebugEventWithoutEventInDatabase() throws TechnicalException {
        when(debugApi.getEventId()).thenReturn("event-id");
        when(debugExecutionContext.getComponent(Api.class)).thenReturn(debugApi);

        when(eventRepository.findById("event-id")).thenReturn(Optional.empty());

        cut.handle(debugExecutionContext);

        verify(eventRepository, never()).update(any());
    }

    @Test
    void shouldConvertMultiMapHeadersToSimpleMap() {
        final HttpHeaders headers = HttpHeaders
            .create()
            .add("transfer-encoding", "chunked")
            .add("X-Gravitee-Transaction-Id", "transaction-id")
            .add("content-type", "application/json")
            .add("X-Gravitee-Request-Id", "request-id")
            .add("accept-encoding", "deflate")
            .add("accept-encoding", "gzip")
            .add("accept-encoding", "compress");

        final Map<String, List<String>> result = cut.convertHeaders(headers);

        assertThat(result.get("transfer-encoding")).hasSize(1);
        assertThat(result.get("transfer-encoding")).contains("chunked");
        assertThat(result.get("X-Gravitee-Transaction-Id")).hasSize(1);
        assertThat(result.get("X-Gravitee-Transaction-Id")).contains("transaction-id");
        assertThat(result.get("content-type")).hasSize(1);
        assertThat(result.get("content-type")).contains("application/json");
        assertThat(result.get("X-Gravitee-Request-Id")).hasSize(1);
        assertThat(result.get("X-Gravitee-Request-Id")).contains("request-id");
        assertThat(result.get("accept-encoding")).hasSize(3);
        assertThat(result.get("accept-encoding")).contains("deflate", "gzip", "compress");
    }

    private VertxHttpServerResponseDebugDecorator getDebugResponse() {
        VertxHttpServerResponseDebugDecorator response = mock(VertxHttpServerResponseDebugDecorator.class);
        lenient().when(response.headers()).thenReturn(HttpHeaders.create());
        lenient().when(response.status()).thenReturn(200);
        lenient().when(response.getBuffer()).thenReturn(Buffer.buffer("{}"));
        return response;
    }

    private TimeoutServerResponseDebugDecorator getTimeoutDebugResponse() {
        TimeoutServerResponseDebugDecorator timeoutResponse = mock(TimeoutServerResponseDebugDecorator.class);
        final VertxHttpServerResponseDebugDecorator debugResponse = getDebugResponse();
        when(timeoutResponse.response()).thenReturn(debugResponse);
        return timeoutResponse;
    }

    private InvokerResponse getInvokerResponse() {
        final InvokerResponse invokerResponse = new InvokerResponse();
        invokerResponse.setStatus(200);
        invokerResponse.getBuffer().appendBuffer(Buffer.buffer("backend response"));
        invokerResponse.setHeaders(HttpHeaders.create().add("X-Header", "backend-header"));
        return invokerResponse;
    }

    private enum ResponseType {
        REGULAR_RESPONSE,
        TIMEOUT_RESPONSE,
    }
}
