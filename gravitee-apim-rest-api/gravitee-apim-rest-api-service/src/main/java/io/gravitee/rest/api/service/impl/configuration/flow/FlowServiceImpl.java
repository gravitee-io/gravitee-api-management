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
package io.gravitee.rest.api.service.impl.configuration.flow;

import static java.nio.charset.Charset.defaultCharset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.*;
import io.gravitee.rest.api.model.TagEntity;
import io.gravitee.rest.api.model.TagReferenceType;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.FlowConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class FlowServiceImpl extends AbstractService implements FlowService {

    @Lazy
    @Autowired
    private FlowRepository flowRepository;

    @Autowired
    private TagService tagService;

    @Autowired
    private FlowConverter flowConverter;

    private String getFileContent(final String path) {
        try {
            InputStream resourceAsStream = this.getClass().getResourceAsStream(path);
            return IOUtils.toString(resourceAsStream, defaultCharset());
        } catch (IOException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying load flow configuration with path %s", path),
                e
            );
        }
    }

    @Override
    public String getConfigurationSchemaForm() {
        return getFileContent("/flow/configuration-schema-form.json");
    }

    @Override
    public String getApiFlowSchemaForm() {
        return getFileContent("/flow/api-flow-schema-form.json");
    }

    @Override
    public String getPlatformFlowSchemaForm(ExecutionContext executionContext) {
        log.debug("Get platform schema form");
        String fileContent = getFileContent("/flow/platform-flow-schema-form.json");
        List<TagEntity> tags = tagService.findByReference(executionContext.getOrganizationId(), TagReferenceType.ORGANIZATION);
        if (tags.size() > 0) {
            log.debug("Append {} tag(s) to platform schema form", tags.size());
            try {
                final ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonSchema = mapper.readTree(fileContent);

                final ObjectNode consumers = (ObjectNode) jsonSchema.get("properties").get("consumers");
                final ArrayNode enumNode = (ArrayNode) consumers.get("items").get("enum");

                Map<String, String> titleMap = new HashMap<>();
                tags.forEach(tagEntity -> titleMap.put(tagEntity.getId(), tagEntity.getName()));

                titleMap.keySet().forEach(enumNode::add);
                JsonNode titleMapNode = mapper.valueToTree(titleMap);

                ObjectNode xSchemaForm = mapper.createObjectNode();
                consumers.set("x-schema-form", xSchemaForm);
                xSchemaForm.set("titleMap", titleMapNode);

                return jsonSchema.toPrettyString();
            } catch (JsonProcessingException ex) {
                throw new TechnicalManagementException(
                    String.format("An error occurs while append tags to platform flow schema form. Tags:%s", tags),
                    ex
                );
            }
        }

        return fileContent;
    }

    @Override
    public List<Flow> findByReference(FlowReferenceType flowReferenceType, String referenceId) {
        try {
            log.debug("Find flows by reference {} - {}", flowReferenceType, flowReferenceType);
            return flowRepository
                .findByReference(flowReferenceType, referenceId)
                .stream()
                .sorted(Comparator.comparing(io.gravitee.repository.management.model.flow.Flow::getOrder))
                .map(flowConverter::toDefinition)
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while find flows by reference", ex);
        }
    }

    @Override
    public List<Flow> save(FlowReferenceType flowReferenceType, String referenceId, List<Flow> flows) {
        log.debug("Saving flows...");
        try {
            log.debug("Save flows for reference {},{}", flowReferenceType, referenceId);
            if (flows == null || flows.isEmpty()) {
                flowRepository.deleteByReferenceIdAndReferenceType(referenceId, flowReferenceType);
                return List.of();
            }
            Map<String, io.gravitee.repository.management.model.flow.Flow> dbFlowsById = flowRepository
                .findByReference(flowReferenceType, referenceId)
                .stream()
                .collect(Collectors.toMap(io.gravitee.repository.management.model.flow.Flow::getId, Function.identity()));

            Set<String> flowIdsToSave = flows.stream().map(Flow::getId).filter(Objects::nonNull).collect(Collectors.toSet());

            for (String dbFlowId : dbFlowsById.keySet()) {
                if (!flowIdsToSave.contains(dbFlowId)) {
                    flowRepository.delete(dbFlowId);
                }
            }

            List<io.gravitee.repository.management.model.flow.Flow> savedFlows = new ArrayList<>();
            List<io.gravitee.repository.management.model.flow.Flow> toCreate = new ArrayList<>();
            List<io.gravitee.repository.management.model.flow.Flow> toUpdate = new ArrayList<>();
            IntStream
                .range(0, flows.size())
                .forEach(i -> {
                    var flow = flows.get(i);
                    if (flow.getId() == null || !dbFlowsById.containsKey(flow.getId())) {
                        toCreate.add(flowConverter.toRepository(flow, flowReferenceType, referenceId, i));
                    } else {
                        toUpdate.add(flowConverter.toRepositoryUpdate(dbFlowsById.get(flow.getId()), flow, i));
                    }
                });

            if (!toCreate.isEmpty()) {
                log.debug("Flows to create: {}", toCreate.size());
                savedFlows.addAll(flowRepository.createAll(toCreate));
            }

            if (!toUpdate.isEmpty()) {
                log.debug("Flows to update: {}", toUpdate.size());
                savedFlows.addAll(flowRepository.updateAll(toUpdate));
            }

            List<Flow> result = savedFlows
                .stream()
                .sorted(Comparator.comparing(io.gravitee.repository.management.model.flow.Flow::getOrder))
                .map(flowConverter::toDefinition)
                .collect(Collectors.toList());

            log.debug("Flows saved: {}!", result.size());
            return result;
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while save flows", ex);
        }
    }
}
