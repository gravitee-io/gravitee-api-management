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

import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.use_case.DisableSubscriptionFormUseCase;
import io.gravitee.apim.core.subscription_form.use_case.EnableSubscriptionFormUseCase;
import io.gravitee.apim.core.subscription_form.use_case.UpdateSubscriptionFormUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SubscriptionFormMapper;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSubscriptionForm;
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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

/**
 * REST resource for a single subscription form (by ID).
 *
 * @author Gravitee.io Team
 */
@CustomLog
public class SubscriptionFormResource extends AbstractResource {

    @PathParam("subscriptionFormId")
    String subscriptionFormId;

    @Inject
    private UpdateSubscriptionFormUseCase updateSubscriptionFormUseCase;

    @Inject
    private EnableSubscriptionFormUseCase enableSubscriptionFormUseCase;

    @Inject
    private DisableSubscriptionFormUseCase disableSubscriptionFormUseCase;

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = { RolePermissionAction.UPDATE }) })
    public Response updateSubscriptionForm(@Valid @NotNull final UpdateSubscriptionForm request) {
        var environmentId = GraviteeContext.getCurrentEnvironment();

        var output = updateSubscriptionFormUseCase.execute(
            new UpdateSubscriptionFormUseCase.Input(environmentId, SubscriptionFormId.of(subscriptionFormId), request.getGmdContent())
        );

        return Response.ok(SubscriptionFormMapper.INSTANCE.toResponse(output.subscriptionForm())).build();
    }

    @POST
    @Path("/_enable")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = { RolePermissionAction.UPDATE }) })
    public Response enableSubscriptionForm() {
        var environmentId = GraviteeContext.getCurrentEnvironment();

        var output = enableSubscriptionFormUseCase.execute(
            new EnableSubscriptionFormUseCase.Input(environmentId, SubscriptionFormId.of(subscriptionFormId))
        );

        return Response.ok(SubscriptionFormMapper.INSTANCE.toResponse(output.subscriptionForm())).build();
    }

    @POST
    @Path("/_disable")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = { RolePermissionAction.UPDATE }) })
    public Response disableSubscriptionForm() {
        var environmentId = GraviteeContext.getCurrentEnvironment();

        var output = disableSubscriptionFormUseCase.execute(
            new DisableSubscriptionFormUseCase.Input(environmentId, SubscriptionFormId.of(subscriptionFormId))
        );

        return Response.ok(SubscriptionFormMapper.INSTANCE.toResponse(output.subscriptionForm())).build();
    }
}
