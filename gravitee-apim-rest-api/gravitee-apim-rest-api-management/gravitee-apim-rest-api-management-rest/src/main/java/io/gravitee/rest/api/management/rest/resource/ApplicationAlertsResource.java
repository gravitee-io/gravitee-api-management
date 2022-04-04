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

import static io.gravitee.rest.api.model.alert.AlertReferenceType.APPLICATION;
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_ALERT;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

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
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Application Alerts")
public class ApplicationAlertsResource extends AbstractResource {

    @Inject
    private AlertService alertService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @Parameter(name = "application", hidden = true)
    private String application;

    @GET
    @Operation(
        summary = "List configured alerts of an application",
        description = "User must have the APPLICATION_ALERT[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of alerts",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = AlertTriggerEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = APPLICATION_ALERT, acls = READ) })
    public List<AlertTriggerEntity> getApplicationAlerts(@QueryParam("event_counts") @DefaultValue("true") Boolean withEventCounts) {
        return withEventCounts
            ? alertService.findByReferenceWithEventCounts(APPLICATION, application)
            : alertService.findByReference(APPLICATION, application);
    }

    @GET
    @Path("status")
    @Operation(summary = "Get alerting status", description = "User must have the MANAGEMENT_ALERT[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Alerting status",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AlertStatusEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ) })
    public AlertStatusEntity getApplicationAlertsStatus() {
        return alertService.getStatus(GraviteeContext.getExecutionContext());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create an alert for an application",
        description = "User must have the APPLICATION_ALERT[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Alert successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AlertTriggerEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_ALERT, acls = RolePermissionAction.CREATE) })
    public AlertTriggerEntity createApplicationAlert(@Valid @NotNull final NewAlertTriggerEntity alertEntity) {
        alertEntity.setReferenceType(APPLICATION);
        alertEntity.setReferenceId(application);
        return alertService.create(GraviteeContext.getExecutionContext(), alertEntity);
    }

    @Path("{alert}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update an alert for an application",
        description = "User must have the APPLICATION_ALERT[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Alert successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AlertTriggerEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_ALERT, acls = RolePermissionAction.UPDATE) })
    public AlertTriggerEntity updateApplicationAlert(
        @PathParam("alert") String alert,
        @Valid @NotNull final UpdateAlertTriggerEntity alertEntity
    ) {
        alertEntity.setId(alert);
        alertEntity.setReferenceType(APPLICATION);
        alertEntity.setReferenceId(application);
        return alertService.update(GraviteeContext.getExecutionContext(), alertEntity);
    }

    @Path("{alert}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete an alert for an application",
        description = "User must have the APPLICATION_ALERT[DELETE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "204",
        description = "Alert successfully deleted",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AlertTriggerEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_ALERT, acls = RolePermissionAction.DELETE) })
    public void deleteApplicationAlert(@PathParam("alert") String alert) {
        alertService.delete(alert, application);
    }
}
