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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.MembershipType;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.resource.param.MembershipTypeParam;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.ApiService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david at graviteesource.com)
 */
@ApiPermissionsRequired(ApiPermission.MANAGE_MEMBERS)
public class ApiMembersResource {

    @Inject
    private ApiService apiService;

    @PathParam("api")
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberEntity> members() {
        return apiService.getMembers(api, null).stream()
                .sorted((o1, o2) -> o1.getUsername().compareTo(o2.getUsername()))
                .collect(Collectors.toList());
    }

    @POST
    public Response save(
            @NotNull @QueryParam("user") String username,
            @NotNull @QueryParam("type") MembershipTypeParam membershipType) {
        if (membershipType.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Membership type must be set").build();
        }

        if (MembershipType.PRIMARY_OWNER.equals(membershipType.getValue())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Illegal membership type 'PRIMARY_OWNER'").build();
        }

        apiService.addOrUpdateMember(api, username, membershipType.getValue());
        return Response.created(URI.create("/apis/" + api + "/members/" + username)).build();
    }

    @POST
    @Path("transfer_ownership")
    @ApiPermissionsRequired(ApiPermission.TRANSFER_OWNERSHIP)
    public Response transferOwnership(@NotNull @QueryParam("user") String username) {
        // change previous primary owner privilege
        // TODO : API must have a single PRIMARY_OWNER, refactor getMembers() code part
        apiService.getMembers(api, MembershipType.PRIMARY_OWNER)
                .forEach(m -> apiService.addOrUpdateMember(api, m.getUsername(), MembershipType.OWNER));
        // set the new primary owner
        apiService.addOrUpdateMember(api, username, MembershipType.PRIMARY_OWNER);
        return Response.ok().build();
    }

    @DELETE
    public Response delete(@NotNull @QueryParam("user") String username) {
        apiService.deleteMember(api, username);
        return Response.ok().build();
    }
}
