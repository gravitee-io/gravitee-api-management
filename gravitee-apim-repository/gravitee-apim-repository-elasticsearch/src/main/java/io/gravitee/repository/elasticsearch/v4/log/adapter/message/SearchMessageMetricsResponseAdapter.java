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

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.repository.log.v4.model.message.MessageMetrics;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchMessageMetricsResponseAdapter {

    private SearchMessageMetricsResponseAdapter() {}

    public static List<MessageMetrics> adapt(SearchHits hits) {
        if (hits == null || hits.getHits() == null) {
            return Collections.emptyList();
        }

        return hits.getHits().stream().map(SearchHit::getSource).map(SearchMessageMetricsResponseAdapter::map).toList();
    }

    private static MessageMetrics map(JsonNode source) {
        return MessageMetrics.builder()
            .timestamp(asTextOrNull(source.get("@timestamp")))
            .apiId(asTextOrNull(source.get("api-id")))
            .apiName(asTextOrNull(source.get("api-name")))
            .requestId(asTextOrNull(source.get("request-id")))
            .clientIdentifier(asTextOrNull(source.get("client-identifier")))
            .correlationId(asTextOrNull(source.get("correlation-id")))
            .operation(asTextOrNull(source.get("operation")))
            .connectorType(asTextOrNull(source.get("connector-type")))
            .connectorId(asTextOrNull(source.get("connector-id")))
            .gateway(asTextOrNull(source.get("gateway")))
            .gatewayLatencyMs(asLongOr(source.get("gateway-latency-ms"), 0L))
            .contentLength(asLongOr(source.get("content-length"), 0L))
            .error(asBooleanOrFalse(source.get("error")))
            .count(asLongOr(source.get("count"), 0L))
            .countIncrement(asLongOr(source.get("count-increment"), 0L))
            .errorCount(asLongOr(source.get("error-count"), 0L))
            .errorCountIncrement(asLongOr(source.get("error-count-increment"), 0L))
            .custom(asMapOrNull(source.get("custom")))
            .additionalMetrics(asMapOrNull(source.get("additional-metrics")))
            .build();
    }
}
