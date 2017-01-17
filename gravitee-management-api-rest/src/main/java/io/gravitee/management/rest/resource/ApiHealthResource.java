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
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.rest.resource.param.HealthParam;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.HealthCheckService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiPermissionsRequired(ApiPermission.ANALYTICS)
@Api(tags = {"API"})
public class ApiHealthResource extends AbstractResource {

    @Inject
    private HealthCheckService healthCheckService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Health statistics for API"
    )
    public Response health(
            @PathParam("api") String api,
            @BeanParam HealthParam healthParam) {
        healthParam.validate();

        return Response.ok(healthCheckService.health(
                api,
                healthParam.getFrom(),
                healthParam.getTo(),
                healthParam.getInterval())).build();
    }
}
