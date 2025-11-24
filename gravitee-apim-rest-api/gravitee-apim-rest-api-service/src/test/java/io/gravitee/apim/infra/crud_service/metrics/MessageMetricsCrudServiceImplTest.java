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

import io.gravitee.apim.core.metrics.crud_service.MessageMetricsCrudService;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.message.MessageLogQuery;
import io.gravitee.repository.log.v4.model.message.MessageMetrics;
import io.gravitee.rest.api.model.analytics.SearchMessageMetricsFilters;
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
class MessageMetricsCrudServiceImplTest {

    @Mock
    private LogRepository logRepository;

    @Captor
    private ArgumentCaptor<MessageLogQuery> queryCaptor;

    private MessageMetricsCrudService cut;

    @BeforeEach
    void before() {
        cut = new MessageMetricsCrudServiceImpl(logRepository);
    }

    @Test
    void should_create_filter_and_map_response() throws AnalyticsException {
        when(logRepository.searchMessageMetrics(any(QueryContext.class), queryCaptor.capture())).thenReturn(
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
        var result = cut.searchApiMessageMetrics(
            GraviteeContext.getExecutionContext(),
            "api-id",
            SearchMessageMetricsFilters.builder()
                .connectorId("webhook")
                .connectorType("entrypoint")
                .operation("subscribe")
                .requestId("req-id")
                .build(),
            new PageableImpl(1, 10)
        );

        assertThat(queryCaptor.getValue()).isInstanceOfSatisfying(MessageLogQuery.class, mq -> {
            assertThat(mq.getSize()).isEqualTo(10);
            assertThat(mq.getPage()).isEqualTo(1);
            assertThat(mq.getFilter()).isInstanceOfSatisfying(MessageLogQuery.Filter.class, f -> {
                assertThat(f.apiId()).isEqualTo("api-id");
                assertThat(f.connectorId()).isEqualTo("webhook");
                assertThat(f.operation()).isEqualTo("subscribe");
                assertThat(f.connectorType()).isEqualTo("entrypoint");
                assertThat(f.requestId()).isEqualTo("req-id");
            });
        });

        assertThat(result.logs())
            .first()
            .isInstanceOfSatisfying(io.gravitee.apim.core.metrics.model.MessageMetrics.class, m -> {
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
}
