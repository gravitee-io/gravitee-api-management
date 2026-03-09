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
import io.gravitee.apim.core.cluster.domain_service.ClusterConfigurationSchemaService;
import io.gravitee.apim.core.cluster.use_case.CreateClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.SearchClusterUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ClusterMapper;
import io.gravitee.rest.api.management.v2.rest.model.ClustersResponse;
import io.gravitee.rest.api.management.v2.rest.model.CreateCluster;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

@CustomLog
public class ClustersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateClusterUseCase createClusterUseCase;

    @Inject
    private SearchClusterUseCase searchClusterUseCase;

    @Inject
    private ClusterConfigurationSchemaService clusterConfigurationSchemaService;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = { RolePermissionAction.CREATE }) })
    public Response createCluster(@Valid @NotNull final CreateCluster createCluster) {
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

        var output = createClusterUseCase.execute(new CreateClusterUseCase.Input(ClusterMapper.INSTANCE.map(createCluster), audit));

        return Response.created(this.getLocationHeader(output.cluster().getId()))
            .entity(ClusterMapper.INSTANCE.map(output.cluster()))
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = { RolePermissionAction.READ }) })
    public ClustersResponse searchClusters(
        @QueryParam("q") String q,
        @BeanParam @Valid PaginationParam paginationParam,
        @QueryParam("sortBy") String sortBy
    ) {
        var executionContext = GraviteeContext.getExecutionContext();

        SearchClusterUseCase.Output result = searchClusterUseCase.execute(
            new SearchClusterUseCase.Input(
                executionContext.getEnvironmentId(),
                q,
                paginationParam.toPageable(),
                sortBy,
                isAdmin(),
                getAuthenticatedUser()
            )
        );

        return new ClustersResponse()
            .data(ClusterMapper.INSTANCE.map(result.pageResult().getContent()))
            .pagination(
                PaginationInfo.computePaginationInfo(
                    result.pageResult().getTotalElements(),
                    result.pageResult().getContent().size(),
                    paginationParam
                )
            )
            .links(computePaginationLinks(result.pageResult().getTotalElements(), paginationParam));
    }

    @GET
    @Path("schema/configuration")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_CLUSTER, acls = { RolePermissionAction.READ }) })
    public Response getConfigurationSchema() {
        return Response.ok(clusterConfigurationSchemaService.getConfigurationSchema()).build();
    }

    @Path("{clusterId}")
    public ClusterResource getClusterResource() {
        return resourceContext.getResource(ClusterResource.class);
    }
}
