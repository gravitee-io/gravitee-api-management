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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_NOTIFICATION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    @Inject
    private GroupService groupService;

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
        if (hasPermission(GraviteeContext.getExecutionContext(), API_NOTIFICATION, api, CREATE, READ, UPDATE, DELETE)) {
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
            PortalNotificationConfigEntity notificationEntity = convert(config);
            checkGroups(executionContext, notificationEntity);
            return portalNotificationConfigService.save(notificationEntity);
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
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        checkGroups(executionContext, config);
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
        portalNotificationConfigEntity.setGroups(generic.getGroups());
        return portalNotificationConfigEntity;
    }

    private void checkGroups(ExecutionContext executionContext, PortalNotificationConfigEntity notificationEntity) {
        if (CollectionUtils.isEmpty(notificationEntity.getGroups())) {
            return;
        }

        String primaryOwnerUserId = membershipService.getPrimaryOwnerUserId(
            executionContext.getOrganizationId(),
            MembershipReferenceType.API,
            notificationEntity.getReferenceId()
        );

        // can't set group if you are not API Primary Owner
        if (!Objects.equals(primaryOwnerUserId, getAuthenticatedUser())) {
            throw new ForbiddenAccessException();
        }

        // check if the groups sent belong to the API
        GenericApiEntity theAPI = apiSearchService.findGenericById(executionContext, api);
        if (!theAPI.getGroups().containsAll(notificationEntity.getGroups())) {
            throw new InvalidDataException(
                "One of the groups is not a member of this API, got [%s] expected one of [%s]".formatted(
                    notificationEntity.getGroups(),
                    theAPI.getGroups()
                )
            );
        }
    }
}
