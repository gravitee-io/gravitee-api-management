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
package io.gravitee.repository.elasticsearch.v4.log;

import static org.assertj.core.api.Assertions.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import io.gravitee.repository.log.v4.model.message.AggregatedMessageLog;
import io.gravitee.repository.log.v4.model.message.MessageLogQuery;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LogElasticsearchRepositoryTest extends AbstractElasticsearchRepositoryTest {

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private LogElasticsearchRepository logV4Repository;

    @Autowired
    private TimeProvider timeProvider;

    private String today;
    private String yesterday;

    @PostConstruct
    public void init() {
        today = timeProvider.getDateToday();
        yesterday = timeProvider.getDateYesterday();
    }

    @Nested
    class SearchConnectionLogDetail {

        @Test
        void should_return_empty_result() {
            var result = logV4Repository.searchConnectionLogDetail(
                queryContext,
                ConnectionLogDetailQuery.builder()
                    .filter(ConnectionLogDetailQuery.Filter.builder().apiIds(Set.of("notExisting")).build())
                    .build()
            );
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_v4_result() {
            var result = logV4Repository.searchConnectionLogDetail(
                queryContext,
                ConnectionLogDetailQuery.builder()
                    .filter(
                        ConnectionLogDetailQuery.Filter.builder()
                            .apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9"))
                            .requestIds(Set.of("26c61cfc-a4cc-4272-861c-fca4cc2272ab"))
                            .build()
                    )
                    .build()
            );
            assertThat(result).hasValueSatisfying(connectionLogDetail -> {
                assertThat(connectionLogDetail)
                    .hasFieldOrPropertyWithValue("apiId", "f1608475-dd77-4603-a084-75dd775603e9")
                    .hasFieldOrPropertyWithValue("requestId", "26c61cfc-a4cc-4272-861c-fca4cc2272ab")
                    .hasFieldOrPropertyWithValue("timestamp", today + "T06:55:39.245Z")
                    .hasFieldOrPropertyWithValue("clientIdentifier", "12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                    .hasFieldOrPropertyWithValue("requestEnded", true);
                assertThat(connectionLogDetail.getEntrypointRequest())
                    .hasFieldOrPropertyWithValue("method", "POST")
                    .hasFieldOrPropertyWithValue("uri", "/jgi-message-logs-kafka/")
                    .extracting(ConnectionLogDetail.Request::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                    .containsEntry("Accept", List.of("application/json, */*;q=0.5"))
                    .containsEntry("X-Request-ID", List.of("41b21c20b0ffba8f15871e5be2913766"));
                assertThat(connectionLogDetail.getEndpointRequest())
                    .hasFieldOrPropertyWithValue("method", "POST")
                    .hasFieldOrPropertyWithValue("uri", "")
                    .extracting(ConnectionLogDetail.Request::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                    .containsEntry("Accept", List.of("application/json, */*;q=0.5"))
                    .containsEntry("X-Request-ID", List.of("41b21c20b0ffba8f15871e5be2913766"));
                assertThat(connectionLogDetail.getEntrypointResponse())
                    .hasFieldOrPropertyWithValue("status", 200)
                    .extracting(ConnectionLogDetail.Response::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                    .containsAllEntriesOf(
                        Map.of(
                            "X-Gravitee-Client-Identifier",
                            List.of("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0"),
                            "X-Gravitee-Transaction-Id",
                            List.of("26c61cfc-a4cc-4272-861c-fca4cc2272ab"),
                            "X-Gravitee-Request-Id",
                            List.of("26c61cfc-a4cc-4272-861c-fca4cc2272ab")
                        )
                    );
                assertThat(connectionLogDetail.getEndpointResponse())
                    .hasFieldOrPropertyWithValue("status", 200)
                    .extracting(ConnectionLogDetail.Response::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                    .isEmpty();
            });
        }

        @Test
        void should_return_v2_result() {
            var result = logV4Repository.searchConnectionLogDetail(
                queryContext,
                ConnectionLogDetailQuery.builder()
                    .filter(ConnectionLogDetailQuery.Filter.builder().requestIds(Set.of("29381bce-df59-47b2-b81b-cedf59c7b23b")).build())
                    .build()
            );
            assertThat(result).hasValueSatisfying(connectionLogDetail -> {
                assertThat(connectionLogDetail)
                    .hasFieldOrPropertyWithValue("apiId", "be0aa9c9-ca1c-4d0a-8aa9-c9ca1c5d0aab")
                    .hasFieldOrPropertyWithValue("requestId", "29381bce-df59-47b2-b81b-cedf59c7b23b")
                    .hasFieldOrPropertyWithValue("clientIdentifier", null)
                    .hasFieldOrPropertyWithValue("requestEnded", true);
                assertThat(connectionLogDetail.getEntrypointRequest())
                    .hasFieldOrPropertyWithValue("method", "POST")
                    .hasFieldOrPropertyWithValue("uri", "/stocks?api-key=a9c898c4-d2f8-49e7-8385-48ab5250dd5b")
                    .extracting(ConnectionLogDetail.Request::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                    .containsEntry("Connection", List.of("keep-alive"))
                    .containsEntry("Content-Length", List.of("21"))
                    .containsEntry("Cache-Control", List.of("no-cache"))
                    .containsEntry("Origin", List.of("chrome-extension://fdmmgilgnpjigdojojpjoooidkmcomcm"))
                    .containsEntry("X-Gravitee-Transaction-Id", List.of("b127e629-3273-4b3f-a7e6-2932736b3ffb"));
                assertThat(connectionLogDetail.getEndpointRequest()).isNull();
                assertThat(connectionLogDetail.getEntrypointResponse())
                    .hasFieldOrPropertyWithValue("status", 403)
                    .hasFieldOrPropertyWithValue(
                        "body",
                        """
                        {
                          "message" : "API Key is not valid or is expired / revoked.",
                          "http_status_code" : 403
                        }"""
                    )
                    .extracting(ConnectionLogDetail.Response::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                    .containsAllEntriesOf(
                        Map.of(
                            "Content-Length",
                            List.of("93"),
                            "X-Gravitee-Transaction-Id",
                            List.of("b127e629-3273-4b3f-a7e6-2932736b3ffb")
                        )
                    );
                assertThat(connectionLogDetail.getEndpointResponse()).isNull();
            });
        }
    }

    @Nested
    class SearchConnectionLogDetailsV2AndV4 {

        @Test
        void should_return_empty_result() {
            var result = logV4Repository.searchConnectionLogDetails(
                queryContext,
                ConnectionLogDetailQuery.builder()
                    .filter(ConnectionLogDetailQuery.Filter.builder().apiIds(Set.of("notExisting")).build())
                    .build()
            );
            assertThat(result.data()).isEmpty();
        }

        @Test
        void should_return_result_for_v4_api() {
            var result = logV4Repository.searchConnectionLogDetails(
                queryContext,
                ConnectionLogDetailQuery.builder()
                    .filter(
                        ConnectionLogDetailQuery.Filter.builder()
                            .apiIds(Set.of("f1608475-dd77-4603-a084-75dd775603e9"))
                            .methods(Set.of(HttpMethod.DELETE, HttpMethod.POST))
                            .build()
                    )
                    .build()
            );
            assertThat(result.total()).isEqualTo(3L);
            var connectionLogDetail = result.data().getFirst();
            assertThat(connectionLogDetail)
                .hasFieldOrPropertyWithValue("apiId", "f1608475-dd77-4603-a084-75dd775603e9")
                .hasFieldOrPropertyWithValue("requestId", "26c61cfc-a4cc-4272-861c-fca4cc2272ab")
                .hasFieldOrPropertyWithValue("timestamp", today + "T06:55:39.245Z")
                .hasFieldOrPropertyWithValue("clientIdentifier", "12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                .hasFieldOrPropertyWithValue("requestEnded", true);
            assertThat(connectionLogDetail.getEntrypointRequest())
                .hasFieldOrPropertyWithValue("method", "POST")
                .hasFieldOrPropertyWithValue("uri", "/jgi-message-logs-kafka/")
                .extracting(ConnectionLogDetail.Request::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                .containsEntry("Accept", List.of("application/json, */*;q=0.5"))
                .containsEntry("X-Request-ID", List.of("41b21c20b0ffba8f15871e5be2913766"));
            assertThat(connectionLogDetail.getEndpointRequest())
                .hasFieldOrPropertyWithValue("method", "POST")
                .hasFieldOrPropertyWithValue("uri", "")
                .extracting(ConnectionLogDetail.Request::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                .containsEntry("Accept", List.of("application/json, */*;q=0.5"))
                .containsEntry("X-Request-ID", List.of("41b21c20b0ffba8f15871e5be2913766"));
            assertThat(connectionLogDetail.getEntrypointResponse())
                .hasFieldOrPropertyWithValue("status", 200)
                .extracting(ConnectionLogDetail.Response::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                .containsAllEntriesOf(
                    Map.of(
                        "X-Gravitee-Client-Identifier",
                        List.of("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0"),
                        "X-Gravitee-Transaction-Id",
                        List.of("26c61cfc-a4cc-4272-861c-fca4cc2272ab"),
                        "X-Gravitee-Request-Id",
                        List.of("26c61cfc-a4cc-4272-861c-fca4cc2272ab")
                    )
                );
            assertThat(connectionLogDetail.getEndpointResponse())
                .hasFieldOrPropertyWithValue("status", 200)
                .extracting(ConnectionLogDetail.Response::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                .isEmpty();
        }

        @Test
        void should_return_result_querying_v2_and_v4_api_logs() {
            var result = logV4Repository.searchConnectionLogDetails(
                queryContext,
                ConnectionLogDetailQuery.builder()
                    .filter(
                        ConnectionLogDetailQuery.Filter.builder()
                            .methods(Set.of(HttpMethod.DELETE, HttpMethod.POST))
                            .bodyText("API Key")
                            .build()
                    )
                    .build()
            );
            assertThat(result.total()).isEqualTo(7L);

            // Oldest and found in the v2 API index
            assertThat(result.data().get(6))
                .extracting(ConnectionLogDetail::getRequestId)
                .isEqualTo("29381bce-df59-47b2-b81b-cedf59c7b23b");

            // Newest and found in the v4 API index
            var mostRecentConnectionLogDetail = result.data().getFirst();
            assertThat(mostRecentConnectionLogDetail)
                .hasFieldOrPropertyWithValue("apiId", "f1608475-dd77-4603-a084-75dd775603e9")
                .hasFieldOrPropertyWithValue("requestId", "26c61cfc-a4cc-4272-861c-fca4cc2272ab")
                .hasFieldOrPropertyWithValue("timestamp", today + "T06:55:39.245Z")
                .hasFieldOrPropertyWithValue("clientIdentifier", "12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                .hasFieldOrPropertyWithValue("requestEnded", true);
            assertThat(mostRecentConnectionLogDetail.getEntrypointRequest())
                .hasFieldOrPropertyWithValue("method", "POST")
                .hasFieldOrPropertyWithValue("uri", "/jgi-message-logs-kafka/")
                .extracting(ConnectionLogDetail.Request::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                .containsEntry("Accept", List.of("application/json, */*;q=0.5"))
                .containsEntry("X-Request-ID", List.of("41b21c20b0ffba8f15871e5be2913766"));
            assertThat(mostRecentConnectionLogDetail.getEndpointRequest())
                .hasFieldOrPropertyWithValue("method", "POST")
                .hasFieldOrPropertyWithValue("uri", "")
                .extracting(ConnectionLogDetail.Request::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                .containsEntry("Accept", List.of("application/json, */*;q=0.5"))
                .containsEntry("X-Request-ID", List.of("41b21c20b0ffba8f15871e5be2913766"));
            assertThat(mostRecentConnectionLogDetail.getEntrypointResponse())
                .hasFieldOrPropertyWithValue("status", 200)
                .extracting(ConnectionLogDetail.Response::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                .containsAllEntriesOf(
                    Map.of(
                        "X-Gravitee-Client-Identifier",
                        List.of("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0"),
                        "X-Gravitee-Transaction-Id",
                        List.of("26c61cfc-a4cc-4272-861c-fca4cc2272ab"),
                        "X-Gravitee-Request-Id",
                        List.of("26c61cfc-a4cc-4272-861c-fca4cc2272ab")
                    )
                );
            assertThat(mostRecentConnectionLogDetail.getEndpointResponse())
                .hasFieldOrPropertyWithValue("status", 200)
                .extracting(ConnectionLogDetail.Response::getHeaders, as(InstanceOfAssertFactories.map(String.class, List.class)))
                .isEmpty();
        }
    }

    @Nested
    class SearchAggregateMessageLog {

        @Test
        void should_return_aggregated_message_log_with_only_entrypoint() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery.builder()
                    .filter(
                        MessageLogQuery.Filter.builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("bf98c96b-fb84-4e25-98c9-6bfb84fe257a")
                            .build()
                    )
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isOne();
            assertThat(result.data()).isEqualTo(
                List.of(
                    AggregatedMessageLog.builder()
                        .requestId("bf98c96b-fb84-4e25-98c9-6bfb84fe257a")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .correlationId("aed3a207-d5c0-4073-93a2-07d5c0007336")
                        .operation("subscribe")
                        .timestamp(yesterday + "T14:08:45.994Z")
                        .entrypoint(
                            AggregatedMessageLog.Message.builder()
                                .id("0")
                                .timestamp(yesterday + "T14:08:45.994Z")
                                .connectorId("http-get")
                                .payload("message")
                                .headers(Map.of("X-Header", List.of("http-get-header")))
                                .metadata(Map.of("MessageMetadata", "http-get-metadata"))
                                .build()
                        )
                        .build()
                )
            );
        }

        @Test
        void should_return_aggregated_message_log_with_only_endpoint() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery.builder()
                    .filter(
                        MessageLogQuery.Filter.builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("96b7d777-36f7-49c1-a5ad-993ad2ae64cb")
                            .build()
                    )
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isOne();
            assertThat(result.data()).isEqualTo(
                List.of(
                    AggregatedMessageLog.builder()
                        .requestId("96b7d777-36f7-49c1-a5ad-993ad2ae64cb")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .correlationId("ebaa9b08-eac8-490d-aa9b-08eac8590d3c")
                        .operation("subscribe")
                        .timestamp(yesterday + "T14:08:59.901Z")
                        .endpoint(
                            AggregatedMessageLog.Message.builder()
                                .id("0")
                                .timestamp(yesterday + "T14:08:59.901Z")
                                .connectorId("kafka")
                                .payload("message")
                                .headers(Map.of("X-Header", List.of("kafka-header")))
                                .metadata(Map.of("MessageMetadata", "kafka-metadata"))
                                .build()
                        )
                        .build()
                )
            );
        }

        @Test
        void should_return_aggregated_message_log_with_entrypoint_and_endpoint_for_publish_operation() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery.builder()
                    .filter(
                        MessageLogQuery.Filter.builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41")
                            .build()
                    )
                    .size(1)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isOne();
            assertThat(result.data()).isEqualTo(
                List.of(
                    AggregatedMessageLog.builder()
                        .requestId("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .correlationId("3aa93e93-eaa3-4fcd-a93e-93eaa3bfcd41")
                        .operation("publish")
                        .timestamp(yesterday + "T13:51:36.161Z")
                        .entrypoint(
                            AggregatedMessageLog.Message.builder()
                                .id("0")
                                .timestamp(yesterday + "T13:51:36.161Z")
                                .connectorId("http-post")
                                .payload("message")
                                .headers(Map.of("X-Header", List.of("http-post-header")))
                                .metadata(Map.of("MessageMetadata", "http-post-metadata"))
                                .build()
                        )
                        .endpoint(
                            AggregatedMessageLog.Message.builder()
                                .id("0")
                                .timestamp(yesterday + "T13:51:37.161Z")
                                .connectorId("kafka")
                                .payload("message")
                                .headers(Map.of("X-Header", List.of("kafka-header")))
                                .metadata(Map.of("MessageMetadata", "kafka-metadata"))
                                .build()
                        )
                        .build()
                )
            );
        }

        @Test
        void should_return_aggregated_message_log_with_entrypoint_and_endpoint_for_subscribe_operation() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery.builder()
                    .filter(
                        MessageLogQuery.Filter.builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4")
                            .build()
                    )
                    .size(1)
                    .build()
            );
            assertThat(result).isNotNull();
            assertThat(result.total()).isOne();
            assertThat(result.data()).isEqualTo(
                List.of(
                    AggregatedMessageLog.builder()
                        .requestId("5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4")
                        .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                        .clientIdentifier("12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0")
                        .correlationId("5fc3b3e5-7aa7-408e-83b3-e57aa7708ed4")
                        .operation("subscribe")
                        .timestamp(yesterday + "T06:54:30.047Z")
                        .entrypoint(
                            AggregatedMessageLog.Message.builder()
                                .id("0")
                                .timestamp(yesterday + "T06:55:39.245Z")
                                .connectorId("http-get")
                                .payload("message")
                                .headers(Map.of("X-Header", List.of("http-get-header")))
                                .metadata(Map.of("MessageMetadata", "http-get-metadata"))
                                .build()
                        )
                        .endpoint(
                            AggregatedMessageLog.Message.builder()
                                .id("0")
                                .timestamp(yesterday + "T06:54:30.047Z")
                                .connectorId("kafka")
                                .payload("message")
                                .headers(Map.of("X-Header", List.of("kafka-header")))
                                .metadata(Map.of("MessageMetadata", "kafka-metadata"))
                                .build()
                        )
                        .build()
                )
            );
        }

        @Test
        void should_return_the_1st_page_of_aggregated_message_logs_of_an_api_and_request_id() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery.builder()
                    .filter(
                        MessageLogQuery.Filter.builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("d789ffdc-d092-4675-97a9-213cf569350d")
                            .build()
                    )
                    .size(1)
                    .build()
            );

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result).isNotNull();
                soft.assertThat(result.total()).isEqualTo(2);
                soft
                    .assertThat(result.data())
                    .extracting(AggregatedMessageLog::getRequestId, AggregatedMessageLog::getCorrelationId)
                    .containsExactly(tuple("d789ffdc-d092-4675-97a9-213cf569350d", "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea49"));
            });
        }

        @Test
        void should_return_the_2nd_page_of_aggregated_message_logs_of_an_api_and_request_id() {
            var result = logV4Repository.searchAggregatedMessageLog(
                queryContext,
                MessageLogQuery.builder()
                    .filter(
                        MessageLogQuery.Filter.builder()
                            .apiId("f1608475-dd77-4603-a084-75dd775603e9")
                            .requestId("d789ffdc-d092-4675-97a9-213cf569350d")
                            .build()
                    )
                    .size(1)
                    .page(2)
                    .build()
            );

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result).isNotNull();
                soft.assertThat(result.total()).isEqualTo(2);
                soft
                    .assertThat(result.data())
                    .extracting(AggregatedMessageLog::getRequestId, AggregatedMessageLog::getCorrelationId)
                    .containsExactly(tuple("d789ffdc-d092-4675-97a9-213cf569350d", "8d6d8bd5-bc42-4aea-ad8b-d5bc421aea48"));
            });
        }
    }
}
