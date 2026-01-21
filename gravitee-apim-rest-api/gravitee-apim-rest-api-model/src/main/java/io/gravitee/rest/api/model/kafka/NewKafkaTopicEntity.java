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

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Configuration for creating a new Kafka topic.
 *
 * @author Gravitee Team
 */
public class NewKafkaTopicEntity {

    @NotBlank(message = "Topic name is required")
    private String name;

    @NotNull(message = "Number of partitions is required")
    @Min(value = 1, message = "At least 1 partition is required")
    private Integer partitions;

    @NotNull(message = "Replication factor is required")
    @Min(value = 1, message = "At least 1 replica is required")
    private Integer replicas;

    private Long retentionMs;
    private String cleanupPolicy;

    public NewKafkaTopicEntity() {}

    public NewKafkaTopicEntity(String name, Integer partitions, Integer replicas) {
        this.name = name;
        this.partitions = partitions;
        this.replicas = replicas;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPartitions() {
        return partitions;
    }

    public void setPartitions(Integer partitions) {
        this.partitions = partitions;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public Long getRetentionMs() {
        return retentionMs;
    }

    public void setRetentionMs(Long retentionMs) {
        this.retentionMs = retentionMs;
    }

    public String getCleanupPolicy() {
        return cleanupPolicy;
    }

    public void setCleanupPolicy(String cleanupPolicy) {
        this.cleanupPolicy = cleanupPolicy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewKafkaTopicEntity that = (NewKafkaTopicEntity) o;
        return (
            Objects.equals(name, that.name) &&
            Objects.equals(partitions, that.partitions) &&
            Objects.equals(replicas, that.replicas) &&
            Objects.equals(retentionMs, that.retentionMs) &&
            Objects.equals(cleanupPolicy, that.cleanupPolicy)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, partitions, replicas, retentionMs, cleanupPolicy);
    }

    @Override
    public String toString() {
        return (
            "NewKafkaTopicEntity{" +
            "name='" +
            name +
            '\'' +
            ", partitions=" +
            partitions +
            ", replicas=" +
            replicas +
            ", retentionMs=" +
            retentionMs +
            ", cleanupPolicy='" +
            cleanupPolicy +
            '\'' +
            '}'
        );
    }
}
