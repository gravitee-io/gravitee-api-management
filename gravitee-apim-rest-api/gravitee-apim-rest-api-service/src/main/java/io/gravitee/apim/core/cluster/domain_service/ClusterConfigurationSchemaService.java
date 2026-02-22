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
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.IOException;
import java.io.InputStream;

@DomainService
public class ClusterConfigurationSchemaService {

    private static final String SCHEMA_PATH = "/cluster/kafka-cluster-configuration-schema-form.json";

    private String cachedSchema;

    public String getConfigurationSchema() {
        if (cachedSchema == null) {
            try (InputStream resourceAsStream = this.getClass().getResourceAsStream(SCHEMA_PATH)) {
                cachedSchema = new String(resourceAsStream.readAllBytes(), UTF_8);
            } catch (IOException e) {
                throw new TechnicalManagementException("An error occurs while trying to load cluster configuration schema", e);
            }
        }
        return cachedSchema;
    }
}
