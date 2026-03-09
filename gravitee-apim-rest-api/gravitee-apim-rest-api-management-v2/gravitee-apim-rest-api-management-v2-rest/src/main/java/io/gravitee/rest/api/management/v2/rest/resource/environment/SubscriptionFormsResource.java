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

import io.gravitee.apim.core.subscription_form.use_case.GetSubscriptionFormForEnvironmentUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SubscriptionFormMapper;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

/**
 * REST resource for managing subscription forms (list/get by environment).
 *
 * @author Gravitee.io Team
 */
@CustomLog
public class SubscriptionFormsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GetSubscriptionFormForEnvironmentUseCase getSubscriptionFormForEnvironmentUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = { RolePermissionAction.READ }) })
    public Response getSubscriptionForm() {
        var environmentId = GraviteeContext.getCurrentEnvironment();

        var output = getSubscriptionFormForEnvironmentUseCase.execute(
            new GetSubscriptionFormForEnvironmentUseCase.Input(environmentId, false)
        );

        return Response.ok(SubscriptionFormMapper.INSTANCE.toResponse(output.subscriptionForm())).build();
    }

    @Path("{subscriptionFormId}")
    public SubscriptionFormResource getSubscriptionFormResource() {
        return resourceContext.getResource(SubscriptionFormResource.class);
    }
}
