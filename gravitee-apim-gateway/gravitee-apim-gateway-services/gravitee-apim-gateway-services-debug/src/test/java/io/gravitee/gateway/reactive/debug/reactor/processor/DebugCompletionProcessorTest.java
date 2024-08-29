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
package io.gravitee.gateway.reactive.debug.reactor.processor;

import static io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.debug.core.invoker.InvokerResponse;
import io.gravitee.gateway.debug.definition.DebugApi;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.debug.policy.steps.PolicyRequestStep;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DebugCompletionProcessorTest {

    @Mock
    private MutableRequest mockRequest;

    private HttpHeaders spyRequestHeaders;

    @Mock
    private MutableResponse mockResponse;

    private HttpHeaders spyResponseHeaders;
    private DebugApi debugApi;
    private CustomComponentProvider componentProvider;
    private DebugExecutionContext debugCtx;

    @Mock
    private EventRepository eventRepository;

    private DebugCompletionProcessor debugCompletionProcessor;

    @BeforeEach
    public void init() {
        spyRequestHeaders = spy(HttpHeaders.create());
        spyResponseHeaders = spy(HttpHeaders.create());
        lenient().when(mockRequest.headers()).thenReturn(spyRequestHeaders);
        lenient().when(mockResponse.headers()).thenReturn(spyResponseHeaders);
        io.gravitee.definition.model.debug.DebugApi debugApi = new io.gravitee.definition.model.debug.DebugApi();
        debugApi.setId("id");
        debugApi.setName("name");
        debugApi.setVersion("version");
        this.debugApi = new DebugApi("event-id", debugApi);
        componentProvider = new CustomComponentProvider();
        componentProvider.add(Api.class, this.debugApi);
        debugCtx = new DebugExecutionContext(mockRequest, mockResponse);
        debugCtx.componentProvider(componentProvider);

        debugCompletionProcessor = new DebugCompletionProcessor(eventRepository, new ObjectMapper());

        lenient().when(mockResponse.headers()).thenReturn(HttpHeaders.create());
        lenient().when(mockResponse.status()).thenReturn(200);
        lenient().when(mockResponse.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer("{}")));

        InvokerResponse invokerResponse = debugCtx.getInvokerResponse();
        invokerResponse.setStatus(200);
        invokerResponse.getBuffer().appendBuffer(Buffer.buffer("backend response"));
        invokerResponse.setHeaders(HttpHeaders.create().add("X-Header", "backend-header"));
    }

    @Test
    public void shouldUpdateEventWithData() throws TechnicalException {
        PolicyRequestStep step1 = new PolicyRequestStep("policy-id-1", ExecutionPhase.REQUEST, "api");
        PolicyRequestStep step2 = new PolicyRequestStep("policy-id-2", ExecutionPhase.REQUEST, "api");
        debugCtx.getDebugSteps().addAll(List.of(step1, step2));

        when(mockResponse.headers()).thenReturn(HttpHeaders.create());
        when(mockResponse.status()).thenReturn(200);
        when(mockResponse.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer("{}")));

        InvokerResponse invokerResponse = debugCtx.getInvokerResponse();
        invokerResponse.setStatus(200);
        invokerResponse.getBuffer().appendBuffer(Buffer.buffer("backend response"));
        invokerResponse.setHeaders(HttpHeaders.create().add("X-Header", "backend-header"));

        Event event = new Event();
        event.setId("event-id");
        event.setProperties(new HashMap<>());
        event.setType(EventType.DEBUG_API);

        when(eventRepository.findById("event-id")).thenReturn(Optional.of(event));

        TestObserver<Void> obs = debugCompletionProcessor.execute(debugCtx).test();
        obs.awaitDone(10, TimeUnit.SECONDS).assertComplete();

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).update(captor.capture());
        Event updatedEvent = captor.getValue();
        assertThat(updatedEvent.getId()).isEqualTo("event-id");
        assertThat(updatedEvent.getProperties()).isEqualTo(Map.of(API_DEBUG_STATUS.getValue(), ApiDebugStatus.SUCCESS.name()));
        assertThat(updatedEvent.getPayload()).isNotBlank();
    }

    @Test
    public void shouldUpdateEventInErrorWhenEventRepositoryThrowsException() throws TechnicalException {
        Event event = new Event();
        event.setId("event-id");
        event.setProperties(new HashMap<>());
        event.setType(EventType.DEBUG_API);

        when(eventRepository.findById("event-id")).thenReturn(Optional.of(event));
        when(eventRepository.update(any())).thenThrow(new TechnicalException("error")).thenAnswer(invocation -> invocation.getArgument(0));

        TestObserver<Void> obs = debugCompletionProcessor.execute(debugCtx).test();
        obs.awaitDone(10, TimeUnit.SECONDS).assertComplete();

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(2)).update(captor.capture());
        assertThat(captor.getAllValues().get(1).getId()).isEqualTo("event-id");
        assertThat(captor.getAllValues().get(1).getProperties())
            .isEqualTo(Map.of(API_DEBUG_STATUS.getValue(), ApiDebugStatus.ERROR.name()));
    }

    @Test
    public void shouldDoNothingWithoutEvent() throws TechnicalException {
        when(eventRepository.findById("event-id")).thenReturn(Optional.empty());

        TestObserver<Void> obs = debugCompletionProcessor.execute(debugCtx).test();
        obs.awaitDone(10, TimeUnit.SECONDS).assertComplete();

        verify(eventRepository, never()).update(any());
    }
}
