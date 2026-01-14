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
 * Represents a Kafka topic.
 *
 * @author Gravitee Team
 */
public class KafkaTopicEntity {

    private String id;
    private String name;
    private int partitions;
    private int replicas;
    private String messages;

    public KafkaTopicEntity() {}

    public KafkaTopicEntity(String name, int partitions, int replicas) {
        this.id = "topic-" + name.hashCode();
        this.name = name;
        this.partitions = partitions;
        this.replicas = replicas;
        this.messages = "0";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaTopicEntity that = (KafkaTopicEntity) o;
        return partitions == that.partitions && replicas == that.replicas && Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, partitions, replicas);
    }

    @Override
    public String toString() {
        return (
            "KafkaTopicEntity{" +
            "id='" +
            id +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", partitions=" +
            partitions +
            ", replicas=" +
            replicas +
            ", messages='" +
            messages +
            '\'' +
            '}'
        );
    }
}
