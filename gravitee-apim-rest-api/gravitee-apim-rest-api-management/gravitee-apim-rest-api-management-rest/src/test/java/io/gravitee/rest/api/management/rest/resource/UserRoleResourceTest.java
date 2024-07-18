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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserReferenceRoleEntity;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class UserRoleResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT_ID = "envId";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_ROLE = "USER";
    private static final String USER_ID = "userId";
    private static final String AUTHENTICATED_USER_ID = "UnitTests";
    private static final RoleEntity ADMIN_ROLE_ENTITY = RoleEntity.builder().id(ADMIN_ROLE).name(ADMIN_ROLE).build();
    private static final RoleEntity USER_ROLE_ENTITY = RoleEntity.builder().id(USER_ROLE).name(USER_ROLE).build();

    @Override
    protected String contextPath() {
        return "users/userId/roles";
    }

    @Before
    public void init() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void should_not_update_user_roles_with_admin_when_not_authorized() {
        UserReferenceRoleEntity userReferenceRoleEntity = new UserReferenceRoleEntity();
        userReferenceRoleEntity.setUser(USER_ID);
        userReferenceRoleEntity.setReferenceId(ENVIRONMENT_ID);
        userReferenceRoleEntity.setReferenceType(MembershipReferenceType.ENVIRONMENT);
        userReferenceRoleEntity.setRoles(List.of(ADMIN_ROLE));

        when(
            membershipService.getRoles(
                eq(MembershipReferenceType.ENVIRONMENT),
                eq(ENVIRONMENT_ID),
                eq(MembershipMemberType.USER),
                eq(USER_ID)
            )
        )
            .thenReturn(Set.of(USER_ROLE_ENTITY));

        when(
            membershipService.getRoles(
                eq(MembershipReferenceType.ENVIRONMENT),
                eq(ENVIRONMENT_ID),
                eq(MembershipMemberType.USER),
                eq(AUTHENTICATED_USER_ID)
            )
        )
            .thenReturn(Set.of(USER_ROLE_ENTITY));

        when(roleService.findAllById(any())).thenReturn(Set.of(ADMIN_ROLE_ENTITY));

        final Response response = orgTarget().request().put(Entity.json(userReferenceRoleEntity));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void should_update_user_roles() {
        UserReferenceRoleEntity userReferenceRoleEntity = new UserReferenceRoleEntity();
        userReferenceRoleEntity.setUser(USER_ID);
        userReferenceRoleEntity.setReferenceId(ENVIRONMENT_ID);
        userReferenceRoleEntity.setReferenceType(MembershipReferenceType.ENVIRONMENT);
        userReferenceRoleEntity.setRoles(List.of(ADMIN_ROLE));

        when(
            membershipService.getRoles(
                eq(MembershipReferenceType.ENVIRONMENT),
                eq(ENVIRONMENT_ID),
                eq(MembershipMemberType.USER),
                eq(USER_ID)
            )
        )
            .thenReturn(Set.of(USER_ROLE_ENTITY));

        when(
            membershipService.getRoles(
                eq(MembershipReferenceType.ENVIRONMENT),
                eq(ENVIRONMENT_ID),
                eq(MembershipMemberType.USER),
                eq(AUTHENTICATED_USER_ID)
            )
        )
            .thenReturn(Set.of(ADMIN_ROLE_ENTITY));

        when(roleService.findAllById(any())).thenReturn(Set.of(ADMIN_ROLE_ENTITY));

        final Response response = orgTarget().request().put(Entity.json(userReferenceRoleEntity));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }
}
