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
package io.gravitee.apim.infra.crud_service.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.log.crud_service.MessageLogsCrudService;
import io.gravitee.apim.infra.crud_service.log.MessageLogsCrudServiceImpl;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.message.MessageMetrics;
import io.gravitee.repository.log.v4.model.message.MessageMetricsQuery;
import io.gravitee.rest.api.model.analytics.SearchMessageLogsFilters;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MessageLogsCrudServiceImplTest {

    @Mock
    private MetricsRepository metricsRepository;

    @Captor
    private ArgumentCaptor<MessageMetricsQuery> queryCaptor;

    private MessageLogsCrudService cut;

    @BeforeEach
    void before() {
        cut = new MessageLogsCrudServiceImpl(metricsRepository);
    }

    @Test
    void should_create_filter_and_map_response() throws AnalyticsException {
        when(metricsRepository.searchMessageMetrics(any(QueryContext.class), queryCaptor.capture())).thenReturn(
            new LogResponse<>(
                1,
                List.of(
                    MessageMetrics.builder()
                        .timestamp("today")
                        .apiId("api-id")
                        .apiName("api name")
                        .requestId("req-id")
                        .clientIdentifier("client-id")
                        .correlationId("cor-id")
                        .operation("subscribe")
                        .connectorType("entrypoint")
                        .connectorId("webhook")
                        .contentLength(4)
                        .count(1)
                        .errorCount(1)
                        .countIncrement(1)
                        .errorCountIncrement(1)
                        .error(true)
                        .gatewayLatencyMs(1000)
                        .custom(Map.of("foo", "bar"))
                        .additionalMetrics(Map.of("int_webhook_body-size", 12, "string_webhook_last_error", "Internal Server Error"))
                        .build()
                )
            )
        );
        var result = cut.searchApiMessageLogs(
            GraviteeContext.getExecutionContext(),
            "api-id",
            SearchMessageLogsFilters.builder()
                .connectorId("webhook")
                .connectorType("entrypoint")
                .operation("subscribe")
                .requestId("req-id")
                .build(),
            new PageableImpl(1, 10)
        );

        assertThat(queryCaptor.getValue()).satisfies(mq -> {
            assertThat(mq.getSize()).isEqualTo(10);
            assertThat(mq.getPage()).isEqualTo(1);
            assertThat(mq.getFilter()).satisfies(f -> {
                assertThat(f.apiId()).isEqualTo("api-id");
                assertThat(f.connectorId()).isEqualTo("webhook");
                assertThat(f.operation()).isEqualTo("subscribe");
                assertThat(f.connectorType()).isEqualTo("entrypoint");
                assertThat(f.requestId()).isEqualTo("req-id");
            });
        });

        assertThat(result.logs())
            .first()
            .satisfies(m -> {
                assertThat(m.getApiId()).isEqualTo("api-id");
                assertThat(m.getApiName()).isEqualTo("api name");
                assertThat(m.getClientIdentifier()).isEqualTo("client-id");
                assertThat(m.getConnectorId()).isEqualTo("webhook");
                assertThat(m.getConnectorType()).isEqualTo("entrypoint");
                assertThat(m.getContentLength()).isEqualTo(4);
                assertThat(m.getCorrelationId()).isEqualTo("cor-id");
                assertThat(m.getCount()).isEqualTo(1);
                assertThat(m.getCountIncrement()).isEqualTo(1);
                assertThat(m.getErrorCount()).isEqualTo(1);
                assertThat(m.getErrorCountIncrement()).isEqualTo(1);
                assertThat(m.getGatewayLatencyMs()).isEqualTo(1000);
                assertThat(m.getOperation()).isEqualTo("subscribe");
                assertThat(m.getRequestId()).isEqualTo("req-id");
                assertThat(m.getTimestamp()).isEqualTo("today");
                assertThat(m.getAdditionalMetrics()).containsEntry("int_webhook_body-size", 12);
                assertThat(m.getAdditionalMetrics()).containsEntry("string_webhook_last_error", "Internal Server Error");
                assertThat(m.getCustom()).containsEntry("foo", "bar");
                assertThat(m.isError()).isTrue();
            });
    }

    @Test
    void should_pass_additional_fields_and_values_to_query() throws AnalyticsException {
        setupEmptyResponse();

        cut.searchApiMessageLogs(
            GraviteeContext.getExecutionContext(),
            "api-id",
            SearchMessageLogsFilters.builder()
                .additional(Map.of("int_webhook_resp-status", List.of("200", "500"), "string_webhook_url", List.of("https://example.com")))
                .build(),
            new PageableImpl(1, 10)
        );

        var filter = queryCaptor.getValue().getFilter();
        assertThat(filter.additional()).isNotNull();
        assertThat(filter.additional().get("int_webhook_resp-status")).containsExactly("200", "500");
        assertThat(filter.additional().get("string_webhook_url")).containsExactly("https://example.com");
    }

    @Test
    void should_handle_null_additional() throws AnalyticsException {
        setupEmptyResponse();

        cut.searchApiMessageLogs(
            GraviteeContext.getExecutionContext(),
            "api-id",
            SearchMessageLogsFilters.builder().additional(null).build(),
            new PageableImpl(1, 10)
        );

        assertThat(queryCaptor.getValue().getFilter().additional()).isNull();
    }

    private void setupEmptyResponse() throws AnalyticsException {
        when(metricsRepository.searchMessageMetrics(any(QueryContext.class), queryCaptor.capture())).thenReturn(
            new LogResponse<>(0, List.of())
        );
    }
}
