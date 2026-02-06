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
package io.gravitee.apim.core.logs_engine.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.repository.ConnectionLogFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.core.logs_engine.model.ApiLogDiagnostic;
import io.gravitee.apim.core.logs_engine.model.ArrayFilter;
import io.gravitee.apim.core.logs_engine.model.Filter;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.logs_engine.model.NumericFilter;
import io.gravitee.apim.core.logs_engine.model.Operator;
import io.gravitee.apim.core.logs_engine.model.Pagination;
import io.gravitee.apim.core.logs_engine.model.SearchLogsRequest;
import io.gravitee.apim.core.logs_engine.model.StringFilter;
import io.gravitee.apim.core.logs_engine.model.TimeRange;
import io.gravitee.apim.core.logs_engine.use_case.SearchEnvironmentLogsUseCase.Input;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.rest.api.model.analytics.Range;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionDiagnosticModel;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchEnvironmentLogsUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo("org-id", "env-id", "user-id");
    private static final Api API1 = ApiFixtures.aProxyApiV4();
    private static final BaseConnectionLog LOG1 = new ConnectionLogFixtures(
        API1.getId(),
        "1",
        UUID.randomUUID().toString()
    ).aConnectionLog();

    private ConnectionLogsCrudService connectionLogsCrudService;
    private UserContextLoader userContextLoader;
    private SearchEnvironmentLogsUseCase useCase;

    @BeforeEach
    void setUp() {
        connectionLogsCrudService = mock(ConnectionLogsCrudService.class);
        userContextLoader = mock(UserContextLoader.class);
        useCase = new SearchEnvironmentLogsUseCase(connectionLogsCrudService, userContextLoader);
    }

    @Nested
    class Filters {

        @Test
        void should_build_filters_with_time_range() {
            var timeRange = new TimeRange(
                OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 1, 11, 0, 0, 0, ZoneOffset.UTC)
            );
            var request = new SearchLogsRequest(timeRange, null, 1, 10);

            var response = when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue())
                .extracting(SearchLogsFilters::from, SearchLogsFilters::to)
                .containsExactly(timeRange.from().toInstant().toEpochMilli(), timeRange.to().toInstant().toEpochMilli());

            assertThat(response.response().pagination().totalCount()).isEqualTo(1);
        }

        @Test
        void should_handle_null_filters_and_time_range() {
            var request = new SearchLogsRequest(null, null, 1, 10);

            var response = when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().from()).isNull();
            assertThat(filtersCaptor.getValue().to()).isNull();
            assertThat(filtersCaptor.getValue().apiIds()).containsExactly(API1.getId());

            assertThat(response.response().pagination().totalCount()).isEqualTo(1);
        }

        @Test
        void should_map_basic_string_and_array_filters() {
            var request = new SearchLogsRequest(
                null,
                List.of(
                    new Filter(new StringFilter(FilterName.API, Operator.EQ, API1.getId())),
                    new Filter(new ArrayFilter(FilterName.APPLICATION, Operator.IN, List.of("app-A", "app-B"))),
                    new Filter(new StringFilter(FilterName.ENTRYPOINT, Operator.EQ, "http")),
                    new Filter(new ArrayFilter(FilterName.PLAN, Operator.IN, List.of("premium"))),
                    new Filter(new StringFilter(FilterName.HTTP_STATUS, Operator.EQ, "200"))
                ),
                1,
                10
            );

            var response = when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            var filters = filtersCaptor.getValue();
            assertThat(filters.apiIds()).containsExactly(API1.getId());
            assertThat(filters.applicationIds()).containsExactlyInAnyOrder("app-A", "app-B");
            assertThat(filters.entrypointIds()).containsExactly("http");
            assertThat(filters.planIds()).containsExactly("premium");
            assertThat(filters.statuses()).containsExactlyInAnyOrder(200);

            assertThat(response.response().pagination().totalCount()).isEqualTo(1);
        }

        @Test
        void should_ignore_string_filter_with_wrong_operator() {
            // StringFilter only supports EQ
            var request = new SearchLogsRequest(
                null,
                List.of(new Filter(new StringFilter(FilterName.API, Operator.IN, API1.getId()))), // Should be ignored
                1,
                10
            );

            var response = when_searching(request);

            assertThat(response.response().pagination().totalCount()).isEqualTo(0);
        }

        @Test
        void should_ignore_array_filter_with_wrong_operator() {
            // ArrayFilter only supports IN
            var request = new SearchLogsRequest(
                null,
                List.of(new Filter(new ArrayFilter(FilterName.APPLICATION, Operator.EQ, List.of("app-1")))), // Should
                // be
                // ignored
                1,
                10
            );

            var response = when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().applicationIds()).isEmpty();

            assertThat(response.response().pagination().totalCount()).isEqualTo(1);
        }

        @ParameterizedTest
        @MethodSource("httpMethodFiltersProvider")
        void should_intersect_http_method_filters(List<Filter> filters, io.gravitee.common.http.HttpMethod[] expectedMethods) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().methods()).containsExactlyInAnyOrder(expectedMethods);
        }

        static Stream<Arguments> httpMethodFiltersProvider() {
            return Stream.of(
                // Intersection: EQ GET && IN [GET, POST] -> GET
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.HTTP_METHOD, Operator.EQ, "GET")),
                        new Filter(new ArrayFilter(FilterName.HTTP_METHOD, Operator.IN, List.of("POST", "GET")))
                    ),
                    new io.gravitee.common.http.HttpMethod[] { io.gravitee.common.http.HttpMethod.GET }
                ),
                // Intersection: IN [GET, POST] && IN [POST, PUT] -> POST
                Arguments.of(
                    List.of(
                        new Filter(new ArrayFilter(FilterName.HTTP_METHOD, Operator.IN, List.of("GET", "POST"))),
                        new Filter(new ArrayFilter(FilterName.HTTP_METHOD, Operator.IN, List.of("POST", "PUT")))
                    ),
                    new io.gravitee.common.http.HttpMethod[] { io.gravitee.common.http.HttpMethod.POST }
                ),
                // Intersection: EQ GET && EQ POST -> EMPTY
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.HTTP_METHOD, Operator.EQ, "GET")),
                        new Filter(new StringFilter(FilterName.HTTP_METHOD, Operator.EQ, "POST"))
                    ),
                    new io.gravitee.common.http.HttpMethod[] {}
                ),
                // Intersection: IN [GET, POST] && EQ PUT -> EMPTY
                Arguments.of(
                    List.of(
                        new Filter(new ArrayFilter(FilterName.HTTP_METHOD, Operator.IN, List.of("GET", "POST"))),
                        new Filter(new StringFilter(FilterName.HTTP_METHOD, Operator.EQ, "PUT"))
                    ),
                    new io.gravitee.common.http.HttpMethod[] {}
                ),
                // No intersection logic needed for single filter
                Arguments.of(
                    List.of(new Filter(new ArrayFilter(FilterName.HTTP_METHOD, Operator.IN, List.of("GET", "POST", "PUT")))),
                    new io.gravitee.common.http.HttpMethod[] {
                        io.gravitee.common.http.HttpMethod.GET,
                        io.gravitee.common.http.HttpMethod.POST,
                        io.gravitee.common.http.HttpMethod.PUT,
                    }
                ),
                // Invalid Method mapping
                Arguments.of(
                    List.of(new Filter(new StringFilter(FilterName.HTTP_METHOD, Operator.EQ, "INVALID_VERB"))),
                    new io.gravitee.common.http.HttpMethod[] { io.gravitee.common.http.HttpMethod.OTHER }
                )
            );
        }

        @ParameterizedTest
        @MethodSource("apiFiltersProvider")
        void should_intersect_api_filters(List<Filter> filters, String[] expectedApiIds) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().apiIds()).containsExactlyInAnyOrder(expectedApiIds);
        }

        static Stream<Arguments> apiFiltersProvider() {
            return Stream.of(
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.API, Operator.EQ, API1.getId())),
                        new Filter(new ArrayFilter(FilterName.API, Operator.IN, List.of(API1.getId(), "api-2")))
                    ),
                    new String[] { API1.getId() }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new ArrayFilter(FilterName.API, Operator.IN, List.of(API1.getId(), "api-2"))),
                        new Filter(new ArrayFilter(FilterName.API, Operator.IN, List.of(API1.getId(), "api-3")))
                    ),
                    new String[] { API1.getId() }
                ),
                Arguments.of(
                    List.of(new Filter(new ArrayFilter(FilterName.API, Operator.IN, List.of(API1.getId(), "api-2", "api-3")))),
                    new String[] { API1.getId() }
                )
            );
        }

        @ParameterizedTest
        @MethodSource("applicationFiltersProvider")
        void should_intersect_application_filters(List<Filter> filters, String[] expectedAppIds) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().applicationIds()).containsExactlyInAnyOrder(expectedAppIds);
        }

        static Stream<Arguments> applicationFiltersProvider() {
            return Stream.of(
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.APPLICATION, Operator.EQ, "app-1")),
                        new Filter(new ArrayFilter(FilterName.APPLICATION, Operator.IN, List.of("app-1", "app-2")))
                    ),
                    new String[] { "app-1" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new ArrayFilter(FilterName.APPLICATION, Operator.IN, List.of("app-1", "app-2"))),
                        new Filter(new ArrayFilter(FilterName.APPLICATION, Operator.IN, List.of("app-2", "app-3")))
                    ),
                    new String[] { "app-2" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.APPLICATION, Operator.EQ, "app-1")),
                        new Filter(new StringFilter(FilterName.APPLICATION, Operator.EQ, "app-2"))
                    ),
                    new String[] {}
                )
            );
        }

        @ParameterizedTest
        @MethodSource("planFiltersProvider")
        void should_intersect_plan_filters(List<Filter> filters, String[] expectedPlanIds) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().planIds()).containsExactlyInAnyOrder(expectedPlanIds);
        }

        static Stream<Arguments> planFiltersProvider() {
            return Stream.of(
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.PLAN, Operator.EQ, "plan-1")),
                        new Filter(new ArrayFilter(FilterName.PLAN, Operator.IN, List.of("plan-1", "plan-2")))
                    ),
                    new String[] { "plan-1" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new ArrayFilter(FilterName.PLAN, Operator.IN, List.of("plan-1", "plan-2"))),
                        new Filter(new ArrayFilter(FilterName.PLAN, Operator.IN, List.of("plan-2", "plan-3")))
                    ),
                    new String[] { "plan-2" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.PLAN, Operator.EQ, "plan-1")),
                        new Filter(new StringFilter(FilterName.PLAN, Operator.EQ, "plan-2"))
                    ),
                    new String[] {}
                )
            );
        }

        @ParameterizedTest
        @MethodSource("entrypointFiltersProvider")
        void should_intersect_entrypoint_filters(List<Filter> filters, String[] expectedEntrypointIds) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().entrypointIds()).containsExactlyInAnyOrder(expectedEntrypointIds);
        }

        static Stream<Arguments> entrypointFiltersProvider() {
            return Stream.of(
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.ENTRYPOINT, Operator.EQ, "entrypoint-1")),
                        new Filter(new ArrayFilter(FilterName.ENTRYPOINT, Operator.IN, List.of("entrypoint-1", "entrypoint-2")))
                    ),
                    new String[] { "entrypoint-1" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new ArrayFilter(FilterName.ENTRYPOINT, Operator.IN, List.of("entrypoint-1", "entrypoint-2"))),
                        new Filter(new ArrayFilter(FilterName.ENTRYPOINT, Operator.IN, List.of("entrypoint-2", "entrypoint-3")))
                    ),
                    new String[] { "entrypoint-2" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.ENTRYPOINT, Operator.EQ, "entrypoint-1")),
                        new Filter(new StringFilter(FilterName.ENTRYPOINT, Operator.EQ, "entrypoint-2"))
                    ),
                    new String[] {}
                )
            );
        }

        @ParameterizedTest
        @MethodSource("httpStatusFiltersProvider")
        void should_intersect_http_status_filters(List<Filter> filters, Integer[] expectedStatuses) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().statuses()).containsExactlyInAnyOrder(expectedStatuses);
        }

        static Stream<Arguments> httpStatusFiltersProvider() {
            return Stream.of(
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.HTTP_STATUS, Operator.EQ, "200")),
                        new Filter(new ArrayFilter(FilterName.HTTP_STATUS, Operator.IN, List.of("200", "201")))
                    ),
                    new Integer[] { 200 }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new ArrayFilter(FilterName.HTTP_STATUS, Operator.IN, List.of("200", "201"))),
                        new Filter(new ArrayFilter(FilterName.HTTP_STATUS, Operator.IN, List.of("201", "204")))
                    ),
                    new Integer[] { 201 }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.HTTP_STATUS, Operator.EQ, "200")),
                        new Filter(new StringFilter(FilterName.HTTP_STATUS, Operator.EQ, "400"))
                    ),
                    new Integer[] {}
                )
            );
        }

        @ParameterizedTest
        @MethodSource("mcpMethodFiltersProvider")
        void should_intersect_mcp_method_filters(List<Filter> filters, String[] expectedMcpMethods) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().mcpMethods()).containsExactlyInAnyOrder(expectedMcpMethods);
        }

        static Stream<Arguments> mcpMethodFiltersProvider() {
            return Stream.of(
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.MCP_METHOD, Operator.EQ, "initialize")),
                        new Filter(new ArrayFilter(FilterName.MCP_METHOD, Operator.IN, List.of("initialize", "tools/list")))
                    ),
                    new String[] { "initialize" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new ArrayFilter(FilterName.MCP_METHOD, Operator.IN, List.of("initialize", "tools/list"))),
                        new Filter(new ArrayFilter(FilterName.MCP_METHOD, Operator.IN, List.of("tools/list", "resources/read")))
                    ),
                    new String[] { "tools/list" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.MCP_METHOD, Operator.EQ, "initialize")),
                        new Filter(new StringFilter(FilterName.MCP_METHOD, Operator.EQ, "ping"))
                    ),
                    new String[] {}
                ),
                Arguments.of(
                    List.of(new Filter(new ArrayFilter(FilterName.MCP_METHOD, Operator.IN, List.of("initialize", "ping", "tools/list")))),
                    new String[] { "initialize", "ping", "tools/list" }
                )
            );
        }

        @ParameterizedTest
        @MethodSource("transactionIdFiltersProvider")
        void should_intersect_transaction_id_filters(List<Filter> filters, String[] expectedTransactionIds) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().transactionIds()).containsExactlyInAnyOrder(expectedTransactionIds);
        }

        static Stream<Arguments> transactionIdFiltersProvider() {
            return Stream.of(
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.TRANSACTION_ID, Operator.EQ, "txn-123")),
                        new Filter(new ArrayFilter(FilterName.TRANSACTION_ID, Operator.IN, List.of("txn-123", "txn-456")))
                    ),
                    new String[] { "txn-123" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new ArrayFilter(FilterName.TRANSACTION_ID, Operator.IN, List.of("txn-123", "txn-456"))),
                        new Filter(new ArrayFilter(FilterName.TRANSACTION_ID, Operator.IN, List.of("txn-456", "txn-789")))
                    ),
                    new String[] { "txn-456" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.TRANSACTION_ID, Operator.EQ, "txn-123")),
                        new Filter(new StringFilter(FilterName.TRANSACTION_ID, Operator.EQ, "txn-456"))
                    ),
                    new String[] {}
                ),
                Arguments.of(
                    List.of(new Filter(new ArrayFilter(FilterName.TRANSACTION_ID, Operator.IN, List.of("txn-123", "txn-456", "txn-789")))),
                    new String[] { "txn-123", "txn-456", "txn-789" }
                )
            );
        }

        @ParameterizedTest
        @MethodSource("requestIdFiltersProvider")
        void should_intersect_request_id_filters(List<Filter> filters, String[] expectedRequestIds) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().requestIds()).containsExactlyInAnyOrder(expectedRequestIds);
        }

        static Stream<Arguments> requestIdFiltersProvider() {
            return Stream.of(
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.REQUEST_ID, Operator.EQ, "req-123")),
                        new Filter(new ArrayFilter(FilterName.REQUEST_ID, Operator.IN, List.of("req-123", "req-456")))
                    ),
                    new String[] { "req-123" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new ArrayFilter(FilterName.REQUEST_ID, Operator.IN, List.of("req-123", "req-456"))),
                        new Filter(new ArrayFilter(FilterName.REQUEST_ID, Operator.IN, List.of("req-456", "req-789")))
                    ),
                    new String[] { "req-456" }
                ),
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.REQUEST_ID, Operator.EQ, "req-123")),
                        new Filter(new StringFilter(FilterName.REQUEST_ID, Operator.EQ, "req-456"))
                    ),
                    new String[] {}
                ),
                Arguments.of(
                    List.of(new Filter(new ArrayFilter(FilterName.REQUEST_ID, Operator.IN, List.of("req-123", "req-456", "req-789")))),
                    new String[] { "req-123", "req-456", "req-789" }
                )
            );
        }

        @ParameterizedTest
        @MethodSource("uriFiltersProvider")
        void should_map_uri_filter(List<Filter> filters, String expectedUri) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            if (expectedUri == null) {
                assertThat(filtersCaptor.getValue().uri()).isNull();
            } else {
                assertThat(filtersCaptor.getValue().uri()).isEqualTo(expectedUri);
            }
        }

        static Stream<Arguments> uriFiltersProvider() {
            return Stream.of(
                Arguments.of(List.of(new Filter(new StringFilter(FilterName.URI, Operator.EQ, "/api/users"))), "/api/users"),
                Arguments.of(
                    List.of(
                        new Filter(new StringFilter(FilterName.URI, Operator.EQ, "/api/users")),
                        new Filter(new StringFilter(FilterName.URI, Operator.EQ, "/api/products"))
                    ),
                    "/api/products" // Last EQ wins
                )
            );
        }

        @ParameterizedTest
        @MethodSource("responseTimeRangeFiltersProvider")
        void should_map_response_time_range_filters(List<Filter> filters, List<Range> expectedRanges) {
            var request = new SearchLogsRequest(null, filters, 1, 10);

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().responseTimeRanges()).isEqualTo(expectedRanges);
        }

        static Stream<Arguments> responseTimeRangeFiltersProvider() {
            return Stream.of(
                // Single GTE filter -> range with from only
                Arguments.of(
                    List.of(new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.GTE, 100))),
                    List.of(new Range(100L, null))
                ),
                // Single LTE filter -> range with to only
                Arguments.of(
                    List.of(new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.LTE, 500))),
                    List.of(new Range(null, 500L))
                ),
                // GTE + LTE -> range with both bounds
                Arguments.of(
                    List.of(
                        new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.GTE, 100)),
                        new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.LTE, 500))
                    ),
                    List.of(new Range(100L, 500L))
                ),
                // Overlapping GTE: GTE 100 then GTE 200 -> last wins (200)
                Arguments.of(
                    List.of(
                        new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.GTE, 100)),
                        new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.GTE, 200))
                    ),
                    List.of(new Range(200L, null))
                ),
                // Overlapping LTE: LTE 500 then LTE 300 -> last wins (300)
                Arguments.of(
                    List.of(
                        new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.LTE, 500)),
                        new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.LTE, 300))
                    ),
                    List.of(new Range(null, 300L))
                ),
                // Overlapping GTE + LTE combined: GTE 100, GTE 200, LTE 500 -> last GTE wins
                Arguments.of(
                    List.of(
                        new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.GTE, 100)),
                        new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.GTE, 200)),
                        new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.LTE, 500))
                    ),
                    List.of(new Range(200L, 500L))
                ),
                // No response time filters -> empty list
                Arguments.of(List.of(new Filter(new StringFilter(FilterName.API, Operator.EQ, API1.getId()))), List.of())
            );
        }

        @Test
        void should_ignore_numeric_filter_with_unsupported_operator() {
            var request = new SearchLogsRequest(
                null,
                List.of(
                    new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.EQ, 100)),
                    new Filter(new NumericFilter(FilterName.RESPONSE_TIME_RANGE, Operator.IN, 200))
                ),
                1,
                10
            );

            when_searching(request);

            var filtersCaptor = ArgumentCaptor.forClass(SearchLogsFilters.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), filtersCaptor.capture(), any(), any());

            assertThat(filtersCaptor.getValue().responseTimeRanges()).isEmpty();
        }
    }

    @Nested
    class PaginationTest {

        @Test
        void should_use_default_pagination_when_not_provided() {
            var request = new SearchLogsRequest(null, null, null, null);

            when_searching(request);

            var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), any(), pageableCaptor.capture(), any());

            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }

        @Test
        void should_use_provided_pagination() {
            var request = new SearchLogsRequest(null, null, 5, 50);

            when_searching(request);

            var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(any(), any(), pageableCaptor.capture(), any());

            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(5);
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        }
    }

    @Nested
    class ExecutionContextTest {

        @Test
        void should_use_audit_info_for_execution_context() {
            var request = new SearchLogsRequest(null, null, null, null);

            when_searching(request);

            var ctxCaptor = ArgumentCaptor.forClass(ExecutionContext.class);
            verify(connectionLogsCrudService).searchApiConnectionLogs(ctxCaptor.capture(), any(), any(), any());

            assertThat(ctxCaptor.getValue().getOrganizationId()).isEqualTo("org-id");
            assertThat(ctxCaptor.getValue().getEnvironmentId()).isEqualTo("env-id");
        }
    }

    @Nested
    class ResultMapping {

        @Test
        void should_return_empty_response_when_service_returns_no_logs() {
            when(userContextLoader.loadApis(any())).thenReturn(
                new UserContext(AUDIT_INFO, Optional.empty(), Optional.empty(), Optional.of(List.of(API1)))
            );
            when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(), any(), any())).thenReturn(
                new io.gravitee.rest.api.model.v4.log.SearchLogsResponse<>(0, List.of())
            );

            var response = useCase.execute(new Input(AUDIT_INFO, new SearchLogsRequest(null, null, 1, 10))).response();

            assertThat(response.data()).isEmpty();
            assertThat(response.pagination())
                .extracting(
                    Pagination::page,
                    Pagination::perPage,
                    Pagination::pageCount,
                    Pagination::pageItemsCount,
                    Pagination::totalCount
                )
                .containsExactly(1, 10, 0, 0, 0L);
        }

        @Test
        void should_map_response_payload_fully() {
            var timestamp = OffsetDateTime.parse("2024-01-01T10:00:00Z");
            var connectionLog = BaseConnectionLog.builder()
                .timestamp(timestamp.toString())
                .requestId("req-id")
                .clientIdentifier("client-id")
                .method(HttpMethod.GET)
                .status(200)
                .requestEnded(true)
                .transactionId("tx-id")
                .planId("plan-id")
                .applicationId("app-id")
                .uri("/uri")
                .endpoint("http://backend")
                .gatewayResponseTime(100L)
                .message("error message")
                .errorKey("error-key")
                .errorComponentName("error-component")
                .errorComponentType("error-type")
                .warnings(List.of(new ConnectionDiagnosticModel("type", "name", "key", "msg")))
                .additionalMetrics(Map.of("custom", "metric"))
                .build();

            when(userContextLoader.loadApis(any())).thenReturn(
                new UserContext(AUDIT_INFO, Optional.empty(), Optional.empty(), Optional.of(List.of(API1)))
            );
            when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(), any(), any())).thenReturn(
                new io.gravitee.rest.api.model.v4.log.SearchLogsResponse<>(1, List.of(connectionLog))
            );

            var response = useCase.execute(new Input(AUDIT_INFO, new SearchLogsRequest(null, null, 1, 10))).response();

            assertThat(response.data()).hasSize(1);
            var log = response.data().getFirst();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(log.requestId()).isEqualTo("req-id");
                soft.assertThat(log.timestamp()).isEqualTo(timestamp);
                soft.assertThat(log.clientIdentifier()).isEqualTo("client-id");
                soft.assertThat(log.method()).isEqualTo(io.gravitee.apim.core.logs_engine.model.HttpMethod.GET);
                soft.assertThat(log.status()).isEqualTo(200);
                soft.assertThat(log.requestEnded()).isTrue();
                soft.assertThat(log.transactionId()).isEqualTo("tx-id");
                soft.assertThat(log.plan().id()).isEqualTo("plan-id");
                soft.assertThat(log.application().id()).isEqualTo("app-id");
                soft.assertThat(log.uri()).isEqualTo("/uri");
                soft.assertThat(log.endpoint()).isEqualTo("http://backend");
                soft.assertThat(log.gatewayResponseTime()).isEqualTo(100);
                soft.assertThat(log.message()).isEqualTo("error message");
                soft.assertThat(log.errorKey()).isEqualTo("error-key");
                soft.assertThat(log.errorComponentName()).isEqualTo("error-component");
                soft.assertThat(log.errorComponentType()).isEqualTo("error-type");

                soft.assertThat(log.warnings()).containsExactly(new ApiLogDiagnostic("type", "name", "key", "msg"));

                soft.assertThat(log.additionalMetrics()).containsEntry("custom", "metric");
            });
        }

        @Test
        void should_cap_gateway_response_time_to_integer_max() {
            var log = BaseConnectionLog.builder().timestamp(OffsetDateTime.now().toString()).gatewayResponseTime(Long.MAX_VALUE).build();

            when(userContextLoader.loadApis(any())).thenReturn(
                new UserContext(AUDIT_INFO, Optional.empty(), Optional.empty(), Optional.of(List.of(API1)))
            );
            when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(), any(), any())).thenReturn(
                new io.gravitee.rest.api.model.v4.log.SearchLogsResponse<>(1, List.of(log))
            );

            var response = useCase.execute(new Input(AUDIT_INFO, new SearchLogsRequest(null, null, 1, 10))).response();

            assertThat(response.data()).hasSize(1);
            assertThat(response.data().getFirst().gatewayResponseTime()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        void should_handle_null_fields_in_mapping() {
            var log = BaseConnectionLog.builder()
                .timestamp(null)
                .planId(null)
                .applicationId(null)
                .method(null)
                .warnings(null)
                .additionalMetrics(null)
                .build();

            when(userContextLoader.loadApis(any())).thenReturn(
                new UserContext(AUDIT_INFO, Optional.empty(), Optional.empty(), Optional.of(List.of(API1)))
            );
            when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(), any(), any())).thenReturn(
                new io.gravitee.rest.api.model.v4.log.SearchLogsResponse<>(1, List.of(log))
            );

            var response = useCase.execute(new Input(AUDIT_INFO, new SearchLogsRequest(null, null, 1, 10))).response();

            var apiLog = response.data().getFirst();
            assertThat(apiLog.timestamp()).isNull();
            assertThat(apiLog.plan()).isNull();
            assertThat(apiLog.application()).isNull();
            assertThat(apiLog.method()).isNull();
            assertThat(apiLog.warnings()).isEmpty();
            assertThat(apiLog.additionalMetrics()).isEmpty();
        }

        @Test
        void should_calculate_pagination_metadata_correctly() {
            // Case 1: Total 11, PerPage 10 -> 2 pages
            when(userContextLoader.loadApis(any())).thenReturn(
                new UserContext(AUDIT_INFO, Optional.empty(), Optional.empty(), Optional.of(List.of(API1)))
            );
            when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(), any(), any())).thenReturn(
                new io.gravitee.rest.api.model.v4.log.SearchLogsResponse<>(11, Collections.emptyList())
            );

            var result = useCase.execute(new Input(AUDIT_INFO, new SearchLogsRequest(null, null, 1, 10))).response();

            assertThat(result.pagination().pageCount()).isEqualTo(2);
            assertThat(result.pagination().totalCount()).isEqualTo(11);

            // Case 2: Total 10, PerPage 10 -> 1 page
            when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(), any(), any())).thenReturn(
                new io.gravitee.rest.api.model.v4.log.SearchLogsResponse<>(10, Collections.emptyList())
            );

            result = useCase.execute(new Input(AUDIT_INFO, new SearchLogsRequest(null, null, 1, 10))).response();
            assertThat(result.pagination().pageCount()).isEqualTo(1);
        }
    }

    private SearchEnvironmentLogsUseCase.Output when_searching(SearchLogsRequest request) {
        when(connectionLogsCrudService.searchApiConnectionLogs(any(), any(), any(), any())).thenReturn(
            new io.gravitee.rest.api.model.v4.log.SearchLogsResponse<>(1, List.of(LOG1))
        );
        when(userContextLoader.loadApis(any())).thenReturn(
            new UserContext(AUDIT_INFO, Optional.empty(), Optional.empty(), Optional.of(List.of(API1)))
        );
        return useCase.execute(new Input(AUDIT_INFO, request));
    }
}
