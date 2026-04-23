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
package io.gravitee.repository.management.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Persisted record of a Kafka plan's port allocation.
 *
 * <p>One row per plan that is configured for port-based routing. Used by the management API to
 * detect port conflicts across plans sharing the same {@code environment_id + sharding_tag}.
 * Mirrors the {@code bootstrap_port / range_start / range_end} fields already stored inline on
 * the plan definition — denormalized here for efficient indexed overlap queries.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(of = { "planId" })
public class KafkaPortRange {

    private String planId;
    private String apiId;
    private String environmentId;
    private String shardingTag;
    private int bootstrapPort;
    private int rangeStart;
    private int rangeEnd;
    private Instant createdAt;
    private Instant updatedAt;
}
