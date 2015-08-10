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
package io.gravitee.management.api.resource;

import io.gravitee.management.api.exceptions.UserNotFoundException;
import io.gravitee.management.api.model.*;
import io.gravitee.management.api.service.ApiService;
import io.gravitee.management.api.service.ApplicationService;
import io.gravitee.management.api.service.TeamService;
import io.gravitee.management.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
@Component
@Produces(MediaType.APPLICATION_JSON)
@Path("/user")
public class AuthenticatedUserResource extends AbstractResource {

    @Autowired
    private ApiService apiService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserService userService;

    /**
     * Get the authenticated user.
     * @return The authenticated user.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserEntity authenticatedUser() throws UserNotFoundException {
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
    public Set<TeamEntity> listTeams() {
        return teamService.findByUser(getAuthenticatedUser());
    }

    /**
     * List applications that are accessible to the authenticated user.
     * @return
     */
    @GET
    @Path("applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApplicationEntity> listApplications() {
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
    public Set<ApiEntity> listApis() {
        return apiService.findByUser(getAuthenticatedUser(), false);
    }

    /**
     * Create a new API for the authenticated user.
     * @param api
     * @return
     */
    @POST
    @Path("apis")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApi(NewApiEntity api) {
        ApiEntity newApi = apiService.createForUser(api, getAuthenticatedUser());
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
    public Response createApplication(NewApplicationEntity application) {
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
