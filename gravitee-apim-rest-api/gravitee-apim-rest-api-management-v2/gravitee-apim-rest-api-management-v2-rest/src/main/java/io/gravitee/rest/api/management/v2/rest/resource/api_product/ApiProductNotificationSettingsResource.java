/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_PRODUCT_NOTIFICATION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "API Product Notifications")
public class ApiProductNotificationSettingsResource extends AbstractResource {

    @Inject
    private PortalNotificationConfigService portalNotificationConfigService;

    @Inject
    private GenericNotificationConfigService genericNotificationConfigService;

    @PathParam("apiProductId")
    @Parameter(name = "apiProductId", required = true)
    private String apiProductId;

    @GET
    @Operation(summary = "Get API Product notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = API_PRODUCT_NOTIFICATION, acls = READ) })
    public List<Object> getApiProductNotificationSettings() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        List<Object> settings = new ArrayList<>();
        settings.add(portalNotificationConfigService.findById(getAuthenticatedUser(), NotificationReferenceType.API_PRODUCT, apiProductId));
        if (hasPermission(executionContext, API_PRODUCT_NOTIFICATION, apiProductId, CREATE, READ, UPDATE, DELETE)) {
            settings.addAll(genericNotificationConfigService.findByReference(NotificationReferenceType.API_PRODUCT, apiProductId));
        }
        return settings;
    }

    @POST
    @Operation(summary = "Create API Product notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createApiProductNotificationSettings(GenericNotificationConfigEntity config) {
        if (
            !apiProductId.equals(config.getReferenceId()) || !NotificationReferenceType.API_PRODUCT.name().equals(config.getReferenceType())
        ) {
            throw new ForbiddenAccessException();
        }
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            config.getConfigType().equals(NotificationConfigType.GENERIC) &&
            hasPermission(executionContext, API_PRODUCT_NOTIFICATION, apiProductId, CREATE)
        ) {
            return genericNotificationConfigService.create(config);
        }
        if (
            config.getConfigType().equals(NotificationConfigType.PORTAL) &&
            hasPermission(executionContext, API_PRODUCT_NOTIFICATION, apiProductId, READ)
        ) {
            PortalNotificationConfigEntity notificationEntity = convert(config);
            return portalNotificationConfigService.save(notificationEntity);
        }
        throw new ForbiddenAccessException();
    }

    @PUT
    @Path("{notificationId}")
    @Operation(summary = "Update generic API Product notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = API_PRODUCT_NOTIFICATION, acls = UPDATE) })
    public GenericNotificationConfigEntity updateApiProductGenericNotificationSettings(
        @PathParam("notificationId") String notificationId,
        GenericNotificationConfigEntity config
    ) {
        if (
            !apiProductId.equals(config.getReferenceId()) ||
            !NotificationReferenceType.API_PRODUCT.name().equals(config.getReferenceType()) ||
            !config.getConfigType().equals(NotificationConfigType.GENERIC) ||
            !notificationId.equals(config.getId())
        ) {
            throw new ForbiddenAccessException();
        }
        return genericNotificationConfigService.update(config);
    }

    @PUT
    @Operation(summary = "Update portal API Product notification settings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = API_PRODUCT_NOTIFICATION, acls = READ) })
    public PortalNotificationConfigEntity updateApiProductPortalNotificationSettings(PortalNotificationConfigEntity config) {
        if (
            !apiProductId.equals(config.getReferenceId()) ||
            !NotificationReferenceType.API_PRODUCT.name().equals(config.getReferenceType()) ||
            !config.getConfigType().equals(NotificationConfigType.PORTAL)
        ) {
            throw new ForbiddenAccessException();
        }
        return portalNotificationConfigService.save(config);
    }

    @DELETE
    @Path("{notificationId}")
    @Operation(summary = "Delete API Product notification settings")
    @Permissions({ @Permission(value = API_PRODUCT_NOTIFICATION, acls = DELETE) })
    public Response deleteApiProductNotificationSettings(@PathParam("notificationId") String notificationId) {
        genericNotificationConfigService.delete(notificationId);
        return Response.noContent().build();
    }

    private PortalNotificationConfigEntity convert(GenericNotificationConfigEntity generic) {
        PortalNotificationConfigEntity entity = new PortalNotificationConfigEntity();
        entity.setConfigType(generic.getConfigType());
        entity.setReferenceType(generic.getReferenceType());
        entity.setReferenceId(generic.getReferenceId());
        entity.setUser(getAuthenticatedUser());
        entity.setHooks(generic.getHooks());
        entity.setGroups(generic.getGroups());
        return entity;
    }
}
