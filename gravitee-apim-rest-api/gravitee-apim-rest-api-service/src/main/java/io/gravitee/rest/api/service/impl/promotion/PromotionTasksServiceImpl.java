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
package io.gravitee.rest.api.service.impl.promotion;

import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_API;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.TaskEntity;
import io.gravitee.rest.api.model.TaskType;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.model.promotion.PromotionQuery;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.gravitee.rest.api.service.promotion.PromotionTasksService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class PromotionTasksServiceImpl extends AbstractService implements PromotionTasksService {

    private final PromotionService promotionService;
    private final PermissionService permissionService;
    private final EnvironmentService environmentService;
    private final ApiSearchService apiSearchService;
    private final ObjectMapper objectMapper;

    public PromotionTasksServiceImpl(
        PromotionService promotionService,
        PermissionService permissionService,
        EnvironmentService environmentService,
        ApiSearchService apiSearchService,
        ObjectMapper objectMapper
    ) {
        this.promotionService = promotionService;
        this.permissionService = permissionService;
        this.environmentService = environmentService;
        this.apiSearchService = apiSearchService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TaskEntity> getPromotionTasks(final ExecutionContext executionContext) {
        List<EnvironmentEntity> environments = environmentService.findByOrganization(executionContext.getOrganizationId());

        List<EnvironmentEntity> environmentsWithCreationPermissions = environments
            .stream()
            .filter(environment -> permissionService.hasPermission(executionContext, ENVIRONMENT_API, environment.getId(), CREATE))
            .collect(toList());

        List<EnvironmentEntity> environmentsWithUpdatePermissions = environments
            .stream()
            .filter(environment -> permissionService.hasPermission(executionContext, ENVIRONMENT_API, environment.getId(), UPDATE))
            .collect(toList());

        List<TaskEntity> tasks = new ArrayList<>();
        tasks.addAll(getPromotionTasksForEnvironments(environmentsWithCreationPermissions, false));
        tasks.addAll(getPromotionTasksForEnvironments(environmentsWithUpdatePermissions, true));
        return tasks;
    }

    private List<TaskEntity> getPromotionTasksForEnvironments(List<EnvironmentEntity> environments, boolean selectUpdatePromotion) {
        if (environments.isEmpty()) {
            return emptyList();
        }

        List<String> envCockpitIds = environments.stream().map(EnvironmentEntity::getCockpitId).filter(Objects::nonNull).collect(toList());
        Map<String, List<String>> environmentIdsByCockpitId = environments
            .stream()
            .filter(environment -> environment.getCockpitId() != null)
            .collect(groupingBy(EnvironmentEntity::getCockpitId, mapping(EnvironmentEntity::getId, toList())));

        final PromotionQuery promotionQuery = new PromotionQuery();
        promotionQuery.setStatuses(Collections.singletonList(PromotionEntityStatus.TO_BE_VALIDATED));
        promotionQuery.setTargetEnvCockpitIds(envCockpitIds);

        final Page<PromotionEntity> promotionsPage = promotionService.search(promotionQuery, new SortableImpl("created_at", false), null);

        final PromotionQuery previousPromotionsQuery = new PromotionQuery();
        previousPromotionsQuery.setStatuses(Collections.singletonList(PromotionEntityStatus.ACCEPTED));
        previousPromotionsQuery.setTargetEnvCockpitIds(envCockpitIds);
        previousPromotionsQuery.setTargetApiExists(true);

        List<PromotionEntity> previousPromotions = promotionService
            .search(previousPromotionsQuery, new SortableImpl("created_at", false), null)
            .getContent();

        final Map<String, List<String>> promotionByApiWithTargetApiId = previousPromotions
            .stream()
            .collect(groupingBy(PromotionEntity::getApiId, Collectors.mapping(PromotionEntity::getTargetApiId, toList())));

        return promotionsPage
            .getContent()
            .stream()
            .map(promotionEntity -> {
                Optional<String> foundTargetApiId = promotionByApiWithTargetApiId
                    .getOrDefault(promotionEntity.getApiId(), emptyList())
                    .stream()
                    .filter(StringUtils::hasText)
                    .findFirst();

                boolean isUpdate = foundTargetApiId.isPresent() && apiSearchService.exists(foundTargetApiId.get());
                return convert(
                    promotionEntity,
                    isUpdate,
                    foundTargetApiId,
                    onlyEnvironmentId(environmentIdsByCockpitId.get(promotionEntity.getTargetEnvCockpitId()))
                );
            })
            .filter(taskEntity ->
                ((Boolean) ((Map<String, Object>) taskEntity.getData()).getOrDefault("isApiUpdate", false) == selectUpdatePromotion)
            )
            .collect(toList());
    }

    private TaskEntity convert(
        PromotionEntity promotionEntity,
        boolean isUpdate,
        Optional<String> foundTargetApiId,
        String targetEnvironmentId
    ) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setType(TaskType.PROMOTION_APPROVAL);
        taskEntity.setCreatedAt(promotionEntity.getCreatedAt());

        JsonNode definition = readDefinition(promotionEntity);

        Map<String, Object> data = new HashMap<>();
        data.put("apiName", apiNameOf(definition));
        data.put("apiId", promotionEntity.getApiId());
        data.put("sourceEnvironmentName", promotionEntity.getSourceEnvName());
        data.put("targetEnvironmentName", promotionEntity.getTargetEnvName());
        data.put("authorDisplayName", promotionEntity.getAuthor().getDisplayName());
        data.put("authorEmail", promotionEntity.getAuthor().getEmail());
        data.put("promotionId", promotionEntity.getId());
        data.put("isApiUpdate", isUpdate);

        foundTargetApiId.ifPresent(targetApiId -> data.put("targetApiId", targetApiId));
        // Lets a console route the review to the module owning the API type, in the environment it lands in —
        // for a first promotion there is no target API yet, so neither can be read off one.
        apiTypeOf(definition).ifPresent(apiType -> data.put("apiType", apiType));
        if (targetEnvironmentId != null) {
            data.put("targetEnvironmentId", targetEnvironmentId);
        }

        taskEntity.setData(data);
        return taskEntity;
    }

    /**
     * APIM does not keep cockpit ids unique; when one names several environments there is no telling which of them the
     * promotion lands in, so none is given rather than a guess.
     */
    private static String onlyEnvironmentId(List<String> environmentIds) {
        return environmentIds != null && environmentIds.size() == 1 ? environmentIds.getFirst() : null;
    }

    private JsonNode readDefinition(PromotionEntity promotion) {
        String apiDefinition = promotion.getApiDefinition();
        if (apiDefinition == null || apiDefinition.isBlank()) {
            return MissingNode.getInstance();
        }
        try {
            return objectMapper.readTree(apiDefinition);
        } catch (JsonProcessingException e) {
            log.warn("Failed to read the API definition of promotion {} for API {}", promotion.getId(), promotion.getApiId(), e);
            return MissingNode.getInstance();
        }
    }

    /** A v2 definition carries its name at the root, a v4 export under {@code api}. */
    private static String apiNameOf(JsonNode definition) {
        for (JsonNode name : List.of(definition.path("name"), definition.path("api").path("name"))) {
            if (!name.isMissingNode() && !name.isNull()) {
                return name.asText();
            }
        }
        return "Unknown API name";
    }

    /**
     * Only a v4 export declares a type, stored as the enum's name ({@code LLM_PROXY}) by the definition serializer, and
     * exposed as its label ({@code llm-proxy}) like the rest of the task metadata. A name this installation doesn't know,
     * from a newer one, is left out rather than failing the task list.
     */
    private static Optional<String> apiTypeOf(JsonNode definition) {
        String storedType = definition.path("api").path("type").textValue();
        return Arrays.stream(ApiType.values())
            .filter(type -> type.name().equals(storedType))
            .findFirst()
            .map(ApiType::getLabel);
    }
}
