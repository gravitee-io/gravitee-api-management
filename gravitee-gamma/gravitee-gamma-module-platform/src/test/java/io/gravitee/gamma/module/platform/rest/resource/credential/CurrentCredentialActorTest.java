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
package io.gravitee.gamma.module.platform.rest.resource.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor.SecretAccess;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CurrentCredentialActorTest {

    private final PermissionService permissionService = mock(PermissionService.class);

    private record Roles(Set<String> roles) implements SecurityContext {
        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public boolean isUserInRole(String role) {
            return roles.contains(role);
        }

        @Override
        public boolean isSecure() {
            return true;
        }

        @Override
        public String getAuthenticationScheme() {
            return null;
        }
    }

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentOrganization("org-1");
        GraviteeContext.setCurrentEnvironment("env-1");
        var user = new UserDetails("user-1", "", List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
        SecurityContextHolder.clearContext();
    }

    private void grantSecret(RolePermissionAction action) {
        when(permissionService.hasPermission(any(), eq(RolePermission.ENVIRONMENT_SECRET), eq("env-1"), eq(action))).thenReturn(true);
    }

    @Test
    void should_act_as_the_authenticated_user_in_the_environment_of_the_request() {
        var actor = CurrentCredentialActor.of(new Roles(Set.of()), permissionService);

        assertThat(actor.userId()).isEqualTo("user-1");
        assertThat(actor.organizationId()).isEqualTo("org-1");
        assertThat(actor.environmentId()).isEqualTo("env-1");
        assertThat(actor.admin()).isFalse();
    }

    @Test
    void should_treat_an_organization_admin_as_admin() {
        assertThat(CurrentCredentialActor.of(new Roles(Set.of("ORGANIZATION:ADMIN")), permissionService).admin()).isTrue();
    }

    @Test
    void should_treat_an_environment_admin_as_admin() {
        assertThat(CurrentCredentialActor.of(new Roles(Set.of("ENVIRONMENT:ADMIN")), permissionService).admin()).isTrue();
    }

    @Test
    void should_not_treat_an_api_publisher_as_admin() {
        assertThat(CurrentCredentialActor.of(new Roles(Set.of("ENVIRONMENT:API_PUBLISHER")), permissionService).admin()).isFalse();
    }

    @Test
    void should_carry_each_secret_permission_the_user_holds_on_the_environment() {
        grantSecret(RolePermissionAction.READ);
        grantSecret(RolePermissionAction.DELETE);

        var actor = CurrentCredentialActor.of(new Roles(Set.of()), permissionService);

        assertThat(actor.secretAccess()).containsExactlyInAnyOrder(SecretAccess.READ, SecretAccess.DELETE);
    }

    @Test
    void should_refuse_a_request_without_an_authenticated_user() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> CurrentCredentialActor.of(new Roles(Set.of()), permissionService)).isInstanceOf(
            NotAllowedDomainException.class
        );
    }
}
