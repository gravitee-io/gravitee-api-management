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
import io.gravitee.management.model.notification.NotifierEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.NotifierService;
import io.gravitee.management.service.notification.Hook;
import io.gravitee.management.service.notification.PortalHook;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationDefaultReferenceId;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import java.util.Arrays;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/configuration")
@Api(tags = {"Configuration"})
public class ConfigurationResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private NotifierService notifierService;

    @GET
    @Path("/hooks")
    @ApiOperation("Get the list of available hooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Hook[] getHooks() {
        return Arrays.stream(PortalHook.values()).filter(h -> !h.isHidden()).toArray(Hook[]::new);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("notifiers")
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_NOTIFICATION, acls = RolePermissionAction.READ)
    })
    public List<NotifierEntity> getNotifiers() {
        return notifierService.list(NotificationReferenceType.PORTAL, PortalNotificationDefaultReferenceId.DEFAULT.name());
    }

    @Path("views")
    public ViewsResource getViewResource() {
        return resourceContext.getResource(ViewsResource.class);
    }

    @Path("groups")
    public GroupsResource getGroupResource() {
        return resourceContext.getResource(GroupsResource.class);
    }

    @Path("tags")
    public TagsResource getTagResource() {
        return resourceContext.getResource(TagsResource.class);
    }

    @Path("tenants")
    public TenantsResource getTenantsResource() {
        return resourceContext.getResource(TenantsResource.class);
    }

    @Path("metadata")
    public MetadataResource getMetadataResource() {
        return resourceContext.getResource(MetadataResource.class);
    }

    @Path("rolescopes")
    public RoleScopesResource getRoleScopesResource() {
        return resourceContext.getResource(RoleScopesResource.class);
    }

    @Path("notificationsettings")
    public PortalNotificationSettingsResource getNotificationSettingsResource() {
        return resourceContext.getResource(PortalNotificationSettingsResource.class);
    }

    @Path("top-apis")
    public TopApisResource getTopApisResource() {
        return resourceContext.getResource(TopApisResource.class);
    }

    @Path("plans")
    public PlansResource getPlansResource() {
        return resourceContext.getResource(PlansResource.class);
    }
}
