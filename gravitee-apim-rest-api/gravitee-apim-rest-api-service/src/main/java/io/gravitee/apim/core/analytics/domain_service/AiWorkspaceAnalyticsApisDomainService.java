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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * The APIs an AI workspace reader may count the calls of. {@code AI_WORKSPACE_USER[READ]} on a workspace opens the
 * aggregates of the calls made through it, never the rest of its APIs' traffic: a caller must keep the query narrowed
 * to {@code api-product = workspace}, so that an API shared with another workspace never shows that workspace's calls.
 */
@DomainService
@RequiredArgsConstructor
@CustomLog
public class AiWorkspaceAnalyticsApisDomainService {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiQueryService apiQueryService;
    private final PermissionDomainService permissionDomainService;

    /** Empty when the user does not read the workspace, or when it is no workspace of the current environment. */
    public List<Api> findWorkspaceApis(ExecutionContext executionContext, String userId, String workspaceId) {
        var readsWorkspace = permissionDomainService.hasPermission(
            executionContext.getOrganizationId(),
            userId,
            RolePermission.AI_WORKSPACE_USER,
            workspaceId,
            RolePermissionAction.READ
        );
        if (!readsWorkspace) {
            log.debug("Workspace [{}] refused to user [{}]: no AI_WORKSPACE_USER[READ] on it", workspaceId, userId);
            return List.of();
        }
        var workspaces = apiProductQueryService
            .findByEnvironmentIdAndIdIn(executionContext.getEnvironmentId(), Set.of(workspaceId))
            .stream()
            .filter(product -> product.getKind() == ApiProductKind.AI_WORKSPACE)
            .toList();
        if (workspaces.isEmpty()) {
            log.debug(
                "Workspace [{}] refused to user [{}]: no AI workspace of that id in environment [{}]",
                workspaceId,
                userId,
                executionContext.getEnvironmentId()
            );
            return List.of();
        }
        var apiIds = workspaces
            .stream()
            .flatMap(product -> Stream.ofNullable(product.getApiIds()).flatMap(Set::stream))
            .toList();
        // An empty id list does not restrict the search: it would widen the scope to the whole environment.
        if (apiIds.isEmpty()) {
            log.debug("Workspace [{}] refused to user [{}]: it holds no API", workspaceId, userId);
            return List.of();
        }
        return apiQueryService
            .search(
                ApiSearchCriteria.builder().environmentId(executionContext.getEnvironmentId()).ids(apiIds).build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build()
            )
            .toList();
    }
}
