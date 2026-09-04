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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.apim.core.performance_target.use_case.DeletePerformanceTargetUseCase;
import io.gravitee.apim.core.performance_target.use_case.EvaluatePerformanceTargetUseCase;
import io.gravitee.apim.core.performance_target.use_case.GetLatestPerformanceTargetEvaluationUseCase;
import io.gravitee.apim.core.performance_target.use_case.GetPerformanceTargetEvaluationHistoryUseCase;
import io.gravitee.apim.core.performance_target.use_case.GetPerformanceTargetUseCase;
import io.gravitee.apim.core.performance_target.use_case.UpdatePerformanceTargetUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PerformanceTargetMapper;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTarget;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetEvaluation;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetEvaluationsResponse;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePerformanceTarget;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class EnvironmentPerformanceTargetResource extends AbstractResource {

    @PathParam("targetId")
    private String targetId;

    @Inject
    private GetPerformanceTargetUseCase getPerformanceTargetUseCase;

    @Inject
    private UpdatePerformanceTargetUseCase updatePerformanceTargetUseCase;

    @Inject
    private DeletePerformanceTargetUseCase deletePerformanceTargetUseCase;

    @Inject
    private GetLatestPerformanceTargetEvaluationUseCase getLatestPerformanceTargetEvaluationUseCase;

    @Inject
    private GetPerformanceTargetEvaluationHistoryUseCase getPerformanceTargetEvaluationHistoryUseCase;

    @Inject
    private EvaluatePerformanceTargetUseCase evaluatePerformanceTargetUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public PerformanceTarget getPerformanceTarget() {
        var target = getPerformanceTargetUseCase.execute(new GetPerformanceTargetUseCase.Input(environmentId(), targetId)).target();
        return PerformanceTargetMapper.INSTANCE.map(target);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.UPDATE }) })
    public PerformanceTarget updatePerformanceTarget(@Valid @NotNull UpdatePerformanceTarget request) {
        var updated = updatePerformanceTargetUseCase
            .execute(new UpdatePerformanceTargetUseCase.Input(environmentId(), targetId, PerformanceTargetMapper.INSTANCE.map(request)))
            .target();
        return PerformanceTargetMapper.INSTANCE.map(updated);
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.DELETE }) })
    public Response deletePerformanceTarget() {
        deletePerformanceTargetUseCase.execute(new DeletePerformanceTargetUseCase.Input(environmentId(), targetId));
        return Response.noContent().build();
    }

    @POST
    @Path("_evaluate")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.UPDATE }) })
    public PerformanceTargetEvaluation evaluatePerformanceTarget() {
        var evaluation = evaluatePerformanceTargetUseCase
            .execute(new EvaluatePerformanceTargetUseCase.Input(environmentId(), targetId))
            .evaluation();
        return PerformanceTargetMapper.INSTANCE.map(evaluation);
    }

    @GET
    @Path("evaluations/latest")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public PerformanceTargetEvaluation getLatestEvaluation() {
        var latest = getLatestPerformanceTargetEvaluationUseCase
            .execute(new GetLatestPerformanceTargetEvaluationUseCase.Input(environmentId(), targetId))
            .evaluation();
        return PerformanceTargetMapper.INSTANCE.map(latest);
    }

    @GET
    @Path("evaluations")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public PerformanceTargetEvaluationsResponse getEvaluations(@BeanParam @Valid PaginationParam paginationParam) {
        var page = getPerformanceTargetEvaluationHistoryUseCase
            .execute(
                new GetPerformanceTargetEvaluationHistoryUseCase.Input(
                    environmentId(),
                    targetId,
                    new PageableImpl(paginationParam.getPage(), paginationParam.getPerPage())
                )
            )
            .evaluations();

        return new PerformanceTargetEvaluationsResponse()
            .data(page.getContent().stream().map(PerformanceTargetMapper.INSTANCE::map).toList())
            .pagination(
                PaginationInfo.computePaginationInfo(page.getTotalElements(), Math.toIntExact(page.getPageElements()), paginationParam)
            )
            .links(computePaginationLinks(page.getTotalElements(), paginationParam));
    }

    private static String environmentId() {
        return GraviteeContext.getExecutionContext().getEnvironmentId();
    }
}
