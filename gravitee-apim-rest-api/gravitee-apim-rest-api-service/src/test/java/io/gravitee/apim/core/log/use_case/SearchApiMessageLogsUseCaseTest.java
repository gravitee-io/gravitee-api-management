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
package io.gravitee.apim.core.log.use_case;

import static fixtures.core.log.model.MessageLogFixtures.aMessageLog;
import static org.assertj.core.api.Assertions.tuple;

import inmemory.MessageLogCrudServiceInMemory;
import io.gravitee.apim.core.log.model.AggregatedMessageLog;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.stream.IntStream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
class SearchApiMessageLogsUseCaseTest {

    private static final String API_ID = "my-api";
    private static final String REQUEST_ID = "request-id";
    private SearchApiMessageLogsUseCase usecase;
    private final MessageLogCrudServiceInMemory messageLogStorageService = new MessageLogCrudServiceInMemory();

    @BeforeEach
    void setUp() {
        usecase = new SearchApiMessageLogsUseCase(messageLogStorageService);
    }

    @AfterEach
    void tearDown() {
        messageLogStorageService.reset();
        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_messages_logs_of_an_api() {
        var expectedMessageLog = aMessageLog(API_ID, REQUEST_ID);
        messageLogStorageService.initWith(
            List.of(
                expectedMessageLog,
                aMessageLog("other-api", "a-request-id"),
                aMessageLog(API_ID, "other-request-id"),
                aMessageLog("other-api", "another-request-id")
            )
        );

        var result = usecase.execute(GraviteeContext.getExecutionContext(), new SearchApiMessageLogsUseCase.Input(API_ID, REQUEST_ID));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isOne();
            soft
                .assertThat(result.data())
                .isEqualTo(
                    List.of(
                        AggregatedMessageLog.builder()
                            .apiId(API_ID)
                            .requestId(REQUEST_ID)
                            .apiId(API_ID)
                            .clientIdentifier(expectedMessageLog.getClientIdentifier())
                            .correlationId(expectedMessageLog.getCorrelationId())
                            .parentCorrelationId(expectedMessageLog.getParentCorrelationId())
                            .timestamp(expectedMessageLog.getTimestamp())
                            .operation(expectedMessageLog.getOperation())
                            .entrypoint(expectedMessageLog.getEntrypoint())
                            .endpoint(expectedMessageLog.getEndpoint())
                            .build()
                    )
                );
        });
    }

    @Test
    void should_return_api_message_logs_sorted_by_desc_timestamp() {
        messageLogStorageService.initWith(
            List.of(
                aMessageLog(API_ID, REQUEST_ID).toBuilder().correlationId("correlation-1").timestamp("2020-02-01T20:00:00.00Z").build(),
                aMessageLog(API_ID, REQUEST_ID).toBuilder().correlationId("correlation-2").timestamp("2020-02-02T20:00:00.00Z").build(),
                aMessageLog(API_ID, REQUEST_ID).toBuilder().correlationId("correlation-3").timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = usecase.execute(GraviteeContext.getExecutionContext(), new SearchApiMessageLogsUseCase.Input(API_ID, REQUEST_ID));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(3);
            soft
                .assertThat(result.data())
                .extracting(AggregatedMessageLog::getCorrelationId, AggregatedMessageLog::getTimestamp)
                .containsExactly(
                    tuple("correlation-3", "2020-02-04T20:00:00.00Z"),
                    tuple("correlation-2", "2020-02-02T20:00:00.00Z"),
                    tuple("correlation-1", "2020-02-01T20:00:00.00Z")
                );
        });
    }

    @Test
    void should_return_the_page_requested() {
        var expectedTotal = 15;
        var pageNumber = 2;
        var pageSize = 5;
        messageLogStorageService.initWith(
            IntStream.range(0, expectedTotal)
                .mapToObj(i -> aMessageLog(API_ID, REQUEST_ID).toBuilder().correlationId(String.valueOf(i)).build())
                .toList()
        );

        var result = usecase.execute(
            GraviteeContext.getExecutionContext(),
            new SearchApiMessageLogsUseCase.Input(API_ID, REQUEST_ID, new PageableImpl(pageNumber, pageSize))
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(expectedTotal);
            soft.assertThat(result.data()).extracting(AggregatedMessageLog::getCorrelationId).containsExactly("5", "6", "7", "8", "9");
        });
    }
}
