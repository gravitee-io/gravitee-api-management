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

import io.gravitee.apim.core.analytics.domain_service.EnvironmentAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoader;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
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
public class ManagementContextLoader implements AnalyticsQueryContextLoader {

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
    public AnalyticsQueryContext load(AuditInfo auditInfo, ApiDataKind kind) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var userId = auditInfo.actor().userId();

        var apis = readableApis.findReadableApis(executionContext, userId, isAdmin(), kind);

        var authorizedApiIds = apis.stream().map(Api::getId).collect(Collectors.toSet());
        var apiNamesById = mapApiIdsToNames(apis);
        var apiIdsByType = groupApiIdsByType(apis);

        return new AnalyticsQueryContext(
            auditInfo,
            executionContext,
            authorizedApiIds,
            apiNamesById,
            Map.of(),
            apiIdsByType,
            Optional.empty()
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
