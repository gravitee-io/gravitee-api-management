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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cluster.use_case.GetClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.UpdateClusterUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ClusterMapper;
import io.gravitee.rest.api.management.v2.rest.model.UpdateCluster;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class ClusterResource extends AbstractResource {

    @PathParam("clusterId")
    String clusterId;

    @Inject
    private GetClusterUseCase getClusterUseCase;

    @Inject
    private UpdateClusterUseCase updateClusterUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // TODO add permissions
    //      @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = { RolePermissionAction.READ }) })
    public Response getCluster() {
        var executionContext = GraviteeContext.getExecutionContext();

        var output = getClusterUseCase.execute(new GetClusterUseCase.Input(clusterId, executionContext.getEnvironmentId()));

        return Response.ok(this.getLocationHeader(output.cluster().getId())).entity(ClusterMapper.INSTANCE.map(output.cluster())).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // TODO add permissions
    //      @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = { RolePermissionAction.UPDATE }) })
    public Response updateCluster(@Valid @NotNull final UpdateCluster updateCluster) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        AuditInfo audit = AuditInfo
            .builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor
                    .builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();

        var output = updateClusterUseCase.execute(
            new UpdateClusterUseCase.Input(clusterId, ClusterMapper.INSTANCE.map(updateCluster), audit)
        );

        return Response.ok(this.getLocationHeader(output.cluster().getId())).entity(ClusterMapper.INSTANCE.map(output.cluster())).build();
    }
}
