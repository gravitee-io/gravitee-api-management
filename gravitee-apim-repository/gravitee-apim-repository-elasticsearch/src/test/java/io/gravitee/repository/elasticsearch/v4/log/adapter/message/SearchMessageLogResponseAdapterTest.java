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
package io.gravitee.repository.elasticsearch.v4.log.adapter.message;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.elasticsearch.AbstractAdapterTest;
import io.gravitee.repository.log.v4.model.message.AggregatedMessageLog;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SearchMessageLogResponseAdapterTest extends AbstractAdapterTest {

    @Test
    void should_return_empty_when_hit_list_is_empty() {
        assertThat(SearchMessageLogResponseAdapter.adapt(new SearchHits())).isEmpty();
    }

    @Test
    void should_return_empty_when_hit_is_null() {
        assertThat(SearchMessageLogResponseAdapter.adapt(null)).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generate")
    void should_build_message_log(String hit, List<AggregatedMessageLog> expected) {
        final SearchResponse searchResponse = buildSearchHit(hit);

        final List<AggregatedMessageLog> result = SearchMessageLogResponseAdapter.adapt(searchResponse.getSearchHits());

        assertThat(result).isEqualTo(expected);
    }

    private static Stream<Arguments> generate() {
        AggregatedMessageLog.AggregatedMessageLogBuilder messageLog = AggregatedMessageLog.builder()
            .apiId("api-id")
            .clientIdentifier("client-identifier")
            .requestId("request-id")
            .correlationId("correlation-id")
            .operation("subscribe")
            .timestamp("2023-11-07T15:18:56.868+01:00");

        AggregatedMessageLog.Message.MessageBuilder message = AggregatedMessageLog.Message.builder()
            .id("message-id")
            .timestamp("2023-11-07T15:18:56.868+01:00")
            .connectorId("connector-id")
            .isError(false);

        return Stream.of(
            Arguments.of(
                "message-log.json",
                List.of(
                    messageLog
                        .endpoint(
                            message.payload("mock message").headers(Map.of("foo", List.of("bar"))).metadata(Map.of("foo", "bar")).build()
                        )
                        .build()
                )
            ),
            Arguments.of(
                "message-without-headers.json",
                List.of(messageLog.endpoint(message.payload("mock message").headers(null).metadata(Map.of("foo", "bar")).build()).build())
            ),
            Arguments.of(
                "message-without-metadata.json",
                List.of(
                    messageLog
                        .endpoint(message.payload("mock message").headers(Map.of("foo", List.of("bar"))).metadata(null).build())
                        .build()
                )
            ),
            Arguments.of(
                "message-without-payload.json",
                List.of(
                    messageLog
                        .endpoint(message.payload(null).headers(Map.of("foo", List.of("bar"))).metadata(Map.of("foo", "bar")).build())
                        .build()
                )
            )
        );
    }
}
