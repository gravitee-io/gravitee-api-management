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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.alert.AlertEntity;
import io.gravitee.management.model.alert.NewAlertEntity;
import io.gravitee.management.model.alert.UpdateAlertEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.AlertService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;

import static io.gravitee.management.model.alert.AlertReferenceType.API;
import static io.gravitee.management.model.permissions.RolePermission.API_ALERT;
import static io.gravitee.management.model.permissions.RolePermissionAction.READ;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API", "Alerts"})
public class ApiAlertsResource extends AbstractResource {

    @Autowired
    private AlertService alertService;

    @GET
    @ApiOperation(value = "List configured alerts of a given API")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = API_ALERT, acls = READ)
    })
    public List<AlertEntity> list(@PathParam("api") String api) {
        return alertService.findByReference(API, api);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_ALERT, acls = RolePermissionAction.CREATE)
    })
    public AlertEntity create(@PathParam("api") String api, @Valid @NotNull final NewAlertEntity alertEntity) {
        alertEntity.setReferenceType(API);
        alertEntity.setReferenceId(api);
        return alertService.create(alertEntity);
    }

    @Path("{alert}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_ALERT, acls = RolePermissionAction.UPDATE)
    })
    public AlertEntity update(@PathParam("api") String api, @PathParam("alert") String alert, @Valid @NotNull final UpdateAlertEntity alertEntity) {
        alertEntity.setId(alert);
        alertEntity.setReferenceType(API);
        alertEntity.setReferenceId(api);
        return alertService.update(alertEntity);
    }

    @Path("{alert}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_ALERT, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("api") String api, @PathParam("alert") String alert) {
        alertService.delete(alert, api);
    }
}
