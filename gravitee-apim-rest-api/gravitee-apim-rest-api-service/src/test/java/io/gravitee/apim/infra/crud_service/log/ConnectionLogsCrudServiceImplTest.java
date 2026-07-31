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
package io.gravitee.apim.infra.crud_service.log;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConnectionLogsCrudServiceImplTest {

    LogRepository logRepository;

    ConnectionLogsCrudServiceImpl logCrudService;
    MetricsRepository metricsRepository;

    @BeforeEach
    void setUp() {
        logRepository = mock(LogRepository.class);
        metricsRepository = mock(MetricsRepository.class);
        logCrudService = new ConnectionLogsCrudServiceImpl(logRepository, metricsRepository);
    }

    @Nested
    class SearchApiConnectionLogDetail {

        @Test
        void should_search_api_connection_log_detail() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetail(any(QueryContext.class), any())).thenReturn(
                Optional.of(ConnectionLogDetail.builder().build())
            );

            logCrudService.searchApiConnectionLog(GraviteeContext.getExecutionContext(), "apiId", "requestId");

            var captorQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorConnectionLogDetailQuery = ArgumentCaptor.forClass(ConnectionLogDetailQuery.class);
            verify(logRepository).searchConnectionLogDetail(captorQueryContext.capture(), captorConnectionLogDetailQuery.capture());

            final QueryContext queryContext = captorQueryContext.getValue();
            assertThat(queryContext.getOrgId()).isEqualTo("DEFAULT");
            assertThat(queryContext.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorConnectionLogDetailQuery.getValue()).isEqualTo(
                ConnectionLogDetailQuery.builder()
                    .filter(ConnectionLogDetailQuery.Filter.builder().apiIds(Set.of("apiId")).requestIds(Set.of("requestId")).build())
                    .build()
            );
        }

        @Test
        void should_return_empty_api_connection_log_detail() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetail(any(QueryContext.class), any())).thenReturn(Optional.empty());

            assertThat(logCrudService.searchApiConnectionLog(GraviteeContext.getExecutionContext(), "apiId", "requestId")).isEmpty();
        }

        @Test
        void should_return_api_connection_log_detail() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetail(any(QueryContext.class), any())).thenReturn(
                Optional.of(
                    ConnectionLogDetail.builder()
                        .timestamp("2023-10-27T07:41:39.317+02:00")
                        .apiId("apiId")
                        .requestId("requestId")
                        .clientIdentifier("8eec8b53-edae-4954-ac8b-53edae1954e4")
                        .requestEnded(true)
                        .entrypointRequest(
                            ConnectionLogDetail.Request.builder()
                                .method("GET")
                                .uri("/test?param=paramValue")
                                .headers(
                                    Map.of(
                                        "Accept-Encoding",
                                        List.of("gzip, deflate, br"),
                                        "Host",
                                        List.of("localhost:8082"),
                                        "X-Gravitee-Transaction-Id",
                                        List.of("e220afa7-4c77-4280-a0af-a74c7782801c"),
                                        "X-Gravitee-Request-Id",
                                        List.of("e220afa7-4c77-4280-a0af-a74c7782801c")
                                    )
                                )
                                .build()
                        )
                        .endpointRequest(
                            ConnectionLogDetail.Request.builder()
                                .method("GET")
                                .uri("")
                                .headers(
                                    Map.of(
                                        "Accept-Encoding",
                                        List.of("gzip, deflate, br"),
                                        "Host",
                                        List.of("localhost:8082"),
                                        "X-Gravitee-Request-Id",
                                        List.of("e220afa7-4c77-4280-a0af-a74c7782801c"),
                                        "X-Gravitee-Transaction-Id",
                                        List.of("e220afa7-4c77-4280-a0af-a74c7782801c")
                                    )
                                )
                                .build()
                        )
                        .entrypointResponse(
                            ConnectionLogDetail.Response.builder()
                                .status(200)
                                .headers(
                                    Map.of(
                                        "Content-Type",
                                        List.of("text/plain"),
                                        "X-Gravitee-Client-Identifier",
                                        List.of("8eec8b53-edae-4954-ac8b-53edae1954e4"),
                                        "X-Gravitee-Request-Id",
                                        List.of("e220afa7-4c77-4280-a0af-a74c7782801c"),
                                        "X-Gravitee-Transaction-Id",
                                        List.of("e220afa7-4c77-4280-a0af-a74c7782801c")
                                    )
                                )
                                .build()
                        )
                        .endpointResponse(ConnectionLogDetail.Response.builder().status(200).headers(Map.of()).build())
                        .build()
                )
            );

            var result = logCrudService.searchApiConnectionLog(GraviteeContext.getExecutionContext(), "apiId", "requestId");

            SoftAssertions.assertSoftly(soft -> {
                assertThat(result).hasValueSatisfying(connectionLogDetail -> {
                    assertThat(connectionLogDetail)
                        .hasFieldOrPropertyWithValue("apiId", "apiId")
                        .hasFieldOrPropertyWithValue("requestId", "requestId")
                        .hasFieldOrPropertyWithValue("timestamp", "2023-10-27T07:41:39.317+02:00")
                        .hasFieldOrPropertyWithValue("clientIdentifier", "8eec8b53-edae-4954-ac8b-53edae1954e4")
                        .hasFieldOrPropertyWithValue("requestEnded", true);
                    assertThat(connectionLogDetail.getEntrypointRequest())
                        .hasFieldOrPropertyWithValue("method", "GET")
                        .hasFieldOrPropertyWithValue("uri", "/test?param=paramValue")
                        .extracting(
                            io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail.Request::getHeaders,
                            as(InstanceOfAssertFactories.map(String.class, List.class))
                        )
                        .containsAllEntriesOf(
                            Map.of(
                                "Accept-Encoding",
                                List.of("gzip, deflate, br"),
                                "Host",
                                List.of("localhost:8082"),
                                "X-Gravitee-Transaction-Id",
                                List.of("e220afa7-4c77-4280-a0af-a74c7782801c"),
                                "X-Gravitee-Request-Id",
                                List.of("e220afa7-4c77-4280-a0af-a74c7782801c")
                            )
                        );
                    assertThat(connectionLogDetail.getEndpointRequest())
                        .hasFieldOrPropertyWithValue("method", "GET")
                        .hasFieldOrPropertyWithValue("uri", "")
                        .extracting(
                            io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail.Request::getHeaders,
                            as(InstanceOfAssertFactories.map(String.class, List.class))
                        )
                        .containsAllEntriesOf(
                            Map.of(
                                "Accept-Encoding",
                                List.of("gzip, deflate, br"),
                                "Host",
                                List.of("localhost:8082"),
                                "X-Gravitee-Request-Id",
                                List.of("e220afa7-4c77-4280-a0af-a74c7782801c"),
                                "X-Gravitee-Transaction-Id",
                                List.of("e220afa7-4c77-4280-a0af-a74c7782801c")
                            )
                        );
                    assertThat(connectionLogDetail.getEntrypointResponse())
                        .hasFieldOrPropertyWithValue("status", 200)
                        .extracting(
                            io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail.Response::getHeaders,
                            as(InstanceOfAssertFactories.map(String.class, List.class))
                        )
                        .containsAllEntriesOf(
                            Map.of(
                                "Content-Type",
                                List.of("text/plain"),
                                "X-Gravitee-Client-Identifier",
                                List.of("8eec8b53-edae-4954-ac8b-53edae1954e4"),
                                "X-Gravitee-Request-Id",
                                List.of("e220afa7-4c77-4280-a0af-a74c7782801c"),
                                "X-Gravitee-Transaction-Id",
                                List.of("e220afa7-4c77-4280-a0af-a74c7782801c")
                            )
                        );
                    assertThat(connectionLogDetail.getEndpointResponse())
                        .hasFieldOrPropertyWithValue("status", 200)
                        .extracting(
                            io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail.Response::getHeaders,
                            as(InstanceOfAssertFactories.map(String.class, List.class))
                        )
                        .isEmpty();
                });
            });
        }
    }

    @Nested
    class SearchApiConnectionLogsWithBodyText {

        @Test
        void should_return_empty_when_no_body_text_matches() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetails(any(QueryContext.class), any())).thenReturn(new LogResponse<>(0, List.of()));

            var result = logCrudService.searchApiConnectionLogs(
                GraviteeContext.getExecutionContext(),
                Set.of("api-1"),
                SearchLogsFilters.builder().from(0L).to(1L).bodyText("needle").build(),
                new PageableImpl(1, 10),
                List.of(DefinitionVersion.V4)
            );

            assertThat(result.total()).isZero();
            verify(logRepository).searchConnectionLogDetails(any(QueryContext.class), any());
            verify(metricsRepository, never()).searchMetrics(any(), any(), any());
        }

        @Test
        void should_perform_two_phase_search_when_body_text_matches() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetails(any(QueryContext.class), any())).thenReturn(
                new LogResponse<>(
                    2,
                    List.of(ConnectionLogDetail.builder().requestId("r-1").build(), ConnectionLogDetail.builder().requestId("r-2").build())
                )
            );

            when(metricsRepository.searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V4)))).thenReturn(
                new LogResponse<>(2, List.of(Metrics.builder().requestId("r-1").build(), Metrics.builder().requestId("r-2").build()))
            );

            var result = logCrudService.searchApiConnectionLogs(
                GraviteeContext.getExecutionContext(),
                Set.of("api-1"),
                SearchLogsFilters.builder().from(0L).to(1L).bodyText("needle").build(),
                new PageableImpl(1, 10),
                List.of(DefinitionVersion.V4)
            );

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.logs()).hasSize(2);

            var detailCaptor = ArgumentCaptor.forClass(ConnectionLogDetailQuery.class);
            verify(logRepository).searchConnectionLogDetails(any(QueryContext.class), detailCaptor.capture());
            assertThat(detailCaptor.getValue().getFilter().getBodyText()).isEqualTo("needle");
            assertThat(detailCaptor.getValue().getFilter().getApiIds()).containsExactly("api-1");

            var metricsCaptor = ArgumentCaptor.forClass(MetricsQuery.class);
            verify(metricsRepository).searchMetrics(any(QueryContext.class), metricsCaptor.capture(), eq(List.of(DefinitionVersion.V4)));
            assertThat(metricsCaptor.getValue().getFilter().getRequestIds()).containsExactlyInAnyOrder("r-1", "r-2");
        }

        @Test
        void should_skip_two_phase_when_body_text_is_blank() throws AnalyticsException {
            when(metricsRepository.searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V4)))).thenReturn(
                new LogResponse<>(0, List.of())
            );

            logCrudService.searchApiConnectionLogs(
                GraviteeContext.getExecutionContext(),
                Set.of("api-1"),
                SearchLogsFilters.builder().from(0L).to(1L).bodyText("").build(),
                new PageableImpl(1, 10),
                List.of(DefinitionVersion.V4)
            );

            verify(logRepository, never()).searchConnectionLogDetails(any(), any());
            verify(metricsRepository).searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V4)));
        }

        @Test
        void should_skip_two_phase_when_body_text_is_null() throws AnalyticsException {
            when(metricsRepository.searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V4)))).thenReturn(
                new LogResponse<>(0, List.of())
            );

            logCrudService.searchApiConnectionLogs(
                GraviteeContext.getExecutionContext(),
                Set.of("api-1"),
                SearchLogsFilters.builder().from(0L).to(1L).bodyText(null).build(),
                new PageableImpl(1, 10),
                List.of(DefinitionVersion.V4)
            );

            verify(logRepository, never()).searchConnectionLogDetails(any(), any());
            verify(metricsRepository).searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V4)));
        }

        @Test
        void should_not_pass_metric_fields_to_v4_log_filter_when_combined_with_statuses() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetails(any(QueryContext.class), any())).thenReturn(
                new LogResponse<>(1, List.of(ConnectionLogDetail.builder().requestId("r-1").build()))
            );

            when(metricsRepository.searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V4)))).thenReturn(
                new LogResponse<>(1, List.of(Metrics.builder().requestId("r-1").build()))
            );

            logCrudService.searchApiConnectionLogs(
                GraviteeContext.getExecutionContext(),
                Set.of("api-1"),
                SearchLogsFilters.builder()
                    .from(0L)
                    .to(1L)
                    .bodyText("needle")
                    .statuses(Set.of(200))
                    .methods(Set.of(HttpMethod.GET))
                    .uri("/test")
                    .build(),
                new PageableImpl(1, 10),
                List.of(DefinitionVersion.V4)
            );

            var detailCaptor = ArgumentCaptor.forClass(ConnectionLogDetailQuery.class);
            verify(logRepository).searchConnectionLogDetails(any(QueryContext.class), detailCaptor.capture());

            var logFilter = detailCaptor.getValue().getFilter();
            assertThat(logFilter.getBodyText()).isEqualTo("needle");
            assertThat(logFilter.getApiIds()).containsExactly("api-1");
            assertThat(logFilter.getStatuses()).isNull();
            assertThat(logFilter.getMethods()).isNull();
            assertThat(logFilter.getUri()).isNull();

            var metricsCaptor = ArgumentCaptor.forClass(MetricsQuery.class);
            verify(metricsRepository).searchMetrics(any(QueryContext.class), metricsCaptor.capture(), eq(List.of(DefinitionVersion.V4)));

            var metricsFilter = metricsCaptor.getValue().getFilter();
            assertThat(metricsFilter.getStatuses()).containsExactly(200);
            assertThat(metricsFilter.getMethods()).containsExactly(HttpMethod.GET);
            assertThat(metricsFilter.getUri()).isEqualTo("/test");
            assertThat(metricsFilter.getRequestIds()).containsExactly("r-1");
        }

        @Test
        void should_reset_pagination_to_page_1_for_metrics_phase() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetails(any(QueryContext.class), any())).thenReturn(
                new LogResponse<>(1, List.of(ConnectionLogDetail.builder().requestId("r-1").build()))
            );

            when(metricsRepository.searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V4)))).thenReturn(
                new LogResponse<>(1, List.of(Metrics.builder().requestId("r-1").build()))
            );

            logCrudService.searchApiConnectionLogs(
                GraviteeContext.getExecutionContext(),
                Set.of("api-1"),
                SearchLogsFilters.builder().from(0L).to(1L).bodyText("needle").build(),
                new PageableImpl(3, 10),
                List.of(DefinitionVersion.V4)
            );

            var metricsCaptor = ArgumentCaptor.forClass(MetricsQuery.class);
            verify(metricsRepository).searchMetrics(any(QueryContext.class), metricsCaptor.capture(), eq(List.of(DefinitionVersion.V4)));
            assertThat(metricsCaptor.getValue().getPage()).isEqualTo(1);
            assertThat(metricsCaptor.getValue().getSize()).isEqualTo(10);
        }

        @Test
        void should_use_body_search_total_when_metrics_returns_full_page() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetails(any(QueryContext.class), any())).thenReturn(
                new LogResponse<>(
                    25,
                    List.of(
                        ConnectionLogDetail.builder().requestId("r-1").build(),
                        ConnectionLogDetail.builder().requestId("r-2").build(),
                        ConnectionLogDetail.builder().requestId("r-3").build()
                    )
                )
            );

            when(metricsRepository.searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V4)))).thenReturn(
                new LogResponse<>(
                    3,
                    List.of(
                        Metrics.builder().requestId("r-1").build(),
                        Metrics.builder().requestId("r-2").build(),
                        Metrics.builder().requestId("r-3").build()
                    )
                )
            );

            var result = logCrudService.searchApiConnectionLogs(
                GraviteeContext.getExecutionContext(),
                Set.of("api-1"),
                SearchLogsFilters.builder().from(0L).to(1L).bodyText("needle").build(),
                new PageableImpl(1, 3),
                List.of(DefinitionVersion.V4)
            );

            assertThat(result.total()).isEqualTo(25);
            assertThat(result.logs()).hasSize(3);
        }
    }

    @Nested
    class SearchApplicationConnectionLogs {

        @Test
        void should_search_connection_logs_with_empty_query() throws AnalyticsException {
            when(
                metricsRepository.searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4)))
            ).thenReturn(new LogResponse<>(1, List.of()));

            logCrudService.searchApplicationConnectionLogs(
                GraviteeContext.getExecutionContext(),
                "application-id",
                SearchLogsFilters.builder().build(),
                new PageableImpl(1, 20)
            );

            var captorQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorMetricsQuery = ArgumentCaptor.forClass(MetricsQuery.class);
            verify(metricsRepository).searchMetrics(
                captorQueryContext.capture(),
                captorMetricsQuery.capture(),
                eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4))
            );

            final QueryContext queryContext = captorQueryContext.getValue();
            assertThat(queryContext.getOrgId()).isEqualTo("DEFAULT");
            assertThat(queryContext.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorMetricsQuery.getValue()).isEqualTo(
                MetricsQuery.builder().filter(MetricsQuery.Filter.builder().applicationIds(Set.of("application-id")).build()).build()
            );
        }

        @Test
        void should_search_connection_logs_with_query() throws AnalyticsException {
            when(
                metricsRepository.searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4)))
            ).thenReturn(new LogResponse<>(1, List.of()));

            logCrudService.searchApplicationConnectionLogs(
                GraviteeContext.getExecutionContext(),
                "application-id",
                SearchLogsFilters.builder()
                    .to(1L)
                    .from(0L)
                    .apiIds(Set.of("api-1"))
                    .applicationIds(Set.of("app-1"))
                    .entrypointIds(Set.of("entrypoint-id"))
                    .planIds(Set.of("plan-1"))
                    .methods(Set.of(HttpMethod.GET))
                    .statuses(Set.of(3))
                    .errorKeys(Set.of("GATEWAY_OAUTH2_ACCESS_DENIED"))
                    .build(),
                new PageableImpl(1, 20)
            );

            var captorQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorMetricsQuery = ArgumentCaptor.forClass(MetricsQuery.class);
            verify(metricsRepository).searchMetrics(
                captorQueryContext.capture(),
                captorMetricsQuery.capture(),
                eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4))
            );

            final QueryContext queryContext = captorQueryContext.getValue();
            assertThat(queryContext.getOrgId()).isEqualTo("DEFAULT");
            assertThat(queryContext.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorMetricsQuery.getValue()).isEqualTo(
                MetricsQuery.builder()
                    .filter(
                        MetricsQuery.Filter.builder()
                            .to(1L)
                            .from(0L)
                            .apiIds(Set.of("api-1"))
                            .applicationIds(Set.of("application-id"))
                            .entrypointIds(Set.of("entrypoint-id"))
                            .planIds(Set.of("plan-1"))
                            .methods(Set.of(HttpMethod.GET))
                            .statuses(Set.of(3))
                            .errorKeys(Set.of("GATEWAY_OAUTH2_ACCESS_DENIED"))
                            .build()
                    )
                    .build()
            );
        }

        @Test
        void should_search_only_search_log_details_if_no_body_text_found() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetails(any(QueryContext.class), any())).thenReturn(new LogResponse<>(0, List.of()));

            logCrudService.searchApplicationConnectionLogs(
                GraviteeContext.getExecutionContext(),
                "application-id",
                SearchLogsFilters.builder()
                    .to(1L)
                    .from(0L)
                    .apiIds(Set.of("api-1"))
                    .applicationIds(Set.of("app-1"))
                    .entrypointIds(Set.of("entrypoint-id"))
                    .planIds(Set.of("plan-1"))
                    .methods(Set.of(HttpMethod.GET))
                    .statuses(Set.of(3))
                    .bodyText("curl")
                    .build(),
                new PageableImpl(1, 3)
            );

            var captorConnectionLogDetailQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorConnectionLogDetailQuery = ArgumentCaptor.forClass(ConnectionLogDetailQuery.class);
            verify(logRepository).searchConnectionLogDetails(
                captorConnectionLogDetailQueryContext.capture(),
                captorConnectionLogDetailQuery.capture()
            );

            verify(metricsRepository, never()).searchMetrics(any(), any(), any());

            final QueryContext captorConnectionLogDetailQueryContextValue = captorConnectionLogDetailQueryContext.getValue();
            assertThat(captorConnectionLogDetailQueryContextValue.getOrgId()).isEqualTo("DEFAULT");
            assertThat(captorConnectionLogDetailQueryContextValue.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorConnectionLogDetailQuery.getValue()).isEqualTo(
                ConnectionLogDetailQuery.builder()
                    .size(3)
                    .projectionFields(List.of("_id", "request-id"))
                    .filter(ConnectionLogDetailQuery.Filter.builder().to(1L).from(0L).apiIds(Set.of("api-1")).bodyText("curl").build())
                    .build()
            );
        }

        @Test
        void should_search_connection_logs_with_body_text_filter() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetails(any(QueryContext.class), any())).thenReturn(
                new LogResponse<>(
                    3,
                    List.of(
                        ConnectionLogDetail.builder().requestId("r-1").build(),
                        ConnectionLogDetail.builder().requestId("r-2").build(),
                        ConnectionLogDetail.builder().requestId("r-3").build()
                    )
                )
            );

            when(
                metricsRepository.searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4)))
            ).thenReturn(
                new LogResponse<>(
                    3,
                    List.of(
                        Metrics.builder().requestId("r-1").build(),
                        Metrics.builder().requestId("r-2").build(),
                        Metrics.builder().requestId("r-3").build()
                    )
                )
            );

            logCrudService.searchApplicationConnectionLogs(
                GraviteeContext.getExecutionContext(),
                "application-id",
                SearchLogsFilters.builder()
                    .to(1L)
                    .from(0L)
                    .apiIds(Set.of("api-1"))
                    .applicationIds(Set.of("app-1"))
                    .entrypointIds(Set.of("entrypoint-id"))
                    .planIds(Set.of("plan-1"))
                    .methods(Set.of(HttpMethod.GET))
                    .statuses(Set.of(3))
                    .bodyText("curl")
                    .build(),
                new PageableImpl(1, 20)
            );

            var captorConnectionLogDetailQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorConnectionLogDetailQuery = ArgumentCaptor.forClass(ConnectionLogDetailQuery.class);
            verify(logRepository).searchConnectionLogDetails(
                captorConnectionLogDetailQueryContext.capture(),
                captorConnectionLogDetailQuery.capture()
            );

            var captorMetricsQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorMetricsQuery = ArgumentCaptor.forClass(MetricsQuery.class);
            verify(metricsRepository).searchMetrics(
                captorMetricsQueryContext.capture(),
                captorMetricsQuery.capture(),
                eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4))
            );

            final QueryContext captorConnectionLogDetailQueryContextValue = captorConnectionLogDetailQueryContext.getValue();
            assertThat(captorConnectionLogDetailQueryContextValue.getOrgId()).isEqualTo("DEFAULT");
            assertThat(captorConnectionLogDetailQueryContextValue.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorConnectionLogDetailQuery.getValue()).isEqualTo(
                ConnectionLogDetailQuery.builder()
                    .projectionFields(List.of("_id", "request-id"))
                    .filter(ConnectionLogDetailQuery.Filter.builder().to(1L).from(0L).apiIds(Set.of("api-1")).bodyText("curl").build())
                    .build()
            );

            final QueryContext queryContext = captorMetricsQueryContext.getValue();
            assertThat(queryContext.getOrgId()).isEqualTo("DEFAULT");
            assertThat(queryContext.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorMetricsQuery.getValue()).isEqualTo(
                MetricsQuery.builder()
                    .filter(
                        MetricsQuery.Filter.builder()
                            .to(1L)
                            .from(0L)
                            .apiIds(Set.of("api-1"))
                            .applicationIds(Set.of("application-id"))
                            .entrypointIds(Set.of("entrypoint-id"))
                            .planIds(Set.of("plan-1"))
                            .methods(Set.of(HttpMethod.GET))
                            .statuses(Set.of(3))
                            .requestIds(Set.of("r-1", "r-2", "r-3"))
                            .build()
                    )
                    .build()
            );
        }

        @Test
        void should_search_connection_logs_with_body_text_filter_and_on_full_page_return_log_details_total() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetails(any(QueryContext.class), any())).thenReturn(
                new LogResponse<>(
                    5,
                    List.of(
                        ConnectionLogDetail.builder().requestId("r-1").build(),
                        ConnectionLogDetail.builder().requestId("r-2").build(),
                        ConnectionLogDetail.builder().requestId("r-3").build()
                    )
                )
            );

            when(
                metricsRepository.searchMetrics(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4)))
            ).thenReturn(
                new LogResponse<>(
                    3,
                    List.of(
                        Metrics.builder().requestId("r-1").build(),
                        Metrics.builder().requestId("r-2").build(),
                        Metrics.builder().requestId("r-3").build()
                    )
                )
            );

            logCrudService.searchApplicationConnectionLogs(
                GraviteeContext.getExecutionContext(),
                "application-id",
                SearchLogsFilters.builder()
                    .to(1L)
                    .from(0L)
                    .apiIds(Set.of("api-1"))
                    .applicationIds(Set.of("app-1"))
                    .entrypointIds(Set.of("entrypoint-id"))
                    .planIds(Set.of("plan-1"))
                    .methods(Set.of(HttpMethod.GET))
                    .statuses(Set.of(3))
                    .bodyText("curl")
                    .build(),
                new PageableImpl(1, 3)
            );

            var captorConnectionLogDetailQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorConnectionLogDetailQuery = ArgumentCaptor.forClass(ConnectionLogDetailQuery.class);
            verify(logRepository).searchConnectionLogDetails(
                captorConnectionLogDetailQueryContext.capture(),
                captorConnectionLogDetailQuery.capture()
            );

            var captorMetricsQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorMetricsQuery = ArgumentCaptor.forClass(MetricsQuery.class);
            verify(metricsRepository).searchMetrics(
                captorMetricsQueryContext.capture(),
                captorMetricsQuery.capture(),
                eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4))
            );

            final QueryContext captorConnectionLogDetailQueryContextValue = captorConnectionLogDetailQueryContext.getValue();
            assertThat(captorConnectionLogDetailQueryContextValue.getOrgId()).isEqualTo("DEFAULT");
            assertThat(captorConnectionLogDetailQueryContextValue.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorConnectionLogDetailQuery.getValue()).isEqualTo(
                ConnectionLogDetailQuery.builder()
                    .size(3)
                    .projectionFields(List.of("_id", "request-id"))
                    .filter(ConnectionLogDetailQuery.Filter.builder().to(1L).from(0L).apiIds(Set.of("api-1")).bodyText("curl").build())
                    .build()
            );

            final QueryContext queryContext = captorMetricsQueryContext.getValue();
            assertThat(queryContext.getOrgId()).isEqualTo("DEFAULT");
            assertThat(queryContext.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorMetricsQuery.getValue()).isEqualTo(
                MetricsQuery.builder()
                    .filter(
                        MetricsQuery.Filter.builder()
                            .to(1L)
                            .from(0L)
                            .apiIds(Set.of("api-1"))
                            .applicationIds(Set.of("application-id"))
                            .entrypointIds(Set.of("entrypoint-id"))
                            .planIds(Set.of("plan-1"))
                            .methods(Set.of(HttpMethod.GET))
                            .statuses(Set.of(3))
                            .requestIds(Set.of("r-1", "r-2", "r-3"))
                            .build()
                    )
                    .size(3)
                    .build()
            );
        }
    }
}
