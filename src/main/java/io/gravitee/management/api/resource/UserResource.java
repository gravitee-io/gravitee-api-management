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
import io.gravitee.management.api.model.ApiEntity;
import io.gravitee.management.api.model.ApplicationEntity;
import io.gravitee.management.api.model.TeamEntity;
import io.gravitee.management.api.model.UserEntity;
import io.gravitee.management.api.service.ApiService;
import io.gravitee.management.api.service.ApplicationService;
import io.gravitee.management.api.service.TeamService;
import io.gravitee.management.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class UserResource {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ApplicationService applicationService;

    private String username;

    public void setUsername(String username) {
        this.username = username;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserEntity getUserInfo() throws UserNotFoundException {
        return getCurrentUser();
    }

    /**
     * List public teams for the specified user.
     * @return Public teams for the specified user.
     */
    @GET
    @Path("teams")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<TeamEntity> getUserPublicTeams() throws UserNotFoundException {
        UserEntity user = getCurrentUser();
        return teamService.findByUser(user.getUsername());
    }

    /**
     * List public APIs for the specified user.
     * @return Public APIs for the specified user.
     */
    @GET
    @Path("apis")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApiEntity> getUserPublicApis() throws UserNotFoundException {
        UserEntity user = getCurrentUser();
        return apiService.findByUser(user.getUsername(), true);
    }

    /**
     * List applications for the specified user.
     * @return Applications for the specified user.
     */
    @GET
    @Path("applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApplicationEntity> getUserApplications() throws UserNotFoundException {
        UserEntity user = getCurrentUser();
        return applicationService.findByUser(user.getUsername());
    }

    private UserEntity getCurrentUser() throws UserNotFoundException {
        Optional<UserEntity> user = userService.findByName(username);
        if (! user.isPresent()) {
            throw new UserNotFoundException();
        }

        return user.get();
    }
}
