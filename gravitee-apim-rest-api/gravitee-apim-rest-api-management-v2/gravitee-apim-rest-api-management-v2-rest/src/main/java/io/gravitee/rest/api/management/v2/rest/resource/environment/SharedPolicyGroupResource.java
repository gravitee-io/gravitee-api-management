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

import io.gravitee.apim.core.shared_policy_group.use_case.GetSharedPolicyGroupUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SharedPolicyGroupMapper;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.SHARED_POLICY_GROUP, acls = { RolePermissionAction.READ }) })
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
}
