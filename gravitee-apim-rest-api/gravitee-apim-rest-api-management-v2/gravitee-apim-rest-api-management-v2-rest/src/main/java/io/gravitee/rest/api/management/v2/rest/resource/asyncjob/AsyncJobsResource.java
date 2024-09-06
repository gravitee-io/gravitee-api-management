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
package io.gravitee.rest.api.management.v2.rest.resource.asyncjob;

import io.gravitee.apim.core.async_job.use_case.ListUserAsyncJobsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.AsyncJobMapper;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJob;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJobStatus;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJobsResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.Optional;

@Path("/environments/{envId}/async-jobs")
public class AsyncJobsResource extends AbstractResource {

    @Inject
    private ListUserAsyncJobsUseCase listUserAsyncJobsUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public AsyncJobsResponse listAsyncJobs(
        @PathParam("envId") String environmentId,
        @BeanParam @Valid PaginationParam paginationParam,
        @QueryParam("type") AsyncJob.TypeEnum type,
        @QueryParam("status") AsyncJobStatus status,
        @QueryParam("sourceId") String sourceId
    ) {
        var jobs = listUserAsyncJobsUseCase
            .execute(
                new ListUserAsyncJobsUseCase.Input(
                    environmentId,
                    getAuthenticatedUser(),
                    Optional.ofNullable(type).map(AsyncJobMapper.INSTANCE::map),
                    Optional.ofNullable(status).map(AsyncJobMapper.INSTANCE::map),
                    Optional.ofNullable(sourceId),
                    Optional.of(new PageableImpl(paginationParam.getPage(), paginationParam.getPerPage()))
                )
            )
            .jobs();

        var totalCount = jobs.getTotalElements();
        var pageItemsCount = Math.toIntExact(jobs.getPageElements());
        return new AsyncJobsResponse()
            .data(AsyncJobMapper.INSTANCE.map(jobs.getContent()))
            .pagination(PaginationInfo.computePaginationInfo(totalCount, pageItemsCount, paginationParam))
            .links(computePaginationLinks(totalCount, paginationParam));
    }
}
