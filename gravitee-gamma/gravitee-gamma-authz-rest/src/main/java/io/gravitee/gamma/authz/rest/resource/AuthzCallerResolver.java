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
package io.gravitee.gamma.authz.rest.resource;

import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

final class AuthzCallerResolver {

    private AuthzCallerResolver() {}

    static AuthzCallerContext resolve(SecurityContext securityContext, String pathEnvironmentId) {
        String org = GraviteeContext.getCurrentOrganization();
        String env = GraviteeContext.getCurrentEnvironment();
        if (org == null || env == null) {
            throw new UnauthorizedAccessException();
        }
        if (!env.equals(pathEnvironmentId)) {
            throw new ForbiddenAccessException();
        }
        Principal principal = securityContext == null ? null : securityContext.getUserPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new UnauthorizedAccessException();
        }
        return AuthzCallerContext.ofUser(org, env, principal.getName());
    }
}
