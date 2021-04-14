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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.reset;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.model.ApiMembership;
import io.gravitee.rest.api.management.rest.model.RoleMembership;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RoleUsersResourceTest extends AbstractResourceTest {

    private static final String SCOPE = "ORGANIZATION";
    private static final String ROLE = "my-role";

    @Override
    protected String contextPath() {
        return "configuration/rolescopes/";
    }

    @Test
    public void shouldAddRoleToUser() {
        reset(membershipService);

        RoleMembership roleMembership = new RoleMembership();
        roleMembership.setId(USER_NAME);

        final Response response = target(SCOPE).path("roles").path(ROLE).path("users").request().post(Entity.json(roleMembership));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertNull(response.getHeaders().getFirst(HttpHeaders.LOCATION));

        ArgumentCaptor<MembershipService.MembershipReference> memberShipRefCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipReference.class
        );
        ArgumentCaptor<MembershipService.MembershipRole> memberShipRoleCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipRole.class
        );
        ArgumentCaptor<MembershipService.MembershipMember> memberShipUserCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipMember.class
        );

        Mockito
            .verify(membershipService)
            .addRoleToMemberOnReference(memberShipRefCaptor.capture(), memberShipUserCaptor.capture(), memberShipRoleCaptor.capture());
        assertEquals(GraviteeContext.getCurrentOrganization(), memberShipRefCaptor.getValue().getId());
        assertEquals(MembershipReferenceType.ORGANIZATION, memberShipRefCaptor.getValue().getType());
        assertEquals(ROLE, memberShipRoleCaptor.getValue().getName());
        assertEquals(RoleScope.ORGANIZATION, memberShipRoleCaptor.getValue().getScope());
        assertEquals(USER_NAME, memberShipUserCaptor.getValue().getMemberId());
    }
}
