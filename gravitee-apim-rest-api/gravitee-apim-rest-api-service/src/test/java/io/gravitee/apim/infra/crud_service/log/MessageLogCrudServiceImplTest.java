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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.log.crud_service.MessageLogCrudService;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.message.MessageLog;
import io.gravitee.repository.log.v4.model.message.MessageLogQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.connector.ConnectorType;
import io.gravitee.rest.api.model.v4.log.message.BaseMessageLog;
import io.gravitee.rest.api.model.v4.log.message.MessageOperation;
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
        when(logRepository.searchMessageLog(any())).thenReturn(new LogResponse<>(0L, List.of()));

        cut.searchApiMessageLog("api-id", "request-id", new PageableImpl(1, 10));

        var captor = ArgumentCaptor.forClass(MessageLogQuery.class);
        verify(logRepository).searchMessageLog(captor.capture());

        assertThat(captor.getValue())
            .isEqualTo(
                MessageLogQuery
                    .builder()
                    .filter(MessageLogQuery.Filter.builder().apiId("api-id").requestId("request-id").build())
                    .page(1)
                    .size(10)
                    .build()
            );
    }

    @Test
    void should_return_api_message_logs() throws AnalyticsException {
        final MessageLog expectedMessageLog = MessageLog
            .builder()
            .apiId("api-id")
            .clientIdentifier("client-identifier")
            .timestamp("2020-02-01T20:00:00.00Z")
            .requestId("request-id")
            .clientIdentifier("client-identifier")
            .correlationId("correlation-id")
            .parentCorrelationId("parent-correlation-id")
            .connectorType(ConnectorType.ENTRYPOINT.getLabel())
            .connectorId("http-get")
            .operation(MessageOperation.SUBSCRIBE.getLabel())
            .message(
                MessageLog.Message
                    .builder()
                    .id("message-id")
                    .payload("message-payload")
                    .headers(Map.of("X-Header", List.of("header-value")))
                    .metadata(Map.of("X-Metdata", "metadata-value"))
                    .build()
            )
            .build();
        when(logRepository.searchMessageLog(any())).thenReturn(new LogResponse<>(1L, List.of(expectedMessageLog)));

        var result = cut.searchApiMessageLog("api-id", "request-id", new PageableImpl(1, 10));

        SoftAssertions.assertSoftly(soft -> {
            assertThat(result.total()).isOne();
            assertThat(result.logs())
                .isEqualTo(
                    List.of(
                        BaseMessageLog
                            .builder()
                            .apiId(expectedMessageLog.getApiId())
                            .requestId(expectedMessageLog.getRequestId())
                            .clientIdentifier(expectedMessageLog.getClientIdentifier())
                            .timestamp(expectedMessageLog.getTimestamp())
                            .correlationId(expectedMessageLog.getCorrelationId())
                            .parentCorrelationId("parent-correlation-id")
                            .connectorType(ConnectorType.fromLabel(expectedMessageLog.getConnectorType()))
                            .connectorId("http-get")
                            .operation(MessageOperation.fromLabel(expectedMessageLog.getOperation()))
                            .message(
                                BaseMessageLog.Message
                                    .builder()
                                    .id(expectedMessageLog.getMessage().getId())
                                    .payload(expectedMessageLog.getMessage().getPayload())
                                    .headers(expectedMessageLog.getMessage().getHeaders())
                                    .metadata(expectedMessageLog.getMessage().getMetadata())
                                    .build()
                            )
                            .build()
                    )
                );
        });
    }
}
