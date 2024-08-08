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

import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_GROUP;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.model.GroupMembership;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupMembersResourceTest extends AbstractResourceTest {

    private static final String GROUP_ID = "group-id";
    private static final String USERNAME = "user";
    private static final String DEFAULT_API_ROLE = "DEFAULT_API_ROLE";
    private static final String DEFAULT_APPLICATION_ROLE = "DEFAULT_APPLICATION_ROLE";

    @Override
    protected String contextPath() {
        return "configuration/groups/" + GROUP_ID + "/members/";
    }

    //ADD

    private void initADDmock() {
        reset(roleService, groupService, membershipService);
        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP_ID)).thenReturn(mock(GroupEntity.class));

        RoleEntity defaultApiRole = mock(RoleEntity.class);
        when(defaultApiRole.getName()).thenReturn(DEFAULT_API_ROLE);
        RoleEntity defaultApplicationRole = mock(RoleEntity.class);
        when(defaultApplicationRole.getName()).thenReturn(DEFAULT_APPLICATION_ROLE);
        when(roleService.findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.API))
            .thenReturn(Collections.singletonList(defaultApiRole));
        when(roleService.findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.APPLICATION))
            .thenReturn(Collections.singletonList(defaultApplicationRole));

        RoleEntity customApiRole = new RoleEntity();
        customApiRole.setId("API_CUSTOM_API");
        customApiRole.setName("CUSTOM_API");
        customApiRole.setScope(RoleScope.API);
        when(roleService.findByScopeAndName(RoleScope.API, "CUSTOM_API", GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(customApiRole));

        RoleEntity customApplicationRole = new RoleEntity();
        customApplicationRole.setId("APP_CUSTOM_APP");
        customApplicationRole.setName("CUSTOM_APP");
        customApplicationRole.setScope(RoleScope.APPLICATION);
        when(roleService.findByScopeAndName(RoleScope.APPLICATION, "CUSTOM_APP", GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(customApplicationRole));

        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setId(USERNAME);
        when(membershipService.addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any()))
            .thenReturn(memberEntity);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(ENVIRONMENT_GROUP),
                eq("DEFAULT"),
                eq(CREATE),
                eq(UPDATE),
                eq(DELETE)
            )
        )
            .thenReturn(true);
    }

    @Test
    public void shouldAddMemberWithCustomApplicationRoleAndCustomApiRole() {
        initADDmock();
        MemberRoleEntity apiRole = new MemberRoleEntity();
        apiRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.API);
        apiRole.setRoleName("CUSTOM_API");
        MemberRoleEntity appRole = new MemberRoleEntity();
        appRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION);
        appRole.setRoleName("CUSTOM_APP");

        GroupMembership groupMembership = new GroupMembership();
        groupMembership.setId(USERNAME);
        groupMembership.setRoles(List.of(apiRole, appRole));

        final Response response = envTarget().request().post(Entity.json(Set.of(groupMembership)));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.API);
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.APPLICATION);
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "CUSTOM_API")
            );

        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "CUSTOM_APP")
            );
    }

    @Test
    public void shouldAddMemberWithDefaultGroupRole() {
        initADDmock();

        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setRoles(Map.of(RoleScope.API, "DEFAULT_GROUP_API_ROLE", RoleScope.APPLICATION, "DEFAULT_GROUP_APPLICATION_ROLE"));
        groupEntity.setSystemInvitation(true);
        groupEntity.setLockApiRole(true);
        groupEntity.setLockApplicationRole(true);
        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP_ID)).thenReturn(groupEntity);

        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(ENVIRONMENT_GROUP),
                eq("DEFAULT"),
                eq(CREATE),
                eq(UPDATE),
                eq(DELETE)
            )
        )
            .thenReturn(false);

        MemberRoleEntity apiRole = new MemberRoleEntity();
        apiRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.API);
        apiRole.setRoleName("CUSTOM_API");
        MemberRoleEntity appRole = new MemberRoleEntity();
        appRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION);
        appRole.setRoleName("CUSTOM_APP");

        GroupMembership groupMembership = new GroupMembership();
        groupMembership.setId(USERNAME);
        groupMembership.setRoles(Arrays.asList(apiRole, appRole));

        final Response response = envTarget().request().post(Entity.json(Collections.singleton(groupMembership)));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.API);
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.APPLICATION);
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "DEFAULT_GROUP_API_ROLE")
            );

        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "DEFAULT_GROUP_APPLICATION_ROLE")
            );
    }

    @Test
    public void shouldAddMemberWithDefaultOrganizationRole() {
        initADDmock();

        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setSystemInvitation(true);
        groupEntity.setLockApiRole(true);
        groupEntity.setLockApplicationRole(true);
        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP_ID)).thenReturn(groupEntity);

        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(ENVIRONMENT_GROUP),
                eq("DEFAULT"),
                eq(CREATE),
                eq(UPDATE),
                eq(DELETE)
            )
        )
            .thenReturn(false);

        MemberRoleEntity apiRole = new MemberRoleEntity();
        apiRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.API);
        apiRole.setRoleName("CUSTOM_API");
        MemberRoleEntity appRole = new MemberRoleEntity();
        appRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION);
        appRole.setRoleName("CUSTOM_APP");

        GroupMembership groupMembership = new GroupMembership();
        groupMembership.setId(USERNAME);
        groupMembership.setRoles(Arrays.asList(apiRole, appRole));

        final Response response = envTarget().request().post(Entity.json(Collections.singleton(groupMembership)));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(roleService).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.API);
        verify(roleService).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.APPLICATION);
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, DEFAULT_API_ROLE)
            );

        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, DEFAULT_APPLICATION_ROLE)
            );
    }

    //UPDATE
    private void initUPDATEmock() {
        reset(roleService, groupService, membershipService);
        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP_ID)).thenReturn(mock(GroupEntity.class));

        RoleEntity customApiRole = new RoleEntity();
        customApiRole.setId("API_CUSTOM_API");
        customApiRole.setName("CUSTOM_API");
        customApiRole.setScope(RoleScope.API);
        when(roleService.findByScopeAndName(RoleScope.API, "CUSTOM_API", GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(customApiRole));

        RoleEntity customApplicationRole = new RoleEntity();
        customApplicationRole.setId("APP_CUSTOM_APP");
        customApplicationRole.setName("CUSTOM_APP");
        customApplicationRole.setScope(RoleScope.APPLICATION);
        when(roleService.findByScopeAndName(RoleScope.APPLICATION, "CUSTOM_APP", GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(customApplicationRole));

        RoleEntity customIntegrationRole = new RoleEntity();
        customIntegrationRole.setId("APP_CUSTOM_INTEGRATION");
        customIntegrationRole.setName("CUSTOM_APP");
        customIntegrationRole.setScope(RoleScope.INTEGRATION);
        when(roleService.findByScopeAndName(RoleScope.INTEGRATION, "CUSTOM_APP", GraviteeContext.getCurrentOrganization()))
            .thenReturn(Optional.of(customIntegrationRole));
    }

    @Test
    public void shouldUpdateNothing() {
        initUPDATEmock();

        GroupMembership groupMembership = new GroupMembership();
        groupMembership.setId(USERNAME);

        final Response response = envTarget().request().post(Entity.json(Collections.singleton(groupMembership)));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.API);
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.APPLICATION);
        verify(membershipService, never()).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
    }

    @Test
    public void shouldUpdateApiRole() {
        initUPDATEmock();

        MemberRoleEntity apiRole = new MemberRoleEntity();
        apiRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.API);
        apiRole.setRoleName("CUSTOM_API");

        GroupMembership groupMembership = new GroupMembership();
        groupMembership.setId(USERNAME);
        groupMembership.setRoles(Collections.singletonList(apiRole));

        final Response response = envTarget().request().post(Entity.json(Collections.singleton(groupMembership)));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.API);
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.APPLICATION);
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.INTEGRATION);
        verify(membershipService, times(1)).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "CUSTOM_API")
            );
    }

    @Test
    public void shouldUpdateApplicationRole() {
        initUPDATEmock();

        MemberRoleEntity appRole = new MemberRoleEntity();
        appRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION);
        appRole.setRoleName("CUSTOM_APP");

        GroupMembership groupMembership = new GroupMembership();
        groupMembership.setId(USERNAME);
        groupMembership.setRoles(Collections.singletonList(appRole));

        final Response response = envTarget().request().post(Entity.json(Collections.singleton(groupMembership)));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.API);
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.APPLICATION);
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.INTEGRATION);
        verify(membershipService, times(1)).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "CUSTOM_APP")
            );
    }

    @Test
    public void shouldUpdateIntegrationRole() {
        initUPDATEmock();

        MemberRoleEntity integrationRole = new MemberRoleEntity();
        integrationRole.setRoleScope(RoleScope.INTEGRATION);
        integrationRole.setRoleName("CUSTOM_APP");

        GroupMembership groupMembership = new GroupMembership();
        groupMembership.setId(USERNAME);
        groupMembership.setRoles(Collections.singletonList(integrationRole));

        final Response response = envTarget().request().post(Entity.json(Collections.singleton(groupMembership)));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.API);
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.APPLICATION);
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.INTEGRATION);
        verify(membershipService, times(1)).addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.INTEGRATION, "CUSTOM_APP")
            );
    }

    @Test
    public void shouldUpdateApiAndApplicationAndIntegrationRole() {
        initUPDATEmock();
        MemberRoleEntity apiRole = new MemberRoleEntity();
        apiRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.API);
        apiRole.setRoleName("CUSTOM_API");
        MemberRoleEntity appRole = new MemberRoleEntity();
        appRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION);
        appRole.setRoleName("CUSTOM_APP");
        MemberRoleEntity integrationRole = new MemberRoleEntity();
        integrationRole.setRoleScope(io.gravitee.rest.api.model.permissions.RoleScope.INTEGRATION);
        integrationRole.setRoleName("CUSTOM_APP");

        GroupMembership groupMembership = new GroupMembership();
        groupMembership.setId(USERNAME);
        groupMembership.setRoles(Arrays.asList(apiRole, appRole, integrationRole));

        final Response response = envTarget().request().post(Entity.json(Collections.singleton(groupMembership)));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.API);
        verify(roleService, never()).findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), RoleScope.APPLICATION);
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, "CUSTOM_API")
            );
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, "CUSTOM_APP")
            );

        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.GROUP, GROUP_ID),
                new MembershipService.MembershipMember(USERNAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.INTEGRATION, "CUSTOM_APP")
            );
    }
}
