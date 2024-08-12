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
import io.gravitee.apim.core.shared_policy_group.use_case.DeleteSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.DeploySharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.GetSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.SearchSharedPolicyGroupHistoryUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.SearchSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.UndeploySharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.UpdateSharedPolicyGroupUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SharedPolicyGroupMapper;
import io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroupHistoriesResponse;
import io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroupsResponse;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSharedPolicyGroup;
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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@Slf4j
public class SharedPolicyGroupResource extends AbstractResource {

    @PathParam("sharedPolicyGroupId")
    String sharedPolicyGroupId;

    @Inject
    private GetSharedPolicyGroupUseCase getSharedPolicyGroupUseCase;

    @Inject
    private UpdateSharedPolicyGroupUseCase updateSharedPolicyGroupUseCase;

    @Inject
    private DeploySharedPolicyGroupUseCase deploySharedPolicyGroupUseCase;

    @Inject
    private UndeploySharedPolicyGroupUseCase undeploySharedPolicyGroupUseCase;

    @Inject
    private DeleteSharedPolicyGroupUseCase deleteSharedPolicyGroupUseCase;

    @Inject
    private SearchSharedPolicyGroupHistoryUseCase searchSharedPolicyGroupHistoryUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { RolePermissionAction.READ }) })
    public Response getSharedPolicyGroup() {
        var executionContext = GraviteeContext.getExecutionContext();

        var output = getSharedPolicyGroupUseCase.execute(
            new GetSharedPolicyGroupUseCase.Input(executionContext.getEnvironmentId(), sharedPolicyGroupId)
        );

        return Response
            .ok(this.getLocationHeader(output.sharedPolicyGroup().getId()))
            .entity(SharedPolicyGroupMapper.INSTANCE.map(output.sharedPolicyGroup()))
            .build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { RolePermissionAction.UPDATE }) })
    public Response updateSharedPolicyGroup(@Valid @NotNull final UpdateSharedPolicyGroup updateSharedPolicyGroup) {
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
        var output = updateSharedPolicyGroupUseCase.execute(
            new UpdateSharedPolicyGroupUseCase.Input(
                sharedPolicyGroupId,
                SharedPolicyGroupMapper.INSTANCE.map(updateSharedPolicyGroup),
                audit
            )
        );

        return Response
            .ok(this.getLocationHeader(output.sharedPolicyGroup().getId()))
            .entity(SharedPolicyGroupMapper.INSTANCE.map(output.sharedPolicyGroup()))
            .build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { RolePermissionAction.DELETE }) })
    public Response deleteSharedPolicyGroup() {
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

        deleteSharedPolicyGroupUseCase.execute(new DeleteSharedPolicyGroupUseCase.Input(sharedPolicyGroupId, audit));

        return Response.noContent().build();
    }

    @GET
    @Path("/histories")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { RolePermissionAction.READ }) })
    public SharedPolicyGroupHistoriesResponse getSharedPolicyGroupHistories(
        @BeanParam @Valid PaginationParam paginationParam,
        @QueryParam("sortBy") String sortBy
    ) {
        var executionContext = GraviteeContext.getExecutionContext();

        var result = searchSharedPolicyGroupHistoryUseCase.execute(
            new SearchSharedPolicyGroupHistoryUseCase.Input(
                executionContext.getEnvironmentId(),
                sharedPolicyGroupId,
                paginationParam.toPageable(),
                sortBy
            )
        );

        return new SharedPolicyGroupHistoriesResponse()
            .data(SharedPolicyGroupMapper.INSTANCE.map(result.result().getContent()))
            .pagination(
                PaginationInfo.computePaginationInfo(
                    result.result().getTotalElements(),
                    result.result().getContent().size(),
                    paginationParam
                )
            )
            .links(computePaginationLinks(result.result().getTotalElements(), paginationParam));
    }

    @POST
    @Path("/_deploy")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { RolePermissionAction.UPDATE }) })
    public Response deploySharedPolicyGroup() {
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

        var output = deploySharedPolicyGroupUseCase.execute(
            new DeploySharedPolicyGroupUseCase.Input(sharedPolicyGroupId, executionContext.getEnvironmentId(), audit)
        );

        return Response
            .accepted()
            .tag(Long.toString(output.sharedPolicyGroup().getDeployedAt().toInstant().toEpochMilli()))
            .lastModified(Date.from(output.sharedPolicyGroup().getDeployedAt().toInstant()))
            .entity(SharedPolicyGroupMapper.INSTANCE.map(output.sharedPolicyGroup()))
            .build();
    }

    @POST
    @Path("/_undeploy")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { RolePermissionAction.UPDATE }) })
    public Response undeploySharedPolicyGroup() {
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

        var output = undeploySharedPolicyGroupUseCase.execute(
            new UndeploySharedPolicyGroupUseCase.Input(sharedPolicyGroupId, executionContext.getEnvironmentId(), audit)
        );

        return Response
            .accepted()
            .tag(Long.toString(output.sharedPolicyGroup().getDeployedAt().toInstant().toEpochMilli()))
            .lastModified(Date.from(output.sharedPolicyGroup().getDeployedAt().toInstant()))
            .entity(SharedPolicyGroupMapper.INSTANCE.map(output.sharedPolicyGroup()))
            .build();
    }
}
