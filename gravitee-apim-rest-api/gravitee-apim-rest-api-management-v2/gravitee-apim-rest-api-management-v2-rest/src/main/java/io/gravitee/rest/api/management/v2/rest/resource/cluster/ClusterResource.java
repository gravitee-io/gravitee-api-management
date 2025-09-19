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
package io.gravitee.rest.api.management.v2.rest.resource.cluster;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cluster.use_case.DeleteClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.GetClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.UpdateClusterGroupsUseCase;
import io.gravitee.apim.core.cluster.use_case.UpdateClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.members.GetClusterPermissionsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ClusterMapper;
import io.gravitee.rest.api.management.v2.rest.model.UpdateCluster;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClusterResource extends AbstractResource {

    @PathParam("clusterId")
    String clusterId;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GetClusterUseCase getClusterUseCase;

    @Inject
    private UpdateClusterUseCase updateClusterUseCase;

    @Inject
    private DeleteClusterUseCase deleteClusterUseCase;

    @Inject
    private UpdateClusterGroupsUseCase updateClusterGroupsUseCase;

    @Inject
    private GetClusterPermissionsUseCase getClusterPermissionsUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.CLUSTER_DEFINITION, acls = { RolePermissionAction.READ }) })
    public Response getCluster() {
        var executionContext = GraviteeContext.getExecutionContext();

        var output = getClusterUseCase.execute(new GetClusterUseCase.Input(clusterId, executionContext.getEnvironmentId()));

        return Response.ok(this.getLocationHeader(output.cluster().getId())).entity(ClusterMapper.INSTANCE.map(output.cluster())).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.CLUSTER_DEFINITION, acls = { RolePermissionAction.UPDATE }) })
    public Response updateCluster(@Valid @NotNull final UpdateCluster updateCluster) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        AuditInfo audit = AuditInfo.builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor.builder()
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

    @DELETE
    @Permissions({ @Permission(value = RolePermission.CLUSTER_DEFINITION, acls = { RolePermissionAction.DELETE }) })
    public Response deleteCluster() {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        AuditInfo audit = AuditInfo.builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor.builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();

        deleteClusterUseCase.execute(new DeleteClusterUseCase.Input(clusterId, audit));

        return Response.noContent().build();
    }

    @PUT
    @Path("groups")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.CLUSTER_MEMBER, acls = { RolePermissionAction.UPDATE }) })
    public Response updateClusterGroups(@Valid @NotNull final List<@NotNull String> groups) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        AuditInfo audit = AuditInfo.builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor.builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();

        Set<String> groupsSet = Collections.unmodifiableSet(new LinkedHashSet<>(groups));

        UpdateClusterGroupsUseCase.Output output = updateClusterGroupsUseCase.execute(
            new UpdateClusterGroupsUseCase.Input(clusterId, groupsSet, audit)
        );

        return Response.ok().entity(output.groups()).build();
    }

    @GET
    @Path("permissions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClusterPermissions() {
        var output = getClusterPermissionsUseCase.execute(
            new GetClusterPermissionsUseCase.Input(isAuthenticated(), isAdmin(), getAuthenticatedUser(), clusterId)
        );
        return Response.ok(output.permissions()).build();
    }

    @Path("members")
    public ClusterMembersResource getClusterMembersResource() {
        return resourceContext.getResource(ClusterMembersResource.class);
    }
}
