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

import io.gravitee.management.api.model.ApplicationEntity;
import io.gravitee.management.api.model.NewApplicationEntity;
import io.gravitee.management.api.service.ApplicationService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class TeamApplicationsResource {

    @PathParam("teamName")
    private String teamName;

    @Inject
    private ApplicationService applicationService;

    /**
     * List applications for the specified team.
     * @return Applications for the specified team.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApplicationEntity> getApplications() {
        return applicationService.findByTeam(teamName);
    }

    /**
     * Create a new application for the specified team.
     * @param newApplicationEntity
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApplication(NewApplicationEntity newApplicationEntity) {
        ApplicationEntity applicationEntity = applicationService.createForTeam(newApplicationEntity, teamName);

        return Response
                .created(URI.create("/applications/" + applicationEntity.getName()))
                .entity(applicationEntity)
                .build();
    }
}
