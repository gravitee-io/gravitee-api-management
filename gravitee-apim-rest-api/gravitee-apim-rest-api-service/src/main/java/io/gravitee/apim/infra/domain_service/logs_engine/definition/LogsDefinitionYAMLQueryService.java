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
package io.gravitee.apim.infra.domain_service.logs_engine.definition;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.gravitee.apim.core.logs_engine.model.LogsDefinition;
import io.gravitee.apim.core.logs_engine.model.LogsDefinitionSpec;
import io.gravitee.apim.core.logs_engine.model.LogsFilterSpec;
import io.gravitee.apim.core.logs_engine.query_service.LogsDefinitionQueryService;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
public class LogsDefinitionYAMLQueryService implements LogsDefinitionQueryService {

    private static final String LOGS_DEFINITION_FILE = "logs/definition/logs-definition.yaml";

    private static final YAMLMapper YAML = new YAMLMapper();

    private final LogsDefinitionSpec spec;

    public LogsDefinitionYAMLQueryService() {
        spec = readYAMLDefinition().spec();
    }

    private static LogsDefinition readYAMLDefinition() {
        try (var stream = LogsDefinitionYAMLQueryService.class.getClassLoader().getResourceAsStream(LOGS_DEFINITION_FILE)) {
            if (stream == null) {
                throw new IllegalStateException("Logs definition file not found on classpath: " + LOGS_DEFINITION_FILE);
            }
            return YAML.readValue(stream, LogsDefinition.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse logs definition file: " + LOGS_DEFINITION_FILE, e);
        }
    }

    @Override
    public List<LogsFilterSpec> getFilters() {
        return spec.filters();
    }
}
