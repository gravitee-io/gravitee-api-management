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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.alert.AlertStatusEntity;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.AlertService;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;

import static io.gravitee.rest.api.model.alert.AlertReferenceType.APPLICATION;
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_ALERT;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Application Alerts"})
public class ApplicationAlertsResource extends AbstractResource {

    @Inject
    private AlertService alertService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @ApiParam(name = "application", hidden = true)
    private String application;

    @GET
    @ApiOperation(value = "List configured alerts of an application",
            notes = "User must have the APPLICATION_ALERT[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of alerts", response = AlertTriggerEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = APPLICATION_ALERT, acls = READ)
    })
    public List<AlertTriggerEntity> getApplicationAlerts(@QueryParam("event_counts") @DefaultValue("true") Boolean withEventCounts) {
        return withEventCounts ? alertService.findByReferenceWithEventCounts(APPLICATION, application)
                : alertService.findByReference(APPLICATION, application);
    }

    @GET
    @Path("status")
    @ApiOperation(value = "Get alerting status",
            notes = "User must have the MANAGEMENT_ALERT[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Alerting status", response = AlertStatusEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ)
    })
    public AlertStatusEntity getApplicationAlertsStatus() {
        return alertService.getStatus();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create an alert for an application",
            notes = "User must have the APPLICATION_ALERT[CREATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Alert successfully created", response = AlertTriggerEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_ALERT, acls = RolePermissionAction.CREATE)
    })
    public AlertTriggerEntity createApplicationAlert(@Valid @NotNull final NewAlertTriggerEntity alertEntity) {
        alertEntity.setReferenceType(APPLICATION);
        alertEntity.setReferenceId(application);
        return alertService.create(alertEntity);
    }

    @Path("{alert}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update an alert for an application",
            notes = "User must have the APPLICATION_ALERT[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Alert successfully updated", response = AlertTriggerEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_ALERT, acls = RolePermissionAction.UPDATE)
    })
    public AlertTriggerEntity updateApplicationAlert(@PathParam("alert") String alert, @Valid @NotNull final UpdateAlertTriggerEntity alertEntity) {
        alertEntity.setId(alert);
        alertEntity.setReferenceType(APPLICATION);
        alertEntity.setReferenceId(application);
        return alertService.update(alertEntity);
    }

    @Path("{alert}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete an alert for an application",
            notes = "User must have the APPLICATION_ALERT[DELETE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Alert successfully deleted", response = AlertTriggerEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_ALERT, acls = RolePermissionAction.DELETE)
    })
    public void deleteApplicationAlert(@PathParam("alert") String alert) {
        alertService.delete(alert, application);
    }
}
