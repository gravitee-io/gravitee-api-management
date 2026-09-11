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

import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.use_case.DisableSubscriptionFormUseCase;
import io.gravitee.apim.core.subscription_form.use_case.EnableSubscriptionFormUseCase;
import io.gravitee.apim.core.subscription_form.use_case.GetSubscriptionFormUseCase;
import io.gravitee.apim.core.subscription_form.use_case.SetDefaultSubscriptionFormUseCase;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

/**
 * REST resource for a single subscription form of the catalog (by ID).
 *
 * @author Gravitee.io Team
 */
@CustomLog
public class SubscriptionFormResource extends AbstractResource {

    private static final SubscriptionFormMapper mapper = SubscriptionFormMapper.INSTANCE;

    @PathParam("subscriptionFormId")
    String subscriptionFormId;

    @Inject
    private GetSubscriptionFormUseCase getSubscriptionFormUseCase;

    @Inject
    private UpdateSubscriptionFormUseCase updateSubscriptionFormUseCase;

    @Inject
    private EnableSubscriptionFormUseCase enableSubscriptionFormUseCase;

    @Inject
    private DisableSubscriptionFormUseCase disableSubscriptionFormUseCase;

    @Inject
    private SetDefaultSubscriptionFormUseCase setDefaultSubscriptionFormUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = { RolePermissionAction.READ }) })
    public Response getSubscriptionForm() {
        var output = getSubscriptionFormUseCase.execute(
            new GetSubscriptionFormUseCase.Input(GraviteeContext.getCurrentEnvironment(), subscriptionFormId())
        );
        return Response.ok(mapper.toResponse(output)).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = { RolePermissionAction.UPDATE }) })
    public Response updateSubscriptionForm(@Valid @NotNull final UpdateSubscriptionForm request) {
        var output = updateSubscriptionFormUseCase.execute(
            new UpdateSubscriptionFormUseCase.Input(
                GraviteeContext.getCurrentEnvironment(),
                subscriptionFormId(),
                request.getName(),
                request.getGmdContent()
            )
        );
        return Response.ok(mapper.toResponse(output.subscriptionForm())).build();
    }

    @POST
    @Path("/_enable")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = { RolePermissionAction.UPDATE }) })
    public Response enableSubscriptionForm() {
        var output = enableSubscriptionFormUseCase.execute(
            new EnableSubscriptionFormUseCase.Input(GraviteeContext.getCurrentEnvironment(), subscriptionFormId())
        );
        return Response.ok(mapper.toResponse(output.subscriptionForm())).build();
    }

    @POST
    @Path("/_disable")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = { RolePermissionAction.UPDATE }) })
    public Response disableSubscriptionForm() {
        var output = disableSubscriptionFormUseCase.execute(
            new DisableSubscriptionFormUseCase.Input(GraviteeContext.getCurrentEnvironment(), subscriptionFormId())
        );
        return Response.ok(mapper.toResponse(output.subscriptionForm())).build();
    }

    @POST
    @Path("/_default")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = { RolePermissionAction.UPDATE }) })
    public Response setDefaultSubscriptionForm() {
        var output = setDefaultSubscriptionFormUseCase.execute(
            new SetDefaultSubscriptionFormUseCase.Input(GraviteeContext.getCurrentEnvironment(), subscriptionFormId())
        );
        return Response.ok(mapper.toResponse(output.subscriptionForm())).build();
    }

    /**
     * The path segment is a form id only when it parses as one: anything else names no form of the environment,
     * and is answered like any other unknown id rather than as a server error.
     */
    private SubscriptionFormId subscriptionFormId() {
        try {
            return SubscriptionFormId.of(subscriptionFormId);
        } catch (IllegalArgumentException e) {
            throw new SubscriptionFormNotFoundException(
                "Subscription form not found with id [ " + subscriptionFormId + " ]",
                subscriptionFormId
            );
        }
    }
}
