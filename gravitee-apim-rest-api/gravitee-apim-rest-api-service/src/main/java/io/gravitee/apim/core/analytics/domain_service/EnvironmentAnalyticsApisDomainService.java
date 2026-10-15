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
import io.gravitee.apim.core.analytics.model.ApiDataKind;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * The APIs of an environment a user may read, by kind of data. Aggregates are opened by the API analytics permission on each API
 * the user is a member of, directly or through a group, or for the whole environment by ENVIRONMENT_PLATFORM[READ] on it. The
 * requests themselves are opened by the API log permission on each member API only: the platform permission never widens them.
 * An organization admin reads every API. No other permission, ENVIRONMENT_API[READ] included, opens any data.
 */
@DomainService
@RequiredArgsConstructor
public class EnvironmentAnalyticsApisDomainService {

    private final ApiQueryService apiQueryService;
    private final ApiAuthorizationDomainService apiAuthorizationDomainService;
    private final PermissionDomainService permissionDomainService;

    /** The V2 and V4 APIs whose aggregates the user may read, for the callers that only deal with those. */
    public List<Api> findReadableApis(ExecutionContext executionContext, String userId) {
        return findReadableApis(
            executionContext,
            userId,
            false,
            ApiDataKind.AGGREGATES,
            List.of(DefinitionVersion.V4, DefinitionVersion.V2)
        );
    }

    /** The APIs of every type and definition version whose data of this kind the user may read. */
    public List<Api> findReadableApis(ExecutionContext executionContext, String userId, boolean isAdmin, ApiDataKind kind) {
        return findReadableApis(executionContext, userId, isAdmin, kind, null);
    }

    /**
     * Same scoping as {@link #findReadableApis(ExecutionContext, String, boolean, ApiDataKind)}, narrowed to one API: the API of
     * the environment is fetched, then the permission is checked on that API alone, which resolves direct and group roles itself.
     */
    public Optional<Api> findReadableApi(
        ExecutionContext executionContext,
        String userId,
        boolean isAdmin,
        ApiDataKind kind,
        String apiId
    ) {
        return search(criteria(executionContext, null, List.of(apiId)))
            .stream()
            .findFirst()
            .filter(
                api -> canReadEveryApi(executionContext, userId, isAdmin, kind) || canRead(executionContext, userId, kind, api.getId())
            );
    }

    private List<Api> findReadableApis(
        ExecutionContext executionContext,
        String userId,
        boolean isAdmin,
        ApiDataKind kind,
        Collection<DefinitionVersion> definitionVersions
    ) {
        if (canReadEveryApi(executionContext, userId, isAdmin, kind)) {
            return search(criteria(executionContext, definitionVersions, null));
        }

        var memberApiIds = apiAuthorizationDomainService.findMemberApiIds(executionContext, userId);
        // An empty id list does not restrict the search: it would widen the scope to the whole environment.
        if (memberApiIds.isEmpty()) {
            return List.of();
        }
        return search(criteria(executionContext, definitionVersions, List.copyOf(memberApiIds)))
            .stream()
            .filter(api -> canRead(executionContext, userId, kind, api.getId()))
            .toList();
    }

    public Map<DefinitionVersion, List<String>> findReadableApiIdsByDefinitionVersion(ExecutionContext executionContext, String userId) {
        return findReadableApis(executionContext, userId)
            .stream()
            .collect(groupingBy(EnvironmentAnalyticsApisDomainService::definitionVersion, mapping(Api::getId, toList())));
    }

    private static ApiSearchCriteria criteria(
        ExecutionContext executionContext,
        Collection<DefinitionVersion> definitionVersions,
        List<String> apiIds
    ) {
        return ApiSearchCriteria.builder()
            .environmentId(executionContext.getEnvironmentId())
            .definitionVersion(definitionVersions)
            .ids(apiIds)
            .build();
    }

    private boolean canReadEveryApi(ExecutionContext executionContext, String userId, boolean isAdmin, ApiDataKind kind) {
        return (
            isAdmin ||
            (kind.isOpenedByPlatformPermission() &&
                permissionDomainService.hasPermission(
                    executionContext.getOrganizationId(),
                    userId,
                    RolePermission.ENVIRONMENT_PLATFORM,
                    executionContext.getEnvironmentId(),
                    RolePermissionAction.READ
                ))
        );
    }

    private boolean canRead(ExecutionContext executionContext, String userId, ApiDataKind kind, String apiId) {
        return permissionDomainService.hasPermission(
            executionContext.getOrganizationId(),
            userId,
            kind.getApiPermission(),
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
