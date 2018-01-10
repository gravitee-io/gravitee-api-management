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
import io.gravitee.management.model.notification.GenericNotificationConfigEntity;
import io.gravitee.management.model.notification.NotificationConfigType;
import io.gravitee.management.model.notification.PortalNotificationConfigEntity;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.GenericNotificationConfigService;
import io.gravitee.management.service.PortalNotificationConfigService;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static io.gravitee.management.model.permissions.RolePermission.API_NOTIFICATION;
import static io.gravitee.management.model.permissions.RolePermissionAction.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API", "Notifications"})
public class ApiNotificationSettingsResource extends AbstractResource {

    @Autowired
    private PortalNotificationConfigService portalNotificationConfigService;

    @Autowired
    private GenericNotificationConfigService genericNotificationConfigService;

    @GET
    @ApiOperation(value = "Get notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = API_NOTIFICATION, acls = READ)
    })
    public List<Object> get(@PathParam("api") String api) {
        List<Object> settings = new ArrayList<>();
        settings.add(portalNotificationConfigService.findById(getAuthenticatedUser(), NotificationReferenceType.API, api));
        if (hasPermission(API_NOTIFICATION, api, CREATE, UPDATE, DELETE)){
            settings.addAll(genericNotificationConfigService.findByReference(NotificationReferenceType.API, api));
        }
        return settings;
    }

    @POST
    @ApiOperation(value = "Create notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Object create(@PathParam("api") String api, GenericNotificationConfigEntity config) {
        if (!api.equals(config.getReferenceId())
                || !NotificationReferenceType.API.name().equals(config.getReferenceType())) {
            throw new ForbiddenAccessException();
        }
        if (config.getConfigType().equals(NotificationConfigType.GENERIC)
                && hasPermission(API_NOTIFICATION, CREATE)) {
            return genericNotificationConfigService.create(config);
        } else if (config.getConfigType().equals(NotificationConfigType.PORTAL)
                && hasPermission(API_NOTIFICATION, READ)) {
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
            @Permission(value = API_NOTIFICATION, acls = UPDATE)
    })
    public GenericNotificationConfigEntity update(
            @PathParam("api") String api,
            @PathParam("notificationId") String notificationId,
            GenericNotificationConfigEntity config) {
        if (!api.equals(config.getReferenceId())
                || !NotificationReferenceType.API.name().equals(config.getReferenceType())
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
            @Permission(value = API_NOTIFICATION, acls = READ)
    })
    public PortalNotificationConfigEntity update(
            @PathParam("api") String api,
            PortalNotificationConfigEntity config) {
        if (!api.equals(config.getReferenceId())
                || !NotificationReferenceType.API.name().equals(config.getReferenceType())
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
            @Permission(value = API_NOTIFICATION, acls = DELETE)
    })
    public Response delete(
            @PathParam("api") String api,
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
