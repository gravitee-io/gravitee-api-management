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
import io.gravitee.rest.api.model.alert.AlertStatusEntity;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.NewAlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.AlertService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;

import static io.gravitee.rest.api.model.alert.AlertReferenceType.PLATFORM;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Platform", "Alerts"})
public class PlatformAlertsResource extends AbstractResource {

    private final static String PLATFORM_REFERENCE_ID = "default";

    @Autowired
    private AlertService alertService;

    @GET
    @ApiOperation(value = "List configured alerts of the platform")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ)
    })
    public List<AlertTriggerEntity> list() {
        return alertService.findByReference(PLATFORM, PLATFORM_REFERENCE_ID);
    }

    @GET
    @Path("status")
    @ApiOperation(value = "Get the status of alerting module")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = READ)
    })
    public AlertStatusEntity status() {
        return alertService.getStatus();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.CREATE)
    })
    public AlertTriggerEntity create(@Valid @NotNull final NewAlertTriggerEntity alertEntity) {
        alertEntity.setReferenceType(PLATFORM);
        alertEntity.setReferenceId(PLATFORM_REFERENCE_ID);
        return alertService.create(alertEntity);
    }

    @Path("{alert}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.UPDATE)
    })
    public AlertTriggerEntity update(@PathParam("alert") String alert, @Valid @NotNull final UpdateAlertTriggerEntity alertEntity) {
        alertEntity.setId(alert);
        alertEntity.setReferenceType(PLATFORM);
        alertEntity.setReferenceId(PLATFORM_REFERENCE_ID);
        return alertService.update(alertEntity);
    }

    @Path("{alert}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_ALERT, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("alert") String alert) {
        alertService.delete(alert, PLATFORM_REFERENCE_ID);
    }
}
