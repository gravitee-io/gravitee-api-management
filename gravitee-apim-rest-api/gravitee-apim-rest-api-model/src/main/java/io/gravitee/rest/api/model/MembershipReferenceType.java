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
package io.gravitee.rest.api.model;

import io.gravitee.rest.api.model.permissions.RoleScope;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Schema(enumAsRef = true)
@RequiredArgsConstructor
public enum MembershipReferenceType {
    APPLICATION(EnumSet.of(RoleScope.APPLICATION)),
    API(EnumSet.of(RoleScope.API)),
    GROUP(EnumSet.of(RoleScope.GROUP, RoleScope.API, RoleScope.APPLICATION, RoleScope.INTEGRATION)),
    ENVIRONMENT(EnumSet.allOf(RoleScope.class)),
    ORGANIZATION(EnumSet.allOf(RoleScope.class)),
    PLATFORM(EnumSet.allOf(RoleScope.class)),
    INTEGRATION(EnumSet.allOf(RoleScope.class));

    private final EnumSet<RoleScope> roleScopes;

    public boolean allowedRoleScope(RoleScope scope) {
        return roleScopes.contains(scope);
    }

    public RoleScope findScope() {
        return roleScopes.size() == 1
            ? roleScopes.iterator().next()
            : roleScopes.stream().filter(scope -> scope.name().equals(name())).findFirst().orElse(roleScopes.iterator().next());
    }
}
