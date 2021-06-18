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
import static java.util.stream.Collectors.toList;

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
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.gravitee.rest.api.service.promotion.PromotionTasksService;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
        ApiService apiService,
        ObjectMapper objectMapper
    ) {
        this.promotionService = promotionService;
        this.permissionService = permissionService;
        this.environmentService = environmentService;
        this.apiService = apiService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TaskEntity> getPromotionTasks(String organizationId) {
        List<EnvironmentEntity> environments = environmentService
            .findByOrganization(organizationId)
            .stream()
            .filter(environment -> permissionService.hasPermission(ENVIRONMENT_API, environment.getId(), CREATE, UPDATE))
            .collect(toList());

        List<String> cockpitIds = environments.stream().map(EnvironmentEntity::getCockpitId).filter(Objects::nonNull).collect(toList());

        final PromotionQuery promotionQuery = new PromotionQuery();
        promotionQuery.setStatus(PromotionEntityStatus.TO_BE_VALIDATED);
        promotionQuery.setTargetEnvCockpitIds(cockpitIds);

        final Page<PromotionEntity> promotionsPage = promotionService.search(promotionQuery, new SortableImpl("created_at", false), null);

        return promotionsPage.getContent().stream().map(this::convert).collect(toList());
    }

    private TaskEntity convert(PromotionEntity promotionEntity) {
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
        data.put("sourceEnvironmentName", promotionEntity.getSourceEnvName());
        data.put("targetEnvironmentName", promotionEntity.getTargetEnvName());
        data.put("authorDisplayName", promotionEntity.getAuthor().getDisplayName());
        data.put("authorEmail", promotionEntity.getAuthor().getEmail());
        data.put("authorPicture", promotionEntity.getAuthor().getPicture());
        // FIXME: Need to check the parent API and not the api directly
        // Will be done with "Accepting/Rejecting a Promotion Request"
        data.put("isApiUpdate", apiService.exists(promotionEntity.getApiId()));

        taskEntity.setData(data);
        return taskEntity;
    }
}
