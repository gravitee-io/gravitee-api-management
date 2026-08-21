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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.debug.core.invoker.InvokerResponse;
import io.gravitee.gateway.debug.definition.DebugApiV2;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactive.core.context.MutableRequest;
import io.gravitee.gateway.reactive.core.context.MutableResponse;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Single;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Which path a debug session reports to the console.
 *
 * <p>The console builds its timeline by spreading the preprocessor step over the request that was
 * submitted in the form, so whatever the preprocessor step carries wins. Until the gateway put the
 * executed path there, the panel could only ever echo what the user typed — and that is wrong the
 * moment {@code http.pathHandling} resolves the path, because it shows a request that never reached
 * a single policy.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DebugReportedPathTest {

    private static final String SUBMITTED_PATH = "/a/../b";
    private static final String EXECUTED_PATH = "/b";
    private static final String EVENT_ID = "event-id";

    @Mock
    private MutableRequest mockRequest;

    @Mock
    private MutableResponse mockResponse;

    @Mock
    private Node node;

    @Mock
    private EventRepository eventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void init() {
        lenient().when(mockRequest.headers()).thenReturn(HttpHeaders.create());
        lenient().when(mockResponse.headers()).thenReturn(HttpHeaders.create());
        lenient().when(mockResponse.status()).thenReturn(200);
        lenient().when(mockResponse.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer("{}")));
    }

    @Nested
    class The_preprocessor_step {

        @Test
        void should_carry_the_path_the_policies_actually_ran_on() throws Exception {
            // Given a context built before the acceptor matched, which is when the dispatcher builds
            // it — pathInfo is still unreadable at that point.
            DebugExecutionContext debugCtx = debugContext();
            // And the acceptor having matched, which is what makes pathInfo resolvable.
            when(mockRequest.pathInfo()).thenReturn(EXECUTED_PATH);
            debugCtx.captureInitialPath();

            // When the debug session completes.
            String payload = completeAndCapturePayload(debugCtx);

            // Then the console reads the resolved path, not the one that was typed.
            assertThat(pathIn(payload)).isEqualTo(EXECUTED_PATH).isNotEqualTo(SUBMITTED_PATH);
        }

        @Test
        void should_be_empty_when_the_request_was_answered_before_any_api_matched() throws Exception {
            // Given a context that never reached the pre-processor chain — a path the gateway
            // refused, for instance. There is no resolved path to report, and the console falls back
            // to the submitted one, which is correct: nothing was resolved.
            DebugExecutionContext debugCtx = debugContext();

            String payload = completeAndCapturePayload(debugCtx);

            assertThat(pathIn(payload)).isNull();
        }

        @Test
        void should_be_read_before_the_policies_run_so_a_rewrite_does_not_backdate_it() throws Exception {
            // Given a path captured at the start of the request...
            DebugExecutionContext debugCtx = debugContext();
            when(mockRequest.pathInfo()).thenReturn(EXECUTED_PATH);
            debugCtx.captureInitialPath();
            // ...that a policy then rewrites. Stubbed leniently on purpose: nothing must read it
            // back, and a strict stub would fail for being unused — which is the property under
            // test, but stated by Mockito rather than by the assertion.
            lenient().when(mockRequest.pathInfo()).thenReturn("/rewritten-by-a-policy");

            // When the debug session completes.
            String payload = completeAndCapturePayload(debugCtx);

            // Then the step still reports the state the policies were handed. Reporting the later
            // value would credit the gateway with a rewrite a policy made, which is precisely the
            // distinction the timeline exists to draw.
            assertThat(pathIn(payload)).isEqualTo(EXECUTED_PATH);
        }
    }

    private DebugExecutionContext debugContext() {
        io.gravitee.definition.model.debug.DebugApiV2 definition = new io.gravitee.definition.model.debug.DebugApiV2();
        definition.setId("id");
        definition.setName("name");
        definition.setVersion("version");

        DebugApiV2 debugApi = new DebugApiV2(EVENT_ID, definition);
        CustomComponentProvider componentProvider = new CustomComponentProvider();
        componentProvider.add(Api.class, debugApi);
        componentProvider.add(Node.class, node);

        DebugExecutionContext debugCtx = new DebugExecutionContext(mockRequest, mockResponse);
        debugCtx.componentProvider(componentProvider);

        InvokerResponse invokerResponse = debugCtx.getInvokerResponse();
        invokerResponse.setStatus(200);
        invokerResponse.setHeaders(HttpHeaders.create());
        return debugCtx;
    }

    private String completeAndCapturePayload(final DebugExecutionContext debugCtx) throws TechnicalException {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setProperties(new HashMap<>());
        event.setType(EventType.DEBUG_API);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        new DebugCompletionProcessor(eventRepository, objectMapper)
            .execute(debugCtx)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertComplete();

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).update(captor.capture());
        return captor.getValue().getPayload();
    }

    private String pathIn(final String payload) throws Exception {
        return objectMapper.readTree(payload).path("preprocessorStep").path("path").asText(null);
    }
}
