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
package io.gravitee.rest.api.service.common;

import static org.springframework.security.core.authority.AuthorityUtils.*;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.UserRoleEntity;
import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author GraviteeSource Team
 */
public class SecurityContextHelper {

    private SecurityContextHelper() {}

    public static void authenticateAs(UserEntity user) {
        authenticateAs(user, false);
    }

    public static void authenticateAsSystem(String userId, Set<UserRoleEntity> userRoles) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setRoles(userRoles);
        authenticateAs(user, true);
    }

    private static void authenticateAs(UserEntity user, boolean isSystem) {
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return new Authentication() {
                        @Override
                        public Collection<? extends GrantedAuthority> getAuthorities() {
                            return createAuthorityList(computeAuthorities(user));
                        }

                        @Override
                        public Object getCredentials() {
                            return null;
                        }

                        @Override
                        public Object getDetails() {
                            return null;
                        }

                        @Override
                        public Object getPrincipal() {
                            return new UserDetails(user.getId(), "", user.getEmail(), getAuthorities(), isSystem);
                        }

                        @Override
                        public boolean isAuthenticated() {
                            return true;
                        }

                        @Override
                        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
                            /* noop */
                        }

                        @Override
                        public String getName() {
                            return ((UserDetails) getPrincipal()).getDisplayName();
                        }
                    };
                }

                @Override
                public void setAuthentication(Authentication authentication) {
                    /* noop */
                }
            }
        );
    }

    private static String[] computeAuthorities(UserEntity user) {
        if (user.getRoles() == null) {
            return new String[] {};
        }
        return user.getRoles().stream().map(role -> role.getScope().name() + ":" + role.getName()).toArray(String[]::new);
    }
}
