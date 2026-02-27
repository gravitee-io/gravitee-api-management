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

import io.gravitee.apim.core.analytics_engine.domain_service.FilterPreProcessor;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ManagementFilterPreProcessor implements FilterPreProcessor {

    private static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();
    protected static final String UNKNOWN_SERVICE = "1";
    private final ApiAuthorizationService apiAuthorizationService;
    private final ApiRepository apiRepository;

    @Override
    public MetricsContext buildFilters(MetricsContext context) {
        var userApis = findUserApis(
            context.auditInfo().organizationId(),
            context.auditInfo().environmentId(),
            context.auditInfo().actor().userId()
        );

        var userApisIds = userApis.keySet();
        userApis.put(UNKNOWN_SERVICE, UNKNOWN_SERVICE);
        var permissionsFilter = new Filter(API, IN, userApisIds);

        return context.withFilters(List.of(permissionsFilter)).withApiNamesById(userApis);
    }

    private static Map<String, String> mapApiIdsToNames(Collection<Api> apis) {
        return apis.stream().collect(Collectors.toMap(Api::getId, Api::getName));
    }

    protected boolean isAdmin() {
        return SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .anyMatch(
                (Predicate<GrantedAuthority>) grantedAuthority -> {
                    var authority = grantedAuthority.getAuthority();
                    return authority.equalsIgnoreCase(ORGANIZATION_ADMIN);
                }
            );
    }

    private Map<String, String> findUserApis(String organizationId, String environmentId, String userId) {
        ExecutionContext executionContext = new ExecutionContext(organizationId, environmentId);

        ApiCriteria.Builder apiCriteriaBuilder = new ApiCriteria.Builder().environmentId(environmentId);

        if (!isAdmin()) {
            Set<String> userApiIds = apiAuthorizationService.findApiIdsByUserId(executionContext, userId, null, true);

            apiCriteriaBuilder.ids(userApiIds);
        }

        List<Api> apis = apiRepository.search(apiCriteriaBuilder.build(), ApiFieldFilter.defaultFields());

        return mapApiIdsToNames(apis);
    }
}
