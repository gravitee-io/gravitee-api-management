/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractService extends TransactionalService {

    public final static String MANAGEMENT_ADMIN = RoleScope.MANAGEMENT.name() + ':' + SystemRole.ADMIN.name();
    public final static String PORTAL_ADMIN = RoleScope.PORTAL.name() + ':' + SystemRole.ADMIN.name();

    String getAuthenticatedUsername() {
        UserDetails authenticatedUser = getAuthenticatedUser();
        return authenticatedUser == null ? null : authenticatedUser.getUsername();
    }

    UserDetails getAuthenticatedUser() {
        if (isAuthenticated()) {
            return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }
        return null;
    }

    protected boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails;
    }

    protected boolean isAdmin() {
        return isUserInRole(MANAGEMENT_ADMIN) || isUserInRole(PORTAL_ADMIN);
    }

    private boolean isUserInRole(final String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> role.equals(auth.getAuthority()));
    }
}
