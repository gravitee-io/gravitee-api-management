/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.management.model.flow;

import java.util.Objects;

public class FlowConsumer {

    private FlowConsumerType consumerType;

    private String consumerId;

    public FlowConsumer() {
    }

    public FlowConsumer(FlowConsumerType consumerType, String consumerId) {
        this.consumerType = consumerType;
        this.consumerId = consumerId;
    }

    public FlowConsumerType getConsumerType() {
        return consumerType;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerType(FlowConsumerType consumerType) {
        this.consumerType = consumerType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowConsumer flowConsumer = (FlowConsumer) o;
        return Objects.equals(consumerType, flowConsumer.consumerType) &&
            Objects.equals(consumerId, flowConsumer.consumerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumerType, consumerId);
    }
}
