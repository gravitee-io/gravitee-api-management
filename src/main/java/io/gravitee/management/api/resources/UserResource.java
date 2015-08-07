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

import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.Application;
import io.gravitee.repository.model.Team;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class UserResource {

    @Context
    private ResourceContext resourceContext;

    private String username;

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * List public teams for the specified user.
     * @return Public teams for the specified user.
     */
    @GET
    @Path("teams")
    public Set<Team> getTeams() {
        return null;
    }

    /**
     * List public APIs for the specified user.
     * @return Public APIs for the specified user.
     */
    @GET
    @Path("apis")
    public Set<Api> getApis() {
        return null;
    }

    /**
     * List applications for the specified user.
     * @return Applications for the specified user.
     */
    @GET
    @Path("applications")
    public Set<Application> getApplications() {
        return null;
    }
}
