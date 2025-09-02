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
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import io.gravitee.apim.core.gateway.use_case.GetInstanceDetailUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.InstancesMapper;
import io.gravitee.rest.api.management.v2.rest.model.InstanceDetailResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

public class InstancesResource extends AbstractResource {

    @Inject
    private GetInstanceDetailUseCase getInstanceDetailUseCase;

    @GET
    @Path("{instanceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_INSTANCE, acls = RolePermissionAction.READ) })
    public InstanceDetailResponse getInstanceById(@PathParam("instanceId") String instanceId) {
        return getInstanceDetailUseCase
            .execute(new GetInstanceDetailUseCase.Input(GraviteeContext.getExecutionContext(), instanceId))
            .instance()
            .map(InstancesMapper.INSTANCE::map)
            .orElseThrow(() -> new NotFoundException("Instance: " + instanceId + "not found"));
    }
}
