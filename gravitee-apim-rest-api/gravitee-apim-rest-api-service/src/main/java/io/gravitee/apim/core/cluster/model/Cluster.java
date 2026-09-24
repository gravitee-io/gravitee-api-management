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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

<<<<<<< HEAD
@Builder
=======
@Builder(toBuilder = true)
@NoArgsConstructor
>>>>>>> e269957 (fix(rest-api): add credential-free copy of a cluster)
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

    public Cluster withoutCredentials() {
        return toBuilder().configuration(withoutCredentials(configuration)).build();
    }

    private static Object withoutCredentials(Object node) {
        if (node instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<Object, Object>();
            map.forEach((key, value) ->
                copy.put(
                    key,
                    "security".equals(key) && value instanceof Map<?, ?> security ? protocolOnly(security) : withoutCredentials(value)
                )
            );
            return copy;
        }
        if (node instanceof List<?> list) {
            return list.stream().map(Cluster::withoutCredentials).toList();
        }
        return node;
    }

    private static Map<Object, Object> protocolOnly(Map<?, ?> security) {
        var copy = new LinkedHashMap<Object, Object>();
        if (security.containsKey("protocol")) {
            copy.put("protocol", security.get("protocol"));
        }
        return copy;
    }
}
