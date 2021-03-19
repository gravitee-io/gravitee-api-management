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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
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
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;

import static io.gravitee.rest.api.model.alert.AlertReferenceType.API;
import static io.gravitee.rest.api.model.permissions.RolePermission.API_ALERT;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API Alerts"})
public class ApiAlertsResource extends AbstractResource {

    @Inject
    private AlertService alertService;

    @Inject
    private AlertAnalyticsService alertAnalyticsService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @GET
    @ApiOperation(value = "List alerts of an API",
            notes = "User must have the API_ALERT[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of alerts", response = AlertTriggerEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = API_ALERT, acls = READ)
    })
    public List<AlertTriggerEntity> getApiAlerts(@QueryParam("event_counts") @DefaultValue("true") Boolean withEventCounts) {
        return withEventCounts ? alertService.findByReferenceWithEventCounts(API, api)
                : alertService.findByReference(API, api);
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
    public AlertStatusEntity getApiAlertsStatus() {
        return alertService.getStatus();
    }

    @GET
    @Path("analytics")
    @ApiOperation(value = "List configured alerts of the API",
            notes = "User must have the API_ALERT[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of alerts", response = AlertTriggerEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = API_ALERT, acls = READ)
    })
    public AlertAnalyticsEntity getPlatformAlertsAnalytics(@BeanParam AlertAnalyticsParam param) {
        param.validate();
        return alertAnalyticsService.findByReference(API,
                api,
                new AlertAnalyticsQuery
                        .Builder()
                        .from(param.getFrom())
                        .to(param.getTo())
                        .build()
        );
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create an alert for an API",
            notes = "User must have the API_ALERT[CREATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Alert successfully created", response = AlertTriggerEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_ALERT, acls = RolePermissionAction.CREATE)
    })
    public AlertTriggerEntity createApiAlert(@Valid @NotNull final NewAlertTriggerEntity alertEntity) {
        alertEntity.setReferenceType(API);
        alertEntity.setReferenceId(api);
        return alertService.create(alertEntity);
    }

    @Path("{alert}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update an alert for an API",
            notes = "User must have the API_ALERT[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Alert successfully updated", response = AlertTriggerEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_ALERT, acls = RolePermissionAction.UPDATE)
    })
    public AlertTriggerEntity updateApiAlert(@PathParam("alert") String alert, @Valid @NotNull final UpdateAlertTriggerEntity alertEntity) {
        alertEntity.setId(alert);
        alertEntity.setReferenceType(API);
        alertEntity.setReferenceId(api);
        return alertService.update(alertEntity);
    }

    @Path("{alert}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete an alert for an API",
            notes = "User must have the API_ALERT[DELETE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Alert successfully deleted", response = AlertTriggerEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_ALERT, acls = RolePermissionAction.DELETE)
    })
    public void deleteApiAlert(@PathParam("alert") String alert) {
        alertService.delete(alert, api);
    }

    @GET
    @Path("{alert}/events")
    @ApiOperation(value = "Retrieve the list of events for an alert",
            notes = "User must have the MANAGEMENT_ALERT[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of events"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ)
    })
    public Page<AlertEventEntity> getApiAlertEvents(@PathParam("alert") String alert, @BeanParam AlertEventSearchParam param) {
        return alertService.findEvents(
                alert,
                new AlertEventQuery.Builder()
                        .from(param.getFrom())
                        .to(param.getTo())
                        .pageNumber(param.getPage())
                        .pageSize(param.getSize())
                        .build());
    }
}
