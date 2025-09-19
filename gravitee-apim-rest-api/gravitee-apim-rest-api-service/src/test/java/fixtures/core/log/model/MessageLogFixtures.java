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
package fixtures.core.log.model;

import io.gravitee.apim.core.log.model.AggregatedMessageLog;
import io.gravitee.apim.core.log.model.MessageOperation;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MessageLogFixtures {

    private static final Supplier<AggregatedMessageLog.AggregatedMessageLogBuilder> BASE_BUILDER_SUPPLIER = () ->
        AggregatedMessageLog.builder()
            .requestId("request-id")
            .clientIdentifier("client-identifier")
            .correlationId("correlation-id")
            .parentCorrelationId("parent-correlation-id")
            .operation(MessageOperation.SUBSCRIBE)
            .entrypoint(
                AggregatedMessageLog.Message.builder()
                    .id("message-id")
                    .connectorId("http-get")
                    .payload("message-payload")
                    .isError(false)
                    .headers(Map.of("X-Header", List.of("header-value")))
                    .metadata(Map.of("X-Metdata", "metadata-value"))
                    .build()
            )
            .endpoint(
                AggregatedMessageLog.Message.builder()
                    .id("message-id")
                    .connectorId("kafka")
                    .payload("message-payload")
                    .isError(false)
                    .headers(Map.of("X-Header", List.of("header-value")))
                    .metadata(Map.of("X-Metdata", "metadata-value"))
                    .build()
            )
            .timestamp("2020-02-01T20:00:00.00Z");

    public static AggregatedMessageLog aMessageLog() {
        return BASE_BUILDER_SUPPLIER.get().build();
    }

    public static AggregatedMessageLog aMessageLog(String apiId, String requestId) {
        return BASE_BUILDER_SUPPLIER.get().apiId(apiId).requestId(requestId).build();
    }
}
