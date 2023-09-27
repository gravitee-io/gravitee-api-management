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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.kubernetes.v1alpha1.ApiDefinitionResource;
import io.gravitee.kubernetes.mapper.CustomResourceDefinitionMapper;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.UpdatePageEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.kubernetes.v1alpha1.ApiExportQuery;
import io.gravitee.rest.api.service.ApiExportService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import io.gravitee.rest.api.service.v4.validation.PathValidationService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class ApiExportServiceImpl extends AbstractService implements ApiExportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExportServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final PageService pageService;
    private final PlanService planService;
    private final ApiService apiService;
    private final PathValidationService pathValidationService;
    private final ApiConverter apiConverter;
    private final PlanConverter planConverter;
    private final CustomResourceDefinitionMapper customResourceDefinitionMapper;

    public ApiExportServiceImpl(
        ObjectMapper objectMapper,
        PageService pageService,
        PlanService planService,
        ApiService apiService,
        PathValidationService pathValidationService,
        ApiConverter apiConverter,
        PlanConverter planConverter,
        CustomResourceDefinitionMapper customResourceDefinitionMapper
    ) {
        this.objectMapper = objectMapper;
        this.pageService = pageService;
        this.planService = planService;
        this.apiService = apiService;
        this.pathValidationService = pathValidationService;
        this.apiConverter = apiConverter;
        this.planConverter = planConverter;
        this.customResourceDefinitionMapper = customResourceDefinitionMapper;
    }

    @Override
    public String exportAsJson(
        final ExecutionContext executionContext,
        final String apiId,
        String exportVersion,
        String... filteredFields
    ) {
        ApiEntity apiEntity = apiService.findById(executionContext, apiId);
        generateAndSaveCrossId(executionContext, apiEntity);
        // set metadata for serialize process
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ApiSerializer.METADATA_EXPORT_VERSION, exportVersion);
        metadata.put(ApiSerializer.METADATA_FILTERED_FIELDS_LIST, Arrays.asList(filteredFields));
        apiEntity.setMetadata(metadata);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiEntity);
        } catch (final Exception e) {
            LOGGER.error("An error occurs while trying to JSON serialize the API {}", apiEntity, e);
        }
        return "";
    }

    @Override
    public String exportAsCustomResourceDefinition(ExecutionContext executionContext, String apiId, ApiExportQuery exportQuery) {
        String json = exportAsJson(executionContext, apiId, "2.0.0", exportQuery.getExcludedFields());
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            String name = jsonNode.get("name").asText();

            ApiDefinitionResource apiDefinitionResource = new ApiDefinitionResource(name, (ObjectNode) jsonNode);

            if (exportQuery.isRemoveIds()) {
                apiDefinitionResource.removeIds();
            }

            if (exportQuery.hasContextRef()) {
                apiDefinitionResource.setContextRef(exportQuery.getContextRefName(), exportQuery.getContextRefNamespace());
            }

            if (exportQuery.hasContextPath()) {
                String contextPath = pathValidationService.sanitizePath(exportQuery.getContextPath());
                apiDefinitionResource.setContextPath(contextPath);
            }

            if (exportQuery.hasVersion()) {
                apiDefinitionResource.setVersion(exportQuery.getVersion());
            }

            return customResourceDefinitionMapper.toCustomResourceDefinition(apiDefinitionResource);
        } catch (final Exception e) {
            LOGGER.error(String.format("An error occurs while trying to convert API %s to CRD", apiId), e);
            throw new TechnicalManagementException(e);
        }
    }

    private void generateAndSaveCrossId(ExecutionContext executionContext, ApiEntity api) {
        if (StringUtils.isEmpty(api.getCrossId())) {
            api.setCrossId(UuidString.generateRandom());
            apiService.update(executionContext, api.getId(), apiConverter.toUpdateApiEntity(api));
        }
        planService.findByApi(api.getId()).forEach(plan -> generateAndSaveCrossId(executionContext, plan));
        pageService
            .findByApi(executionContext.getEnvironmentId(), api.getId())
            .forEach(page -> generateAndSaveCrossId(executionContext, page));
    }

    private void generateAndSaveCrossId(ExecutionContext executionContext, PlanEntity plan) {
        if (StringUtils.isEmpty(plan.getCrossId())) {
            plan.setCrossId(UuidString.generateRandom());
            planService.update(executionContext, planConverter.toUpdatePlanEntity(plan));
        }
    }

    private void generateAndSaveCrossId(ExecutionContext executionContext, PageEntity page) {
        if (StringUtils.isEmpty(page.getCrossId())) {
            page.setCrossId(UuidString.generateRandom());
            UpdatePageEntity updatePageEntity = new UpdatePageEntity();
            updatePageEntity.setCrossId(page.getCrossId());
            pageService.update(executionContext, page.getId(), updatePageEntity, true);
        }
    }
}
