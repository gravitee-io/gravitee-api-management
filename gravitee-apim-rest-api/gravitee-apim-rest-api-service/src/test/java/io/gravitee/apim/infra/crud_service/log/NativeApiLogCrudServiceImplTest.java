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

import static fixtures.core.model.NativeApiLogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.log.crud_service.NativeApiLogCrudService;
import io.gravitee.apim.core.log.model.NativeApiLog;
import io.gravitee.apim.core.log.model.NativeConnectionStatus;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetrics;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricsQuery;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class NativeApiLogCrudServiceImplTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");

    @Mock
    private MetricsRepository metricsRepository;

    private NativeApiLogCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NativeApiLogCrudServiceImpl(metricsRepository);
    }

    @Nested
    class FindLog {

        @Test
        void maps_metrics_into_log() throws AnalyticsException {
            stubRepositoryReturning(Optional.of(buildNativeApiMetrics(API_ID, REQUEST_ID)));

            var found = service.findLog(EXECUTION_CONTEXT, API_ID, REQUEST_ID, FROM_MILLIS, TO_MILLIS).orElseThrow();

            assertThat(found.getApiId()).isEqualTo(API_ID);
            assertThat(found.getRequestId()).isEqualTo(REQUEST_ID);
            assertThat(found.getTransactionId()).isEqualTo(TRANSACTION_ID);
            assertThat(found.getApplicationId()).isEqualTo(APPLICATION_ID);
            assertThat(found.getPlanId()).isEqualTo(PLAN_ID);
            assertThat(found.getClientIdentifier()).isEqualTo(CLIENT_IDENTIFIER);
            assertThat(found.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
            assertThat(found.getEntrypointId()).isEqualTo(ENTRYPOINT_ID);
            assertThat(found.getGateway()).isEqualTo(GATEWAY);
            assertThat(found.getRemoteAddress()).isEqualTo(REMOTE_ADDRESS);
            assertThat(found.getLocalAddress()).isEqualTo(LOCAL_ADDRESS);
            assertThat(found.getHost()).isEqualTo(HOST);
            assertThat(found.getErrorKey()).isEqualTo(ERROR_KEY);
            assertThat(found.getMessage()).isEqualTo(MESSAGE);
            assertThat(found.getTimestamp()).isEqualTo(TIMESTAMP_ISO);
            assertThat(found.getConnectionStatus()).isEqualTo(NativeConnectionStatus.CONNECTION_ERROR);
            assertThat(found.getClientId()).isEqualTo(CLIENT_ID);
            assertThat(found.getBrokerId()).isEqualTo(BROKER_ID);
            assertThat(found.getConnectionDurationMs()).isEqualTo(CONNECTION_DURATION_MS);
        }

        @Test
        void returns_empty_when_repository_empty() throws AnalyticsException {
            stubRepositoryReturning(Optional.empty());

            assertThat(service.findLog(EXECUTION_CONTEXT, API_ID, REQUEST_ID, FROM_MILLIS, TO_MILLIS)).isEmpty();
        }

        @Test
        void throws_TechnicalManagementException_on_AnalyticsException() throws AnalyticsException {
            stubRepositoryThrowing(new AnalyticsException("Simulated Elasticsearch failure"));

            assertThatThrownBy(() -> service.findLog(EXECUTION_CONTEXT, API_ID, REQUEST_ID, FROM_MILLIS, TO_MILLIS))
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessageContaining(API_ID)
                .hasMessageContaining(REQUEST_ID);
        }

        @Test
        void findLog_returns_null_typed_fields_when_additional_metrics_keys_absent() throws AnalyticsException {
            stubRepositoryReturning(
                Optional.of(NativeApiMetrics.builder().apiId(API_ID).requestId(REQUEST_ID).additionalMetrics(Map.of()).build())
            );

            var found = service.findLog(EXECUTION_CONTEXT, API_ID, REQUEST_ID, FROM_MILLIS, TO_MILLIS).orElseThrow();

            assertThat(found.getConnectionStatus()).isNull();
            assertThat(found.getConnectionDurationMs()).isNull();
            assertThat(found.getClientId()).isNull();
            assertThat(found.getBrokerId()).isNull();
        }

        @Test
        void findLog_returns_null_connectionDurationMs_when_value_not_number() throws AnalyticsException {
            stubRepositoryReturning(
                Optional.of(
                    NativeApiMetrics.builder()
                        .apiId(API_ID)
                        .requestId(REQUEST_ID)
                        .additionalMetrics(Map.of(NativeApiMetricKeys.CONNECTION_DURATION_MS, "not-a-number"))
                        .build()
                )
            );

            var found = service.findLog(EXECUTION_CONTEXT, API_ID, REQUEST_ID, FROM_MILLIS, TO_MILLIS).orElseThrow();

            assertThat(found.getConnectionDurationMs()).isNull();
        }

        private void stubRepositoryReturning(Optional<NativeApiMetrics> result) throws AnalyticsException {
            when(metricsRepository.findNativeApiMetrics(any(), eq(API_ID), eq(REQUEST_ID), eq(FROM_MILLIS), eq(TO_MILLIS))).thenReturn(
                result
            );
        }

        private void stubRepositoryThrowing(AnalyticsException ex) throws AnalyticsException {
            when(metricsRepository.findNativeApiMetrics(any(), eq(API_ID), eq(REQUEST_ID), eq(FROM_MILLIS), eq(TO_MILLIS))).thenThrow(ex);
        }
    }

    @Nested
    class SearchLogs {

        @Test
        void returns_all_logs_from_repository() throws AnalyticsException {
            stubRepositoryReturning(
                new LogResponse<>(
                    3L,
                    List.of(
                        NativeApiMetrics.builder().apiId(API_ID).requestId(REQUEST_ID).build(),
                        NativeApiMetrics.builder().apiId(API_ID).requestId("r2").build(),
                        NativeApiMetrics.builder().apiId(API_ID).requestId("r3").build()
                    )
                )
            );

            var response = searchWithDefaults();

            assertThat(response.total()).isEqualTo(3);
            assertThat(response.logs()).hasSize(3).extracting(NativeApiLog::getRequestId).containsExactly(REQUEST_ID, "r2", "r3");
        }

        @Test
        void sets_restricted_fields_null() throws AnalyticsException {
            stubRepositoryReturning(new LogResponse<>(1L, List.of(buildNativeApiMetrics(API_ID, REQUEST_ID))));

            var log = searchWithDefaults().logs().getFirst();

            assertThat(log.getApiId()).isEqualTo(API_ID);
            assertThat(log.getRequestId()).isEqualTo(REQUEST_ID);
            assertThat(log.getTransactionId()).isEqualTo(TRANSACTION_ID);
            assertThat(log.getTimestamp()).isEqualTo(TIMESTAMP_ISO);
            assertThat(log.getApplicationId()).isEqualTo(APPLICATION_ID);
            assertThat(log.getPlanId()).isEqualTo(PLAN_ID);
            assertThat(log.getClientIdentifier()).isEqualTo(CLIENT_IDENTIFIER);
            assertThat(log.getEntrypointId()).isEqualTo(ENTRYPOINT_ID);
            assertThat(log.getConnectionStatus()).isEqualTo(NativeConnectionStatus.CONNECTION_ERROR);
            assertThat(log.getConnectionDurationMs()).isEqualTo(CONNECTION_DURATION_MS);

            assertThat(log.getSubscriptionId()).isNull();
            assertThat(log.getGateway()).isNull();
            assertThat(log.getRemoteAddress()).isNull();
            assertThat(log.getLocalAddress()).isNull();
            assertThat(log.getHost()).isNull();
            assertThat(log.getErrorKey()).isNull();
            assertThat(log.getMessage()).isNull();
            assertThat(log.getClientId()).isNull();
            assertThat(log.getBrokerId()).isNull();
        }

        @Test
        void propagates_filter_fields_into_native_metrics_query() throws AnalyticsException {
            var captor = ArgumentCaptor.forClass(NativeApiMetricsQuery.class);
            when(metricsRepository.searchNativeApiMetrics(any(), captor.capture())).thenReturn(new LogResponse<>(0L, List.of()));

            service.searchLogs(
                EXECUTION_CONTEXT,
                API_ID,
                NativeApiLogCrudService.Filter.builder()
                    .from(FROM_MILLIS)
                    .to(TO_MILLIS)
                    .applicationIds(Set.of(APPLICATION_ID))
                    .planIds(Set.of(PLAN_ID))
                    .connectionStatuses(Set.of(NativeConnectionStatus.CONNECTED))
                    .build(),
                2,
                50
            );

            var captured = captor.getValue();
            assertThat(captured.getApiId()).isEqualTo(API_ID);
            assertThat(captured.getFrom()).isEqualTo(FROM_MILLIS);
            assertThat(captured.getTo()).isEqualTo(TO_MILLIS);
            assertThat(captured.getApplicationIds()).containsExactly(APPLICATION_ID);
            assertThat(captured.getPlanIds()).containsExactly(PLAN_ID);
            assertThat(captured.getConnectionStatuses()).containsExactly(NativeConnectionStatus.CONNECTED.name());
            assertThat(captured.getPage()).isEqualTo(2);
            assertThat(captured.getSize()).isEqualTo(50);
        }

        @Test
        void throws_TechnicalManagementException_on_AnalyticsException() throws AnalyticsException {
            stubRepositoryThrowing(new AnalyticsException("Simulated Elasticsearch failure"));

            assertThatThrownBy(this::searchWithDefaults).isInstanceOf(TechnicalManagementException.class).hasMessageContaining(API_ID);
        }

        private SearchLogsResponse<NativeApiLog> searchWithDefaults() {
            return service.searchLogs(
                EXECUTION_CONTEXT,
                API_ID,
                NativeApiLogCrudService.Filter.builder().from(0L).to(1000L).build(),
                1,
                20
            );
        }

        private void stubRepositoryReturning(LogResponse<NativeApiMetrics> response) throws AnalyticsException {
            when(metricsRepository.searchNativeApiMetrics(any(), any(NativeApiMetricsQuery.class))).thenReturn(response);
        }

        private void stubRepositoryThrowing(AnalyticsException ex) throws AnalyticsException {
            when(metricsRepository.searchNativeApiMetrics(any(), any(NativeApiMetricsQuery.class))).thenThrow(ex);
        }
    }
}
