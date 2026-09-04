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

import io.gravitee.apim.core.performance_target.use_case.CreatePerformanceTargetUseCase;
import io.gravitee.apim.core.performance_target.use_case.GetLatestPerformanceTargetEvaluationsByReferencesUseCase;
import io.gravitee.apim.core.performance_target.use_case.GetPerformanceTargetsByReferenceUseCase;
import io.gravitee.apim.core.performance_target.use_case.GetPerformanceTargetsSummaryUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PerformanceTargetMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreatePerformanceTarget;
import io.gravitee.rest.api.management.v2.rest.model.LatestPerformanceTargetEvaluationsRequest;
import io.gravitee.rest.api.management.v2.rest.model.LatestPerformanceTargetEvaluationsResponse;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetEvaluation;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetsResponse;
import io.gravitee.rest.api.management.v2.rest.model.PerformanceTargetsSummary;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;

public class EnvironmentPerformanceTargetsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreatePerformanceTargetUseCase createPerformanceTargetUseCase;

    @Inject
    private GetPerformanceTargetsByReferenceUseCase getPerformanceTargetsByReferenceUseCase;

    @Inject
    private GetPerformanceTargetsSummaryUseCase getPerformanceTargetsSummaryUseCase;

    @Inject
    private GetLatestPerformanceTargetEvaluationsByReferencesUseCase getLatestPerformanceTargetEvaluationsByReferencesUseCase;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.CREATE }) })
    public Response createPerformanceTarget(@Valid @NotNull CreatePerformanceTarget request) {
        var created = createPerformanceTargetUseCase
            .execute(new CreatePerformanceTargetUseCase.Input(PerformanceTargetMapper.INSTANCE.map(request), getAuditInfo()))
            .target();
        return Response.created(uriInfo.getAbsolutePathBuilder().path(created.id()).build())
            .entity(PerformanceTargetMapper.INSTANCE.map(created))
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public PerformanceTargetsResponse getPerformanceTargetsByReference(@QueryParam("reference") @NotEmpty String reference) {
        var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();
        var targets = getPerformanceTargetsByReferenceUseCase
            .execute(new GetPerformanceTargetsByReferenceUseCase.Input(environmentId, reference))
            .targets();
        return new PerformanceTargetsResponse().data(targets.stream().map(PerformanceTargetMapper.INSTANCE::map).toList());
    }

    @GET
    @Path("_summary")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public PerformanceTargetsSummary getPerformanceTargetsSummary() {
        var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();
        var summary = getPerformanceTargetsSummaryUseCase.execute(new GetPerformanceTargetsSummaryUseCase.Input(environmentId)).summary();
        return PerformanceTargetMapper.INSTANCE.map(summary);
    }

    @POST
    @Path("evaluations/_latest")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.READ }) })
    public LatestPerformanceTargetEvaluationsResponse getLatestEvaluations(
        @Valid @NotNull LatestPerformanceTargetEvaluationsRequest request
    ) {
        var environmentId = GraviteeContext.getExecutionContext().getEnvironmentId();
        var latestByReference = getLatestPerformanceTargetEvaluationsByReferencesUseCase
            .execute(new GetLatestPerformanceTargetEvaluationsByReferencesUseCase.Input(environmentId, request.getReferences()))
            .latestByReference();

        var data = new LinkedHashMap<String, PerformanceTargetEvaluation>();
        latestByReference.forEach((reference, evaluation) ->
            data.put(reference, evaluation == null ? null : PerformanceTargetMapper.INSTANCE.map(evaluation))
        );
        return new LatestPerformanceTargetEvaluationsResponse().data(data);
    }

    @Path("{targetId}")
    public EnvironmentPerformanceTargetResource getEnvironmentPerformanceTargetResource() {
        return resourceContext.getResource(EnvironmentPerformanceTargetResource.class);
    }
}
