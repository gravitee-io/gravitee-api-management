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
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name.APPLICATION;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.EQ;
import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.IN;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryFilterDecorator;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
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
public class ApiAnalyticsQueryFilterDecoratorImpl implements AnalyticsQueryFilterDecorator {

    private static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();

    private static final String UNKNOWN_APPLICATION = "Unknown";

    private final ApiSearchService apiSearchService;

    private final ApplicationService applicationSearchService;

    @Override
    public MetricsContext getFilteredContext(MetricsContext context, List<Filter> requestFilters) {
        var allowedApis = getAllowedApis(requestFilters);
        var allowedApplications = getAllowedApplications(requestFilters);
        var updatedFilters = updateContextFilters(context.filters(), allowedApis);

        return context.withApiNamesById(allowedApis).withApplicationNameById(allowedApplications).withFilters(updatedFilters);
    }

    private Map<String, String> getAllowedApis(List<Filter> filters) {
        QueryBuilder<ApiEntity> queryBuilder = QueryBuilder.create(ApiEntity.class);

        var apiIds = getIdsFromFilters(filters, API);
        if (!(apiIds.isEmpty())) {
            queryBuilder.setFilters(Map.of("id", apiIds));
        }

        boolean admin = isAdmin();
        Principal userPrincipal = getUserPrincipal();
        String name = userPrincipal.getName();
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        Page<GenericApiEntity> search = apiSearchService.search(
            executionContext,
            name,
            admin,
            queryBuilder,
            new PageableImpl(0, Integer.MAX_VALUE),
            false,
            true
        );

        List<GenericApiEntity> content = search.getContent();

        return mapApiIdsToNames(content);
    }

    private Map<String, String> getAllowedApplications(List<Filter> filters) {
        var query = new ApplicationQuery();

        var applicationIds = getIdsFromFilters(filters, APPLICATION);
        if (!(applicationIds.isEmpty())) {
            query.setIds(applicationIds);
        }

        if (!isAdmin()) {
            query.setUser(getAuthenticatedUser());
        }

        List<ApplicationListItem> content = applicationSearchService
            .search(GraviteeContext.getExecutionContext(), query, null, null)
            .getContent();
        return mapApplicationIdsToNames(content);
    }

    /**
     * getIdsFromFilters returns all the IDs found in a list of filters.
     *
     * @param filterName specifies the filters to look for
     */
    public static Set<String> getIdsFromFilters(List<Filter> filters, FilterSpec.Name filterName) {
        if (filters.isEmpty()) {
            return Set.of();
        }

        List<Filter> filtersByName = filters
            .stream()
            .filter(filter -> filter.name() == filterName)
            .toList();

        if (filtersByName.isEmpty()) {
            return Set.of();
        }

        List eqFilters = filtersByName
            .stream()
            .filter(filter -> filter.operator() == EQ)
            .map(Filter::value)
            .toList();

        var objectMapper = new ObjectMapper();
        List inFilters = filtersByName
            .stream()
            .filter(filter -> filter.operator() == IN)
            .map(Filter::value)
            .filter(Collection.class::isInstance)
            .flatMap(o -> ((Collection<?>) o).stream())
            .map(v -> objectMapper.convertValue(v, new TypeReference<>() {}))
            .toList();

        var apiIds = new HashSet<String>(eqFilters);
        apiIds.addAll(inFilters);

        return apiIds;
    }

    /**
     * addApiFilters adds a filter on API IDs to the existing filters.
     */
    public static List<Filter> updateContextFilters(List<Filter> filters, Map<String, String> allowedApis) {
        var allowedApiIds = allowedApis.keySet().stream().toList();

        var updatedFilters = new ArrayList<>(filters);
        updatedFilters.add(new Filter(API, IN, allowedApiIds));

        return updatedFilters;
    }

    private static Map<String, String> mapApiIdsToNames(Collection<GenericApiEntity> apis) {
        return apis.stream().collect(Collectors.toMap(GenericApiEntity::getId, GenericApiEntity::getName));
    }

    private static Map<String, String> mapApplicationIdsToNames(List<ApplicationListItem> applications) {
        Map<String, String> applicationMap = applications
            .stream()
            .collect(Collectors.toMap(ApplicationListItem::getId, ApplicationListItem::getName));
        applicationMap.put("1", UNKNOWN_APPLICATION);

        return applicationMap;
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
