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
package io.gravitee.apim.infra.query_service.analytics_engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.TrafficScope;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesQuery;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class FilterValuesQueryServiceImplTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private FilterValuesQueryServiceImpl service;

    @Captor
    private ArgumentCaptor<FilterValuesQuery> queryCaptor;

    @Test
    void should_make_single_es_call_for_page_one() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(
            new FilterValuesResult(List.of("gw-1", "gw-2"), Map.of("value", "gw-2"), 42)
        );

        var result = service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, null, null, 1, 10, null, null, null);

        verify(analyticsRepository, times(1)).searchFilterValues(any(), queryCaptor.capture());
        var capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery.afterKey()).isNull();
        assertThat(capturedQuery.esFieldName()).isEqualTo("gateway");
        assertThat(capturedQuery.size()).isEqualTo(10);

        assertThat(result.data())
            .extracting(fv -> fv.value())
            .containsExactly("gw-1", "gw-2");
        assertThat(result.afterKey()).containsEntry("value", "gw-2");
        assertThat(result.totalFilteredCount()).isEqualTo(42L);
    }

    @Test
    void should_iterate_composite_pages_to_reach_requested_page() {
        var page1AfterKey = Map.<String, Object>of("value", "gw-10");
        when(analyticsRepository.searchFilterValues(any(), argThat(q -> q != null && q.afterKey() == null))).thenReturn(
            new FilterValuesResult(List.of("gw-1", "gw-2"), page1AfterKey, 25)
        );
        when(
            analyticsRepository.searchFilterValues(
                any(),
                argThat(q -> q != null && q.afterKey() != null && q.afterKey().equals(page1AfterKey))
            )
        ).thenReturn(new FilterValuesResult(List.of("gw-11", "gw-12"), Map.of("value", "gw-20"), 25));

        var result = service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, null, null, 2, 10, null, null, null);

        verify(analyticsRepository, times(2)).searchFilterValues(any(), queryCaptor.capture());
        var queries = queryCaptor.getAllValues();
        assertThat(queries.get(0).afterKey()).isNull();
        assertThat(queries.get(1).afterKey()).isEqualTo(page1AfterKey);

        assertThat(result.data())
            .extracting(fv -> fv.value())
            .containsExactly("gw-11", "gw-12");
        assertThat(result.totalFilteredCount()).isEqualTo(25L);
    }

    @Test
    void should_return_empty_page_when_data_exhausted_before_target_page() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("gw-1"), null, 1));

        var result = service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, null, null, 3, 10, null, null, null);

        verify(analyticsRepository, times(1)).searchFilterValues(any(), any());
        assertThat(result.data()).isEmpty();
        assertThat(result.totalFilteredCount()).isEqualTo(1L);
    }

    @Test
    void should_return_empty_page_when_after_key_is_empty_map_before_target_page() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("gw-1"), Map.of(), 5));

        var result = service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, null, null, 2, 10, null, null, null);

        verify(analyticsRepository, times(1)).searchFilterValues(any(), any());
        assertThat(result.data()).isEmpty();
        assertThat(result.totalFilteredCount()).isEqualTo(5L);
    }

    @Test
    void should_propagate_total_count_from_first_es_response() {
        var page1AfterKey = Map.<String, Object>of("value", "gw-5");
        when(analyticsRepository.searchFilterValues(any(), argThat(q -> q != null && q.afterKey() == null))).thenReturn(
            new FilterValuesResult(List.of("gw-1"), page1AfterKey, 100)
        );
        when(analyticsRepository.searchFilterValues(any(), argThat(q -> q != null && q.afterKey() != null))).thenReturn(
            new FilterValuesResult(List.of("gw-6"), null, 100)
        );

        var result = service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, null, null, 2, 5, null, null, null);

        assertThat(result.totalFilteredCount()).isEqualTo(100L);
    }

    @Test
    void should_pass_time_range_to_es_query() {
        var from = Instant.parse("2025-01-01T00:00:00Z");
        var to = Instant.parse("2025-12-31T23:59:59Z");

        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("gw-1"), null, 1));

        service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, from, to, 1, 10, null, null, null);

        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        var capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery.from()).isEqualTo(from.toEpochMilli());
        assertThat(capturedQuery.to()).isEqualTo(to.toEpochMilli());
    }

    @Test
    void should_pass_search_pattern_to_es_query() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("gw-prod"), null, 1));

        service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, null, null, 1, 10, null, "prod", null);

        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().searchPattern()).isEqualTo("prod");
    }

    @Test
    void should_resolve_es_field_name_for_api_filter() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("api-1"), null, 1));

        service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.API, null, null, 1, 10, null, null, null);

        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().esFieldName()).isEqualTo("api-id");
    }

    @Test
    void should_resolve_es_field_name_for_api_product_filter() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("product-1"), null, 1));

        service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.API_PRODUCT, null, null, 1, 10, null, null, null);

        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().esFieldName()).isEqualTo("api-product-id");
    }

    @Test
    void should_pass_authorized_api_ids_to_es_query() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("gw-1"), null, 1));

        var apiIds = Set.of("api-1", "api-2");
        service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, null, null, 1, 10, null, null, apiIds);

        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().apiIds()).containsExactlyInAnyOrder("api-1", "api-2");
    }

    @Test
    void should_pass_the_traffic_scope_to_es_query() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("gw-1"), null, 1));

        service.searchFilterValues(
            ORG_ID,
            ENV_ID,
            FilterSpec.Name.GATEWAY,
            null,
            null,
            1,
            10,
            null,
            null,
            Set.of("studio", "orders"),
            new TrafficScope(Set.of("studio"), Set.of("mcp"))
        );

        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().trafficScope()).isEqualTo(
            new io.gravitee.repository.analytics.engine.api.query.TrafficScope(Set.of("studio"), Set.of("mcp"))
        );
    }

    @Test
    void should_leave_api_ids_null_on_query_when_authorized_api_ids_not_provided() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("gw-1"), null, 1));

        service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, null, null, 1, 10, null, null, null);

        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().apiIds()).isNull();
    }

    @Test
    void should_keep_only_the_values_found_in_the_calls_of_the_authorized_apis() {
        var from = Instant.parse("2026-01-01T00:00:00Z");
        var to = Instant.parse("2026-01-02T00:00:00Z");
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("app-1"), null, 1));

        var found = service.findValuesInCalls(
            ORG_ID,
            ENV_ID,
            FilterSpec.Name.APPLICATION,
            from,
            to,
            Set.of("app-1", "app-2"),
            Set.of("api-1")
        );

        assertThat(found).containsExactly("app-1");
        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        var query = queryCaptor.getValue();
        assertThat(query.esFieldName()).isEqualTo("application-id");
        assertThat(query.values()).containsExactlyInAnyOrder("app-1", "app-2");
        assertThat(query.apiIds()).containsExactly("api-1");
        assertThat(query.size()).isEqualTo(2);
        assertThat(query.from()).isEqualTo(from.toEpochMilli());
        assertThat(query.to()).isEqualTo(to.toEpochMilli());
    }

    @Test
    void should_keep_the_calls_made_through_the_api_product_only() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("app-1"), null, 1));

        service.findValuesInCalls(ORG_ID, ENV_ID, FilterSpec.Name.APPLICATION, null, null, Set.of("app-1"), Set.of("api-1"), "product-1");
        service.searchFilterValues(
            ORG_ID,
            ENV_ID,
            FilterSpec.Name.APPLICATION,
            null,
            null,
            1,
            10,
            null,
            null,
            Set.of("api-1"),
            null,
            "product-1"
        );

        verify(analyticsRepository, times(2)).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getAllValues()).extracting(FilterValuesQuery::apiProductId).containsExactly("product-1", "product-1");
    }

    @Test
    void should_split_many_values_into_bounded_queries_and_merge_what_they_find() {
        var values = IntStream.range(0, 2_500)
            .mapToObj(i -> "app-" + i)
            .collect(Collectors.toSet());
        when(analyticsRepository.searchFilterValues(any(), any())).thenAnswer(invocation -> {
            FilterValuesQuery query = invocation.getArgument(1);
            var inCalls = query
                .values()
                .stream()
                .filter(value -> value.endsWith("7"))
                .toList();
            return new FilterValuesResult(inCalls, null, inCalls.size());
        });

        var found = service.findValuesInCalls(ORG_ID, ENV_ID, FilterSpec.Name.APPLICATION, null, null, values, Set.of("api-1"));

        assertThat(found)
            .hasSize(250)
            .allSatisfy(value -> assertThat(value).endsWith("7"));
        verify(analyticsRepository, times(3)).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getAllValues()).allSatisfy(query -> {
            assertThat(query.values()).hasSizeLessThanOrEqualTo(1_000);
            assertThat(query.size()).isEqualTo(query.values().size());
        });
        assertThat(
            queryCaptor
                .getAllValues()
                .stream()
                .flatMap(query -> query.values().stream())
        ).containsExactlyInAnyOrderElementsOf(values);
    }

    @Test
    void should_find_no_value_without_querying_the_calls_when_no_api_is_authorized() {
        var found = service.findValuesInCalls(ORG_ID, ENV_ID, FilterSpec.Name.APPLICATION, null, null, Set.of("app-1"), Set.of());

        assertThat(found).isEmpty();
        verifyNoInteractions(analyticsRepository);
    }

    @Test
    void should_find_no_value_without_querying_the_calls_when_none_is_asked() {
        var found = service.findValuesInCalls(ORG_ID, ENV_ID, FilterSpec.Name.APPLICATION, null, null, Set.of(), Set.of("api-1"));

        assertThat(found).isEmpty();
        verifyNoInteractions(analyticsRepository);
    }

    @ParameterizedTest
    @MethodSource("filterToEsFieldMappings")
    void should_resolve_es_field_name(FilterSpec.Name filterName, String expectedEsField) {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("v1"), null, 1));

        service.searchFilterValues(ORG_ID, ENV_ID, filterName, null, null, 1, 10, null, null, null);

        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().esFieldName()).isEqualTo(expectedEsField);
    }

    static Stream<Arguments> filterToEsFieldMappings() {
        return Stream.of(
            Arguments.of(FilterSpec.Name.EDGE_PROVIDER, "additional-metrics.keyword_edge_provider"),
            Arguments.of(FilterSpec.Name.EDGE_PROCESS, "additional-metrics.keyword_edge_process"),
            Arguments.of(FilterSpec.Name.EDGE_CLIENT, "client-identifier"),
            Arguments.of(FilterSpec.Name.EDGE_TYPE, "additional-metrics.keyword_edge_type"),
            Arguments.of(FilterSpec.Name.MESSAGE_CONNECTOR_TYPE, "connector-type"),
            Arguments.of(FilterSpec.Name.ENTRYPOINT, "entrypoint-id"),
            Arguments.of(FilterSpec.Name.ERROR_KEY, "error-key"),
            Arguments.of(FilterSpec.Name.REQUEST_ID, "request-id"),
            Arguments.of(FilterSpec.Name.TRANSACTION_ID, "transaction-id"),
            // The field the analytics engine filters on (HTTPFieldResolver): `path` is declared but never written.
            Arguments.of(FilterSpec.Name.HTTP_PATH, "path-info.keyword"),
            Arguments.of(FilterSpec.Name.URI, "uri")
        );
    }
}
