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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name.API;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.IN;

import io.gravitee.apim.core.analytics_engine.domain_service.PermissionsPreprocessor;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.security.Principal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
@RequiredArgsConstructor
public class PermissionsPreprocessorImpl implements PermissionsPreprocessor {

    private static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();

    private final ApiSearchService apiSearchService;

    @Override
    public Map<String, String> findAllowedApis() {
        QueryBuilder<ApiEntity> queryBuilder = QueryBuilder.create(ApiEntity.class);

        var admin = isAdmin();
        var userPrincipal = getUserPrincipal();
        var name = userPrincipal.getName();
        var executionContext = GraviteeContext.getExecutionContext();

        var content = apiSearchService
            .search(executionContext, name, admin, queryBuilder, new PageableImpl(0, Integer.MAX_VALUE), false, true)
            .getContent();

        return mapApiIdsToNames(content);
    }

    @Override
    public List<Filter> buildFilterForAllowedApis(MetricsContext context) {
        var allowedApiIds = context.apiNameById().get().keySet().stream().toList();

        return List.of(new Filter(API, IN, allowedApiIds));
    }

    /**
     * addApiFilters adds a filter on API IDs to the existing filters.
     */

    private static Map<String, String> mapApiIdsToNames(Collection<GenericApiEntity> apis) {
        return apis.stream().collect(Collectors.toMap(GenericApiEntity::getId, GenericApiEntity::getName));
    }

    protected boolean isAdmin() {
        return isUserInRole(ORGANIZATION_ADMIN);
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
