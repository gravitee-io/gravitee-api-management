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
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.EnvironmentMapper;
import io.gravitee.rest.api.management.v2.rest.model.Environment;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.Collection;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/environments")
public class EnvironmentsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EnvironmentService environmentService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Environment> getEnvironments() {
        String organizationId = GraviteeContext.getCurrentOrganization();
        return EnvironmentMapper.INSTANCE.convertCollection(this.environmentService.findByOrganization(organizationId));
    }

    @Path("{envId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Environment getEnvironment(@PathParam("envId") String envId) {
        // TODO: Determine if hrid or envId is acceptable for envId
        return EnvironmentMapper.INSTANCE.convert(environmentService.findByOrgAndIdOrHrid(GraviteeContext.getCurrentOrganization(), envId));
    }
}
