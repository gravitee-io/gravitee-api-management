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
package io.gravitee.apim.core.analytics.domain_service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.domain_service.ApiAuthorizationDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * The V2 and V4 APIs of an environment whose analytics a user may read, in aggregate. Holding ENVIRONMENT_PLATFORM[READ] on the
 * environment, as an organization admin does, opens every API of it; otherwise only the APIs the user is a member of, directly or
 * through a group, with API_ANALYTICS[READ] are readable, so the aggregates never show more than the per-API analytics allow.
 */
@DomainService
@RequiredArgsConstructor
public class EnvironmentAnalyticsApisDomainService {

    private final ApiQueryService apiQueryService;
    private final ApiAuthorizationDomainService apiAuthorizationDomainService;
    private final PermissionDomainService permissionDomainService;

    public List<Api> findReadableApis(ExecutionContext executionContext, String userId) {
        var criteria = ApiSearchCriteria.builder()
            .environmentId(executionContext.getEnvironmentId())
            .definitionVersion(List.of(DefinitionVersion.V4, DefinitionVersion.V2));
        if (canReadEveryApi(executionContext, userId)) {
            return search(criteria.build());
        }

        var memberApiIds = apiAuthorizationDomainService.findMemberApiIds(executionContext, userId);
        // An empty id list does not restrict the search: it would widen the scope to the whole environment.
        if (memberApiIds.isEmpty()) {
            return List.of();
        }
        return search(criteria.ids(List.copyOf(memberApiIds)).build())
            .stream()
            .filter(api -> canReadAnalytics(executionContext, userId, api.getId()))
            .toList();
    }

    public Map<DefinitionVersion, List<String>> findReadableApiIdsByDefinitionVersion(ExecutionContext executionContext, String userId) {
        return findReadableApis(executionContext, userId)
            .stream()
            .collect(groupingBy(EnvironmentAnalyticsApisDomainService::definitionVersion, mapping(Api::getId, toList())));
    }

    private boolean canReadEveryApi(ExecutionContext executionContext, String userId) {
        return permissionDomainService.hasPermission(
            executionContext.getOrganizationId(),
            userId,
            RolePermission.ENVIRONMENT_PLATFORM,
            executionContext.getEnvironmentId(),
            RolePermissionAction.READ
        );
    }

    private boolean canReadAnalytics(ExecutionContext executionContext, String userId, String apiId) {
        return permissionDomainService.hasPermission(
            executionContext.getOrganizationId(),
            userId,
            RolePermission.API_ANALYTICS,
            apiId,
            RolePermissionAction.READ
        );
    }

    private List<Api> search(ApiSearchCriteria criteria) {
        return apiQueryService
            .search(criteria, null, ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build())
            .toList();
    }

    private static DefinitionVersion definitionVersion(Api api) {
        return api.getDefinitionVersion() != null ? api.getDefinitionVersion() : DefinitionVersion.V2;
    }
}
