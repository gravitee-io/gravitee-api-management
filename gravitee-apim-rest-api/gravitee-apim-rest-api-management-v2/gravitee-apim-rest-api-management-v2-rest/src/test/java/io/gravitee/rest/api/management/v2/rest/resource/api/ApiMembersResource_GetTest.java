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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public class ApiMembersResource_GetTest extends AbstractResourceTest {

    private static final String apiId = "my-api";
    private static final String ENVIRONMENT = "my-env";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + apiId + "/members";
    }

    @Before
    public void init() {
        reset(membershipService);
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);
    }

    @Test
    public void should_return_403_when_user_not_permitted_to_list_members() {
        when(permissionService.hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_MEMBER), eq(apiId), any()))
            .thenReturn(false);

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_list_one_api_member() {
        var role = new RoleEntity();
        role.setName("OWNER");
        var member = new MemberEntity();
        member.setId("memberId");
        member.setDisplayName("John Doe");
        member.setRoles(List.of(role));
        member.setType(MembershipMemberType.USER);

        Set<MemberEntity> membershipList = Set.of(member);
        when(membershipService.getMembersByReference(eq(GraviteeContext.getExecutionContext()), eq(MembershipReferenceType.API), eq(apiId)))
            .thenReturn(membershipList);

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var apiResponse = response.readEntity(MembersResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertEquals("memberId", apiResponse.getData().get(0).getId());
        assertEquals("John Doe", apiResponse.getData().get(0).getDisplayName());
        assertEquals(1, apiResponse.getData().get(0).getRoles().size());
        assertEquals("OWNER", apiResponse.getData().get(0).getRoles().get(0).getName());
        assertEquals(1, apiResponse.getPagination().getPage().intValue());
        assertEquals(1, apiResponse.getPagination().getPageCount().intValue());
        assertEquals(10, apiResponse.getPagination().getPerPage().intValue());
        assertEquals(1, apiResponse.getPagination().getTotalCount().intValue());
        assertEquals(1, apiResponse.getPagination().getPageItemsCount().intValue());
    }

    @Test
    public void should_list_multiple_members() {
        var owner = new RoleEntity();
        owner.setName("OWNER");

        var user = new RoleEntity();
        user.setName("USER");

        var member1 = new MemberEntity();
        member1.setId("member1");
        member1.setDisplayName("John Doe");
        member1.setRoles(List.of(owner));
        member1.setType(MembershipMemberType.USER);

        var member2 = new MemberEntity();
        member2.setId("member2");
        member2.setDisplayName("Jane Doe");
        member2.setRoles(List.of(user));
        member2.setType(MembershipMemberType.USER);

        var member3 = new MemberEntity();
        member3.setId("member3");
        member3.setDisplayName("Richard Roe");
        member3.setRoles(List.of(user));
        member3.setType(MembershipMemberType.USER);

        var member4 = new MemberEntity();
        member4.setId("member4");
        member4.setDisplayName("Baby Doe");
        member4.setRoles(List.of(user));
        member4.setType(MembershipMemberType.USER);

        Set<MemberEntity> membershipList = Set.of(member1, member2, member3, member4);
        when(membershipService.getMembersByReference(eq(GraviteeContext.getExecutionContext()), eq(MembershipReferenceType.API), eq(apiId)))
            .thenReturn(membershipList);

        final Response response = rootTarget()
            .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 1)
            .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, 2)
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var apiResponse = response.readEntity(MembersResponse.class);

        assertEquals(2, apiResponse.getData().size());
        assertEquals(1, apiResponse.getPagination().getPage().intValue());
        assertEquals(2, apiResponse.getPagination().getPageCount().intValue());
        assertEquals(2, apiResponse.getPagination().getPerPage().intValue());
        assertEquals(4, apiResponse.getPagination().getTotalCount().intValue());
        assertEquals(2, apiResponse.getPagination().getPageItemsCount().intValue());

        assertEquals(
            List.of("John Doe", "Jane Doe"),
            apiResponse.getData().stream().map(Member::getDisplayName).collect(Collectors.toList())
        );
    }
}
