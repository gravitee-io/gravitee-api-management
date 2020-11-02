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
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_NOTIFICATION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Application Notifications"})
public class ApplicationNotificationSettingsResource extends AbstractResource {

    @Inject
    private PortalNotificationConfigService portalNotificationConfigService;

    @Inject
    private GenericNotificationConfigService genericNotificationConfigService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @ApiParam(name = "application", hidden = true)
    private String application;

    @GET
    @ApiOperation(value = "Get notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = APPLICATION_NOTIFICATION, acls = READ)
    })
    public List<Object> getApplicationNotificationSettings() {
        List<Object> settings = new ArrayList<>();
        settings.add(portalNotificationConfigService.findById(getAuthenticatedUser(), NotificationReferenceType.APPLICATION, application));
        if (hasPermission(APPLICATION_NOTIFICATION, application, CREATE, UPDATE, DELETE)){
            settings.addAll(genericNotificationConfigService.findByReference(NotificationReferenceType.APPLICATION, application));
        }
        return settings;
    }

    @POST
    @ApiOperation(value = "Create notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createApplicationNotificationSettings(GenericNotificationConfigEntity config) {
        if (!application.equals(config.getReferenceId())
                || !NotificationReferenceType.APPLICATION.name().equals(config.getReferenceType())) {
            throw new ForbiddenAccessException();
        }
        if (config.getConfigType().equals(NotificationConfigType.GENERIC)
                && hasPermission(APPLICATION_NOTIFICATION, application, CREATE)) {
            return genericNotificationConfigService.create(config);
        } else if (config.getConfigType().equals(NotificationConfigType.PORTAL)
                && hasPermission(APPLICATION_NOTIFICATION, application, READ)) {
            return portalNotificationConfigService.save(convert(config));
        }
        throw new ForbiddenAccessException();
    }

    @PUT
    @Path("{notificationId}")
    @ApiOperation(value = "Update generic notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = APPLICATION_NOTIFICATION, acls = UPDATE)
    })
    public GenericNotificationConfigEntity updateApplicationGeneralNotificationSettings(
            @PathParam("notificationId") String notificationId,
            GenericNotificationConfigEntity config) {
        if (!application.equals(config.getReferenceId())
                || !NotificationReferenceType.APPLICATION.name().equals(config.getReferenceType())
                || !config.getConfigType().equals(NotificationConfigType.GENERIC)
                || !notificationId.equals(config.getId())) {
            throw new ForbiddenAccessException();
        }
        return genericNotificationConfigService.update(config);
    }

    @PUT
    @ApiOperation(value = "Update portal notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = APPLICATION_NOTIFICATION, acls = READ)
    })
    public PortalNotificationConfigEntity updateApplicationPortalNotificationSettings(
            PortalNotificationConfigEntity config) {
        if (!application.equals(config.getReferenceId())
                || !NotificationReferenceType.APPLICATION.name().equals(config.getReferenceType())
                || !config.getConfigType().equals(NotificationConfigType.PORTAL)) {
            throw new ForbiddenAccessException();
        }
        config.setUser(getAuthenticatedUser());
        return portalNotificationConfigService.save(config);
    }

    @DELETE
    @Path("{notificationId}")
    @ApiOperation(value = "Delete notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = APPLICATION_NOTIFICATION, acls = DELETE)
    })
    public Response deleteApplicationNotificationSettings(
            @PathParam("notificationId") String notificationId) {
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
