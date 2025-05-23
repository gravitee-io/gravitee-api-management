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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ExecutionModeUpgrader implements Upgrader {

    private static final int BULK_SIZE = 100;
    private final ApiRepository apiRepository;
    private int modelCounter;
    private final ObjectMapper objectMapper;

    @Autowired
    public ExecutionModeUpgrader(@Lazy ApiRepository apiRepository, @Lazy ObjectMapper objectMapper) {
        this.apiRepository = apiRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.EXECUTION_MODE_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::migrateApiEvents);
    }

    private boolean migrateApiEvents() throws UpgraderException {
        log.info("Starting migrating execution mode for APIs");
        modelCounter = 0;
        ApiCriteria onlyV2ApiCriteria = new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.V2)).build();

        var apis = apiRepository.search(onlyV2ApiCriteria, null, new ApiFieldFilter.Builder().excludePicture().build()).toList();

        for (var api : apis) {
            try {
                if (api.getDefinition() != null) {
                    JsonNode apiDefinitionNode = objectMapper.readTree(api.getDefinition());
                    if (apiDefinitionNode.isObject()) {
                        ObjectNode objectNode = (ObjectNode) apiDefinitionNode;
                        JsonNode executionMode = objectNode.get("execution_mode");
                        if (executionMode != null && executionMode.asText().equals("jupiter")) {
                            modelCounter++;
                            objectNode.put("execution_mode", ExecutionMode.V4_EMULATION_ENGINE.getLabel());
                            api.setDefinition(objectMapper.writeValueAsString(apiDefinitionNode));
                            apiRepository.update(api);
                        }
                    }
                }
            } catch (JsonProcessingException | TechnicalException e) {
                log.error("Error processing API {}: {}", api.getId(), e.getMessage());
                throw new UpgraderException(e);
            }
        }
        log.info("{} jupiter APIs have been migrated to use v4-emulation-engine execution mode", modelCounter);
        return true;
    }
}
