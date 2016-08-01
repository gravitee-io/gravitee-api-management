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
import io.gravitee.management.model.permissions.ApplicationPermission;
import io.gravitee.management.rest.resource.param.MembershipTypeParam;
import io.gravitee.management.rest.security.ApplicationPermissionsRequired;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.UserService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApplicationMembersResource {

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    @PathParam("application")
    private String application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.READ)
    public List<MemberEntity> members() {
        return applicationService.getMembers(application, null).stream()
                .sorted((o1, o2) -> o1.getUsername().compareTo(o2.getUsername()))
                .collect(Collectors.toList());
    }

    @POST
    @ApplicationPermissionsRequired(ApplicationPermission.MANAGE_MEMBERS)
    public Response create(
            @NotNull @QueryParam("user") String username,
            @NotNull @QueryParam("type") MembershipTypeParam membershipType) {
        if (membershipType.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        applicationService.addOrUpdateMember(application, username, membershipType.getValue());
        return Response.created(URI.create("/applications/" + application + "/members/" + username)).build();
    }

    @DELETE
    @ApplicationPermissionsRequired(ApplicationPermission.MANAGE_MEMBERS)
    public Response delete(@NotNull @QueryParam("user") String username) {
        userService.findByName(username);
        applicationService.deleteMember(application, username);
        return Response.ok().build();
    }
}
