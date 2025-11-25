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

import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name.API;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.IN;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryFilterDecorator;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.security.Principal;
import java.util.HashSet;
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
public class ApiAnalyticsQueryFilterDecoratorImpl implements AnalyticsQueryFilterDecorator {

    private static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();
    private static final String ENVIRONMENT_ADMIN = RoleScope.ENVIRONMENT.name() + ':' + SystemRole.ADMIN.name();

    private final ApiAuthorizationService apiAuthorizationService;

    private final PermissionService permissionService;

    @Override
    public List<Filter> applyPermissionBasedFilters(@NotNull List<Filter> filters) {
        if (filters.isEmpty()) {
            var allowedApiIds = this.allowedApiIds();
            var filter = new Filter(API, IN, allowedApiIds);
            return List.of(filter);
        }

        return getUpdatedFilters(filters);
    }

    private @NotNull List<Filter> getUpdatedFilters(@NotNull List<Filter> filters) {
        return filters
            .stream()
            .map(filter -> {
                if (filter.name() == API) {
                    return switch (filter.operator()) {
                        case IN -> getUpdatedInFilter(filter);
                        case EQ -> getUpdatedEqFilter(filter);
                        default -> filter;
                    };
                }

                return filter;
            })
            .toList();
    }

    private @NotNull Filter getUpdatedInFilter(@NotNull Filter filter) {
        if (!(filter.value() instanceof Iterable<?>)) {
            throw new IllegalArgumentException("Filter value must be an Iterable");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> wantedApiIds = objectMapper.convertValue(filter.value(), new TypeReference<>() {});

        var apiIds = this.allowedApiIdsFilteredBy(new HashSet<>(wantedApiIds));

        return new Filter(filter.name(), IN, apiIds);
    }

    private @NotNull Filter getUpdatedEqFilter(Filter filter) {
        String apiId = filter.value().toString();

        var apiIds = this.allowedApiIdsFilteredBy(apiId);

        if (apiIds.contains(apiId)) {
            return filter;
        }

        return new Filter(filter.name(), IN, List.of());
    }

    // Find all API IDs a user can access.
    @NotNull
    private List<String> allowedApiIds() {
        return allowedApiIdsFilteredBy((Set<String>) null);
    }

    // Find all API IDs a user can access.
    // If wantedApiIds is null, the method will return all APIs the user can access. This is equivalent to calling allowedApiIds().
    // If wantedApiIds is empty, an empty list will be returned.
    // If wantedApiIds is not empty, those IDs will limit the IDs that can be returned.
    @NotNull
    private List<String> allowedApiIdsFilteredBy(Set<String> wantedApiIds) {
        if (CollectionUtils.isInitializedAndEmpty(wantedApiIds)) {
            return List.of();
        }

        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (isAdmin()) {
            Set<String> idsByEnvironment = apiAuthorizationService.findIdsByEnvironment(executionContext.getEnvironmentId());

            if (wantedApiIds == null) {
                return idsByEnvironment.stream().toList();
            }

            idsByEnvironment.retainAll(wantedApiIds);
            return idsByEnvironment.stream().toList();
        }

        var query = new ApiQuery();
        if (wantedApiIds != null) {
            query.setIds(wantedApiIds);
        }

        return apiAuthorizationService
            .findIdsByUser(executionContext, getAuthenticatedUser(), query, false)
            .stream()
            .filter(appId -> permissionService.hasPermission(executionContext, API_ANALYTICS, appId, READ))
            .toList();
    }

    // Find all API IDs a user can access and returns a list with a single item if apiId is allowed.
    @NotNull
    private List<String> allowedApiIdsFilteredBy(String apiId) {
        var wantedSet = new HashSet<>(List.of(apiId));
        return allowedApiIdsFilteredBy(wantedSet);
    }

    protected boolean isAdmin() {
        return isUserInRole(ORGANIZATION_ADMIN) || isUserInRole(ENVIRONMENT_ADMIN);
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
