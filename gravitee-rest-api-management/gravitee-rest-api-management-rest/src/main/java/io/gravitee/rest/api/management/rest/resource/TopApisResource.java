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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.NewTopApiEntity;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.UpdateTopApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.annotations.Api;

import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Top APIs"})
public class TopApisResource extends AbstractResource  {

    @Context
    private UriInfo uriInfo;

    @Autowired
    private TopApiService topApiService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_TOP_APIS, acls = RolePermissionAction.READ)
    })
    public List<TopApiEntity> list()  {
        return topApiService.findAll().stream()
                .peek(addPictureUrl())
                .collect(toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_TOP_APIS, acls = RolePermissionAction.CREATE)
    })
    public List<TopApiEntity> create(@Valid @NotNull final NewTopApiEntity topApi) {
        return topApiService.create(topApi).stream()
                .peek(addPictureUrl())
                .collect(toList());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_TOP_APIS, acls = RolePermissionAction.UPDATE)
    })
    public List<TopApiEntity> update(@Valid @NotNull final List<UpdateTopApiEntity> topApis) {
        return topApiService.update(topApis).stream()
                .peek(addPictureUrl())
                .collect(toList());
    }

    @Path("{topAPI}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.PORTAL_TOP_APIS, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("topAPI") String topAPI) {
        topApiService.delete(topAPI);
    }

    private Consumer<TopApiEntity> addPictureUrl() {
        return topApiEntity -> {
            final UriBuilder ub = uriInfo.getBaseUriBuilder();
            final UriBuilder uriBuilder = ub.path("organizations").path(GraviteeContext.getCurrentOrganization())
                    .path("environments").path(GraviteeContext.getCurrentEnvironment())
                    .path("apis").path(topApiEntity.getApi()).path("picture");
            topApiEntity.setPictureUrl(uriBuilder.build().toString());
        };
    }
}
