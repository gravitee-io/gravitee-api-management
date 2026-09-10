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
package io.gravitee.gamma.rest.core.observability.filter.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.gravitee.gamma.rest.core.observability.filter.exception.ObservabilityFilterNotFoundException;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.inmemory.InMemoryObservabilityFilterDataPort;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetObservabilityFilterValuesUseCaseTest {

    private final InMemoryObservabilityFilterDataPort dataPort = new InMemoryObservabilityFilterDataPort();
    private final GetObservabilityFilterValuesUseCase useCase = new GetObservabilityFilterValuesUseCase(new SpiFilterRegistry(), dataPort);

    @Test
    void should_return_enum_values_with_labels_for_an_enum_filter() {
        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API_TYPE", null, null, null, null, null));

        assertThat(output.values().totalElements()).isEqualTo(8L);
        assertThat(output.values().data())
            .extracting(FilterValue::value, FilterValue::label)
            .contains(tuple("NATIVE", "Kafka (native)"), tuple("HTTP_PROXY", "HTTP Proxy"), tuple("A2A", "A2A"));
    }

    @Test
    void should_substring_filter_enum_values_case_insensitively_on_value_or_label() {
        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API_TYPE", "kafka", null, null, null, null));

        // Matches the label "Kafka (native)" even though the value is NATIVE.
        assertThat(output.values().data()).extracting(FilterValue::value).containsExactly("NATIVE");
    }

    @Test
    void should_paginate_enum_values_1_based() {
        var firstPage = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API_TYPE", null, null, null, 1, 2));
        var secondPage = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API_TYPE", null, null, null, 2, 2));

        assertThat(firstPage.values().data()).hasSize(2);
        assertThat(secondPage.values().data()).hasSize(2);
        assertThat(firstPage.values().totalElements()).isEqualTo(8L);
        assertThat(firstPage.values().data()).isNotEqualTo(secondPage.values().data());
    }

    @Test
    void should_throw_404_when_filter_is_unknown() {
        assertThatThrownBy(() ->
            useCase.execute(new GetObservabilityFilterValuesUseCase.Input("DOES_NOT_EXIST", null, null, null, null, null))
        ).isInstanceOf(ObservabilityFilterNotFoundException.class);
    }

    @Test
    void should_delegate_keyword_filter_to_the_data_port_with_resolved_pagination() {
        dataPort.givenKeywordValues("API", List.of(new FilterValue("api-1", "Petstore"), new FilterValue("api-2", "Weather")));

        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API", "pet", null, null, null, null));

        assertThat(output.values().data()).containsExactly(new FilterValue("api-1", "Petstore"));
        assertThat(dataPort.lastCall()).hasValueSatisfying(call -> {
            assertThat(call.filterName()).isEqualTo("API");
            assertThat(call.query()).isEqualTo("pet");
            // null page/perPage are resolved to the defaults (1 / 10) before reaching the port.
            assertThat(call.page()).isEqualTo(1);
            assertThat(call.perPage()).isEqualTo(10);
        });
        // The resolved pagination is surfaced on the output so the REST layer builds a consistent envelope.
        assertThat(output.page()).isEqualTo(1);
        assertThat(output.perPage()).isEqualTo(10);
    }

    @Test
    void should_offer_the_no_entrypoint_value_after_the_stored_entrypoint_ids() {
        dataPort.givenKeywordValues("ENTRYPOINT", List.of(new FilterValue("http-proxy", null), new FilterValue("mcp-studio", null)));

        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("ENTRYPOINT", null, null, null, null, null));

        assertThat(output.values().data())
            .extracting(FilterValue::value, FilterValue::label)
            .containsExactly(
                tuple("http-proxy", null),
                tuple("mcp-studio", null),
                tuple("(none)", "No entrypoint (refused before routing)")
            );
        assertThat(output.values().totalElements()).isEqualTo(3L);
    }

    @Test
    void should_place_the_no_entrypoint_value_right_after_the_last_stored_id_across_pages() {
        dataPort.givenKeywordValues(
            "ENTRYPOINT",
            List.of(new FilterValue("a", null), new FilterValue("b", null), new FilterValue("c", null))
        );

        var firstPage = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("ENTRYPOINT", null, null, null, 1, 2));
        var secondPage = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("ENTRYPOINT", null, null, null, 2, 2));

        assertThat(firstPage.values().data()).extracting(FilterValue::value).containsExactly("a", "b");
        assertThat(firstPage.values().totalElements()).isEqualTo(4L);
        assertThat(secondPage.values().data()).extracting(FilterValue::value).containsExactly("c", "(none)");
    }

    @Test
    void should_open_a_page_for_the_no_entrypoint_value_when_the_stored_ids_fill_the_last_one() {
        dataPort.givenKeywordValues("ENTRYPOINT", List.of(new FilterValue("a", null), new FilterValue("b", null)));

        var secondPage = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("ENTRYPOINT", null, null, null, 2, 2));

        assertThat(secondPage.values().data()).extracting(FilterValue::value).containsExactly("(none)");
        assertThat(secondPage.values().totalElements()).isEqualTo(3L);
    }

    @Test
    void should_narrow_the_no_entrypoint_value_the_way_the_store_narrows_stored_ids() {
        // The store matches a KEYWORD value exactly, case-insensitively; offering the synthetic value on a
        // partial query would make it the only answer to any prefix of its label.
        dataPort.givenKeywordValues("ENTRYPOINT", List.of(new FilterValue("http-proxy", null)));

        var exact = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("ENTRYPOINT", "(NONE)", null, null, null, null));
        var partial = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("ENTRYPOINT", "refused", null, null, null, null));

        assertThat(exact.values().data()).extracting(FilterValue::value).containsExactly("(none)");
        assertThat(partial.values().data()).extracting(FilterValue::value).doesNotContain("(none)");
    }

    @Test
    void should_keep_the_synthetic_value_out_of_other_keyword_filters() {
        dataPort.givenKeywordValues("API", List.of(new FilterValue("api-1", "Petstore")));

        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API", null, null, null, null, null));

        assertThat(output.values().data()).extracting(FilterValue::value).containsExactly("api-1");
        assertThat(output.values().totalElements()).isEqualTo(1L);
    }

    @Test
    void should_clamp_per_page_to_the_max_and_surface_it_on_the_output() {
        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API_TYPE", null, null, null, 1, 500));

        // perPage is clamped to MAX_PER_PAGE (100) and the clamped value is what the REST layer must use.
        assertThat(output.perPage()).isEqualTo(100);
        assertThat(output.page()).isEqualTo(1);
    }

    @Test
    void should_reject_a_page_beyond_the_maximum_before_reaching_the_data_port() {
        dataPort.givenKeywordValues("ENTRYPOINT", List.of(new FilterValue("http-proxy", null)));

        assertThatThrownBy(() ->
            useCase.execute(new GetObservabilityFilterValuesUseCase.Input("ENTRYPOINT", null, null, null, 10_001, null))
        )
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.filter.values_page_out_of_range");
        assertThat(dataPort.lastCall()).isEmpty();
    }

    @Test
    void should_reject_a_page_below_the_first_one_before_reaching_the_data_port() {
        // The OpenAPI parameter declares `minimum: 1`, and the platform use case refuses both bounds.
        // Normalising a non-positive page to 1 would serve a different page than the caller asked for.
        dataPort.givenKeywordValues("ENTRYPOINT", List.of(new FilterValue("http-proxy", null)));

        assertThatThrownBy(() -> useCase.execute(new GetObservabilityFilterValuesUseCase.Input("ENTRYPOINT", null, null, null, 0, null)))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.filter.values_page_out_of_range");
        assertThat(dataPort.lastCall()).isEmpty();
    }

    @Test
    void should_default_an_absent_page_to_the_first_one() {
        // `page` is optional and defaults to 1 in the spec: absent is not the same as out of range.
        dataPort.givenKeywordValues("ENTRYPOINT", List.of(new FilterValue("http-proxy", null)));

        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("ENTRYPOINT", null, null, null, null, null));

        assertThat(output.page()).isEqualTo(1);
    }

    @Test
    void should_not_touch_the_data_port_for_number_filter() {
        assertThatThrownBy(() ->
            useCase.execute(new GetObservabilityFilterValuesUseCase.Input("HTTP_STATUS", null, null, null, null, null))
        ).isInstanceOf(UnsupportedObservabilityFilterException.class);
        assertThat(dataPort.lastCall()).isEmpty();
    }

    @Test
    void should_not_touch_the_data_port_for_string_filter() {
        assertThatThrownBy(() ->
            useCase.execute(new GetObservabilityFilterValuesUseCase.Input("URI", null, null, null, null, null))
        ).isInstanceOf(UnsupportedObservabilityFilterException.class);
        assertThat(dataPort.lastCall()).isEmpty();
    }

    @Test
    void should_answer_400_for_a_keyword_filter_whose_values_the_store_cannot_list() {
        // NATIVE_TOPIC is a KEYWORD filter whose values live in the event-metrics stream the store does
        // not aggregate over: the catalog offers the filter, not its values, and says so before the port.
        assertThatThrownBy(() ->
            useCase.execute(new GetObservabilityFilterValuesUseCase.Input("NATIVE_TOPIC", null, null, null, null, null))
        )
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.filter.value_listing_not_supported");
        assertThat(dataPort.lastCall()).isEmpty();
    }

    @Test
    void should_restrict_api_type_enum_values_when_apiTypes_constraint_is_provided() {
        var output = useCase.execute(
            new GetObservabilityFilterValuesUseCase.Input("API_TYPE", null, null, null, null, null, Set.of(ApiType.MCP, ApiType.LLM))
        );

        assertThat(output.values().totalElements()).isEqualTo(2L);
        assertThat(output.values().data()).extracting(FilterValue::value).containsExactlyInAnyOrder("MCP", "LLM");
    }

    @Test
    void should_propagate_apiTypes_to_the_data_port_for_keyword_filters() {
        dataPort.givenKeywordValues("API", List.of(new FilterValue("api-1", "Petstore")));

        useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API", null, null, null, null, null, Set.of(ApiType.HTTP_PROXY)));

        assertThat(dataPort.lastCall()).hasValueSatisfying(call -> assertThat(call.apiTypes()).containsExactly(ApiType.HTTP_PROXY));
    }
}
