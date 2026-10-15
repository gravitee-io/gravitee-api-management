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

import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    private final EnvironmentAnalyticsApisDomainService readableApis;

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
    public UserContext loadApis(UserContext context, ApiDataKind kind) {
        var executionContext = executionContext(context);
        var userId = context.auditInfo().actor().userId();

        var apis = readableApis.findReadableApis(executionContext, userId, isAdmin(), kind);

        return context.withApis(apis).withApiNamesById(mapApiIdsToNames(apis));
    }

    @Override
    public UserContext loadApi(UserContext context, String apiId, ApiDataKind kind) {
        var executionContext = executionContext(context);
        var userId = context.auditInfo().actor().userId();

        List<Api> apis = readableApis.findReadableApi(executionContext, userId, isAdmin(), kind, apiId).stream().toList();

        return context.withApis(apis).withApiNamesById(mapApiIdsToNames(apis));
    }

    private static ExecutionContext executionContext(UserContext context) {
        return new ExecutionContext(context.auditInfo().organizationId(), context.auditInfo().environmentId());
    }

    private static Map<String, String> mapApiIdsToNames(Collection<Api> apis) {
        return apis.stream().collect(Collectors.toMap(Api::getId, Api::getName));
    }
}
