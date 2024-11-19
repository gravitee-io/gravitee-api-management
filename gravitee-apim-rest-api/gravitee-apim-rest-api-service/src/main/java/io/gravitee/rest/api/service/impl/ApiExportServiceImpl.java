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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.kubernetes.v1alpha1.ApiDefinitionResource;
import io.gravitee.kubernetes.mapper.CustomResourceDefinitionMapper;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdatePageEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.kubernetes.v1alpha1.ApiExportQuery;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.ApiExportService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final RoleService roleService;
    private final ApiConverter apiConverter;
    private final PlanConverter planConverter;
    private final CustomResourceDefinitionMapper customResourceDefinitionMapper;

    public ApiExportServiceImpl(
        ObjectMapper objectMapper,
        PageService pageService,
        PlanService planService,
        ApiService apiService,
        RoleService roleService,
        ApiConverter apiConverter,
        PlanConverter planConverter,
        CustomResourceDefinitionMapper customResourceDefinitionMapper
    ) {
        this.objectMapper = objectMapper;
        this.pageService = pageService;
        this.planService = planService;
        this.apiService = apiService;
        this.roleService = roleService;
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
        ApiEntity apiEntity = getApi(executionContext, apiId, exportVersion, filteredFields);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiEntity);
        } catch (final Exception e) {
            LOGGER.error("An error occurs while trying to JSON serialize the API {}", apiEntity, e);
        }
        return "";
    }

    @Override
    public String exportAsCustomResourceDefinition(ExecutionContext executionContext, String apiId, ApiExportQuery exportQuery) {
        try {
            ApiEntity apiEntity = getApi(executionContext, apiId, "2.0.0", exportQuery.getExcludedFields());

            String json = objectMapper.writeValueAsString(apiEntity);

            JsonNode jsonNode = objectMapper.readTree(json);

            String name = jsonNode.get("name").asText();

            ApiDefinitionResource apiDefinitionResource = new ApiDefinitionResource(name, (ObjectNode) jsonNode);

            if (apiDefinitionResource.hasMembers()) {
                mapCRDMembers(executionContext, apiDefinitionResource.getMembers());
            }

            if (apiDefinitionResource.hasPages()) {
                prepareApiPagesForExport(apiDefinitionResource.getPages());
            }

            apiDefinitionResource.setState(apiEntity.getState() == null ? Lifecycle.State.STOPPED.name() : apiEntity.getState().name());

            if (exportQuery.isRemoveIds()) {
                apiDefinitionResource.removeIds();
            }

            if (exportQuery.hasContextRef()) {
                apiDefinitionResource.setContextRef(exportQuery.getContextRefName(), exportQuery.getContextRefNamespace());
            }

            if (exportQuery.hasContextPath()) {
                String contextPath = Path.sanitizePath(exportQuery.getContextPath());
                apiDefinitionResource.setContextPath(contextPath);
            }

            if (exportQuery.hasVersion()) {
                apiDefinitionResource.setVersion(exportQuery.getVersion());
            }

            ((ObjectNode) jsonNode).set(
                    "notifyMembers",
                    BooleanNode.valueOf(!jsonNode.get("disable_membership_notifications").asBoolean())
                );

            ((ObjectNode) jsonNode).remove("disable_membership_notifications");

            return customResourceDefinitionMapper.toCustomResourceDefinition(apiDefinitionResource);
        } catch (final Exception e) {
            LOGGER.error(String.format("An error occurs while trying to convert API %s to CRD", apiId), e);
            throw new TechnicalManagementException(e);
        }
    }

    private ApiEntity getApi(ExecutionContext executionContext, String apiId, String exportVersion, String... filteredFields) {
        ApiEntity apiEntity = apiService.findById(executionContext, apiId);
        generateAndSaveCrossId(executionContext, apiEntity);
        // set metadata for serialize process
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ApiSerializer.METADATA_EXPORT_VERSION, exportVersion);
        metadata.put(ApiSerializer.METADATA_FILTERED_FIELDS_LIST, Arrays.asList(filteredFields));
        apiEntity.setMetadata(metadata);
        return apiEntity;
    }

    private void mapCRDMembers(ExecutionContext executionContext, ArrayNode members) {
        var roleIdToName = roleService
            .findByScope(RoleScope.API, executionContext.getOrganizationId())
            .stream()
            .collect(Collectors.toMap(RoleEntity::getId, RoleEntity::getName));

        var it = members.iterator();
        while (it.hasNext()) {
            var member = it.next();
            JsonNode roles = member.get("roles");

            // Can't be null. A member should always have a role
            JsonNode roleId = roles.iterator().next();
            var roleName = roleIdToName.get(roleId.asText());

            if (SystemRole.PRIMARY_OWNER.name().equals(roleName)) {
                it.remove();
            } else {
                ((ObjectNode) member).remove("roles");
                ((ObjectNode) member).put("role", roleName);
            }
        }
    }

    private void prepareApiPagesForExport(ObjectNode pages) throws JsonProcessingException {
        Iterator<Map.Entry<String, JsonNode>> iterator = pages.fields();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> pageJsonNode = iterator.next();
            PageEntity page = objectMapper.treeToValue(pageJsonNode.getValue(), PageEntity.class);

            if (
                (PageType.MARKDOWN.name().equals(page.getType()) || PageType.SWAGGER.name().equals(page.getType())) &&
                page.getSource() != null &&
                "github-fetcher".equals(page.getSource().getType())
            ) {
                // Remove auto-fetched pages that was generated from ROOT github source
                if (page.getMetadata() != null && "auto_fetched".equals(page.getMetadata().get("graviteeio/fetcher_type"))) {
                    iterator.remove();
                } else {
                    ((ObjectNode) pageJsonNode.getValue()).remove("content");
                }
            } else if (
                PageType.FOLDER.name().equals(page.getType()) &&
                page.getSource() != null &&
                "github-fetcher".equals(page.getSource().getType())
            ) {
                // Remove auto-generated folders generated from ROOT github fetcher
                iterator.remove();
            } else if (
                (PageType.SWAGGER.name().equals(page.getType()) || PageType.MARKDOWN.name().equals(page.getType())) &&
                page.getSource() != null &&
                ("http-fetcher".equals(page.getSource().getType()))
            ) {
                ((ObjectNode) pageJsonNode.getValue()).remove("content");
            }
        }
    }

    private void generateAndSaveCrossId(ExecutionContext executionContext, ApiEntity api) {
        if (StringUtils.isEmpty(api.getCrossId())) {
            api.setCrossId(UuidString.generateRandom());
            apiService.update(executionContext, api.getId(), apiConverter.toUpdateApiEntity(api));
        }
        planService.findByApi(executionContext, api.getId()).forEach(plan -> generateAndSaveCrossId(executionContext, plan));
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
