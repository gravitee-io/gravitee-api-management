/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.alert.AlertStatusEntity;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.AlertMapper;
import io.gravitee.rest.api.portal.rest.model.Alert;
import io.gravitee.rest.api.portal.rest.model.AlertInput;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.ApplicationAlertService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationAlertMaximumException;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationAlertsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationAlertService applicationAlertService;

    @Inject
    private AlertMapper alertMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_ALERT, acls = RolePermissionAction.READ) })
    public Response getAlertsByApplicationId(@PathParam("applicationId") String applicationId) {
        checkPlugins(GraviteeContext.getExecutionContext());
        List<Alert> alerts = applicationAlertService
            .findByApplication(applicationId)
            .stream()
            .sorted(Comparator.comparing(AlertTriggerEntity::getCreatedAt))
            .map(alert -> alertMapper.convert(alert))
            .collect(Collectors.toList());

        return Response.ok(alerts).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_ALERT, acls = RolePermissionAction.CREATE) })
    public Response createApplicationAlert(
        @PathParam("applicationId") String applicationId,
        @Valid @NotNull(message = "Input must not be null.") AlertInput alertInput
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        alertInput.setApplication(applicationId);

        checkPlugins(executionContext);

        if (applicationAlertService.findByApplication(applicationId).size() == 10) {
            throw new ApplicationAlertMaximumException(applicationId, 10);
        }

        final NewAlertTriggerEntity newAlertTriggerEntity = alertMapper.convert(alertInput);

        final AlertTriggerEntity alert = applicationAlertService.create(executionContext, applicationId, newAlertTriggerEntity);

        return Response.created(this.getLocationHeader(alert.getId())).entity(alertMapper.convert(alert)).build();
    }

    @GET
    @Path("status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_ALERT, acls = READ) })
    public Response getApplicationAlertStatus() {
        return Response.ok(applicationAlertService.getStatus(GraviteeContext.getExecutionContext())).build();
    }

    @Path("{alertId}")
    public ApplicationAlertResource getApplicationAlertResource() {
        return resourceContext.getResource(ApplicationAlertResource.class);
    }

    private void checkPlugins(final ExecutionContext executionContext) {
        AlertStatusEntity alertStatus = applicationAlertService.getStatus(executionContext);
        if (!alertStatus.isEnabled() || alertStatus.getPlugins() == 0) {
            throw new ForbiddenAccessException();
        }
    }
}
