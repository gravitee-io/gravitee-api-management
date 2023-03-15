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
package io.gravitee.rest.api.management.v4.rest.resource.installation;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v4.rest.mapper.EnvironmentMapper;
import io.gravitee.rest.api.management.v4.rest.model.Environment;
import io.gravitee.rest.api.management.v4.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v4.rest.resource.api.ApisResource;
import io.gravitee.rest.api.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Parameter;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

public class EnvironmentResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EnvironmentService environmentService;

    @PathParam("envId")
    @Parameter(name = "envId", required = true, description = "The environment ID")
    private String envId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Environment getEnvironment() {
        return EnvironmentMapper.INSTANCE.convert(environmentService.findById(envId));
    }

    @Path("/apis")
    public ApisResource getApisResource() {
        return resourceContext.getResource(ApisResource.class);
    }
}
