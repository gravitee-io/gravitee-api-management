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
import io.gravitee.management.service.AnalyticsService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@ApiPermissionsRequired(ApiPermission.ANALYTICS)
public class ApiHealthResource extends AbstractResource {

    @PathParam("api")
    private String api;

    @Inject
    private AnalyticsService analyticsService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response hits(@BeanParam HealthParam healthParam) {
        healthParam.validate();

        return Response.ok(analyticsService.health(
                api,
                healthParam.getFrom(),
                healthParam.getTo(),
                healthParam.getInterval())).build();
    }
}
