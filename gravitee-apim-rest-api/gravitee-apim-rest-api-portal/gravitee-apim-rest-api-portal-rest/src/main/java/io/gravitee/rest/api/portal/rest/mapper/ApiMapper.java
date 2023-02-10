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
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.User;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApiMapper {
    ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    @Mapping(source = "lifecycleState", target = "draft", qualifiedByName = "isDraft")
    @Mapping(source = "state", target = "running", qualifiedByName = "isRunning")
    @Mapping(source = "visibility", target = "public", qualifiedByName = "isPublic")
    @Mapping(source = "labels", target = "labels", qualifiedByName = "calculateLabels")
    @Mapping(source = "primaryOwner", target = "owner", qualifiedByName = "addOwner")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "addDate")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "addDate")
    @Mapping(source = "apiVersion", target = "version")
    Api toApi(GenericApiEntity api);

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
