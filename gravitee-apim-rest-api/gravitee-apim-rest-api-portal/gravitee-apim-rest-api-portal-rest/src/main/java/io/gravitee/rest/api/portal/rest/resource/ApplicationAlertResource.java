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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.alert.AlertStatusEntity;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.alert.UpdateAlertTriggerEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.AlertMapper;
import io.gravitee.rest.api.portal.rest.model.Alert;
import io.gravitee.rest.api.portal.rest.model.AlertInput;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.ApplicationAlertService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import java.util.Date;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationAlertResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationAlertService applicationAlertService;

    @Inject
    private AlertMapper alertMapper;

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_ALERT, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationAlert(@PathParam("applicationId") String applicationId, @PathParam("alertId") String alertId) {
        LOGGER.info("Deleting alert {}", alertId);

        checkPlugins();
        applicationAlertService.delete(alertId, applicationId);
        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_ALERT, acls = RolePermissionAction.UPDATE) })
    public Response updateAlert(
        @PathParam("applicationId") String applicationId,
        @PathParam("alertId") String alertId,
        @Valid @NotNull(message = "Input must not be null.") AlertInput alertInput
    ) {
        LOGGER.info("Updating alert {}", alertId);
        alertInput.setApplicationId(applicationId);

        checkPlugins();
        final UpdateAlertTriggerEntity updateAlertTriggerEntity = alertMapper.convertToUpdate(alertInput);
        updateAlertTriggerEntity.setId(alertId);

        final AlertTriggerEntity updated = applicationAlertService.update(applicationId, updateAlertTriggerEntity);

        Alert alert = alertMapper.convert(updated);
        return Response.ok(alert).build();
    }

    private void checkPlugins() {
        AlertStatusEntity alertStatus = applicationAlertService.getStatus();
        if (!alertStatus.isEnabled() || alertStatus.getPlugins() == 0) {
            throw new ForbiddenAccessException();
        }
    }
}
