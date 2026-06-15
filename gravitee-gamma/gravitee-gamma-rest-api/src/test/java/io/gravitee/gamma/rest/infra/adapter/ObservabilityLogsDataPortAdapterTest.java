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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.logs.model.ApiReference;
import io.gravitee.gamma.rest.core.observability.logs.model.LogsSearchQuery;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityLogsDataPortAdapterTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";

    @Mock
    private ConnectionLogsCrudService connectionLogsCrudService;

    @Mock
    private UserContextLoader userContextLoader;

    @Mock
    private PlanCrudService planCrudService;

    @Mock
    private ApplicationCrudService applicationCrudService;

    @Mock
    private InstanceQueryService instanceQueryService;

    @Mock
    private ApiProductQueryService apiProductQueryService;

    private ObservabilityLogsDataPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ObservabilityLogsDataPortAdapter(
            connectionLogsCrudService,
            userContextLoader,
            planCrudService,
            applicationCrudService,
            instanceQueryService,
            apiProductQueryService
        );
    }

    @Test
    void should_reject_filter_not_translatable_to_log_search() {
        var query = queryWith(new FilterCondition("HTTP_STATUS_CODE_GROUP", FilterOperator.IN, List.of("2XX")));

        assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
            .isInstanceOf(UnsupportedObservabilityFilterException.class)
            .hasMessageContaining("HTTP_STATUS_CODE_GROUP");

        verifyNoInteractions(connectionLogsCrudService);
    }

    @Test
    void should_propagate_api_type_onto_log_rows() {
        when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(SearchLogsFilters.class), any(), any())).thenReturn(
            new SearchLogsResponse<>(1, List.of(BaseConnectionLog.builder().apiId("api-1").build()))
        );

        var page = adapter.searchLogs(ORG, ENV, queryWith());

        assertThat(page.data()).hasSize(1);
        assertThat(page.data().getFirst().apiType()).isEqualTo("HTTP_PROXY");
        assertThat(page.data().getFirst().apiName()).isEqualTo("API 1");
    }

    @Nested
    class HttpStatusFilter {

        @Test
        void should_translate_eq_to_single_status() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("200")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().statuses()).containsExactly(200);
        }

        @Test
        void should_reject_gte_until_range_translation_lands() {
            // Interim: range operators on HTTP_STATUS are rejected (400) until GMA-683 wires an ES range.
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("500")));

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("HTTP_STATUS");

            verifyNoInteractions(connectionLogsCrudService);
        }

        @Test
        void should_reject_lte_until_range_translation_lands() {
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.LTE, List.of("299")));

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .hasMessageContaining("HTTP_STATUS");

            verifyNoInteractions(connectionLogsCrudService);
        }

        @Test
        void should_reject_out_of_range_status_code() {
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("1000")));

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("1000");
        }

        @Test
        void should_reject_status_code_below_minimum() {
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("99")));

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("99");
        }

        @Test
        void should_reject_non_numeric_status_code() {
            var query = queryWith(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("abc")));

            assertThatThrownBy(() -> adapter.searchLogs(ORG, ENV, query))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("abc");
        }
    }

    @Nested
    class ResponseTimeFilter {

        @Test
        void should_translate_gte_to_response_time_from() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_GATEWAY_RESPONSE_TIME", FilterOperator.GTE, List.of("100")));

            adapter.searchLogs(ORG, ENV, query);

            var ranges = captureSearchFilters().responseTimeRanges();
            assertThat(ranges).hasSize(1);
            assertThat(ranges.getFirst().from()).isEqualTo(100L);
            assertThat(ranges.getFirst().to()).isNull();
        }

        @Test
        void should_translate_lte_to_response_time_to() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_GATEWAY_RESPONSE_TIME", FilterOperator.LTE, List.of("500")));

            adapter.searchLogs(ORG, ENV, query);

            var ranges = captureSearchFilters().responseTimeRanges();
            assertThat(ranges).hasSize(1);
            assertThat(ranges.getFirst().from()).isNull();
            assertThat(ranges.getFirst().to()).isEqualTo(500L);
        }

        @Test
        void should_translate_eq_to_exact_range() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_GATEWAY_RESPONSE_TIME", FilterOperator.EQ, List.of("250")));

            adapter.searchLogs(ORG, ENV, query);

            var ranges = captureSearchFilters().responseTimeRanges();
            assertThat(ranges).hasSize(1);
            assertThat(ranges.getFirst().from()).isEqualTo(250L);
            assertThat(ranges.getFirst().to()).isEqualTo(250L);
        }
    }

    @Nested
    class OtherFilters {

        @Test
        void should_translate_http_method() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_METHOD", FilterOperator.EQ, List.of("GET")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().methods()).containsExactly(HttpMethod.GET);
        }

        @Test
        void should_fallback_to_other_for_unknown_method() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("HTTP_METHOD", FilterOperator.EQ, List.of("UNKNOWN")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().methods()).containsExactly(HttpMethod.OTHER);
        }

        @Test
        void should_translate_application_filter() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("APPLICATION", FilterOperator.IN, List.of("app-1", "app-2")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().applicationIds()).containsExactlyInAnyOrder("app-1", "app-2");
        }

        @Test
        void should_translate_uri_filter() {
            stubEmptySearchResult();
            var query = queryWith(new FilterCondition("URI", FilterOperator.EQ, List.of("/api/v1/users")));

            adapter.searchLogs(ORG, ENV, query);

            assertThat(captureSearchFilters().uri()).isEqualTo("/api/v1/users");
        }
    }

    private LogsSearchQuery queryWith(FilterCondition... conditions) {
        return LogsSearchQuery.builder()
            .apiIds(Set.of("api-1"))
            .apisById(Map.of("api-1", new ApiReference("API 1", "HTTP_PROXY")))
            .conditions(List.of(conditions))
            .page(1)
            .perPage(20)
            .build();
    }

    private void stubEmptySearchResult() {
        when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(SearchLogsFilters.class), any(), any())).thenReturn(
            new SearchLogsResponse<>(0, List.of())
        );
    }

    private SearchLogsFilters captureSearchFilters() {
        var captor = ArgumentCaptor.forClass(SearchLogsFilters.class);
        verify(connectionLogsCrudService).searchApiConnectionLogs(any(), captor.capture(), any(), any());
        return captor.getValue();
    }
}
