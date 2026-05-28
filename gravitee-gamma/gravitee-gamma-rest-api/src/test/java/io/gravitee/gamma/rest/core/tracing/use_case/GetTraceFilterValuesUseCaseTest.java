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

import io.gravitee.gamma.rest.core.tracing.exception.TraceFilterNotFoundException;
import io.gravitee.gamma.rest.core.tracing.exception.UnsupportedFilterException;
import io.gravitee.gamma.rest.core.tracing.model.FilterOperator;
import io.gravitee.gamma.rest.core.tracing.model.FilterType;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterValue;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterContributor;
import io.gravitee.gamma.rest.infra.adapter.SpiTraceFilterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetTraceFilterValuesUseCaseTest {

    private static final TraceFilterSpec ENUM_HTTP_METHOD = new TraceFilterSpec(
        "HTTP_METHOD",
        "HTTP method",
        FilterType.ENUM,
        List.of(FilterOperator.EQ),
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"),
        null
    );

    private static final TraceFilterSpec NUMBER_FILTER = new TraceFilterSpec(
        "HTTP_STATUS_CODE",
        "HTTP status code",
        FilterType.NUMBER,
        List.of(FilterOperator.EQ),
        null,
        null
    );

    private static final TraceFilterSpec STRING_FILTER = new TraceFilterSpec(
        "HTTP_ROUTE",
        "HTTP route",
        FilterType.STRING,
        List.of(FilterOperator.EQ),
        null,
        null
    );

    private final GetTraceFilterValuesUseCase useCase = useCaseWith(
        contributor(null, List.of(ENUM_HTTP_METHOD, NUMBER_FILTER, STRING_FILTER))
    );

    @Test
    void should_return_all_enum_values_when_no_query_filter_supplied() {
        var output = useCase.execute(new GetTraceFilterValuesUseCase.Input(null, "HTTP_METHOD", null, null, null));

        assertThat(output.page().data())
            .extracting(TraceFilterValue::value)
            .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");
        assertThat(output.page().totalElements()).isEqualTo(7L);
    }

    @Test
    void should_substring_filter_enum_values_case_insensitively_when_query_supplied() {
        var output = useCase.execute(new GetTraceFilterValuesUseCase.Input(null, "HTTP_METHOD", "ge", null, null));

        // "GET" contains "ge" case-insensitively; nothing else matches.
        assertThat(output.page().data()).extracting(TraceFilterValue::value).containsExactly("GET");
        assertThat(output.page().totalElements()).isEqualTo(1L);
    }

    @Test
    void should_paginate_enum_values_with_1_based_pages_mirroring_apim() {
        // perPage=3 on the 7-element enum → page 1 = [GET, POST, PUT], page 2 = [PATCH, DELETE, HEAD], page 3 = [OPTIONS].
        var page1 = useCase.execute(new GetTraceFilterValuesUseCase.Input(null, "HTTP_METHOD", null, 1, 3));
        var page2 = useCase.execute(new GetTraceFilterValuesUseCase.Input(null, "HTTP_METHOD", null, 2, 3));
        var page3 = useCase.execute(new GetTraceFilterValuesUseCase.Input(null, "HTTP_METHOD", null, 3, 3));

        assertThat(page1.page().data()).extracting(TraceFilterValue::value).containsExactly("GET", "POST", "PUT");
        assertThat(page2.page().data()).extracting(TraceFilterValue::value).containsExactly("PATCH", "DELETE", "HEAD");
        assertThat(page3.page().data()).extracting(TraceFilterValue::value).containsExactly("OPTIONS");
        // totalElements stays at 7 across pages — the count is unfiltered by pagination.
        assertThat(page1.page().totalElements()).isEqualTo(7L);
        assertThat(page2.page().totalElements()).isEqualTo(7L);
        assertThat(page3.page().totalElements()).isEqualTo(7L);
    }

    @Test
    void should_return_empty_page_when_page_index_exceeds_available_data() {
        var output = useCase.execute(new GetTraceFilterValuesUseCase.Input(null, "HTTP_METHOD", null, 99, 10));

        assertThat(output.page().data()).isEmpty();
        assertThat(output.page().totalElements()).isEqualTo(7L);
    }

    @Test
    void should_reject_NUMBER_filter_with_UnsupportedFilterException() {
        assertThatThrownBy(() -> useCase.execute(new GetTraceFilterValuesUseCase.Input(null, "HTTP_STATUS_CODE", null, null, null)))
            .isInstanceOf(UnsupportedFilterException.class)
            .hasMessageContaining("NUMBER")
            .hasMessageContaining("HTTP_STATUS_CODE");
    }

    @Test
    void should_reject_STRING_filter_with_UnsupportedFilterException() {
        assertThatThrownBy(() -> useCase.execute(new GetTraceFilterValuesUseCase.Input(null, "HTTP_ROUTE", null, null, null)))
            .isInstanceOf(UnsupportedFilterException.class)
            .hasMessageContaining("STRING")
            .hasMessageContaining("HTTP_ROUTE");
    }

    @Test
    void should_throw_TraceFilterNotFoundException_for_unknown_filter_name() {
        // The addressed sub-resource doesn't exist in the registry → 404 path via NotFoundDomainException.
        assertThatThrownBy(() -> useCase.execute(new GetTraceFilterValuesUseCase.Input(null, "UNKNOWN_FILTER", null, null, null)))
            .isInstanceOf(TraceFilterNotFoundException.class)
            .hasMessageContaining("UNKNOWN_FILTER");
    }

    private static GetTraceFilterValuesUseCase useCaseWith(TraceFilterContributor... contributors) {
        return new GetTraceFilterValuesUseCase(new SpiTraceFilterRegistry(List.of(contributors)));
    }

    private static TraceFilterContributor contributor(String moduleId, List<TraceFilterSpec> filters) {
        return new TraceFilterContributor() {
            @Override
            public String moduleId() {
                return moduleId;
            }

            @Override
            public List<TraceFilterSpec> getFilters() {
                return filters;
            }
        };
    }
}
