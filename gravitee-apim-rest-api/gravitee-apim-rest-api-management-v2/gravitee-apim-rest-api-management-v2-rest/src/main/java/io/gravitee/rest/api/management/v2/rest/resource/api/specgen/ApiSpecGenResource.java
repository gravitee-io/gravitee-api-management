/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.resource.api.specgen;

import io.gravitee.apim.infra.scoring.SpecGenService;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.api.specgen.response.SpecGenState;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;

public class ApiSpecGenResource extends AbstractResource {

    @Inject
    private SpecGenService specGenService;

    @PathParam("apiId")
    private String apiId;

    @GET
    @Path("/_state")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public void getState(@Suspended final AsyncResponse response) {
        specGenService
            .getState(apiId)
            .map(reply -> new SpecGenState(reply.getRequestState()))
            .subscribe(response::resume, response::resume);
    }

    @GET
    @Path("/_start")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.CREATE }) })
    public void postJob(@Suspended final AsyncResponse response) {
        specGenService.postJob(apiId).map(reply -> new SpecGenState(reply.getRequestState())).subscribe(response::resume, response::resume);
    }
}
