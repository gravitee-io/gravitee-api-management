/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.definition.model.flow.*;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.*;
import io.gravitee.rest.api.model.TagEntity;
import io.gravitee.rest.api.model.TagReferenceType;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.FlowConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class FlowServiceImpl extends AbstractService implements FlowService {

    private final Logger LOGGER = LoggerFactory.getLogger(FlowServiceImpl.class);

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
            throw new TechnicalManagementException("An error occurs while trying load flow configuration", e);
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
    public String getPlatformFlowSchemaForm() {
        LOGGER.debug("Get platform schema form");
        String fileContent = getFileContent("/flow/platform-flow-schema-form.json");
        List<TagEntity> tags = tagService.findByReference(GraviteeContext.getCurrentOrganization(), TagReferenceType.ORGANIZATION);
        if (tags.size() > 0) {
            LOGGER.debug("Append {} tag(s) to platform schema form", tags.size());
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
                final String error = "An error occurs while append tags to platform flow schema form";
                LOGGER.error(error, ex);
                throw new TechnicalManagementException(error, ex);
            }
        }

        return fileContent;
    }

    @Override
    public List<Flow> findByReference(FlowReferenceType flowReferenceType, String referenceId) {
        try {
            LOGGER.debug("Find flows by reference {} - {}", flowReferenceType, flowReferenceType);
            return flowRepository
                .findByReference(flowReferenceType, referenceId)
                .stream()
                .sorted(Comparator.comparing(io.gravitee.repository.management.model.flow.Flow::getOrder))
                .map(flowConverter::toDefinition)
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            final String error = "An error occurs while find flows by reference";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public List<Flow> save(FlowReferenceType flowReferenceType, String referenceId, List<Flow> flows) {
        try {
            LOGGER.debug("Save flows for reference {},{}", flowReferenceType, flowReferenceType);
            flowRepository.deleteByReference(flowReferenceType, referenceId);
            if (flows == null) {
                return List.of();
            } else {
                for (int order = 0; order < flows.size(); ++order) {
                    flowRepository.create(flowConverter.toModel(flows.get(order), flowReferenceType, referenceId, order));
                }
                return flows;
            }
        } catch (TechnicalException ex) {
            final String error = "An error occurs while save flows";
            LOGGER.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }
}
