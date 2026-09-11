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

import io.gravitee.apim.core.subscription_form.use_case.CreateSubscriptionFormUseCase;
import io.gravitee.apim.core.subscription_form.use_case.GetSubscriptionFormTemplateUseCase;
import io.gravitee.apim.core.subscription_form.use_case.ListSubscriptionFormsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.SubscriptionFormMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreateSubscriptionForm;
import io.gravitee.rest.api.management.v2.rest.model.SubscriptionFormTemplate;
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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

/**
 * REST resource for the subscription form catalog of an environment (list, create).
 *
 * @author Gravitee.io Team
 */
@CustomLog
public class SubscriptionFormsResource extends AbstractResource {

    private static final SubscriptionFormMapper mapper = SubscriptionFormMapper.INSTANCE;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ListSubscriptionFormsUseCase listSubscriptionFormsUseCase;

    @Inject
    private CreateSubscriptionFormUseCase createSubscriptionFormUseCase;

    @Inject
    private GetSubscriptionFormTemplateUseCase getSubscriptionFormTemplateUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SUBSCRIPTION_FORM, acls = { RolePermissionAction.READ }) })
    public Response listSubscriptionForms() {
        var output = listSubscriptionFormsUseCase.execute(new ListSubscriptionFormsUseCase.Input(GraviteeContext.getCurrentEnvironment()));
        return Response.ok(mapper.toResponse(output.subscriptionForms())).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SUBSCRIPTION_FORM, acls = { RolePermissionAction.CREATE }) })
    public Response createSubscriptionForm(@Valid @NotNull final CreateSubscriptionForm request) {
        var output = createSubscriptionFormUseCase.execute(
            new CreateSubscriptionFormUseCase.Input(
                GraviteeContext.getCurrentEnvironment(),
                request.getName(),
                request.getGmdContent(),
                request.getApiIds()
            )
        );
        return Response.created(this.getLocationHeader(output.subscriptionForm().getId().toString()))
            .entity(mapper.toResponse(output.subscriptionForm()))
            .build();
    }

    @GET
    @Path("/_template")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SUBSCRIPTION_FORM, acls = { RolePermissionAction.READ }) })
    public Response getSubscriptionFormTemplate() {
        var output = getSubscriptionFormTemplateUseCase.execute();
        return Response.ok(new SubscriptionFormTemplate().gmdContent(output.gmdContent())).build();
    }

    @Path("{subscriptionFormId}")
    public SubscriptionFormResource getSubscriptionFormResource() {
        return resourceContext.getResource(SubscriptionFormResource.class);
    }
}
