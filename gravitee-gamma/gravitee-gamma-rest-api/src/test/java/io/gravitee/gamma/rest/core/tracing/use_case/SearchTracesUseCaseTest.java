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

import io.gravitee.gamma.rest.core.tracing.TracingResourceFilters;
import io.gravitee.gamma.rest.core.tracing.exception.UnsupportedFilterException;
import io.gravitee.gamma.rest.core.tracing.inmemory.InMemoryTracingPort;
import io.gravitee.gamma.rest.core.tracing.model.FilterCondition;
import io.gravitee.gamma.rest.core.tracing.model.FilterOperator;
import io.gravitee.gamma.rest.core.tracing.model.Trace;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchTracesUseCaseTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";
    private static final String API_ID = "api-1";
    private static final String OTHER_API = "api-2";

    private final InMemoryTracingPort tracingPort = new InMemoryTracingPort();
    private final SearchTracesUseCase useCase = new SearchTracesUseCase(tracingPort);

    @BeforeEach
    void reset() {
        tracingPort.reset();
    }

    @Test
    void should_scope_query_with_env_and_api_resource_attribute_filters() {
        // Anchor seeded traces to "now minus an hour" so they always fall inside the default 24h
        // lookback window regardless of wall-clock date — a hardcoded ISO date here drifts out of
        // the window once the local clock crosses 24h past it.
        Instant withinDefaultWindow = Instant.now().minus(Duration.ofHours(1));
        givenTrace("t-match", withinDefaultWindow, API_ID);
        givenTrace("t-other-api", withinDefaultWindow, OTHER_API);

        SearchTracesUseCase.Output output = useCase.execute(input(API_ID, List.of(), null, null, null, null));

        assertThat(output.traces().getContent()).extracting(Trace::traceId).containsExactly("t-match");
        // Module isn't part of the scope envelope — apiId alone is sufficient since each apiId belongs
        // to exactly one module (api type → module). Asserting env + api is enough to prove scoping.
        assertThat(tracingPort.getLastResourceAttributeFilters())
            .containsEntry(TracingResourceFilters.ENV_ID_KEY, ENV)
            .containsEntry(TracingResourceFilters.API_ID_KEY, API_ID)
            .doesNotContainKey("gravitee.module");
    }

    @Test
    void should_apply_default_24h_lookback_when_start_and_end_are_null() {
        useCase.execute(input(API_ID, List.of(), null, null, null, null));

        // The use case anchors on `now` and rewinds 24h; precise wallclock comparison would be flaky, so
        // assert the window is exactly 24h wide instead.
        Duration window = Duration.between(tracingPort.getLastStart(), tracingPort.getLastEnd());
        assertThat(window).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void should_anchor_default_lookback_on_provided_end_when_start_is_null() {
        Instant end = Instant.parse("2026-01-15T10:00:00Z");
        useCase.execute(input(API_ID, List.of(), null, end, null, null));

        assertThat(tracingPort.getLastEnd()).isEqualTo(end);
        assertThat(tracingPort.getLastStart()).isEqualTo(end.minus(Duration.ofHours(24)));
    }

    @Test
    void should_apply_default_page_and_perPage_when_caller_did_not_pin_them() {
        useCase.execute(input(API_ID, List.of(), null, null, null, null));

        // Use case default is page 1 (1-based) + perPage 20; the port sees page 0 (0-based) because
        // the use case subtracts 1 at the boundary. The 1-based caller convention aligns with the
        // apim management v2 logs/analytics surface; the 0-based port keeps its internal pagination
        // math unchanged.
        assertThat(tracingPort.getLastPerPage()).isEqualTo(20);
        assertThat(tracingPort.getLastPage()).isZero();
    }

    @Test
    void should_pass_through_explicit_pagination_parameters_converting_1_based_to_0_based() {
        // Caller's 1-based page 3 with perPage 50 → port receives 0-based page 2 (3 - 1).
        useCase.execute(input(API_ID, List.of(), null, null, 3, 50));

        assertThat(tracingPort.getLastPage()).isEqualTo(2);
        assertThat(tracingPort.getLastPerPage()).isEqualTo(50);
    }

    @Test
    void should_translate_supported_eq_filters_into_attribute_filters_passed_to_the_port() {
        List<FilterCondition> filters = List.of(
            new FilterCondition("HTTP_METHOD", FilterOperator.EQ, List.of("POST")),
            new FilterCondition("HTTP_STATUS_CODE", FilterOperator.EQ, List.of("500"))
        );

        useCase.execute(input(API_ID, filters, null, null, null, null));

        // HTTP_METHOD maps to attribute key http.method; HTTP_STATUS_CODE maps to http.status_code.
        // Asserted via the in-memory port's last-seen attributeFilters map — the translator is the
        // unit covered here, not the ES adapter that lives downstream of the port.
        assertThat(tracingPort.getLastAttributeFilters()).containsEntry("http.method", "POST").containsEntry("http.status_code", "500");
    }

    @Test
    void should_reject_unknown_filter_name_with_UnsupportedFilterException() {
        List<FilterCondition> filters = List.of(new FilterCondition("UNKNOWN_FILTER", FilterOperator.EQ, List.of("x")));

        assertThatThrownBy(() -> useCase.execute(input(API_ID, filters, null, null, null, null)))
            .isInstanceOf(UnsupportedFilterException.class)
            .hasMessageContaining("UNKNOWN_FILTER");
    }

    @Test
    void should_reject_unsupported_operator_with_UnsupportedFilterException() {
        // HTTP_METHOD is a supported filter, but only eq is wired through in the slim cut — `in`
        // arrives in the follow-up PR that lifts the SPI to multi-value criteria.
        List<FilterCondition> filters = List.of(new FilterCondition("HTTP_METHOD", FilterOperator.IN, List.of("GET", "POST")));

        assertThatThrownBy(() -> useCase.execute(input(API_ID, filters, null, null, null, null)))
            .isInstanceOf(UnsupportedFilterException.class)
            .hasMessageContaining("in")
            .hasMessageContaining("HTTP_METHOD");
    }

    private void givenTrace(String traceId, Instant startTime, String apiId) {
        tracingPort.givenTrace(
            ORG,
            ENV,
            Map.of(TracingResourceFilters.ENV_ID_KEY, ENV, TracingResourceFilters.API_ID_KEY, apiId),
            new Trace(traceId, startTime, 1_000L, "gateway", "GET /pets", false),
            null
        );
    }

    private SearchTracesUseCase.Input input(
        String apiId,
        List<FilterCondition> filters,
        Instant start,
        Instant end,
        Integer page,
        Integer perPage
    ) {
        return new SearchTracesUseCase.Input(ORG, ENV, apiId, filters, start, end, page, perPage);
    }
}
