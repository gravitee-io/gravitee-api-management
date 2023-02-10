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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApiLinks;
import io.gravitee.rest.api.portal.rest.model.RatingSummary;
import io.gravitee.rest.api.portal.rest.model.User;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper(componentModel = "spring")
public abstract class ApiMapper {

    @Autowired
    private RatingService ratingService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private static ApiEntrypointService apiEntrypointService;

    static ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    @Mapping(source = "api.lifecycleState", target = "draft", qualifiedByName = "isDraft")
    @Mapping(source = "api.state", target = "running", qualifiedByName = "isRunning")
    @Mapping(source = "api.visibility", target = "public", qualifiedByName = "isPublic")
    @Mapping(source = "api.labels", target = "labels", qualifiedByName = "calculateLabels")
    @Mapping(source = "api.primaryOwner", target = "owner", qualifiedByName = "addOwner")
    @Mapping(source = "api.createdAt", target = "createdAt", qualifiedByName = "addDate")
    @Mapping(source = "api.updatedAt", target = "updatedAt", qualifiedByName = "addDate")
    @Mapping(source = "api.apiVersion", target = "version")
    abstract Api mapToApi(GenericApiEntity api, ExecutionContext executionContext);

    @Mapping(target = "links", expression = "java(basePath + \"/links\")")
    @Mapping(target = "metrics", expression = "java(basePath + \"/metrics\")")
    @Mapping(target = "pages", expression = "java(basePath + \"/pages\")")
    @Mapping(target = "plans", expression = "java(basePath + \"/plans\")")
    @Mapping(target = "ratings", expression = "java(basePath + \"/ratings\")")
    @Mapping(source = "basePath", target = "self")
    abstract ApiLinks mapToApiLinks(String basePath, Date updateDate);

    @AfterMapping
    static void afterMapToApiLinks(@MappingTarget ApiLinks apiLinks, String basePath, Date updateDate) {
        final String hash = updateDate == null ? "" : String.valueOf(updateDate.getTime());
        apiLinks.setPicture(basePath + "/picture?" + hash);
        apiLinks.setBackground(basePath + "/background?" + hash);
    }

    @Named("isDraft")
    static boolean isDraft(ApiLifecycleState lifecycleState) {
        return ApiLifecycleState.UNPUBLISHED.equals(lifecycleState) || ApiLifecycleState.CREATED.equals(lifecycleState);
    }

    @Named("isRunning")
    static boolean isRunning(Lifecycle.State lifecycleState) {
        return Lifecycle.State.STARTED.equals(lifecycleState);
    }

    @Named("isPublic")
    static boolean isPublic(Visibility visibility) {
        return Visibility.PUBLIC.equals(visibility);
    }

    @Named("calculateLabels")
    static List<String> calculateLabels(List<String> labels) {
        return labels == null ? new ArrayList<>() : new ArrayList<>(labels);
    }

    @Named("addOwner")
    static User addOwner(PrimaryOwnerEntity entity) {
        return UserMapper.INSTANCE.primaryOwnerEntityToUser(entity);
    }

    @Named("addDate")
    static OffsetDateTime addDate(Date date) {
        return Objects.isNull(date) ? null : date.toInstant().atOffset(ZoneOffset.UTC);
    }

    public Api convert(ExecutionContext executionContext, GenericApiEntity api) {
        final Api apiItem = ApiMapper.INSTANCE.mapToApi(api, executionContext);

        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(executionContext, api);
        if (apiEntrypoints != null) {
            List<String> entrypoints = apiEntrypoints.stream().map(ApiEntrypointEntity::getTarget).collect(Collectors.toList());
            apiItem.setEntrypoints(entrypoints);
        }

        if (ratingService.isEnabled(executionContext)) {
            final RatingSummaryEntity ratingSummaryEntity = ratingService.findSummaryByApi(executionContext, api.getId());
            RatingSummary ratingSummary = new RatingSummary()
                .average(ratingSummaryEntity.getAverageRate())
                .count(BigDecimal.valueOf(ratingSummaryEntity.getNumberOfRatings()));
            apiItem.setRatingSummary(ratingSummary);
        }

        boolean isCategoryModeEnabled =
            this.parameterService.findAsBoolean(executionContext, Key.PORTAL_APIS_CATEGORY_ENABLED, ParameterReferenceType.ENVIRONMENT);
        if (isCategoryModeEnabled && api.getCategories() != null) {
            apiItem.setCategories(
                api
                    .getCategories()
                    .stream()
                    .filter(
                        categoryId -> {
                            try {
                                categoryService.findNotHiddenById(categoryId, executionContext.getEnvironmentId());
                                return true;
                            } catch (CategoryNotFoundException v) {
                                return false;
                            }
                        }
                    )
                    .collect(Collectors.toList())
            );
        } else {
            apiItem.setCategories(new ArrayList<>());
        }

        return apiItem;
    }

    public ApiLinks computeApiLinks(String basePath, Date updateDate) {
        return ApiMapper.INSTANCE.mapToApiLinks(basePath, updateDate);
    }
}
