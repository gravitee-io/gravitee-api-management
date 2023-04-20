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
package io.gravitee.apim.plugin.reactor.policy.tracing;

import static org.mockito.Mockito.verify;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.tracing.api.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TracingMessageHookTest {

    @Mock
    private ExecutionContext executionContext;

    @Spy
    private Span span;

    private TracingMessageHook cut;

    @BeforeEach
    void setUp() {
        cut = new TracingMessageHook();
    }

    @Test
    void should_add_message_request_span() {
        cut.withAttributes("policy-id", executionContext, ExecutionPhase.MESSAGE_REQUEST, span);
        verify(span).withAttribute(TracingMessageHook.SPAN_MESSAGE_ATTR, "incoming");
        verify(span).withAttribute("policy", "policy-id");
    }

    @Test
    void should_add_message_response_span() {
        cut.withAttributes("policy-id", executionContext, ExecutionPhase.MESSAGE_RESPONSE, span);
        verify(span).withAttribute(TracingMessageHook.SPAN_MESSAGE_ATTR, "outgoing");
        verify(span).withAttribute("policy", "policy-id");
    }
}
