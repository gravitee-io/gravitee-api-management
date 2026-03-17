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
package io.gravitee.rest.api.management.v2.rest.resource.user;

import io.gravitee.apim.core.user.use_case.GetUserApisUseCase;
import io.gravitee.apim.core.user.use_case.GetUserApplicationsUseCase;
import io.gravitee.apim.core.user.use_case.GetUserGroupsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.model.UserApi;
import io.gravitee.rest.api.management.v2.rest.model.UserApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.UserApplication;
import io.gravitee.rest.api.management.v2.rest.model.UserApplicationsResponse;
import io.gravitee.rest.api.management.v2.rest.model.UserGroup;
import io.gravitee.rest.api.management.v2.rest.model.UserGroupsResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

public class UserResource extends AbstractResource {

    @Inject
    private GetUserApisUseCase getUserApisUseCase;

    @Inject
    private GetUserApplicationsUseCase getUserApplicationsUseCase;

    @Inject
    private GetUserGroupsUseCase getUserGroupsUseCase;

    @GET
    @Path("/apis")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.READ) })
    public Response getUserApis(
        @PathParam("userId") String userId,
        @BeanParam PaginationParam paginationParam,
        @QueryParam("environmentId") String environmentId
    ) {
        var output = getUserApisUseCase.execute(
            new GetUserApisUseCase.Input(userId, environmentId, paginationParam.getPage(), paginationParam.getPerPage())
        );

        List<UserApi> data = output
            .data()
            .stream()
            .map(api ->
                new UserApi()
                    .id(api.getId())
                    .name(api.getName())
                    .version(api.getVersion())
                    .visibility(api.getVisibility())
                    .environmentId(api.getEnvironmentId())
                    .environmentName(api.getEnvironmentName())
            )
            .toList();

        return Response.ok(
            new UserApisResponse()
                .data(data)
                .pagination(PaginationInfo.computePaginationInfo(output.totalCount(), data.size(), paginationParam))
                .links(computePaginationLinks(output.totalCount(), paginationParam))
        ).build();
    }

    @GET
    @Path("/applications")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.READ) })
    public Response getUserApplications(
        @PathParam("userId") String userId,
        @BeanParam PaginationParam paginationParam,
        @QueryParam("environmentId") String environmentId
    ) {
        var output = getUserApplicationsUseCase.execute(
            new GetUserApplicationsUseCase.Input(userId, environmentId, paginationParam.getPage(), paginationParam.getPerPage())
        );

        List<UserApplication> data = output
            .data()
            .stream()
            .map(app ->
                new UserApplication()
                    .id(app.getId())
                    .name(app.getName())
                    .environmentId(app.getEnvironmentId())
                    .environmentName(app.getEnvironmentName())
            )
            .toList();

        return Response.ok(
            new UserApplicationsResponse()
                .data(data)
                .pagination(PaginationInfo.computePaginationInfo(output.totalCount(), data.size(), paginationParam))
                .links(computePaginationLinks(output.totalCount(), paginationParam))
        ).build();
    }

    @GET
    @Path("/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_USERS, acls = RolePermissionAction.READ) })
    public Response getUserGroups(
        @PathParam("userId") String userId,
        @BeanParam PaginationParam paginationParam,
        @QueryParam("environmentId") String environmentId
    ) {
        var output = getUserGroupsUseCase.execute(
            new GetUserGroupsUseCase.Input(userId, environmentId, paginationParam.getPage(), paginationParam.getPerPage())
        );

        List<UserGroup> data = output
            .data()
            .stream()
            .map(group ->
                new UserGroup()
                    .id(group.getId())
                    .name(group.getName())
                    .environmentId(group.getEnvironmentId())
                    .environmentName(group.getEnvironmentName())
                    .roles(group.getRoles())
            )
            .toList();

        return Response.ok(
            new UserGroupsResponse()
                .data(data)
                .pagination(PaginationInfo.computePaginationInfo(output.totalCount(), data.size(), paginationParam))
                .links(computePaginationLinks(output.totalCount(), paginationParam))
        ).build();
    }
}
