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

import static io.gravitee.rest.api.model.permissions.RolePermission.API_NOTIFICATION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.Response;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "API Notifications")
public class ApiNotificationSettingsResource extends AbstractResource {

    @Inject
    private PortalNotificationConfigService portalNotificationConfigService;

    @Inject
    private GenericNotificationConfigService genericNotificationConfigService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @GET
    @Operation(summary = "Get notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = API_NOTIFICATION, acls = READ) })
    public List<Object> getApiNotificationSettings() {
        List<Object> settings = new ArrayList<>();
        settings.add(portalNotificationConfigService.findById(getAuthenticatedUser(), NotificationReferenceType.API, api));
        if (hasPermission(GraviteeContext.getExecutionContext(), API_NOTIFICATION, api, CREATE, UPDATE, DELETE)) {
            settings.addAll(genericNotificationConfigService.findByReference(NotificationReferenceType.API, api));
        }
        return settings;
    }

    @POST
    @Operation(summary = "Create notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createApiNotificationSettings(GenericNotificationConfigEntity config) {
        if (!api.equals(config.getReferenceId()) || !NotificationReferenceType.API.name().equals(config.getReferenceType())) {
            throw new ForbiddenAccessException();
        }
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            config.getConfigType().equals(NotificationConfigType.GENERIC) && hasPermission(executionContext, API_NOTIFICATION, api, CREATE)
        ) {
            return genericNotificationConfigService.create(config);
        } else if (
            config.getConfigType().equals(NotificationConfigType.PORTAL) && hasPermission(executionContext, API_NOTIFICATION, api, READ)
        ) {
            return portalNotificationConfigService.save(convert(config));
        }
        throw new ForbiddenAccessException();
    }

    @PUT
    @Path("{notificationId}")
    @Operation(summary = "Update generic notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = API_NOTIFICATION, acls = UPDATE) })
    public GenericNotificationConfigEntity updateApiGeneralNotificationSettings(
        @PathParam("notificationId") String notificationId,
        GenericNotificationConfigEntity config
    ) {
        if (
            !api.equals(config.getReferenceId()) ||
            !NotificationReferenceType.API.name().equals(config.getReferenceType()) ||
            !config.getConfigType().equals(NotificationConfigType.GENERIC) ||
            !notificationId.equals(config.getId())
        ) {
            throw new ForbiddenAccessException();
        }
        return genericNotificationConfigService.update(config);
    }

    @PUT
    @Operation(summary = "Update portal notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = API_NOTIFICATION, acls = READ) })
    public PortalNotificationConfigEntity updateApiPortalNotificationSettings(PortalNotificationConfigEntity config) {
        if (
            !api.equals(config.getReferenceId()) ||
            !NotificationReferenceType.API.name().equals(config.getReferenceType()) ||
            !config.getConfigType().equals(NotificationConfigType.PORTAL)
        ) {
            throw new ForbiddenAccessException();
        }
        config.setUser(getAuthenticatedUser());
        return portalNotificationConfigService.save(config);
    }

    @DELETE
    @Path("{notificationId}")
    @Operation(summary = "Delete notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = API_NOTIFICATION, acls = DELETE) })
    public Response deleteApiNotificationSettings(@PathParam("notificationId") String notificationId) {
        genericNotificationConfigService.delete(notificationId);
        return Response.noContent().build();
    }

    private PortalNotificationConfigEntity convert(GenericNotificationConfigEntity generic) {
        PortalNotificationConfigEntity portalNotificationConfigEntity = new PortalNotificationConfigEntity();
        portalNotificationConfigEntity.setConfigType(generic.getConfigType());
        portalNotificationConfigEntity.setReferenceType(generic.getReferenceType());
        portalNotificationConfigEntity.setReferenceId(generic.getReferenceId());
        portalNotificationConfigEntity.setUser(getAuthenticatedUser());
        portalNotificationConfigEntity.setHooks(generic.getHooks());
        return portalNotificationConfigEntity;
    }
}
