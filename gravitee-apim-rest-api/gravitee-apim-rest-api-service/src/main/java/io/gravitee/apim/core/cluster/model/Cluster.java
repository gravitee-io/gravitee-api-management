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
package io.gravitee.apim.core.cluster.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.TimeProvider;
import java.time.Instant;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class Cluster {

    private String id;
    private Instant createdAt;
    private Instant updatedAt;
    private String environmentId;
    private String organizationId;
    private String name;
    private String description;
    private Object configuration;
    private Set<String> groups;

    public KafkaClusterConfiguration getKafkaClusterConfiguration(ObjectMapper objectMapper) {
        return objectMapper.convertValue(this.configuration, KafkaClusterConfiguration.class);
    }

    public void update(UpdateCluster updateCluster) {
        this.updatedAt = TimeProvider.instantNow();
        if (updateCluster.getName() != null) {
            this.name = updateCluster.getName();
        }
        if (updateCluster.getDescription() != null) {
            this.description = updateCluster.getDescription();
        }
        if (updateCluster.getConfiguration() != null) {
            this.configuration = updateCluster.getConfiguration();
        }
    }
}
