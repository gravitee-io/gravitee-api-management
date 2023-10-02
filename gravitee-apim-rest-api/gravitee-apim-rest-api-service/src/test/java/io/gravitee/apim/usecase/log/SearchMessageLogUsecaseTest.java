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
package io.gravitee.apim.usecase.log;

import static org.assertj.core.api.Assertions.tuple;

import fixtures.repository.MessageLogFixtures;
import inmemory.MessageLogCrudServiceInMemory;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.message.BaseMessageLog;
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
class SearchMessageLogUsecaseTest {

    private static final String API_ID = "my-api";
    private static final String REQUEST_ID = "request-id";
    private SearchMessageLogUsecase usecase;
    private final MessageLogCrudServiceInMemory messageLogStorageService = new MessageLogCrudServiceInMemory();
    private final MessageLogFixtures messageLogFixtures = new MessageLogFixtures(API_ID);

    @BeforeEach
    void setUp() {
        usecase = new SearchMessageLogUsecase(messageLogStorageService);
    }

    @AfterEach
    void tearDown() {
        messageLogStorageService.reset();
        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_messages_logs_of_an_api() {
        final BaseMessageLog expectedMessageLog = messageLogFixtures.aMessageLog(REQUEST_ID).toBuilder().build();
        messageLogStorageService.initWith(
            List.of(
                expectedMessageLog,
                messageLogFixtures.aMessageLog().toBuilder().apiId("other-api").build(),
                messageLogFixtures.aMessageLog("other-request-id").toBuilder().build(),
                messageLogFixtures.aMessageLog("another-request-id").toBuilder().apiId("other-api").build()
            )
        );

        var result = usecase.execute(new SearchMessageLogUsecase.Request(API_ID, REQUEST_ID));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isOne();
            soft
                .assertThat(result.data())
                .isEqualTo(
                    List.of(
                        BaseMessageLog
                            .builder()
                            .apiId(API_ID)
                            .requestId(REQUEST_ID)
                            .apiId(API_ID)
                            .clientIdentifier(expectedMessageLog.getClientIdentifier())
                            .correlationId(expectedMessageLog.getCorrelationId())
                            .parentCorrelationId(expectedMessageLog.getParentCorrelationId())
                            .operation(expectedMessageLog.getOperation())
                            .connectorType(expectedMessageLog.getConnectorType())
                            .connectorId(expectedMessageLog.getConnectorId())
                            .timestamp(expectedMessageLog.getTimestamp())
                            .message(expectedMessageLog.getMessage())
                            .build()
                    )
                );
        });
    }

    @Test
    void should_return_api_message_logs_sorted_by_desc_timestamp() {
        messageLogStorageService.initWith(
            List.of(
                messageLogFixtures.aMessageLogWithMessageId("msg1").toBuilder().timestamp("2020-02-01T20:00:00.00Z").build(),
                messageLogFixtures.aMessageLogWithMessageId("msg2").toBuilder().timestamp("2020-02-02T20:00:00.00Z").build(),
                messageLogFixtures.aMessageLogWithMessageId("msg3").toBuilder().timestamp("2020-02-04T20:00:00.00Z").build()
            )
        );

        var result = usecase.execute(new SearchMessageLogUsecase.Request(API_ID, REQUEST_ID));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(3);
            soft
                .assertThat(result.data())
                .extracting(messageLog -> messageLog.getMessage().getId(), BaseMessageLog::getTimestamp)
                .containsExactly(
                    tuple("msg3", "2020-02-04T20:00:00.00Z"),
                    tuple("msg2", "2020-02-02T20:00:00.00Z"),
                    tuple("msg1", "2020-02-01T20:00:00.00Z")
                );
        });
    }

    @Test
    void should_return_the_page_requested() {
        var expectedTotal = 15;
        var pageNumber = 2;
        var pageSize = 5;
        messageLogStorageService.initWith(
            IntStream.range(0, expectedTotal).mapToObj(i -> messageLogFixtures.aMessageLogWithMessageId(String.valueOf(i))).toList()
        );

        var result = usecase.execute(new SearchMessageLogUsecase.Request(API_ID, REQUEST_ID, new PageableImpl(pageNumber, pageSize)));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.total()).isEqualTo(expectedTotal);
            soft
                .assertThat(result.data())
                .extracting(messageLog -> messageLog.getMessage().getId())
                .containsExactly("5", "6", "7", "8", "9");
        });
    }
}
