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
package io.gravitee.rest.api.management.v2.rest.resource.ui;

import io.gravitee.apim.core.theme.use_case.UpdateThemeUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ThemeMapper;
import io.gravitee.rest.api.management.v2.rest.model.UpdateTheme;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ThemeUpdateInvalidException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThemeResource extends AbstractResource {

    @Inject
    private UpdateThemeUseCase updateThemeUseCase;

    @GET
    public Response hehe(@PathParam("themeId") String themeId) {
        return Response.ok(themeId + " hello").build();
    }

    @PUT
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_THEME, acls = { RolePermissionAction.UPDATE }) })
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTheme(@PathParam("themeId") String themeId, @Valid @NotNull final UpdateTheme updateTheme) {
        var mapped = ThemeMapper.INSTANCE.map(updateTheme);
        if (!Objects.equals(themeId, mapped.getId())) {
            throw new ThemeUpdateInvalidException(themeId, updateTheme);
        }
        var result = updateThemeUseCase
            .execute(UpdateThemeUseCase.Input.builder().updateTheme(mapped).executionContext(GraviteeContext.getExecutionContext()).build())
            .result();
        return Response.ok(ThemeMapper.INSTANCE.map(result)).build();
    }
}
