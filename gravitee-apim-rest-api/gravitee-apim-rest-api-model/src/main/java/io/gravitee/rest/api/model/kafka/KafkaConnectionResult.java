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
package io.gravitee.rest.api.model.kafka;

import java.util.Objects;

/**
 * Result of a Kafka connection test.
 *
 * @author Gravitee Team
 */
public class KafkaConnectionResult {

    private boolean success;
    private String message;

    public KafkaConnectionResult() {}

    public KafkaConnectionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static KafkaConnectionResult success(String bootstrapServers) {
        return new KafkaConnectionResult(true, "Successfully connected to " + bootstrapServers);
    }

    public static KafkaConnectionResult failure(String message) {
        return new KafkaConnectionResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaConnectionResult that = (KafkaConnectionResult) o;
        return success == that.success && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, message);
    }

    @Override
    public String toString() {
        return "KafkaConnectionResult{" + "success=" + success + ", message='" + message + '\'' + '}';
    }
}
