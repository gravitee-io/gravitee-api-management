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
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
@RequiredArgsConstructor
public class ApiAnalyticsQueryFilterDecoratorImpl implements AnalyticsQueryFilterDecorator {

    private static final String ENVIRONMENT_ADMIN = RoleScope.ENVIRONMENT.name() + ':' + SystemRole.ADMIN.name();

    private final ApiAuthorizationService apiAuthorizationService;

    private final PermissionService permissionService;

    @Override
    public Map<String, API> getAllowedApis() {
        var allowedApis = new HashMap<String, API>();

        var executionContext = GraviteeContext.getExecutionContext();
        if (isEnvironmentAdmin()) {
            var apiIds = apiAuthorizationService.findIdsByEnvironment(executionContext.getEnvironmentId());
            apiIds.forEach(id -> allowedApis.put(id, new API("")));
            return allowedApis;
        }

        apiAuthorizationService
            .findIdsByUser(executionContext, getAuthenticatedUser(), false)
            .stream()
            .filter(appId -> permissionService.hasPermission(executionContext, API_ANALYTICS, appId, READ))
            .forEach(id -> allowedApis.put(id, new API("")));

        return allowedApis;
    }

    @Override
    public List<Filter> applyPermissionBasedFilters(@NotNull List<Filter> filters, Set<String> allowedApiIds) {
        if (filters.isEmpty()) {
            var filter = new Filter(API, IN, allowedApiIds.stream().toList());
            return List.of(filter);
        }

        return getUpdatedFilters(filters, allowedApiIds);
    }

    private @NotNull List<Filter> getUpdatedFilters(@NotNull List<Filter> filters, Set<String> allowedApiIds) {
        return filters
            .stream()
            .map(filter -> {
                if (filter.name() == API) {
                    return switch (filter.operator()) {
                        case IN -> getUpdatedInFilter(filter, allowedApiIds);
                        case EQ -> getUpdatedEqFilter(filter, allowedApiIds);
                        default -> filter;
                    };
                }

                return filter;
            })
            .toList();
    }

    private @NotNull Filter getUpdatedInFilter(@NotNull Filter filter, Set<String> allowedApiIds) {
        if (!(filter.value() instanceof Iterable<?>)) {
            throw new IllegalArgumentException("Filter value must be an Iterable");
        }

        var objectMapper = new ObjectMapper();
        Set<String> wantedApiIds = objectMapper.convertValue(filter.value(), new TypeReference<>() {});

        wantedApiIds.retainAll(allowedApiIds);
        return new Filter(filter.name(), IN, wantedApiIds.stream().toList());
    }

    private @NotNull Filter getUpdatedEqFilter(Filter filter, Set<String> allowedApiIds) {
        var apiId = filter.value().toString();

        if (allowedApiIds.contains(apiId)) {
            return filter;
        }

        return new Filter(filter.name(), IN, List.of());
    }

    protected boolean isEnvironmentAdmin() {
        return isUserInRole(ENVIRONMENT_ADMIN);
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
                    var authority = grantedAuthority.getAuthority();
                    return authority.equalsIgnoreCase(role);
                }
            );
    }

    protected Principal getUserPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication instanceof AnonymousAuthenticationToken) ? null : authentication;
    }
}
