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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.use_case.ImportGroupCRDUseCase;
import io.gravitee.apim.core.group.use_case.ValidateGroupCRDUseCase;
import io.gravitee.apim.rest.api.automation.helpers.CrdIdHelper;
import io.gravitee.apim.rest.api.automation.mapper.GroupMapper;
import io.gravitee.apim.rest.api.automation.model.GroupSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Set;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ImportGroupCRDUseCase importGroupCRDUseCase;

    @Inject
    private ValidateGroupCRDUseCase validateGroupCRDUseCase;

    @Path("/{hrid}")
    public GroupResource getGroupResource() {
        return resourceContext.getResource(GroupResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(
        @Valid @NotNull GroupSpec spec,
        @QueryParam("dryRun") boolean dryRun,
        @QueryParam("hridContainsUUID") boolean hridContainsUUID,
        @QueryParam("ignoreMembers") boolean ignoreMembers
    ) {
        checkDefaultMemberRolesScopes(spec);

        var auditInfo = getAuditInfo();

        var groupCRDSpec = GroupMapper.INSTANCE.groupSpecToGroupCRDSpec(spec);

        if (hridContainsUUID) {
            groupCRDSpec.setId(spec.getHrid());
            groupCRDSpec.setHrid(null);
        } else {
            CrdIdHelper.generateGroupId(groupCRDSpec, auditInfo);
        }

        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        groupCRDSpec.setIgnoreMembers(ignoreMembers);

        if (dryRun) {
            var status = validateGroupCRDUseCase.execute(new ImportGroupCRDUseCase.Input(auditInfo, groupCRDSpec)).status();

            return Response.ok(GroupMapper.INSTANCE.groupSpecAndStatusToGroupState(spec, status, executionContext)).build();
        }

        var status = importGroupCRDUseCase.execute(new ImportGroupCRDUseCase.Input(auditInfo, groupCRDSpec)).status();

        return Response.ok(GroupMapper.INSTANCE.groupSpecAndStatusToGroupState(spec, status, executionContext)).build();
    }

    private static final Set<String> GROUP_DEFAULT_ROLE_SCOPES = Set.of("API", "APPLICATION", "API_PRODUCT");

    private void checkDefaultMemberRolesScopes(GroupSpec spec) {
        if (spec.getDefaultMemberRoles() == null) {
            return;
        }
        spec
            .getDefaultMemberRoles()
            .keySet()
            .stream()
            .filter(scope -> !GROUP_DEFAULT_ROLE_SCOPES.contains(scope))
            .findFirst()
            .ifPresent(scope -> {
                throw new ValidationDomainException(
                    "defaultMemberRoles: scope [" + scope + "] is not a group default role scope (API, APPLICATION, API_PRODUCT)"
                );
            });
    }
}
