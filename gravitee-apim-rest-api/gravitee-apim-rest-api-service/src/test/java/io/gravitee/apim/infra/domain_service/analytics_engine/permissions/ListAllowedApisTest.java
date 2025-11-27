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
package io.gravitee.apim.infra.domain_service.analytics_engine.permissions;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @author GraviteeSource Team
 */
@DisplayName("analytics filters")
class ListAllowedApisTest extends AbstractTest {

    @Inject
    PermissionService permissionService;

    @Inject
    ApiAuthorizationService apiAuthorizationService;

    static final Authentication authentication = mock(Authentication.class);

    static final String apiId1 = "api-id-1";
    static final String apiId2 = "api-id-2";
    static final String apiId3 = "api-id-3";
    static final String apiId4 = "api-id-4";

    static List<String> environmentApiIds = List.of(apiId1, apiId2, apiId3, apiId4);

    @BeforeEach
    public void setUp() {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    @Test
    void should_allow_administrators_access_to_all_environment_APIs() {
        var environmentAdminRole = "ENVIRONMENT:ADMIN";

        when(apiAuthorizationService.findIdsByEnvironment("DEFAULT")).thenReturn(Set.copyOf(environmentApiIds));

        GrantedAuthority organizationAdmin = () -> environmentAdminRole;
        setAuthorities(organizationAdmin);

        var allowedApis = apiAnalyticsQueryFilterDecorator.getAllowedApis();

        var apiIds = allowedApis.keySet();
        assertThat(apiIds).containsExactlyInAnyOrderElementsOf(environmentApiIds);
    }

    @ParameterizedTest
    @MethodSource("nonEnvironmentAdminRoles")
    void should_allow_access_to_ids_where_user_has_ANALYTICS_READ_role(String role) {
        var user = "testUser";
        setAuthenticatedUsername(user);

        GrantedAuthority environmentUser = () -> role;
        setAuthorities(environmentUser);

        var apisUserIsAMemberOf = Set.of(apiId1, apiId2, apiId3);
        when(apiAuthorizationService.findIdsByUser(any(), eq(user), anyBoolean())).thenReturn(apisUserIsAMemberOf);

        var apisWithAnalyticsPermission = Set.of(apiId1, apiId3);
        when(
            permissionService.hasPermission(any(), eq(API_ANALYTICS), argThat(apisWithAnalyticsPermission::contains), eq(READ))
        ).thenReturn(true);

        var allowedApis = apiAnalyticsQueryFilterDecorator.getAllowedApis();

        var apiIds = allowedApis.keySet();
        assertThat(apiIds).containsExactlyInAnyOrderElementsOf(apisWithAnalyticsPermission);
    }

    // Returns all roles defined in RolePermission formated as SCOPE:PERMISSION
    static Stream<String> nonEnvironmentAdminRoles() {
        var roles = Arrays.stream(RolePermission.values())
            .map(rolePermission -> String.format("%s:%s", rolePermission.getScope(), rolePermission.getPermission()))
            .collect(Collectors.toSet());

        roles.add("ORGANIZATION:ADMIN");

        return roles.stream();
    }

    void setAuthenticatedUsername(String username) {
        when(authentication.getName()).thenReturn(username);
    }

    void setAuthorities(GrantedAuthority... authorities) {
        Collection authorityList = new ArrayList<>(Arrays.stream(authorities).toList());
        when(authentication.getAuthorities()).thenReturn(authorityList);
    }
}
