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
import io.gravitee.management.model.InstanceListItem;
import io.gravitee.management.service.InstanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/instances")
@Api(tags = {"Gateway"})
public class InstancesResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private InstanceService instanceService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List gateway instances")
    public Collection<InstanceListItem> listInstances(@QueryParam("includeStopped") boolean includeStopped) {
        return instanceService.findInstances(includeStopped)
                .stream()
                .collect(Collectors.toList());
    }

    @Path("{instance}")
    public InstanceResource getInstanceResource() {
        return resourceContext.getResource(InstanceResource.class);
    }
}
