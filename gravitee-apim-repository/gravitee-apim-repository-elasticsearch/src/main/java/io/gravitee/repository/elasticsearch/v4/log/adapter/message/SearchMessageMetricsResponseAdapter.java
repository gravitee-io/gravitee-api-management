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
import static io.gravitee.repository.elasticsearch.v4.log.adapter.message.MessageMetricsFields.*;

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
            .timestamp(asTextOrNull(source.get(MessageMetricsFields.TIMESTAMP)))
            .apiId(asTextOrNull(source.get(API_ID)))
            .apiName(asTextOrNull(source.get(API_NAME)))
            .requestId(asTextOrNull(source.get(REQUEST_ID)))
            .clientIdentifier(asTextOrNull(source.get(CLIENT_IDENTIFIER)))
            .correlationId(asTextOrNull(source.get(CORRELATION_ID)))
            .operation(asTextOrNull(source.get(OPERATION)))
            .connectorType(asTextOrNull(source.get(CONNECTOR_TYPE)))
            .connectorId(asTextOrNull(source.get(CONNECTOR_ID)))
            .gateway(asTextOrNull(source.get(GATEWAY)))
            .gatewayLatencyMs(asLongOr(source.get(GATEWAY_LATENCY_MS), 0L))
            .contentLength(asLongOr(source.get(CONTENT_LENGTH), 0L))
            .error(asBooleanOrFalse(source.get(ERROR)))
            .count(asLongOr(source.get(COUNT), 0L))
            .countIncrement(asLongOr(source.get(COUNT_INCREMENT), 0L))
            .errorCount(asLongOr(source.get(ERROR_COUNT), 0L))
            .errorCountIncrement(asLongOr(source.get(ERROR_COUNT_INCREMENT), 0L))
            .custom(asMapOrNull(source.get(CUSTOM)))
            .additionalMetrics(asMapOrNull(source.get(ADDITIONAL_METRICS)))
            .build();
    }
}
