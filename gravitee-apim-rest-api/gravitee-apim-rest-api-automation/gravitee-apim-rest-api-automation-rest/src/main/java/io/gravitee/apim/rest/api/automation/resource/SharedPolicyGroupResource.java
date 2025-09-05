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
package io.gravitee.apim.rest.api.automation.resource;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.shared_policy_group.crud_service.SharedPolicyGroupCrudService;
import io.gravitee.apim.core.shared_policy_group.exception.SharedPolicyGroupNotFoundException;
import io.gravitee.apim.core.shared_policy_group.use_case.DeleteSharedPolicyGroupUseCase;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.SharedPolicyGroupMapper;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SharedPolicyGroupResource extends AbstractResource {

    @PathParam("hrid")
    private String hrid;

    @Inject
    private SharedPolicyGroupCrudService sharedPolicyGroupCrudService;

    @Inject
    private DeleteSharedPolicyGroupUseCase deleteSharedPolicyGroupUseCase;

    @Inject
    protected PermissionService permissionService;

    @GET
    public Response get(@QueryParam("legacy") boolean legacy) {
        var executionContext = GraviteeContext.getExecutionContext();

        if (
            !permissionService.hasPermission(
                executionContext,
                RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP,
                executionContext.getEnvironmentId(),
                RolePermissionAction.READ
            )
        ) {
            throw new ForbiddenAccessException();
        }

        try {
            var sharedPolicyGroup = sharedPolicyGroupCrudService.getByEnvironmentId(
                executionContext.getEnvironmentId(),
                legacy ? hrid : IdBuilder.builder(executionContext, hrid).buildId()
            );
            return Response.ok(SharedPolicyGroupMapper.INSTANCE.toState(sharedPolicyGroup)).build();
        } catch (SharedPolicyGroupNotFoundException e) {
            log.warn("SharedPolicyGroup not found for hrid: {}, operation: get", hrid);
            throw new HRIDNotFoundException(hrid);
        }
    }

    @DELETE
    public Response delete(@QueryParam("dryRun") boolean dryRun, @QueryParam("legacy") boolean legacy) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        if (
            !permissionService.hasPermission(
                executionContext,
                RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP,
                executionContext.getEnvironmentId(),
                RolePermissionAction.DELETE
            )
        ) {
            throw new ForbiddenAccessException();
        }

        var auditInfo = AuditInfo
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

        try {
            var sharedPolicyGroup = sharedPolicyGroupCrudService.getByEnvironmentId(
                executionContext.getEnvironmentId(),
                legacy ? hrid : IdBuilder.builder(executionContext, hrid).buildId()
            );
            if (!dryRun) {
                deleteSharedPolicyGroupUseCase.execute(new DeleteSharedPolicyGroupUseCase.Input(sharedPolicyGroup.getId(), auditInfo));
            }
        } catch (SharedPolicyGroupNotFoundException e) {
            log.warn("SharedPolicyGroup not found for hrid: {}, operation: delete", hrid);
            throw new HRIDNotFoundException(hrid);
        }

        return Response.noContent().build();
    }
}
