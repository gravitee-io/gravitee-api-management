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
package io.gravitee.rest.api.service.v4.impl;

import static java.nio.charset.Charset.defaultCharset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.TagEntity;
import io.gravitee.rest.api.model.TagReferenceType;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.mapper.FlowMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component("FlowServiceImplV4")
public class FlowServiceImpl extends TransactionalService implements FlowService {

    private final FlowRepository flowRepository;
    private final TagService tagService;
    private final FlowMapper flowMapper;

    public FlowServiceImpl(@Lazy final FlowRepository flowRepository, final TagService tagService, final FlowMapper flowMapper) {
        this.flowRepository = flowRepository;
        this.tagService = tagService;
        this.flowMapper = flowMapper;
    }

    private String getFileContent(final String path) {
        try (InputStream resourceAsStream = this.getClass().getResourceAsStream(path)) {
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
        return getFileContent("/flow/v4/api-flow-schema-form.json");
    }

    @Override
    public String getPlatformFlowSchemaForm(final ExecutionContext executionContext) {
        log.debug("Get platform schema form");
        String fileContent = getFileContent("/flow/v4/platform-flow-schema-form.json");
        List<TagEntity> tags = tagService.findByReference(executionContext.getOrganizationId(), TagReferenceType.ORGANIZATION);
        if (tags.size() > 0) {
            log.debug("Append {} tag(s) to platform schema form", tags.size());
            try {
                final ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonSchema = mapper.readTree(fileContent);

                final ObjectNode tagsNode = (ObjectNode) jsonSchema.get("properties").get("tags");
                final ArrayNode enumNode = (ArrayNode) tagsNode.get("items").get("enum");

                Map<String, String> titleMap = new HashMap<>();
                tags.forEach(tagEntity -> titleMap.put(tagEntity.getId(), tagEntity.getName()));

                titleMap.keySet().forEach(enumNode::add);
                JsonNode titleMapNode = mapper.valueToTree(titleMap);

                ObjectNode xSchemaForm = mapper.createObjectNode();
                tagsNode.set("x-schema-form", xSchemaForm);
                xSchemaForm.set("titleMap", titleMapNode);

                return jsonSchema.toPrettyString();
            } catch (JsonProcessingException ex) {
                final String error = "An error occurs while append tags to platform flow schema form";
                log.error(error, ex);
                throw new TechnicalManagementException(error, ex);
            }
        }

        return fileContent;
    }

    @Override
    public List<Flow> findByReference(final FlowReferenceType flowReferenceType, final String referenceId) {
        try {
            log.debug("Find flows by reference {} - {}", flowReferenceType, flowReferenceType);
            return flowRepository
                .findByReference(flowReferenceType, referenceId)
                .stream()
                .sorted(Comparator.comparing(io.gravitee.repository.management.model.flow.Flow::getOrder))
                .map(flowMapper::toDefinition)
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            final String error = "An error occurs while find flows by reference";
            log.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    @Override
    public List<NativeFlow> findNativeFlowByReference(FlowReferenceType flowReferenceType, String referenceId) {
        try {
            log.debug("Find flows by reference {} - {}", flowReferenceType, flowReferenceType);
            return flowRepository
                .findByReference(flowReferenceType, referenceId)
                .stream()
                .sorted(Comparator.comparing(io.gravitee.repository.management.model.flow.Flow::getOrder))
                .map(flowMapper::toNativeDefinition)
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            final String error = "An error occurs while find flows by reference";
            log.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }
}
