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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.rest.api.portal.rest.resource.v4.entrypoint.EntrypointsResource;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("environments/{envId}")
public class EnvironmentsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Path("apis")
    public ApisResource getApisResource() {
        return resourceContext.getResource(ApisResource.class);
    }

    @Path("applications")
    public ApplicationsResource getApplicationsResource() {
        return resourceContext.getResource(ApplicationsResource.class);
    }

    @Path("auth")
    public AuthResource getAuthResource() {
        return resourceContext.getResource(AuthResource.class);
    }

    @Path("categories")
    public CategoriesResource getCategoriesResource() {
        return resourceContext.getResource(CategoriesResource.class);
    }

    @Path("configuration")
    public ConfigurationResource getConfigurationResource() {
        return resourceContext.getResource(ConfigurationResource.class);
    }

    @Path("dashboards")
    public DashboardsResource getDashboardsResource() {
        return resourceContext.getResource(DashboardsResource.class);
    }

    @Path("groups")
    public GroupsResource getGroupsResource() {
        return resourceContext.getResource(GroupsResource.class);
    }

    @Path("info")
    public InfoResource getInfoResource() {
        return resourceContext.getResource(InfoResource.class);
    }

    @Path("media")
    public MediaResource getMediaResource() {
        return resourceContext.getResource(MediaResource.class);
    }

    @Path("pages")
    public PagesResource getPagesResource() {
        return resourceContext.getResource(PagesResource.class);
    }

    @Path("permissions")
    public PermissionsResource getPermissionsResource() {
        return resourceContext.getResource(PermissionsResource.class);
    }

    @Path("subscriptions")
    public SubscriptionsResource getSubscriptionsResource() {
        return resourceContext.getResource(SubscriptionsResource.class);
    }

    @Path("entrypoints")
    public EntrypointsResource getEntrypointsResource() {
        return resourceContext.getResource(EntrypointsResource.class);
    }

    @Path("theme")
    public ThemeResource getThemeResource() {
        return resourceContext.getResource(ThemeResource.class);
    }

    @Path("tickets")
    public TicketsResource getTicketsResource() {
        return resourceContext.getResource(TicketsResource.class);
    }

    @Path("user")
    public UserResource getUserResource() {
        return resourceContext.getResource(UserResource.class);
    }

    @Path("users")
    public UsersResource getUsersResource() {
        return resourceContext.getResource(UsersResource.class);
    }

    @Path("notifiers")
    public NotifiersResource getNotifiersResource() {
        return resourceContext.getResource(NotifiersResource.class);
    }

    @Path("portal-menu-links")
    public PortalMenuLinksResource getPortalMenuLinksResource() {
        return resourceContext.getResource(PortalMenuLinksResource.class);
    }
}
