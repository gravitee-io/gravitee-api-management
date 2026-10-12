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
package io.gravitee.apim.rest.api.mcp.infra;

import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.EnvironmentService;
import java.util.ArrayList;
import org.springframework.security.core.context.SecurityContextHolder;

public class EnvironmentServiceEnvironmentDirectory implements EnvironmentDirectory {

    private static final String ORGANIZATION_ADMIN = RoleScope.ORGANIZATION.name() + ':' + SystemRole.ADMIN.name();

    private final EnvironmentService environmentService;
    private final TriageAccess access;

    public EnvironmentServiceEnvironmentDirectory(EnvironmentService environmentService, TriageAccess access) {
        this.environmentService = environmentService;
        this.access = access;
    }

    @Override
    public Environments environments(McpCaller caller) {
        // An organization admin is a member of no environment; a null user lists them all, as the Console does.
        var members = environmentService.findByUser(caller.organizationId(), isOrganizationAdmin() ? null : caller.userId());
        var readable = new ArrayList<EnvRef>();
        var unreadable = new ArrayList<EnvRef>();
        for (var entity : members) {
            var env = toRef(entity);
            var auditInfo = caller.on(env.id());
            (ApimContext.run(auditInfo, () -> access.canReadTraffic(auditInfo)) ? readable : unreadable).add(env);
        }
        return new Environments(readable, unreadable);
    }

    private static EnvRef toRef(EnvironmentEntity entity) {
        var hrid = entity.getHrids() == null || entity.getHrids().isEmpty() ? null : entity.getHrids().getFirst();
        return new EnvRef(entity.getId(), hrid, entity.getName());
    }

    private static boolean isOrganizationAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return (
            authentication != null &&
            authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority -> ORGANIZATION_ADMIN.equalsIgnoreCase(authority.getAuthority()))
        );
    }
}
