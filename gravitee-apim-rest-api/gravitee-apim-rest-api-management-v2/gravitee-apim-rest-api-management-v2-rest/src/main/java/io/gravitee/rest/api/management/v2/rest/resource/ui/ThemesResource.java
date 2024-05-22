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

import io.gravitee.apim.core.theme.use_case.GetThemesUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ThemeMapper;
import io.gravitee.rest.api.management.v2.rest.model.ThemeType;
import io.gravitee.rest.api.management.v2.rest.model.ThemesResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThemesResource extends AbstractResource {

    @Inject
    private GetThemesUseCase getPortalThemesUseCase;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_THEME, acls = { RolePermissionAction.READ }) })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalThemes(
        @BeanParam @Valid PaginationParam paginationParam,
        @QueryParam("type") ThemeType type,
        @QueryParam("enabled") Boolean enabled
    ) {
        var result = getPortalThemesUseCase
            .execute(
                GetThemesUseCase.Input
                    .builder()
                    .type(ThemeMapper.INSTANCE.map(type))
                    .enabled(enabled)
                    .size(paginationParam.getPerPage())
                    .page(paginationParam.getPage())
                    .build()
            )
            .result();
        return Response
            .ok(
                ThemesResponse
                    .builder()
                    .data(ThemeMapper.INSTANCE.map(result.getContent()))
                    .pagination(
                        PaginationInfo.computePaginationInfo(result.getTotalElements(), (int) result.getPageElements(), paginationParam)
                    )
                    .links(computePaginationLinks(result.getTotalElements(), paginationParam))
                    .build()
            )
            .build();
    }
}
