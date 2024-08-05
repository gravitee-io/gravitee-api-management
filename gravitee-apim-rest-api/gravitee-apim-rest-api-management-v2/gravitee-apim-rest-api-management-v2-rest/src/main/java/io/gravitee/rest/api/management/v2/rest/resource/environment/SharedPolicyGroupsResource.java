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
import io.gravitee.apim.core.shared_policy_group.use_case.CreateSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.GetSharedPolicyGroupPolicyPluginsUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.SearchSharedPolicyGroupUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SharedPolicyGroupMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreateSharedPolicyGroup;
import io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroupsResponse;
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
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@Slf4j
public class SharedPolicyGroupsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase;

    @Inject
    private SearchSharedPolicyGroupUseCase searchSharedPolicyGroupUseCase;

    @Inject
    private GetSharedPolicyGroupPolicyPluginsUseCase getSharedPolicyGroupPolicyPluginsUseCase;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { RolePermissionAction.CREATE }) })
    public Response createSharedPolicyGroup(@Valid @NotNull final CreateSharedPolicyGroup createSharedPolicyGroup) {
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

        var output = createSharedPolicyGroupUseCase.execute(
            new CreateSharedPolicyGroupUseCase.Input(SharedPolicyGroupMapper.INSTANCE.map(createSharedPolicyGroup), audit)
        );

        return Response
            .created(this.getLocationHeader(output.sharedPolicyGroup().getId()))
            .entity(SharedPolicyGroupMapper.INSTANCE.map(output.sharedPolicyGroup()))
            .build();
    }

    @Path("{sharedPolicyGroupId}")
    public SharedPolicyGroupResource getSharedPolicyGroupResource() {
        return resourceContext.getResource(SharedPolicyGroupResource.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP, acls = { RolePermissionAction.READ }) })
    public SharedPolicyGroupsResponse searchSharedPolicyGroups(
        @QueryParam("q") String q,
        @BeanParam @Valid PaginationParam paginationParam,
        @QueryParam("sortBy") String sortBy
    ) {
        var executionContext = GraviteeContext.getExecutionContext();

        var result = searchSharedPolicyGroupUseCase.execute(
            new SearchSharedPolicyGroupUseCase.Input(executionContext.getEnvironmentId(), q, paginationParam.toPageable(), sortBy)
        );

        return new SharedPolicyGroupsResponse()
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

    @GET
    @Path("/policy-plugins")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public Response getPolicyPlugins() {
        var executionContext = GraviteeContext.getExecutionContext();

        var result = getSharedPolicyGroupPolicyPluginsUseCase.execute(
            new GetSharedPolicyGroupPolicyPluginsUseCase.Input(executionContext.getEnvironmentId())
        );

        return Response
            .ok()
            .entity(SharedPolicyGroupMapper.INSTANCE.mapToSharedPolicyGroupPolicyPlugins(result.sharedPolicyGroupPolicyList()))
            .build();
    }
}
