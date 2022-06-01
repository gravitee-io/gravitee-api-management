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
package io.gravitee.rest.api.common;

import static io.gravitee.rest.api.service.common.SecurityContextHelper.*;
import static org.assertj.core.api.Assertions.*;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.UserRoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Set;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author GraviteeSource Team
 */
public class SecurityContextHelperTest {

    private static UserRoleEntity userRole(RoleScope scope, String name) {
        UserRoleEntity role = new UserRoleEntity();
        role.setId(UuidString.generateRandom());
        role.setScope(scope);
        role.setName(name);
        return role;
    }

    private static final Set<UserRoleEntity> USER_ROLES = Set.of(
        userRole(RoleScope.ENVIRONMENT, "ADMIN"),
        userRole(RoleScope.ORGANIZATION, "ADMIN")
    );

    @Test
    public void shouldAuthenticateAsUser() {
        final UserEntity user = new UserEntity();
        user.setId("user-id");
        user.setEmail("test@gravitee.io");
        user.setRoles(USER_ROLES);

        authenticateAs(user);

        assertContextMatches("user-id", "test@gravitee.io", "ENVIRONMENT:ADMIN", "ORGANIZATION:ADMIN");
    }

    @Test
    public void shouldAuthenticateAsSystem() {
        authenticateAsSystem("SYSTEM", USER_ROLES);

        assertContextMatches("SYSTEM", null, "ENVIRONMENT:ADMIN", "ORGANIZATION:ADMIN");
    }

    private void assertContextMatches(String userName, String userEmail, String... authorities) {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext.getAuthentication()).isNotNull();
        final Object principal = securityContext.getAuthentication().getPrincipal();
        assertThat(principal).isInstanceOf(UserDetails.class);
        final UserDetails userDetails = (UserDetails) principal;
        assertThat(userDetails.getUsername()).isEqualTo(userName);
        assertThat(userDetails.getEmail()).isEqualTo(userEmail);
        assertThat(userDetails.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly(authorities);
    }
}
