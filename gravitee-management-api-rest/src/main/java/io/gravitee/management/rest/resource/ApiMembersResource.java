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

import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.rest.resource.param.MembershipTypeParam;
import io.gravitee.management.service.ApiService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiMembersResource {

    @Inject
    private ApiService apiService;

    @PathParam("apiName")
    private String apiName;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<MemberEntity> members() {
        // Check that the API exists
        apiService.findByName(apiName);

        return apiService.getMembers(apiName, null);
    }

    @POST
    public Response create(
            @NotNull @QueryParam("user") String username,
            @NotNull @QueryParam("type") MembershipTypeParam membershipType) {
        if (membershipType.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        // Check that the API exists
        apiService.findByName(apiName);

        apiService.addMember(apiName, username, membershipType.getValue());

        return Response.created(URI.create("/apis/" + apiName + "/members/" + username)).build();
    }

    @DELETE
    public Response delete(@NotNull @QueryParam("user") String username) {
        // Check that the API exists
        apiService.findByName(apiName);

        apiService.deleteMember(apiName, username);

        return Response.ok().build();
    }
}
