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
package io.gravitee.apim.core.api.model.mapper;

import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.v4.ApiType;
import java.util.Date;
import java.util.List;

public class V4toV2RollbackOperator {

    public Api rollback(Api source, io.gravitee.definition.model.Api apiDefinition) {
        var definition = apiDefinition
            .toBuilder()
            .tags(source.getApiDefinitionHttpV4().getTags())
            .resources(mapResources(source.getApiDefinitionHttpV4().getResources()))
            .responseTemplates(source.getApiDefinitionHttpV4().getResponseTemplates())
            .build();
        return new Api(
            source.getId(),
            source.getEnvironmentId(),
            source.getCrossId(),
            source.getHrid(),
            apiDefinition.getName(),
            source.getDescription(),
            apiDefinition.getVersion(),
            source.getOriginContext(),
            DefinitionVersion.V2,
            null,
            null,
            definition,
            null,
            null,
            ApiType.PROXY,
            source.getDeployedAt(),
            source.getCreatedAt(),
            TimeProvider.now(),
            source.getVisibility(),
            source.getLifecycleState(),
            source.getPicture(),
            source.getGroups(),
            source.getCategories(),
            source.getLabels(),
            source.isDisableMembershipNotifications(),
            source.getApiLifecycleState(),
            source.getBackground()
        );
    }

    public Plan rollback(Plan source, io.gravitee.definition.model.Plan planDefinition) {
        return new Plan(
            source.getId(),
            DefinitionVersion.V2,
            source.getCrossId(),
            planDefinition.getName(),
            source.getDescription(),
            source.getCreatedAt(),
            TimeProvider.now(),
            source.getPublishedAt(),
            source.getClosedAt(),
            Date.from(TimeProvider.instantNow()),
            source.getValidation(),
            source.getType(),
            source.getApiId(),
            source.getEnvironmentId(),
            source.getOrder(),
            source.getCharacteristics(),
            source.getExcludedGroups(),
            source.isCommentRequired(),
            source.getCommentMessage(),
            source.getGeneralConditions(),
            null,
            null,
            planDefinition,
            null,
            source.getApiType()
        );
    }

    private List<Resource> mapResources(List<io.gravitee.definition.model.v4.resource.Resource> resources) {
        return stream(resources).map(this::mapResource).toList();
    }

    private Resource mapResource(io.gravitee.definition.model.v4.resource.Resource resource) {
        return Resource
            .builder()
            .enabled(resource.isEnabled())
            .name(resource.getName())
            .type(resource.getType())
            .configuration(resource.getConfiguration())
            .build();
    }
}
