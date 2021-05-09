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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.swagger.annotations.Api;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Flows" })
public class FlowsResource extends AbstractResource {

    @Inject
    private FlowService flowService;

    @GET
    @Path("configuration-schema")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfigurationSchemaForm() {
        return Response.ok(flowService.getConfigurationSchemaForm()).build();
    }

    @GET
    @Path("flow-schema")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPlatformFlowSchemaForm() {
        return Response.ok(flowService.getPlatformFlowSchemaForm()).build();
    }
}
