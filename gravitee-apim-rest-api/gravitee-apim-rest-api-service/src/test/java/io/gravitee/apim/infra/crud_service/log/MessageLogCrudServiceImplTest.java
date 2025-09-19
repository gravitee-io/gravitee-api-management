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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.log.crud_service.MessageLogCrudService;
import io.gravitee.apim.core.log.model.MessageOperation;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.message.AggregatedMessageLog;
import io.gravitee.repository.log.v4.model.message.MessageLogQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class MessageLogCrudServiceImplTest {

    @Mock
    private LogRepository logRepository;

    private MessageLogCrudService cut;

    @BeforeEach
    void setUp() {
        cut = new MessageLogCrudServiceImpl(logRepository);
    }

    @Test
    void should_search_api_message_logs() throws AnalyticsException {
        when(logRepository.searchAggregatedMessageLog(any(QueryContext.class), any())).thenReturn(new LogResponse<>(0L, List.of()));

        cut.searchApiMessageLog(GraviteeContext.getExecutionContext(), "api-id", "request-id", new PageableImpl(1, 10));

        var captorQueryContext = ArgumentCaptor.forClass(QueryContext.class);
        var captorConnectionLogDetailQuery = ArgumentCaptor.forClass(MessageLogQuery.class);
        verify(logRepository).searchAggregatedMessageLog(captorQueryContext.capture(), captorConnectionLogDetailQuery.capture());

        assertThat(captorQueryContext.getValue().getOrgId()).isEqualTo("DEFAULT");
        assertThat(captorQueryContext.getValue().getEnvId()).isEqualTo("DEFAULT");

        assertThat(captorConnectionLogDetailQuery.getValue()).isEqualTo(
            MessageLogQuery.builder()
                .filter(MessageLogQuery.Filter.builder().apiId("api-id").requestId("request-id").build())
                .page(1)
                .size(10)
                .build()
        );
    }

    @Test
    void should_return_api_message_logs() throws AnalyticsException {
        final var expectedMessageLog = AggregatedMessageLog.builder()
            .apiId("api-id")
            .clientIdentifier("client-identifier")
            .timestamp("2020-02-01T20:00:00.00Z")
            .requestId("request-id")
            .clientIdentifier("client-identifier")
            .correlationId("correlation-id")
            .parentCorrelationId("parent-correlation-id")
            .operation(MessageOperation.SUBSCRIBE.getLabel())
            .entrypoint(
                AggregatedMessageLog.Message.builder()
                    .connectorId("http-get")
                    .id("message-id")
                    .payload("message-payload")
                    .headers(Map.of("X-Header", List.of("header-value")))
                    .metadata(Map.of("X-Metdata", "metadata-value"))
                    .build()
            )
            .endpoint(
                AggregatedMessageLog.Message.builder()
                    .connectorId("kafka")
                    .id("message-id")
                    .payload("message-payload")
                    .headers(Map.of("X-Header", List.of("header-value")))
                    .metadata(Map.of("X-Metdata", "metadata-value"))
                    .build()
            )
            .build();
        when(logRepository.searchAggregatedMessageLog(any(QueryContext.class), any())).thenReturn(
            new LogResponse<>(1L, List.of(expectedMessageLog))
        );

        var result = cut.searchApiMessageLog(GraviteeContext.getExecutionContext(), "api-id", "request-id", new PageableImpl(1, 10));

        SoftAssertions.assertSoftly(soft -> {
            assertThat(result.total()).isOne();
            assertThat(result.logs()).isEqualTo(
                List.of(
                    io.gravitee.apim.core.log.model.AggregatedMessageLog.builder()
                        .apiId(expectedMessageLog.getApiId())
                        .requestId(expectedMessageLog.getRequestId())
                        .clientIdentifier(expectedMessageLog.getClientIdentifier())
                        .timestamp(expectedMessageLog.getTimestamp())
                        .correlationId(expectedMessageLog.getCorrelationId())
                        .parentCorrelationId("parent-correlation-id")
                        .operation(MessageOperation.fromLabel(expectedMessageLog.getOperation()))
                        .entrypoint(
                            io.gravitee.apim.core.log.model.AggregatedMessageLog.Message.builder()
                                .connectorId("http-get")
                                .id(expectedMessageLog.getEntrypoint().getId())
                                .payload(expectedMessageLog.getEntrypoint().getPayload())
                                .headers(expectedMessageLog.getEntrypoint().getHeaders())
                                .metadata(expectedMessageLog.getEntrypoint().getMetadata())
                                .build()
                        )
                        .endpoint(
                            io.gravitee.apim.core.log.model.AggregatedMessageLog.Message.builder()
                                .connectorId("kafka")
                                .id(expectedMessageLog.getEndpoint().getId())
                                .payload(expectedMessageLog.getEndpoint().getPayload())
                                .headers(expectedMessageLog.getEndpoint().getHeaders())
                                .metadata(expectedMessageLog.getEndpoint().getMetadata())
                                .build()
                        )
                        .build()
                )
            );
        });
    }
}
