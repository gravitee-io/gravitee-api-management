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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.v4.mcp.Tool;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApiLinks;
import io.gravitee.rest.api.portal.rest.model.ApiType;
import io.gravitee.rest.api.portal.rest.model.DefinitionVersion;
import io.gravitee.rest.api.portal.rest.model.ListenerType;
import io.gravitee.rest.api.portal.rest.model.MCP;
import io.gravitee.rest.api.portal.rest.model.MCPInputSchema;
import io.gravitee.rest.api.portal.rest.model.MCPTool;
import io.gravitee.rest.api.portal.rest.model.RatingSummary;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiMapper {

    @Autowired
    private RatingService ratingService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private ApiEntrypointService apiEntrypointService;

    public Api convert(ExecutionContext executionContext, GenericApiEntity api) {
        final Api apiItem = new Api();
        if (api.getDefinitionVersion() != null) {
            apiItem.setDefinitionVersion(DefinitionVersion.valueOf(api.getDefinitionVersion().name()));
        }
        apiItem.setDescription(api.getDescription());
        apiItem.setType(computeApiType(api));

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(executionContext, api);
        if (apiEntrypoints != null) {
            List<String> entrypoints = apiEntrypoints.stream().map(ApiEntrypointEntity::getTarget).collect(Collectors.toList());
            apiItem.setEntrypoints(entrypoints);
        }
        if (apiEntrypoints != null && !apiEntrypoints.isEmpty()) {
            String apiListenerType = apiEntrypointService.getApiEntrypointsListenerType(api);
            apiItem.setListenerType(ListenerType.valueOf(apiListenerType));
        }

        apiItem.setDraft(api.getLifecycleState() == ApiLifecycleState.UNPUBLISHED || api.getLifecycleState() == ApiLifecycleState.CREATED);
        apiItem.setRunning(api.getState() == Lifecycle.State.STARTED);
        apiItem.setPublic(api.getVisibility() == Visibility.PUBLIC);
        apiItem.setId(api.getId());

        List<String> apiLabels = api.getLabels();
        if (apiLabels != null) {
            apiItem.setLabels(new ArrayList<>(apiLabels));
        } else {
            apiItem.setLabels(new ArrayList<>());
        }

        apiItem.setName(api.getName());

        PrimaryOwnerEntity primaryOwner = api.getPrimaryOwner();
        if (primaryOwner != null) {
            User owner = new User();
            owner.setId(primaryOwner.getId());
            owner.setDisplayName(primaryOwner.getDisplayName());
            owner.setEmail(primaryOwner.getEmail());
            apiItem.setOwner(owner);
        }
        apiItem.setPages(null);
        apiItem.setPlans(null);

        if (ratingService.isEnabled(executionContext)) {
            final RatingSummaryEntity ratingSummaryEntity = ratingService.findSummaryByApi(executionContext, api.getId());
            RatingSummary ratingSummary = new RatingSummary()
                .average(ratingSummaryEntity.getAverageRate())
                .count(BigDecimal.valueOf(ratingSummaryEntity.getNumberOfRatings()));
            apiItem.setRatingSummary(ratingSummary);
        }

        if (api.getCreatedAt() != null) {
            apiItem.setCreatedAt(api.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC));
        }
        if (api.getUpdatedAt() != null) {
            apiItem.setUpdatedAt(api.getUpdatedAt().toInstant().atOffset(ZoneOffset.UTC));
        }

        apiItem.setVersion(api.getApiVersion());

        boolean isCategoryModeEnabled =
            this.parameterService.findAsBoolean(executionContext, Key.PORTAL_APIS_CATEGORY_ENABLED, ParameterReferenceType.ENVIRONMENT);
        if (isCategoryModeEnabled && api.getCategories() != null) {
            apiItem.setCategories(
                api
                    .getCategories()
                    .stream()
                    .filter(categoryId -> {
                        try {
                            categoryService.findNotHiddenById(categoryId, executionContext.getEnvironmentId());
                            return true;
                        } catch (CategoryNotFoundException v) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList())
            );
        } else {
            apiItem.setCategories(new ArrayList<>());
        }

        if (api instanceof ApiEntity apiEntity && io.gravitee.definition.model.v4.ApiType.PROXY.equals(apiEntity.getType())) {
            apiItem.setMcp(convert(apiEntity.getMcp()));
        }

        return apiItem;
    }

    public MCP convert(io.gravitee.definition.model.v4.mcp.MCP mcp) {
        MCP mcpItem = new MCP();
        if (mcp != null) {
            mcpItem.setEnabled(mcp.isEnabled());
            if (mcp.getTools() != null) {
                mcpItem.setTools(mcp.getTools().stream().map(this::convert).collect(Collectors.toList()));
            }
        }
        return mcpItem;
    }

    public MCPTool convert(io.gravitee.definition.model.v4.mcp.Tool tool) {
        MCPTool mcpTool = new MCPTool();
        if (tool != null) {
            mcpTool.setDescription(tool.getDescription());
            mcpTool.setName(tool.getName());
            mcpTool.setInputSchema(convert(tool.getInputSchema()));
        }
        return mcpTool;
    }

    public MCPInputSchema convert(Tool.InputSchema inputSchema) {
        MCPInputSchema mcpInputSchema = new MCPInputSchema();
        if (inputSchema != null) {
            mcpInputSchema.setType(inputSchema.getType());
            mcpInputSchema.setProperties(inputSchema.getProperties());
        }
        return mcpInputSchema;
    }

    public ApiLinks computeApiLinks(String basePath, Date updateDate) {
        ApiLinks apiLinks = new ApiLinks();
        apiLinks.setLinks(basePath + "/links");
        apiLinks.setMetrics(basePath + "/metrics");
        apiLinks.setPages(basePath + "/pages");
        apiLinks.setPlans(basePath + "/plans");
        apiLinks.setRatings(basePath + "/ratings");
        apiLinks.setSelf(basePath);
        final String hash = updateDate == null ? "" : String.valueOf(updateDate.getTime());
        apiLinks.setPicture(basePath + "/picture?" + hash);
        apiLinks.setBackground(basePath + "/background?" + hash);
        return apiLinks;
    }

    private static ApiType computeApiType(GenericApiEntity api) {
        if (api instanceof ApiEntity asHttpApiEntity) {
            return ApiType.fromValue(asHttpApiEntity.getType().name());
        }
        if (api instanceof NativeApiEntity asNativeApiEntity) {
            return ApiType.fromValue(asNativeApiEntity.getType().name());
        }
        return null;
    }
}
