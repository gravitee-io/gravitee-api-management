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
package io.gravitee.management.api.resources;

import io.gravitee.management.api.model.*;
import io.gravitee.management.api.service.ApiService;
import io.gravitee.management.api.service.ApplicationService;
import io.gravitee.management.api.service.TeamService;
import io.gravitee.repository.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
@Produces(MediaType.APPLICATION_JSON)
@Path("/user")
public class AuthenticatedUserResource {

    @Autowired
    private ApiService apiService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private ApplicationService applicationService;

    // TODO: How to get username of the authenticated user ?
    private String username;

    /**
     * Get the authenticated user.
     * @return The authenticated user.
     */
    @GET
    public User authenticatedUser() {
        return null;
    }

    /**
     * List teams that are accessible to the authenticated user.
     * @return
     */
    @GET
    @Path("teams")
    public Set<TeamEntity> listTeams() {
        return teamService.findByUser(username);
    }

    /**
     * List applications that are accessible to the authenticated user.
     * @return
     */
    @GET
    @Path("applications")
    public Set<ApplicationEntity> listApplications() {
        return null;
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
    public Set<ApiEntity> listApis() {
        return apiService.findByUser(username, false);
    }

    /**
     * Create a new API for the authenticated user.
     * @param api
     * @return
     */
    @POST
    @Path("apis")
    public Response createApi(NewApiEntity api) {
        ApiEntity newApi = apiService.createForUser(api, username);
        if (newApi != null) {
            return Response
                    .created(URI.create("/users/" + username + "/apis/" + newApi.getName()))
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
    public Response createApplication(NewApplicationEntity application) {
        ApplicationEntity newApplication = applicationService.createForUser(application, username);
        if (newApplication != null) {
            return Response
                    .created(URI.create("/users/" + username + "/applications/" + newApplication.getName()))
                    .entity(newApplication)
                    .build();
        }

        return Response.serverError().build();
    }
}
