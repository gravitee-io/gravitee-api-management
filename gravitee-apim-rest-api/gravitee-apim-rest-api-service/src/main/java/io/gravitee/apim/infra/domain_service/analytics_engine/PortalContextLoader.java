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
package io.gravitee.apim.infra.domain_service.analytics_engine;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoader;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.infra.domain_service.analytics_engine.mapper.ApiMapper;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class PortalContextLoader implements AnalyticsQueryContextLoader {

    private static final String ENVIRONMENT_ADMIN = RoleScope.ENVIRONMENT.name() + ':' + SystemRole.ADMIN.name();
    private static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();
    private static final ApiFieldFilter API_CONTEXT_FIELDS = new ApiFieldFilter.Builder().excludeDefinition().excludePicture().build();

    private final PortalNavigationApiVisibilityDomainService visibilityDomainService;
    private final ApiRepository apiRepository;
    private final ApplicationService applicationService;

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication
            .getAuthorities()
            .stream()
            .anyMatch(a -> ENVIRONMENT_ADMIN.equalsIgnoreCase(a.getAuthority()) || ORGANIZATION_ADMIN.equalsIgnoreCase(a.getAuthority()));
    }

    @Override
    public AnalyticsQueryContext load(AuditInfo auditInfo) {
        var organizationId = auditInfo.organizationId();
        var environmentId = auditInfo.environmentId();
        var userId = auditInfo.actor().userId();

        var executionContext = new ExecutionContext(organizationId, environmentId);

        List<Api> apis;
        Map<String, String> applicationNamesById;

        if (isAdmin()) {
            apis = ApiMapper.INSTANCE.map(
                apiRepository.search(new ApiCriteria.Builder().environmentId(environmentId).build(), API_CONTEXT_FIELDS)
            );
            applicationNamesById = Map.of();
        } else if (userId == null || userId.isBlank()) {
            List<PortalNavigationApi> visibleNavApis = visibilityDomainService.resolveVisibleItems(environmentId);
            apis = fetchApisForNavItems(environmentId, visibleNavApis);
            applicationNamesById = Map.of();
        } else {
            List<PortalNavigationApi> visibleNavApis = visibilityDomainService.resolveVisibleItems(environmentId, userId);
            apis = fetchApisForNavItems(environmentId, visibleNavApis);
            applicationNamesById = applicationService
                .findByUser(executionContext, userId)
                .stream()
                .collect(Collectors.toMap(ApplicationListItem::getId, ApplicationListItem::getName));
        }

        var authorizedApiIds = apis.stream().map(Api::getId).collect(Collectors.toSet());
        var apiNamesById = mapApiIdsToNames(apis);
        var apiIdsByType = groupApiIdsByType(apis);

        return new AnalyticsQueryContext(auditInfo, executionContext, authorizedApiIds, apiNamesById, applicationNamesById, apiIdsByType);
    }

    private List<Api> fetchApisForNavItems(String environmentId, List<PortalNavigationApi> navItems) {
        if (navItems.isEmpty()) {
            return List.of();
        }
        Set<String> apiIds = navItems.stream().map(PortalNavigationApi::getApiId).collect(Collectors.toSet());
        return ApiMapper.INSTANCE.map(
            apiRepository.search(new ApiCriteria.Builder().environmentId(environmentId).ids(apiIds).build(), API_CONTEXT_FIELDS)
        );
    }

    private static Map<String, String> mapApiIdsToNames(Collection<Api> apis) {
        return apis.stream().collect(Collectors.toMap(Api::getId, Api::getName));
    }

    private static Map<ApiType, Set<String>> groupApiIdsByType(Collection<Api> apis) {
        return apis
            .stream()
            .filter(api -> api.getType() != null)
            .collect(Collectors.groupingBy(Api::getType, Collectors.mapping(Api::getId, Collectors.toSet())));
    }
}
