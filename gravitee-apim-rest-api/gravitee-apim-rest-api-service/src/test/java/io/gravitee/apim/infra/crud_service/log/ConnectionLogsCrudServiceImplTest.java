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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery;
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

public class ConnectionLogsCrudServiceImplTest {

    LogRepository logRepository;

    ConnectionLogsCrudServiceImpl logCrudService;

    @BeforeEach
    void setUp() {
        logRepository = mock(LogRepository.class);
        logCrudService = new ConnectionLogsCrudServiceImpl(logRepository);
    }

    @Nested
    class SearchApiConnectionLogDetail {

        @Test
        void should_search_api_connection_log_detail() throws AnalyticsException {
            when(logRepository.searchConnectionLogDetail(any(QueryContext.class), any()))
                .thenReturn(Optional.of(ConnectionLogDetail.builder().build()));

            logCrudService.searchApiConnectionLog(GraviteeContext.getExecutionContext(), "apiId", "requestId");

            var captorQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorConnectionLogDetailQuery = ArgumentCaptor.forClass(ConnectionLogDetailQuery.class);
            verify(logRepository).searchConnectionLogDetail(captorQueryContext.capture(), captorConnectionLogDetailQuery.capture());

            final QueryContext queryContext = captorQueryContext.getValue();
            assertThat(queryContext.getOrgId()).isEqualTo("DEFAULT");
            assertThat(queryContext.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorConnectionLogDetailQuery.getValue())
                .isEqualTo(
                    ConnectionLogDetailQuery
                        .builder()
                        .filter(ConnectionLogDetailQuery.Filter.builder().apiId("apiId").requestId("requestId").build())
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
            when(logRepository.searchConnectionLogDetail(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(
                        ConnectionLogDetail
                            .builder()
                            .timestamp("2023-10-27T07:41:39.317+02:00")
                            .apiId("apiId")
                            .requestId("requestId")
                            .clientIdentifier("8eec8b53-edae-4954-ac8b-53edae1954e4")
                            .requestEnded(true)
                            .entrypointRequest(
                                ConnectionLogDetail.Request
                                    .builder()
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
                                ConnectionLogDetail.Request
                                    .builder()
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
                                ConnectionLogDetail.Response
                                    .builder()
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
                assertThat(result)
                    .hasValueSatisfying(connectionLogDetail -> {
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
    class SearchApplicationConnectionLogs {

        @Test
        void should_search_connection_logs_with_empty_query() throws AnalyticsException {
            when(
                logRepository.searchConnectionLogs(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4)))
            )
                .thenReturn(new LogResponse<>(1, List.of()));

            logCrudService.searchApplicationConnectionLogs(
                GraviteeContext.getExecutionContext(),
                "application-id",
                SearchLogsFilters.builder().build(),
                new PageableImpl(1, 20)
            );

            var captorQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorConnectionLogQuery = ArgumentCaptor.forClass(ConnectionLogQuery.class);
            verify(logRepository)
                .searchConnectionLogs(
                    captorQueryContext.capture(),
                    captorConnectionLogQuery.capture(),
                    eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4))
                );

            final QueryContext queryContext = captorQueryContext.getValue();
            assertThat(queryContext.getOrgId()).isEqualTo("DEFAULT");
            assertThat(queryContext.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorConnectionLogQuery.getValue())
                .isEqualTo(
                    ConnectionLogQuery
                        .builder()
                        .filter(ConnectionLogQuery.Filter.builder().applicationIds(Set.of("application-id")).build())
                        .build()
                );
        }

        @Test
        void should_search_connection_logs_with_query() throws AnalyticsException {
            when(
                logRepository.searchConnectionLogs(any(QueryContext.class), any(), eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4)))
            )
                .thenReturn(new LogResponse<>(1, List.of()));

            logCrudService.searchApplicationConnectionLogs(
                GraviteeContext.getExecutionContext(),
                "application-id",
                SearchLogsFilters
                    .builder()
                    .to(1L)
                    .from(0L)
                    .apiIds(Set.of("api-1"))
                    .applicationIds(Set.of("app-1"))
                    .entrypointIds(Set.of("entrypoint-id"))
                    .planIds(Set.of("plan-1"))
                    .methods(Set.of(HttpMethod.GET))
                    .statuses(Set.of(3))
                    .build(),
                new PageableImpl(1, 20)
            );

            var captorQueryContext = ArgumentCaptor.forClass(QueryContext.class);
            var captorConnectionLogQuery = ArgumentCaptor.forClass(ConnectionLogQuery.class);
            verify(logRepository)
                .searchConnectionLogs(
                    captorQueryContext.capture(),
                    captorConnectionLogQuery.capture(),
                    eq(List.of(DefinitionVersion.V2, DefinitionVersion.V4))
                );

            final QueryContext queryContext = captorQueryContext.getValue();
            assertThat(queryContext.getOrgId()).isEqualTo("DEFAULT");
            assertThat(queryContext.getEnvId()).isEqualTo("DEFAULT");

            assertThat(captorConnectionLogQuery.getValue())
                .isEqualTo(
                    ConnectionLogQuery
                        .builder()
                        .filter(
                            ConnectionLogQuery.Filter
                                .builder()
                                .to(1L)
                                .from(0L)
                                .apiIds(Set.of("api-1"))
                                .applicationIds(Set.of("application-id"))
                                .entrypointIds(Set.of("entrypoint-id"))
                                .planIds(Set.of("plan-1"))
                                .methods(Set.of(HttpMethod.GET))
                                .statuses(Set.of(3))
                                .build()
                        )
                        .build()
                );
        }
    }
}
