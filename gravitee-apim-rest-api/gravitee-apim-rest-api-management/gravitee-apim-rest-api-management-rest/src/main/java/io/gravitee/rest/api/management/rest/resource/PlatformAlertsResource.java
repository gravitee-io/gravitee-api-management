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

import static io.gravitee.rest.api.model.alert.AlertReferenceType.PLATFORM;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.wrapper.AlertEventPage;
import io.gravitee.rest.api.management.rest.resource.param.AlertAnalyticsParam;
import io.gravitee.rest.api.management.rest.resource.param.AlertEventSearchParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.AlertAnalyticsQuery;
import io.gravitee.rest.api.model.AlertEventQuery;
import io.gravitee.rest.api.model.alert.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.AlertAnalyticsService;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
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
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Platform Alerts")
public class PlatformAlertsResource extends AbstractResource {

    private static final String PLATFORM_REFERENCE_ID = "default";

    @Inject
    private AlertService alertService;

    @Inject
    private AlertAnalyticsService alertAnalyticsService;

    @GET
    @Operation(
        summary = "List configured alerts of the platform",
        description = "User must have the MANAGEMENT_ALERT[READ] permission to use this service"
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
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ) })
    public List<AlertTriggerEntity> getPlatformAlerts(@QueryParam("event_counts") @DefaultValue("true") Boolean withEventCounts) {
        return withEventCounts
            ? alertService.findByReferenceWithEventCounts(GraviteeContext.getExecutionContext(), PLATFORM, PLATFORM_REFERENCE_ID)
            : alertService.findByReference(GraviteeContext.getExecutionContext(), PLATFORM, PLATFORM_REFERENCE_ID);
    }

    @GET
    @Path("analytics")
    @Operation(
        summary = "List configured alerts of the platform",
        description = "User must have the MANAGEMENT_ALERT[READ] permission to use this service"
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
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ) })
    public AlertAnalyticsEntity getPlatformAlertsAnalytics(@BeanParam AlertAnalyticsParam param) {
        param.validate();
        return alertAnalyticsService.findByReference(
                GraviteeContext.getExecutionContext(), PLATFORM,
            PLATFORM_REFERENCE_ID,
            new AlertAnalyticsQuery.Builder().from(param.getFrom()).to(param.getTo()).build()
        );
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
    public AlertStatusEntity getPlatformAlertStatus() {
        return alertService.getStatus();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create an alert for the platform",
        description = "User must have the MANAGEMENT_ALERT[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Alert successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AlertTriggerEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.CREATE) })
    public AlertTriggerEntity createPlatformAlert(@Valid @NotNull final NewAlertTriggerEntity alertEntity) {
        alertEntity.setReferenceType(PLATFORM);
        alertEntity.setReferenceId(PLATFORM_REFERENCE_ID);
        return alertService.create(GraviteeContext.getExecutionContext(), alertEntity);
    }

    @Path("{alert}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update an alert for the platform",
        description = "User must have the MANAGEMENT_ALERT[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Alert successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AlertTriggerEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.UPDATE) })
    public AlertTriggerEntity updatePlatformAlert(
        @PathParam("alert") String alert,
        @Valid @NotNull final UpdateAlertTriggerEntity alertEntity
    ) {
        alertEntity.setId(alert);
        alertEntity.setReferenceType(PLATFORM);
        alertEntity.setReferenceId(PLATFORM_REFERENCE_ID);
        alertEntity.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        return alertService.update(alertEntity);
    }

    @POST
    @Path("{alert}")
    @Operation(
        summary = "Associate the alert to multiple references (API, APPLICATION",
        description = "User must have the MANAGEMENT_ALERT[UPDATE] permission to use this service"
    )
    @ApiResponse(responseCode = "200", description = "Alert successfully associated")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.UPDATE) })
    public Response associatePlatformAlert(@PathParam("alert") String alert, @QueryParam("type") String type) {
        alertService.applyDefaults(GraviteeContext.getExecutionContext(), alert, AlertReferenceType.valueOf(type.toUpperCase()));
        return Response.ok().build();
    }

    @Path("{alert}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete an alert for the platform",
        description = "User must have the MANAGEMENT_ALERT[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Alert successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.DELETE) })
    public void deletePlatformAlert(@PathParam("alert") String alert) {
        alertService.delete(GraviteeContext.getExecutionContext(), alert, PLATFORM_REFERENCE_ID);
    }

    @GET
    @Path("{alert}/events")
    @Operation(
        summary = "Retrieve the list of events for an alert",
        description = "User must have the MANAGEMENT_ALERT[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of events",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AlertEventPage.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ) })
    public AlertEventPage getPlatformAlertEvents(@PathParam("alert") String alert, @BeanParam AlertEventSearchParam param) {
        return new AlertEventPage(
            alertService.findEvents(
                alert,
                new AlertEventQuery.Builder()
                    .from(param.getFrom())
                    .to(param.getTo())
                    .pageNumber(param.getPage())
                    .pageSize(param.getSize())
                    .build()
            )
        );
    }
}
