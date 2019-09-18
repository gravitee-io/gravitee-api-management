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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.NotificationConfigMapper;
import io.gravitee.rest.api.portal.rest.model.GenericNotificationConfig;
import io.gravitee.rest.api.portal.rest.model.NotificationConfig;
import io.gravitee.rest.api.portal.rest.model.NotificationConfigsResponse;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationConfig;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationNotificationSettingsResource extends AbstractResource {

    @Autowired
    private PortalNotificationConfigService portalNotificationConfigService;

    @Autowired
    private GenericNotificationConfigService genericNotificationConfigService;

    @Autowired
    private NotificationConfigMapper notificationConfigMapper;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_NOTIFICATION, acls = RolePermissionAction.READ)
    })
    public Response get(@PathParam("applicationId") String applicationId) {
        List<NotificationConfig> settings = new ArrayList<>();
        settings.add(
                notificationConfigMapper.convert(
                        portalNotificationConfigService.findById(getAuthenticatedUser(), NotificationReferenceType.APPLICATION, applicationId)
                        )
                );
        if (hasPermission(RolePermission.APPLICATION_NOTIFICATION, applicationId, RolePermissionAction.CREATE, RolePermissionAction.UPDATE, RolePermissionAction.DELETE)){
            genericNotificationConfigService.findByReference(NotificationReferenceType.APPLICATION, applicationId).stream()
            .map(notificationConfigMapper::convert)
            .forEach(settings::add);
        }
        return Response
                .ok(new NotificationConfigsResponse().data(settings))
                .build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@PathParam("applicationId") String applicationId, GenericNotificationConfig genericConfig) {
        if (!applicationId.equals(genericConfig.getReferenceId())
                || !NotificationReferenceType.APPLICATION.name().equals(genericConfig.getReferenceType())) {
            throw new ForbiddenAccessException();
        }
        if (NotificationConfigType.GENERIC.name().equals(genericConfig.getConfigType())
                && hasPermission(RolePermission.APPLICATION_NOTIFICATION, applicationId, RolePermissionAction.CREATE)) {
            GenericNotificationConfigEntity entity = genericNotificationConfigService.create(notificationConfigMapper.convert(genericConfig));
            return Response
                    .ok(notificationConfigMapper.convert(entity))
                    .status(Response.Status.CREATED)
                    .build();
            
        } else if (NotificationConfigType.PORTAL.name().equals(genericConfig.getConfigType())
                && hasPermission(RolePermission.APPLICATION_NOTIFICATION, applicationId, RolePermissionAction.READ)) {
            PortalNotificationConfigEntity entity =  portalNotificationConfigService.save(notificationConfigMapper.convertToPortalConfigEntity(genericConfig, getAuthenticatedUser()));
            return Response
                    .ok(notificationConfigMapper.convert(entity))
                    .status(Response.Status.CREATED)
                    .build();
        }
        throw new ForbiddenAccessException();
    }

    @PUT
    @Path("{notificationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_NOTIFICATION, acls = RolePermissionAction.UPDATE)
    })
    public Response update(
            @PathParam("applicationId") String applicationId,
            @PathParam("notificationId") String notificationId,
            GenericNotificationConfig genericConfig) {
        if (!applicationId.equals(genericConfig.getReferenceId())
                || !NotificationReferenceType.APPLICATION.name().equals(genericConfig.getReferenceType())
                || !NotificationConfigType.GENERIC.name().equals(genericConfig.getConfigType())
                || !notificationId.equals(genericConfig.getId())) {
            throw new ForbiddenAccessException();
        }
        GenericNotificationConfigEntity updatedEntity = genericNotificationConfigService.update(notificationConfigMapper.convert(genericConfig));
        return Response
                .ok(notificationConfigMapper.convert(updatedEntity))
                .build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_NOTIFICATION, acls = RolePermissionAction.READ)
    })
    public Response update(
            @PathParam("applicationId") String applicationId,
            PortalNotificationConfig portalConfig) {
        if (!applicationId.equals(portalConfig.getReferenceId())
                || !NotificationReferenceType.APPLICATION.name().equals(portalConfig.getReferenceType())
                || !NotificationConfigType.PORTAL.name().equals(portalConfig.getConfigType())) {
            throw new ForbiddenAccessException();
        }
        portalConfig.setUser(getAuthenticatedUser());
        PortalNotificationConfigEntity updatedEntity = portalNotificationConfigService.save(notificationConfigMapper.convert(portalConfig));
        return Response
                .ok(notificationConfigMapper.convert(updatedEntity))
                .build();
    }

    @DELETE
    @Path("{notificationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_NOTIFICATION, acls = RolePermissionAction.DELETE)
    })
    public Response delete(
            @PathParam("applicationId") String applicationId,
            @PathParam("notificationId") String notificationId) {
        genericNotificationConfigService.delete(notificationId);
        return Response.noContent().build();
    }

}
