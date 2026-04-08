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
package io.gravitee.apim.core.cluster.domain_service;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.cluster.model.ClusterType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@DomainService
public class ClusterConfigurationSchemaService {

    private static final Map<ClusterType, String> SCHEMA_PATHS = Map.of(
        ClusterType.KAFKA_CLUSTER_CONNECTION,
        "/cluster/kafka-cluster-connection-configuration-schema-form.json",
        ClusterType.KAFKA_CLUSTER,
        "/cluster/kafka-cluster-configuration-schema-form.json"
    );

    private final Map<ClusterType, String> cachedSchemas = new ConcurrentHashMap<>();

    public String getConfigurationSchema(ClusterType type) {
        return cachedSchemas.computeIfAbsent(type, this::loadSchema);
    }

    /**
     * @deprecated Use {@link #getConfigurationSchema(ClusterType)} instead.
     */
    @Deprecated
    public String getConfigurationSchema() {
        return getConfigurationSchema(ClusterType.KAFKA_CLUSTER_CONNECTION);
    }

    private String loadSchema(ClusterType type) {
        String path = SCHEMA_PATHS.get(type);
        if (path == null) {
            throw new TechnicalManagementException("No configuration schema found for cluster type: " + type);
        }
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream(path)) {
            return new String(resourceAsStream.readAllBytes(), UTF_8);
        } catch (IOException e) {
            throw new TechnicalManagementException("An error occurs while trying to load cluster configuration schema", e);
        }
    }
}
