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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.use_case.CreateApiProductPlanUseCase;
import io.gravitee.apim.core.plan.use_case.GetPlansUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiProductPlanMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiProductPlansResponse;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiProductPlan;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.model.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;

@CustomLog
public class ApiProductPlansResource extends AbstractResource {

    private final ApiProductPlanMapper planMapper = ApiProductPlanMapper.INSTANCE;

    @Inject
    private CreateApiProductPlanUseCase createApiProductPlanUseCase;

    @Inject
    private GetPlansUseCase getPlansUseCase;

    @PathParam("apiProductId")
    private String apiProductId;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_PLAN, acls = { RolePermissionAction.READ }) })
    public ApiProductPlansResponse getApiProductPlans(
        @QueryParam("statuses") @DefaultValue("PUBLISHED") final Set<PlanStatus> statuses,
        @QueryParam("securities") @Nonnull Set<PlanSecurityType> securities,
        @QueryParam("mode") PlanMode planMode,
        @QueryParam("subscribableBy") String subscribableBy,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        log.debug(
            "Getting Plans for API Product {} (statuses={}, securities={}, mode={}, page={}, perPage={})",
            apiProductId,
            statuses,
            securities,
            planMode,
            paginationParam.getPage(),
            paginationParam.getPerPage()
        );
        if (planMode == null) {
            planMode = PlanMode.STANDARD;
        }
        var planQuery = PlanQuery.builder()
            .referenceId(apiProductId)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .securityType(
                securities
                    .stream()
                    .map(planSecurityType -> io.gravitee.rest.api.model.v4.plan.PlanSecurityType.valueOf(planSecurityType.name()))
                    .collect(Collectors.toList())
            )
            .status(
                statuses
                    .stream()
                    .map(planStatus -> io.gravitee.definition.model.v4.plan.PlanStatus.valueOf(planStatus.name()))
                    .collect(Collectors.toList())
            )
            .mode(planMode);

        var output = getPlansUseCase.execute(
            GetPlansUseCase.Input.forList(
                apiProductId,
                GenericPlanEntity.ReferenceType.API_PRODUCT.name(),
                getAuthenticatedUser(),
                isAdmin(),
                planQuery.build(),
                subscribableBy
            )
        );

        log.debug("Found {} Plans for API Product {} (before pagination)", output.plans().size(), apiProductId);
        List<Plan> paginationData = computePaginationData(output.plans(), paginationParam);
        log.debug("Returning {} Plans for API Product {} (after pagination)", paginationData.size(), apiProductId);
        return new ApiProductPlansResponse()
            .data(planMapper.convert(paginationData))
            .pagination(PaginationInfo.computePaginationInfo(output.plans().size(), paginationData.size(), paginationParam))
            .links(computePaginationLinks(output.plans().size(), paginationParam));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_PLAN, acls = { RolePermissionAction.CREATE }) })
    public Response createApiProductPlan(@Valid @NotNull CreateApiProductPlan createPlan) {
        log.debug("Creating Plan for API Product {}", apiProductId);
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();
        if (PlanSecurityType.KEY_LESS.equals(createPlan.getSecurity().getType())) {
            return Response.status(Response.Status.BAD_REQUEST).entity(planSecurityInvalid()).build();
        }
        var output = createApiProductPlanUseCase.execute(
            new CreateApiProductPlanUseCase.Input(
                apiProductId,
                apiProduct -> planMapper.map(createPlan),
                AuditInfo.builder()
                    .organizationId(executionContext.getOrganizationId())
                    .environmentId(executionContext.getEnvironmentId())
                    .actor(
                        AuditActor.builder()
                            .userId(userDetails.getUsername())
                            .userSource(userDetails.getSource())
                            .userSourceId(userDetails.getSourceId())
                            .build()
                    )
                    .build()
            )
        );
        log.debug("Plan {} created for API Product {}", output.id(), apiProductId);
        return Response.created(this.getLocationHeader(output.id())).entity(planMapper.map(output.plan())).build();
    }

    @Path("{planId}")
    public ApiProductPlanResource getApiProductPlanResource() {
        log.debug("Getting ApiProductPlanResource for API Product {}", apiProductId);
        return resourceContext.getResource(ApiProductPlanResource.class);
    }

    private io.gravitee.rest.api.management.v2.rest.model.Error planSecurityInvalid() {
        return new Error()
            .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
            .message("Plan Security Type KeyLess is not allowed.")
            .technicalCode("planSecurity.invalid");
    }
}
