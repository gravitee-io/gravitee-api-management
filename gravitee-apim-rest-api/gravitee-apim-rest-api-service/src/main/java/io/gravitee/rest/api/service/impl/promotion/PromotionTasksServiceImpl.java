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
package io.gravitee.rest.api.service.impl.promotion;

import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_API;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.TaskEntity;
import io.gravitee.rest.api.model.TaskType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.model.promotion.PromotionQuery;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.gravitee.rest.api.service.promotion.PromotionTasksService;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PromotionTasksServiceImpl extends AbstractService implements PromotionTasksService {

    private final Logger logger = LoggerFactory.getLogger(PromotionTasksServiceImpl.class);

    private final PromotionService promotionService;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final EnvironmentService environmentService;
    private final ApiService apiService;

    public PromotionTasksServiceImpl(
        PromotionService promotionService,
        PermissionService permissionService,
        EnvironmentService environmentService,
        ObjectMapper objectMapper,
        ApiService apiService
    ) {
        this.promotionService = promotionService;
        this.permissionService = permissionService;
        this.environmentService = environmentService;
        this.objectMapper = objectMapper;
        this.apiService = apiService;
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

    @NotNull
    private List<TaskEntity> getPromotionTasksForEnvironments(List<EnvironmentEntity> environments, boolean selectUpdatePromotion) {
        if (environments.isEmpty()) {
            return emptyList();
        }

        List<String> envCockpitIds = environments.stream().map(EnvironmentEntity::getCockpitId).filter(Objects::nonNull).collect(toList());

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
            .map(
                promotionEntity -> {
                    Optional<String> foundTargetApiId = promotionByApiWithTargetApiId
                        .getOrDefault(promotionEntity.getApiId(), emptyList())
                        .stream()
                        .filter(StringUtils::hasText)
                        .findFirst();

                    boolean isUpdate = foundTargetApiId.isPresent() && apiService.exists(foundTargetApiId.get());
                    return convert(promotionEntity, isUpdate, foundTargetApiId);
                }
            )
            .filter(
                taskEntity ->
                    ((Boolean) ((Map<String, Object>) taskEntity.getData()).getOrDefault("isApiUpdate", false) == selectUpdatePromotion)
            )
            .collect(toList());
    }

    private TaskEntity convert(PromotionEntity promotionEntity, boolean isUpdate, Optional<String> foundTargetApiId) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setType(TaskType.PROMOTION_APPROVAL);
        taskEntity.setCreatedAt(promotionEntity.getCreatedAt());

        ApiEntity apiEntity;
        try {
            apiEntity = objectMapper.readValue(promotionEntity.getApiDefinition(), ApiEntity.class);
        } catch (JsonProcessingException e) {
            logger.warn("Problem while deserializing api definition for promotion {}", promotionEntity.getId());
            throw new TechnicalManagementException();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("apiName", apiEntity.getName());
        data.put("apiId", promotionEntity.getApiId());
        data.put("sourceEnvironmentName", promotionEntity.getSourceEnvName());
        data.put("targetEnvironmentName", promotionEntity.getTargetEnvName());
        data.put("authorDisplayName", promotionEntity.getAuthor().getDisplayName());
        data.put("authorEmail", promotionEntity.getAuthor().getEmail());
        data.put("authorPicture", promotionEntity.getAuthor().getPicture());
        data.put("promotionId", promotionEntity.getId());
        data.put("isApiUpdate", isUpdate);

        foundTargetApiId.ifPresent(targetApiId -> data.put("targetApiId", targetApiId));

        taskEntity.setData(data);
        return taskEntity;
    }
}
