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
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.portal.rest.model.Api;
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
import java.util.*;
import java.util.stream.Collectors;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ApiMapstructMapper {

    public static ApiMapstructMapper INSTANCE = Mappers.getMapper(ApiMapstructMapper.class);

    @Autowired
    private ApiEntrypointService apiEntrypointService;

    @Autowired
    private RatingService ratingService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private CategoryService categoryService;

    @Mapping(source = "api.lifecycleState", target = "draft", qualifiedByName = "isDraft")
    @Mapping(source = "api.state", target = "running", qualifiedByName = "isRunning")
    @Mapping(source = "api.visibility", target = "public", qualifiedByName = "isPublic")
    @Mapping(source = "api.labels", target = "labels", qualifiedByName = "calculateLabels")
    @Mapping(source = "api.primaryOwner", target = "owner", qualifiedByName = "addOwner")
    @Mapping(source = "api.createdAt", target = "createdAt", qualifiedByName = "addDate")
    @Mapping(source = "api.updatedAt", target = "updatedAt", qualifiedByName = "addDate")
    @Mapping(source = "api.apiVersion", target = "version")
    abstract Api toApi(GenericApiEntity api, ExecutionContext executionContext);

    @AfterMapping
    protected void queryServices(@MappingTarget Api api, GenericApiEntity entryApi, ExecutionContext executionContext) {
        api.setCategories(calculateEntrypoints(entryApi, executionContext));
        api.setRatingSummary(calculateRatingSummary(entryApi, executionContext));
        api.setCategories(calculateCategories(entryApi, executionContext));
    }

    private List<String> calculateEntrypoints(GenericApiEntity entryApi, ExecutionContext executionContext) {
        List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(executionContext, entryApi);
        return Objects.isNull(apiEntrypoints)
            ? null
            : apiEntrypoints.stream().map(ApiEntrypointEntity::getTarget).collect(Collectors.toList());
    }

    private RatingSummary calculateRatingSummary(GenericApiEntity entryApi, ExecutionContext executionContext) {
        if (ratingService.isEnabled(executionContext)) {
            final RatingSummaryEntity ratingSummaryEntity = ratingService.findSummaryByApi(executionContext, entryApi.getId());
            return new RatingSummary()
                .average(ratingSummaryEntity.getAverageRate())
                .count(BigDecimal.valueOf(ratingSummaryEntity.getNumberOfRatings()));
        }
        return null;
    }

    private List<String> calculateCategories(GenericApiEntity entryApi, ExecutionContext executionContext) {
        boolean isCategoryModeEnabled =
            this.parameterService.findAsBoolean(executionContext, Key.PORTAL_APIS_CATEGORY_ENABLED, ParameterReferenceType.ENVIRONMENT);
        if (isCategoryModeEnabled && Objects.nonNull(entryApi.getCategories())) {
            return entryApi
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
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
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
        return UserMapstructMapper.INSTANCE.primaryOwnerEntityToUser(entity);
    }

    @Named("addDate")
    static OffsetDateTime addDate(Date date) {
        return Objects.isNull(date) ? null : date.toInstant().atOffset(ZoneOffset.UTC);
    }
}
