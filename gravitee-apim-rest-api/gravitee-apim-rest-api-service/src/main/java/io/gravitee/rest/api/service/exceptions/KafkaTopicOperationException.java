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
package io.gravitee.rest.api.service.exceptions;

import io.gravitee.common.http.HttpStatusCode;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when a Kafka topic operation fails.
 *
 * @author Gravitee Team
 */
public class KafkaTopicOperationException extends AbstractManagementException {

    private final String operation;
    private final String topicName;

    public KafkaTopicOperationException(String operation, String topicName, Throwable cause) {
        super("Failed to " + operation + " Kafka topic: " + topicName, cause);
        this.operation = operation;
        this.topicName = topicName;
    }

    public String getOperation() {
        return operation;
    }

    public String getTopicName() {
        return topicName;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.INTERNAL_SERVER_ERROR_500;
    }

    @Override
    public String getTechnicalCode() {
        return "kafka.topic.operation.failed";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("operation", operation);
        parameters.put("topicName", topicName);
        return parameters;
    }
}
