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
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.NotificationInput;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationNotificationResource extends AbstractResource {

    @Autowired
    private PortalNotificationConfigService portalNotificationConfigService;

    @Autowired
    private GenericNotificationConfigService genericNotificationConfigService;

    @Autowired
    private ApplicationService applicationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_NOTIFICATION, acls = RolePermissionAction.READ) })
    public Response get(@PathParam("applicationId") String applicationId) {
        //Does application exists ?
        applicationService.findById(GraviteeContext.getCurrentEnvironment(), applicationId);

        final PortalNotificationConfigEntity portalConfig = portalNotificationConfigService.findById(
            getAuthenticatedUser(),
            NotificationReferenceType.APPLICATION,
            applicationId
        );

        final Set<String> hooks = new HashSet<>(portalConfig.getHooks());

        genericNotificationConfigService
            .findByReference(NotificationReferenceType.APPLICATION, applicationId)
            .forEach(genericNotif -> hooks.addAll(genericNotif.getHooks()));
        return Response.ok(hooks).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_NOTIFICATION, acls = RolePermissionAction.UPDATE) })
    public Response update(
        @PathParam("applicationId") String applicationId,
        @NotNull(message = "Input must not be null.") NotificationInput notification
    ) {
        //Does application exists ?
        applicationService.findById(GraviteeContext.getCurrentEnvironment(), applicationId);

        final PortalNotificationConfigEntity portalConfig = portalNotificationConfigService.findById(
            getAuthenticatedUser(),
            NotificationReferenceType.APPLICATION,
            applicationId
        );
        portalConfig.setHooks(notification.getHooks());
        portalNotificationConfigService.save(portalConfig);

        genericNotificationConfigService
            .findByReference(NotificationReferenceType.APPLICATION, applicationId)
            .forEach(
                genericConfig -> {
                    genericConfig.setHooks(notification.getHooks());
                    genericNotificationConfigService.update(genericConfig);
                }
            );
        return Response.ok(notification.getHooks()).build();
    }
}
