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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.model.ApiMembership;
import io.gravitee.rest.api.service.MembershipService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiMembersResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String MEMBER_1 = "my-member";

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() {
        Mockito.reset(membershipService);
    }

    @Test
    public void shouldCreateMember() {
        ApiMembership apiMembership = new ApiMembership();
        apiMembership.setId(MEMBER_1);
        apiMembership.setRole("my-api-membership-role");

        final Response response = target(API).path("members").request().post(Entity.json(apiMembership));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertNull(response.getHeaders().getFirst(HttpHeaders.LOCATION));

        ArgumentCaptor<MembershipService.MembershipReference> memberShipRefCaptor = ArgumentCaptor.forClass(MembershipService.MembershipReference.class);
        ArgumentCaptor<MembershipService.MembershipRole> memberShipRoleCaptor = ArgumentCaptor.forClass(MembershipService.MembershipRole.class);
        ArgumentCaptor<MembershipService.MembershipMember> memberShipUserCaptor = ArgumentCaptor.forClass(MembershipService.MembershipMember.class);

        Mockito.verify(membershipService).addRoleToMemberOnReference(memberShipRefCaptor.capture(), memberShipUserCaptor.capture(), memberShipRoleCaptor.capture());
        assertEquals(API, memberShipRefCaptor.getValue().getId());
        assertEquals("my-api-membership-role", memberShipRoleCaptor.getValue().getName());
        assertEquals(MEMBER_1, memberShipUserCaptor.getValue().getMemberId());
    }

}
