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
package io.gravitee.apim.infra.domain_service.user;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.apim.infra.domain_service.analytics_engine.mapper.ApiMapper;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.util.Collection;
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
public class UserContextLoaderImpl implements UserContextLoader {

    private static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();

    private final ApiAuthorizationService apiAuthorizationService;
    private final ApiRepository apiRepository;

    private boolean isAdmin() {
        return SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .anyMatch(
                (Predicate<GrantedAuthority>) grantedAuthority -> grantedAuthority.getAuthority().equalsIgnoreCase(ORGANIZATION_ADMIN)
            );
    }

    @Override
    public UserContext loadApis(UserContext context) {
        var organizationId = context.auditInfo().organizationId();
        var environmentId = context.auditInfo().environmentId();
        var userId = context.auditInfo().actor().userId();

        ExecutionContext executionContext = new ExecutionContext(organizationId, environmentId);

        ApiCriteria.Builder apiCriteriaBuilder = new ApiCriteria.Builder().environmentId(environmentId);

        if (!isAdmin()) {
            Set<String> userApiIds = apiAuthorizationService.findApiIdsByUserId(executionContext, userId, null, true);

            apiCriteriaBuilder.ids(userApiIds);
        }

        var apis = ApiMapper.INSTANCE.map(apiRepository.search(apiCriteriaBuilder.build(), ApiFieldFilter.defaultFields()));
        var apiIdsToNames = mapApiIdsToNames(apis);

        return context.withApis(apis).withApiNamesById(apiIdsToNames);
    }

    private static Map<String, String> mapApiIdsToNames(Collection<Api> apis) {
        return apis.stream().collect(Collectors.toMap(Api::getId, Api::getName));
    }
}
