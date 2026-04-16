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
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesQuery;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    void should_pass_authorized_api_ids_to_es_query() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("gw-1"), null, 1));

        var apiIds = Set.of("api-1", "api-2");
        service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, null, null, 1, 10, null, null, apiIds);

        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().apiIds()).containsExactlyInAnyOrder("api-1", "api-2");
    }

    @Test
    void should_leave_api_ids_null_on_query_when_authorized_api_ids_not_provided() {
        when(analyticsRepository.searchFilterValues(any(), any())).thenReturn(new FilterValuesResult(List.of("gw-1"), null, 1));

        service.searchFilterValues(ORG_ID, ENV_ID, FilterSpec.Name.GATEWAY, null, null, 1, 10, null, null, null);

        verify(analyticsRepository).searchFilterValues(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().apiIds()).isNull();
    }
}
