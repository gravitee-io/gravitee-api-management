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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.json.JsonSchemaChecker;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ValidateClusterService {

    private final JsonSchemaChecker jsonSchemaChecker;
    private final ClusterConfigurationSchemaService clusterConfigurationSchemaService;
    private final ObjectMapper objectMapper;

    public void validate(Cluster cluster) {
        if (StringUtils.isEmpty(cluster.getName())) {
            throw new InvalidDataException("Name is required.");
        }
        if (Objects.isNull(cluster.getConfiguration())) {
            throw new InvalidDataException("Configuration is required.");
        }
        try {
            String configJson = objectMapper.writeValueAsString(cluster.getConfiguration());
            String schema = clusterConfigurationSchemaService.getConfigurationSchema();
            jsonSchemaChecker.validate(schema, configJson);
        } catch (JsonProcessingException e) {
            throw new InvalidDataException("Configuration is not valid JSON.");
        }
    }
}
