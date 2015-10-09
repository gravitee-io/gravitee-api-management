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

import io.gravitee.definition.jackson.model.*;
import io.gravitee.management.rest.annotation.Role;
import io.gravitee.management.rest.annotation.RoleType;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.TeamService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.service.exceptions.UserNotFoundException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Path("/user")
@Role(RoleType.OWNER)
public class AuthenticatedUserResource extends AbstractResource {

    @Inject
    private ApiService apiService;

    @Inject
    private TeamService teamService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    /**
     * Get the authenticated user.
     * @return The authenticated user.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserEntity user() throws UserNotFoundException {
        Optional<UserEntity> user = userService.findByName(getAuthenticatedUser());
        if (! user.isPresent()) {
            throw new UserNotFoundException();
        }

        return user.get();
    }

    /**
     * List teams that are accessible to the authenticated user.
     * @return
     */
    @GET
    @Path("teams")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<TeamEntity> teams() {
        return teamService.findByUser(getAuthenticatedUser(), false);
    }

    /**
     * List applications that are accessible to the authenticated user.
     * @return
     */
    @GET
    @Path("applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApplicationEntity> applications() {
        return applicationService.findByUser(getAuthenticatedUser());
    }

    /**
     * List APIs that are accessible to the authenticated user.
     * <p>
     * This includes APIs owned by the authenticated user and APIs that the authenticated user has access to through
     * a team membership.
     * </p>
     *
     * @return APIs that are accessible to the authenticated user.
     */
    @GET
    @Path("apis")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApiEntity> apis() {
        return apiService.findByUser(getAuthenticatedUser(), false);
    }

    /**
     * Create a new API for the authenticated user.
     * @param newApiEntity
     * @return
     */
    @POST
    @Path("apis")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApi(@Valid final NewApiEntity newApiEntity) throws ApiAlreadyExistsException {
        ApiEntity newApi = apiService.createForUser(newApiEntity, getAuthenticatedUser());
        if (newApi != null) {
            return Response
                    .created(URI.create("/apis/" + newApi.getName()))
                    .entity(newApi)
                    .build();
        }

        return Response.serverError().build();
    }

    /**
     * Create a new application for the authenticated user.
     * @param application
     * @return
     */
    @POST
    @Path("applications")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApplication(@Valid final NewApplicationEntity application) {
        ApplicationEntity newApplication = applicationService.createForUser(application, getAuthenticatedUser());
        if (newApplication != null) {
            return Response
                    .created(URI.create("/applications/" + newApplication.getName()))
                    .entity(newApplication)
                    .build();
        }

        return Response.serverError().build();
    }
}
