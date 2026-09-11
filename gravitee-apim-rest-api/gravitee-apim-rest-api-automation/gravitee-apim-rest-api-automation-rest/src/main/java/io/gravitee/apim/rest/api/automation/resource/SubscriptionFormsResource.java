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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.subscription_form.use_case.CreateOrUpdateSubscriptionFormUseCase;
import io.gravitee.apim.core.subscription_form.use_case.ValidateSubscriptionFormUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.mapper.SubscriptionFormMapper;
import io.gravitee.apim.rest.api.automation.model.SubscriptionFormSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
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

public class SubscriptionFormsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateOrUpdateSubscriptionFormUseCase createOrUpdateSubscriptionFormUseCase;

    @Inject
    private ValidateSubscriptionFormUseCase validateSubscriptionFormUseCase;

    @Inject
    private ApiCrudService apiCrudService;

    @Path("/{hrid}")
    public SubscriptionFormResource getSubscriptionFormResource() {
        return resourceContext.getResource(SubscriptionFormResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SUBSCRIPTION_FORM, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(@Valid @NotNull SubscriptionFormSpec spec, @QueryParam("dryRun") boolean dryRun) {
        var auditInfo = getAuditInfo();
        var input = SubscriptionFormMapper.INSTANCE.toSpec(spec, auditInfo);
        var output = dryRun ? validateSubscriptionFormUseCase.execute(input) : createOrUpdateSubscriptionFormUseCase.execute(input);

        var form = output.subscriptionForm();
        var apiHrids = form != null ? SubscriptionFormMapper.INSTANCE.toApiHrids(form.getApiIds(), apiCrudService) : spec.getApiHrids();
        var state = SubscriptionFormMapper.INSTANCE.toState(spec, input.subscriptionFormId().toString(), output, auditInfo, apiHrids);

        // A dry run is a preview: severe findings are its payload, so it always answers 200.
        // A real apply that produced severe errors persisted nothing — it must not report success.
        var applyFailed = !dryRun && output.errors().stream().anyMatch(Validator.Error::isSevere);
        return Response.status(applyFailed ? Response.Status.BAD_REQUEST : Response.Status.OK).entity(state).build();
    }
}
