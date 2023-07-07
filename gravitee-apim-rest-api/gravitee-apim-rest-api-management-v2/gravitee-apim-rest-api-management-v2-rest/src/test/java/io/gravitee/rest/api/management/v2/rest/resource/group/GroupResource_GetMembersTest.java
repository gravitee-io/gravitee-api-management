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

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class GroupResource_GetMembersTest extends AbstractResourceTest {

    private static final String GROUP = "my-group";
    private static final String ENVIRONMENT = "my-env";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/groups/" + GROUP + "/members";
    }

    @Before
    public void init() throws TechnicalException {
        Mockito.reset(membershipService, groupService);
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
    public void should_get_error_if_user_does_not_have_correct_permissions() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.GROUP_MEMBER,
                GROUP,
                RolePermissionAction.READ
            )
        )
            .thenReturn(false);

        final Response response = rootTarget().request().get();

        assertEquals(FORBIDDEN_403, response.getStatus());
        verify(membershipService, never()).getMembersByReference(any(), any(), any(), anyString());
    }

    @Test
    public void should_get_error_when_group_not_found() {
        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP)).thenThrow(new GroupNotFoundException(GROUP));

        final Response response = rootTarget().request().get();

        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void should_get_error_when_technical_management_exception_thrown() {
        when(membershipService.getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.GROUP, GROUP))
            .thenThrow(new TechnicalManagementException("Oops"));

        final Response response = rootTarget().request().get();

        assertEquals(INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void should_return_empty_page_when_no_results() {
        var groupEntity = new GroupEntity();
        groupEntity.setId("group-id-1");
        groupEntity.setName("group-name-1");

        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP)).thenReturn(groupEntity);

        when(membershipService.getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.GROUP, GROUP))
            .thenReturn(Set.of());

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());
        var body = response.readEntity(MembersResponse.class);
        assertNotNull(body.getData());
        assertTrue(body.getData().isEmpty());

        assertNotNull(body.getPagination());
        Pagination pagination = body.getPagination();
        assertNull(pagination.getPage());
        assertNull(pagination.getPageCount());
        assertNull(pagination.getPerPage());
        assertNull(pagination.getPageItemsCount());
        assertNull(pagination.getTotalCount());

        assertNotNull(body.getMetadata());
        ObjectMapper objectMapper = new ObjectMapper();
        LinkedHashMap metadata = objectMapper.convertValue(body.getMetadata(), LinkedHashMap.class);
        assertEquals("group-name-1", metadata.get("groupName"));
    }

    @Test
    public void should_return_results() {
        var groupEntity = new GroupEntity();
        groupEntity.setId("group-id-1");
        groupEntity.setName("group-name-1");

        when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP)).thenReturn(groupEntity);

        when(membershipService.getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.GROUP, GROUP))
            .thenReturn(Set.of(fakeMember("member-1"), fakeMember("member-2")));

        final Response response = rootTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        var body = response.readEntity(MembersResponse.class);
        assertNotNull(body.getData());
        assertEquals(2, body.getData().size());

        assertNotNull(body.getPagination());
        Pagination pagination = body.getPagination();
        assertEquals(1, pagination.getPage().intValue());
        assertEquals(1, pagination.getPageCount().intValue());
        assertEquals(10, pagination.getPerPage().intValue());
        assertEquals(2, pagination.getPageItemsCount().intValue());
        assertEquals(2, pagination.getTotalCount().intValue());

        assertNotNull(body.getMetadata());
        ObjectMapper objectMapper = new ObjectMapper();
        LinkedHashMap metadata = objectMapper.convertValue(body.getMetadata(), LinkedHashMap.class);
        assertEquals("group-name-1", metadata.get("groupName"));
    }

    private MemberEntity fakeMember(String id) {
        var role = new RoleEntity();
        role.setName("OWNER");
        role.setScope(RoleScope.GROUP);

        var member = new MemberEntity();
        member.setId(id);
        member.setDisplayName("John Doe");
        member.setRoles(List.of(role));
        member.setType(MembershipMemberType.USER);
        member.setEmail("john@doe.com");
        member.setReferenceType(MembershipReferenceType.GROUP);

        return member;
    }
}
