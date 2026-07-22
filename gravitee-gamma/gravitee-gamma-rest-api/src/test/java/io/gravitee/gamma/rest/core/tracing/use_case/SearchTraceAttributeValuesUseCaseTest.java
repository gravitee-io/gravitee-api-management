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
package io.gravitee.gamma.rest.core.tracing.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.tracing.TraceScopeFilters;
import io.gravitee.gamma.rest.core.tracing.exception.UnsupportedFilterException;
import io.gravitee.gamma.rest.core.tracing.inmemory.InMemoryTracingPort;
import io.gravitee.gamma.rest.core.tracing.model.Span;
import io.gravitee.gamma.rest.core.tracing.model.SpanKind;
import io.gravitee.gamma.rest.core.tracing.model.SpanStatus;
import io.gravitee.gamma.rest.core.tracing.model.Trace;
import io.gravitee.gamma.rest.core.tracing.model.TraceAttributeValue;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchTraceAttributeValuesUseCaseTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";
    private static final String API_ID = "api-1";

    private final InMemoryTracingPort tracingPort = new InMemoryTracingPort();
    private final SearchTraceAttributeValuesUseCase useCase = new SearchTraceAttributeValuesUseCase(tracingPort);

    @Test
    void should_group_by_conversation_and_surface_the_correlated_entrypoint() {
        Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        // conv-1: two turns (two root spans, two traces), both served through the web-ai entrypoint.
        givenConversationTurn("trace-1", twoHoursAgo, "conv-1", "web-ai");
        givenConversationTurn("trace-2", oneHourAgo, "conv-1", "web-ai");
        // conv-2: a single turn, served through slack.
        givenConversationTurn("trace-3", twoHoursAgo, "conv-2", "slack");

        var output = useCase.execute(
            new SearchTraceAttributeValuesUseCase.Input(
                ORG,
                ENV,
                API_ID,
                "CONVERSATION_ID",
                List.of("gravitee.entrypoint.id"),
                null,
                null,
                null
            )
        );

        // The abstract filter name resolves to the gateway-generic root-span attribute, not the GenAI one.
        assertThat(tracingPort.getLastAttributeKey()).isEqualTo("gravitee.conversation.id");

        assertThat(output.values()).hasSize(2);
        TraceAttributeValue conv1 = byValue(output.values(), "conv-1");
        assertThat(conv1.traceCount()).isEqualTo(2); // two turns
        // The co-located entrypoint id is surfaced per conversation via the correlate mechanism.
        assertThat(conv1.attributes()).containsEntry("gravitee.entrypoint.id", "web-ai");
        TraceAttributeValue conv2 = byValue(output.values(), "conv-2");
        assertThat(conv2.traceCount()).isEqualTo(1);
        assertThat(conv2.attributes()).containsEntry("gravitee.entrypoint.id", "slack");
    }

    @Test
    void should_reject_an_unknown_filter_name() {
        assertThatThrownBy(() ->
            useCase.execute(new SearchTraceAttributeValuesUseCase.Input(ORG, ENV, API_ID, "NOT_A_FILTER", List.of(), null, null, null))
        ).isInstanceOf(UnsupportedFilterException.class);
    }

    private void givenConversationTurn(String traceId, Instant when, String conversationId, String entrypointId) {
        Span rootSpan = new Span(
            "span-" + traceId,
            null,
            "POST /agent",
            "gateway",
            when,
            1_000L,
            SpanStatus.OK,
            SpanKind.SERVER,
            Map.of("gravitee.conversation.id", conversationId, "gravitee.entrypoint.id", entrypointId),
            List.of(),
            List.of()
        );
        tracingPort.givenTrace(
            ORG,
            ENV,
            Map.of(TraceScopeFilters.ENV_ID_KEY, ENV, TraceScopeFilters.API_ID_KEY, API_ID),
            new Trace(traceId, when, 1_000L, "gateway", "POST /agent", SpanStatus.OK, 1),
            List.of(rootSpan)
        );
    }

    private static TraceAttributeValue byValue(List<TraceAttributeValue> values, String value) {
        return values
            .stream()
            .filter(v -> v.value().equals(value))
            .findFirst()
            .orElseThrow();
    }
}
