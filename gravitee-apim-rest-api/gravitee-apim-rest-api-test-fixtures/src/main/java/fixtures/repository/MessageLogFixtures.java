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
package fixtures.repository;

import io.gravitee.rest.api.model.v4.connector.ConnectorType;
import io.gravitee.rest.api.model.v4.log.message.BaseMessageLog;
import io.gravitee.rest.api.model.v4.log.message.MessageOperation;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MessageLogFixtures {

    private static Supplier<BaseMessageLog.BaseMessageLogBuilder<?, ?>> BASE_BUILDER_SUPPLIER = () ->
        BaseMessageLog
            .builder()
            .apiId("api-id")
            .requestId("request-id")
            .clientIdentifier("client-identifier")
            .correlationId("correlation-id")
            .parentCorrelationId("parent-correlation-id")
            .operation(MessageOperation.SUBSCRIBE)
            .connectorType(ConnectorType.ENTRYPOINT)
            .connectorId("http-get")
            .message(
                BaseMessageLog.Message
                    .builder()
                    .id("message-id")
                    .payload("message-payload")
                    .isError(false)
                    .headers(Map.of("X-Header", List.of("header-value")))
                    .metadata(Map.of("X-Metdata", "metadata-value"))
                    .build()
            )
            .timestamp("2020-02-01T20:00:00.00Z");

    private final BaseMessageLog.BaseMessageLogBuilder<?, ?> initialMessageBuilder;

    public MessageLogFixtures(String defaultApiId) {
        initialMessageBuilder = BASE_BUILDER_SUPPLIER.get().apiId(defaultApiId);
    }

    public MessageLogFixtures(String defaultApiId, String defaultRequestId) {
        initialMessageBuilder = BASE_BUILDER_SUPPLIER.get().apiId(defaultApiId).requestId(defaultRequestId);
    }

    public BaseMessageLog aMessageLog() {
        return initialMessageBuilder.build();
    }

    public BaseMessageLog aMessageLogWithMessageId(String messageId) {
        final BaseMessageLog messageLog = initialMessageBuilder.build();
        messageLog.setMessage(messageLog.getMessage().toBuilder().id(messageId).build());
        return messageLog;
    }

    public BaseMessageLog aMessageLog(String requestId) {
        return initialMessageBuilder.requestId(requestId).build();
    }
}
