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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.domain_service.BrowserCallPermission;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
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
import io.gravitee.rest.api.portal.rest.model.RatingSummary;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import java.math.BigDecimal;
import java.net.URI;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> BROWSER_CALL_HEADERS = List.of("Authorization", "Content-Type");

    @Autowired
    private RatingService ratingService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private ApiEntrypointService apiEntrypointService;

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

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
        apiItem.setMcp(computeMcp(api));
        apiItem.setCallableFromPortal(computeCallableFromPortal(executionContext, api));

        boolean isCategoryModeEnabled = this.parameterService.findAsBoolean(
            executionContext,
            Key.PORTAL_APIS_CATEGORY_ENABLED,
            ParameterReferenceType.ENVIRONMENT
        );
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

        return apiItem;
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

    private Boolean computeCallableFromPortal(ExecutionContext executionContext, GenericApiEntity api) {
        if (!(api instanceof ApiEntity httpApi) || httpApi.getListeners() == null) {
            return null;
        }
        return httpApi
            .getListeners()
            .stream()
            .filter(HttpListener.class::isInstance)
            .map(HttpListener.class::cast)
            .findFirst()
            .map(listener -> isCallableFromPortal(executionContext, listener.getCors()))
            .orElse(null);
    }

    private boolean isCallableFromPortal(ExecutionContext executionContext, Cors cors) {
        return (
            cors != null &&
            cors.isEnabled() &&
            BrowserCallPermission.allows(cors, portalOrigin(executionContext.getEnvironmentId()), "POST", BROWSER_CALL_HEADERS)
        );
    }

    private String portalOrigin(String environmentId) {
        String portalUrl = installationAccessQueryService.getPortalUrl(environmentId);
        if (portalUrl == null) {
            return null;
        }
        try {
            URI uri = URI.create(portalUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Map<String, Object> computeMcp(GenericApiEntity api) {
        if (api instanceof ApiEntity asHttpApiEntity) {
            Entrypoint mcpEntrypoint = asHttpApiEntity
                .getListeners()
                .getFirst()
                .getEntrypoints()
                .stream()
                .filter(e -> Objects.equals(e.getType(), "mcp"))
                .findFirst()
                .orElse(null);

            if (mcpEntrypoint == null) {
                return null;
            }

            try {
                return MAPPER.readValue(mcpEntrypoint.getConfiguration(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                return null;
            }
        }
        return null;
    }
}
