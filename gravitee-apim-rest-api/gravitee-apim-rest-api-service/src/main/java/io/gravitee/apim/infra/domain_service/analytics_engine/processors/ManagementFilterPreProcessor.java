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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import java.util.List;
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

    @Override
    public List<Filter> buildFilters(MetricsContext context) {
        if (context.apis().isEmpty()) {
            throw new IllegalStateException("No APIs were found for the current user");
        }

        var apiIds = context.apis().get().stream().map(Api::getId).collect(Collectors.toList());

        var permissionsFilter = new Filter(API, IN, apiIds);

        return List.of(permissionsFilter);
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
}
