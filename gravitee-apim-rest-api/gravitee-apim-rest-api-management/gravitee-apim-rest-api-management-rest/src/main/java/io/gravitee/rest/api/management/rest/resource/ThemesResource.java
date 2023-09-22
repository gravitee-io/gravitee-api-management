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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.theme.NewThemeEntity;
import io.gravitee.rest.api.model.theme.ThemeEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ThemeService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Themes")
public class ThemesResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ThemeService themeService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_THEME, acls = RolePermissionAction.CREATE) })
    public ThemeEntity createTheme(@Valid @NotNull final NewThemeEntity theme) {
        return themeService.create(GraviteeContext.getExecutionContext(), theme);
    }

    @Path("/current")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_THEME, acls = RolePermissionAction.READ) })
    public ThemeEntity getCurrentTheme() {
        return themeService.findOrCreateDefault(GraviteeContext.getExecutionContext());
    }

    @Path("{themeId}")
    public ThemeResource getThemeResource() {
        return resourceContext.getResource(ThemeResource.class);
    }
}
