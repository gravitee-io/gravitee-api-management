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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.InvitationEntity;
import io.gravitee.rest.api.model.InvitationReferenceType;
import io.gravitee.rest.api.model.NewInvitationEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.InvitationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class GroupInvitationsResourceTest extends AbstractResourceTest {

    private GroupEntity group;

    @Autowired
    private InvitationService invitationService;

    @Override
    protected String contextPath() {
        return "configuration/groups/b72c4ad7-10aa-4331-ac4a-d710aad331ab/invitations/";
    }

    @Before
    public void init() {
        group = new GroupEntity();
        group.setId("b72c4ad7-10aa-4331-ac4a-d710aad331ab");
        group.setName("group1");
        group.setMaxInvitation(5);

        when(groupService.findById(GraviteeContext.getExecutionContext(), "b72c4ad7-10aa-4331-ac4a-d710aad331ab")).thenReturn(group);
    }

    @Test
    public void should_throw_when_max_invitations_reached() {
        NewInvitationEntity newInvitation = new NewInvitationEntity();
        newInvitation.setReferenceId("b72c4ad7-10aa-4331-ac4a-d710aad331ab");
        newInvitation.setEmail("test@test.com");
        newInvitation.setReferenceType(InvitationReferenceType.GROUP);
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_GROUP,
                GraviteeContext.getCurrentEnvironment(),
                RolePermissionAction.CREATE,
                RolePermissionAction.UPDATE,
                RolePermissionAction.DELETE
            )
        )
            .thenReturn(false);
        when(groupService.getNumberOfMembers(GraviteeContext.getExecutionContext(), "b72c4ad7-10aa-4331-ac4a-d710aad331ab")).thenReturn(5);

        final Response response = envTarget().request().post(Entity.json(newInvitation));

        assertEquals(400, response.getStatus());

        assertThat(response.readEntity(String.class), containsString("Limitation of 5 members exceeded"));
    }

    @Test
    public void should_throw_when_email_invitation_is_not_allowed() {
        group.setEmailInvitation(false);
        NewInvitationEntity newInvitation = new NewInvitationEntity();
        newInvitation.setReferenceId("b72c4ad7-10aa-4331-ac4a-d710aad331ab");
        newInvitation.setEmail("test@test.com");
        newInvitation.setReferenceType(InvitationReferenceType.GROUP);
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_GROUP,
                GraviteeContext.getCurrentEnvironment(),
                RolePermissionAction.CREATE,
                RolePermissionAction.UPDATE,
                RolePermissionAction.DELETE
            )
        )
            .thenReturn(false);
        when(groupService.getNumberOfMembers(GraviteeContext.getExecutionContext(), "b72c4ad7-10aa-4331-ac4a-d710aad331ab")).thenReturn(2);

        final Response response = envTarget().request().post(Entity.json(newInvitation));

        // We have returned 404 in case of forbidden. Probably, needs to change.
        assertEquals(404, response.getStatus());

        assertThat(
            response.readEntity(String.class),
            containsString("Invitation email is forbidden for group [b72c4ad7-10aa-4331-ac4a-d710aad331ab]")
        );
    }

    @Test
    public void should_return_list_of_users_holding_same_email_id() {
        group.setEmailInvitation(true);
        NewInvitationEntity newInvitation = new NewInvitationEntity();
        newInvitation.setReferenceId("b72c4ad7-10aa-4331-ac4a-d710aad331ab");
        newInvitation.setEmail("test@test.com");
        newInvitation.setReferenceType(InvitationReferenceType.GROUP);
        UserEntity user1 = new UserEntity();
        user1.setEmail("test@test.com");
        UserEntity user2 = new UserEntity();
        user2.setEmail("test@test.com");
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_GROUP,
                GraviteeContext.getCurrentEnvironment(),
                RolePermissionAction.CREATE,
                RolePermissionAction.UPDATE,
                RolePermissionAction.DELETE
            )
        )
            .thenReturn(true);
        when(userService.findByEmail(GraviteeContext.getExecutionContext(), "test@test.com")).thenReturn(List.of(user1, user2));

        final Response response = envTarget().request().post(Entity.json(newInvitation));

        assertEquals(202, response.getStatus());
        List<UserEntity> users = response.readEntity(new GenericType<>() {});
        assertEquals(2, users.size());
    }

    @Test
    public void should_create_invitation() {
        group.setEmailInvitation(true);
        NewInvitationEntity newInvitation = new NewInvitationEntity();
        newInvitation.setReferenceId("b72c4ad7-10aa-4331-ac4a-d710aad331ab");
        newInvitation.setEmail("test@test.com");
        newInvitation.setReferenceType(InvitationReferenceType.GROUP);
        InvitationEntity invitationEntity = new InvitationEntity();
        invitationEntity.setId("e98ede5e-ee8d-4469-8ede-5eee8d14693a");
        invitationEntity.setEmail("test@test.com");
        invitationEntity.setReferenceId("b72c4ad7-10aa-4331-ac4a-d710aad331ab");
        invitationEntity.setReferenceType(InvitationReferenceType.GROUP);
        UserEntity user = new UserEntity();
        user.setEmail("test@test.com");
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_GROUP,
                GraviteeContext.getCurrentEnvironment(),
                RolePermissionAction.CREATE,
                RolePermissionAction.UPDATE,
                RolePermissionAction.DELETE
            )
        )
            .thenReturn(true);
        when(userService.findByEmail(GraviteeContext.getExecutionContext(), "test@test.com")).thenReturn(List.of(user));
        when(invitationService.create(GraviteeContext.getExecutionContext(), newInvitation)).thenReturn(invitationEntity);
        final Response response = envTarget().request().post(Entity.json(newInvitation));

        assertEquals(200, response.getStatus());
        InvitationEntity invitation = response.readEntity(new GenericType<>() {});
        assertEquals("e98ede5e-ee8d-4469-8ede-5eee8d14693a", invitation.getId());
    }
}
