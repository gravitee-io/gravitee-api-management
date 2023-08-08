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
package io.gravitee.rest.api.management.v2.rest.resource.group;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.model.GroupsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.GroupEventRuleEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class GroupsResource_ListTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";
    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/groups";
    }

    @Before
    public void init() throws TechnicalException {
        Mockito.reset(groupService);
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);
        doReturn(environmentEntity).when(environmentService).findById(ENVIRONMENT);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Test
    public void should_control_permissions() {
        when(permissionService.hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.ENVIRONMENT_GROUP), eq(ENVIRONMENT), eq(RolePermissionAction.READ))).thenReturn(false);

        final Response response = rootTarget().queryParam("perPage", 10).queryParam("page", 1).request().get();
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_return_empty_list_if_no_results() {
        when(groupService.findAll(GraviteeContext.getExecutionContext())).thenReturn(new ArrayList<>());

        final Response response = rootTarget().queryParam("perPage", 10).queryParam("page", 1).request().get();
        assertEquals(OK_200, response.getStatus());

        GroupsResponse body = response.readEntity(GroupsResponse.class);
        assertNotNull(body);
        assertNotNull(body.getData());
        assertEquals(0, body.getData().size());

        assertNull(body.getPagination().getPerPage());
        assertNull(body.getPagination().getPage());
        assertNull(body.getPagination().getTotalCount());
    }

    @Test
    public void should_return_groups() {
        Date baseDate = new Date();

        GroupEntity group1 = new GroupEntity();
        group1.setId("group-1");
        group1.setName("group-1");

        GroupEventRuleEntity rule = new GroupEventRuleEntity();
        rule.setEvent("API_CREATE");
        group1.setEventRules(List.of(rule));

        Map<RoleScope, String> roles = new HashMap<>();
        roles.put(RoleScope.API, "READ_ONLY");
        roles.put(RoleScope.APPLICATION, "USER");
        group1.setRoles(roles);

        group1.setApiPrimaryOwner("api-owner-id");
        group1.setCreatedAt(baseDate);
        group1.setUpdatedAt(baseDate);
        group1.setDisableMembershipNotifications(true);
        group1.setEmailInvitation(true);
        group1.setPrimaryOwner(true);
        group1.setSystemInvitation(true);
        group1.setLockApiRole(true);
        group1.setLockApplicationRole(true);
        group1.setManageable(true);
        group1.setMaxInvitation(100);

        GroupEntity group2 = new GroupEntity();
        group2.setId("group-2");
        group2.setName("group-2");

        group2.setEventRules(List.of());
        group2.setRoles(new HashMap<>());
        group2.setApiPrimaryOwner(null);
        group2.setCreatedAt(baseDate);
        group2.setUpdatedAt(baseDate);
        group2.setDisableMembershipNotifications(false);
        group2.setEmailInvitation(false);
        group2.setPrimaryOwner(false);
        group2.setSystemInvitation(false);
        group2.setLockApiRole(false);
        group2.setLockApplicationRole(false);
        group2.setManageable(false);
        group2.setMaxInvitation(null);

        when(groupService.findAll(GraviteeContext.getExecutionContext())).thenReturn(List.of(group1, group2));

        final Response response = rootTarget().queryParam("perPage", 10).queryParam("page", 1).request().get();
        assertEquals(OK_200, response.getStatus());

        GroupsResponse body = response.readEntity(GroupsResponse.class);
        assertNotNull(body);
        assertNotNull(body.getData());
        assertEquals(2, body.getData().size());

        var firstGroup = body.getData().get(0);
        assertEquals("group-1", firstGroup.getId());
        assertEquals("group-1", firstGroup.getName());
        assertEquals("API_CREATE", firstGroup.getEventRules().get(0).getValue());
        assertEquals("READ_ONLY", firstGroup.getApiRole());
        assertEquals("USER", firstGroup.getApplicationRole());
        assertTrue(firstGroup.getPrimaryOwner());
        assertTrue(firstGroup.getDisableMembershipNotifications());
        assertTrue(firstGroup.getManageable());
        assertTrue(firstGroup.getEmailInvitation());
        assertTrue(firstGroup.getSystemInvitation());
        assertTrue(firstGroup.getLockApiRole());
        assertTrue(firstGroup.getLockApplicationRole());
        assertEquals(100, firstGroup.getMaxInvitation().intValue());

        var secondGroup = body.getData().get(1);
        assertEquals("group-2", secondGroup.getId());
        assertEquals("group-2", secondGroup.getName());
        assertEquals(List.of(), secondGroup.getEventRules());
        assertNull(secondGroup.getApiRole());
        assertNull(secondGroup.getApplicationRole());
        assertFalse(secondGroup.getPrimaryOwner());
        assertFalse(secondGroup.getDisableMembershipNotifications());
        assertFalse(secondGroup.getManageable());
        assertFalse(secondGroup.getEmailInvitation());
        assertFalse(secondGroup.getSystemInvitation());
        assertFalse(secondGroup.getLockApiRole());
        assertFalse(secondGroup.getLockApplicationRole());
        assertNull(secondGroup.getMaxInvitation());
    }
}
