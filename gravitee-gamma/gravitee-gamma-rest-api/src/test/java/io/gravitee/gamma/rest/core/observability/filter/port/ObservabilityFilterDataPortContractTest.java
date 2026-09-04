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
package io.gravitee.gamma.rest.core.observability.filter.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort.ResolveRequest;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort.ResolvedLabels;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The behaviour every {@link ObservabilityFilterDataPort} implementation must honour, whatever the
 * store behind it. The in-memory variant runs it on every build; a real-backend variant runs it on
 * the assembled store.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class ObservabilityFilterDataPortContractTest {

    protected abstract ObservabilityFilterDataPort port();

    /** Makes the store hold these distinct values for a KEYWORD filter. */
    protected abstract void givenKeywordValues(String filterName, List<FilterValue> values);

    /** Makes the store know these display names for the ids of an id-based filter. */
    protected abstract void givenLabels(String filterName, Map<String, String> labels);

    @Test
    void should_list_the_distinct_values_of_a_keyword_filter() {
        givenKeywordValues("ENTRYPOINT", List.of(new FilterValue("http-proxy", null), new FilterValue("mcp-studio", null)));

        var page = port().listKeywordValues("ENTRYPOINT", null, null, null, 1, 10, Set.of());

        assertThat(page.data()).extracting(FilterValue::value).containsExactly("http-proxy", "mcp-studio");
        assertThat(page.totalElements()).isEqualTo(2L);
    }

    @Test
    void should_narrow_values_by_a_case_insensitive_substring_of_value_or_label() {
        givenKeywordValues(
            "API",
            List.of(new FilterValue("api-1", "Petstore"), new FilterValue("api-2", "Weather"), new FilterValue("pet-3", "Other"))
        );

        var page = port().listKeywordValues("API", "PET", null, null, 1, 10, Set.of());

        assertThat(page.data()).extracting(FilterValue::value).containsExactly("api-1", "pet-3");
        assertThat(page.totalElements()).isEqualTo(2L);
    }

    @Test
    void should_paginate_values_one_based_and_report_the_total() {
        givenKeywordValues("ENTRYPOINT", List.of(new FilterValue("a", null), new FilterValue("b", null), new FilterValue("c", null)));

        var secondPage = port().listKeywordValues("ENTRYPOINT", null, null, null, 2, 2, Set.of());

        assertThat(secondPage.data()).extracting(FilterValue::value).containsExactly("c");
        assertThat(secondPage.totalElements()).isEqualTo(3L);
    }

    @Test
    void should_return_an_empty_page_for_a_filter_without_values() {
        var page = port().listKeywordValues("ENTRYPOINT", null, null, null, 1, 10, Set.of());

        assertThat(page.data()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void should_resolve_labels_per_filter_for_the_requested_ids_only() {
        givenLabels("API", Map.of("api-1", "Petstore", "api-2", "Weather"));
        givenLabels("APPLICATION", Map.of("app-1", "Mobile"));

        List<ResolvedLabels> resolved = port().resolveLabels(
            List.of(new ResolveRequest("API", List.of("api-1", "unknown")), new ResolveRequest("APPLICATION", List.of("app-1")))
        );

        assertThat(resolved)
            .extracting(ResolvedLabels::filterName, ResolvedLabels::labels)
            .containsExactly(tuple("API", Map.of("api-1", "Petstore")), tuple("APPLICATION", Map.of("app-1", "Mobile")));
    }
}
