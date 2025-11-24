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
package io.gravitee.apim.infra.domain_service.analytics_engine.permissions;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryFilterDecorator;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
@RequiredArgsConstructor
public class AnalyticsQueryFilterDecoratorImpl implements AnalyticsQueryFilterDecorator {

    private static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();

    private final ApiAuthorizationService apiAuthorizationService;

    private final PermissionService permissionService;

    @Override
    public List<Filter> getUpdatedFilters(@NotNull List<Filter> filters) {
        if (!filters.isEmpty()) {
            return filters;
        }

        List<String> apiIds = this.findApiIds();

        Filter f = new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, apiIds);
        filters.add(f);

        return filters;
    }

    @NotNull
    private List<String> findApiIds() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (isAdmin()) {
            Set<String> idsByEnvironment = apiAuthorizationService.findIdsByEnvironment(executionContext.getEnvironmentId());
            return idsByEnvironment.stream().toList();
        }

        return apiAuthorizationService
            .findIdsByUser(executionContext, getAuthenticatedUser(), false)
            .stream()
            .filter(appId -> permissionService.hasPermission(executionContext, API_ANALYTICS, appId, READ))
            .toList();
    }

    protected boolean isAdmin() {
        return isUserInRole(ORGANIZATION_ADMIN);
    }

    protected String getAuthenticatedUser() {
        return getUserPrincipal().getName();
    }

    protected boolean isUserInRole(final String role) {
        return SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .anyMatch(
                (Predicate<GrantedAuthority>) grantedAuthority -> {
                    String authority = grantedAuthority.getAuthority();
                    return authority.equalsIgnoreCase(role);
                }
            );
    }

    protected Principal getUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication instanceof AnonymousAuthenticationToken) ? null : authentication;
    }
}
