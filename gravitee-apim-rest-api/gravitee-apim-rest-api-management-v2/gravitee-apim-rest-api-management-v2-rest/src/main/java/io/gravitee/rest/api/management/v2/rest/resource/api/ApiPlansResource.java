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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static java.util.Comparator.comparingInt;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.use_case.CreatePlanUseCase;
import io.gravitee.apim.core.plan.use_case.UpdateFederatedPlanUseCase;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.rest.api.management.v2.rest.mapper.FlowMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.PlanMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreateGenericPlan;
import io.gravitee.rest.api.management.v2.rest.model.CreatePlanV2;
import io.gravitee.rest.api.management.v2.rest.model.CreatePlanV4;
import io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.model.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.model.PlansResponse;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGenericPlan;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanFederated;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanV2;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanV4;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPlansResource extends AbstractResource {

    private final PlanMapper planMapper = PlanMapper.INSTANCE;
    private final FlowMapper flowMapper = FlowMapper.INSTANCE;

    @Inject
    private PlanService planServiceV4;

    @Inject
    private CreatePlanUseCase createPlanUseCase;

    @Inject
    private io.gravitee.rest.api.service.PlanService planServiceV2;

    @Inject
    private PlanSearchService planSearchService;

    @Inject
    private SubscriptionQueryService subscriptionQueryService;

    @Inject
    private UpdateFederatedPlanUseCase updateFederatedPlanUseCase;

    @PathParam("apiId")
    private String apiId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.READ }) })
    public PlansResponse getApiPlans(
        @QueryParam("statuses") @DefaultValue("PUBLISHED") final Set<PlanStatus> statuses,
        @QueryParam("securities") @Nonnull Set<PlanSecurityType> securities,
        @QueryParam("mode") PlanMode planMode,
        @QueryParam("subscribableBy") String subscribableBy,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        var planQuery = PlanQuery
            .builder()
            .apiId(apiId)
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

        Stream<GenericPlanEntity> plansStream = planSearchService
            .search(GraviteeContext.getExecutionContext(), planQuery.build(), getAuthenticatedUser(), isAdmin())
            .stream()
            .sorted(comparingInt(GenericPlanEntity::getOrder))
            .map(this::filterSensitiveData);

        if (subscribableBy != null) {
            var subscriptions = subscriptionQueryService.findActiveByApplicationIdAndApiId(subscribableBy, apiId);
            var subscribedPlans = subscriptions.stream().map(SubscriptionEntity::getPlanId).toList();

            plansStream =
                plansStream.filter(plan ->
                    (
                        plan.getPlanSecurity() == null ||
                        !List
                            .of(
                                io.gravitee.rest.api.model.v4.plan.PlanSecurityType.KEY_LESS.getLabel(),
                                PlanSecurityType.KEY_LESS.getValue()
                            )
                            .contains(plan.getPlanSecurity().getType())
                    ) &&
                    (subscribedPlans.isEmpty() || !subscribedPlans.contains(plan.getId()))
                );
        }

        List<GenericPlanEntity> plans = plansStream.toList();
        List<GenericPlanEntity> paginationData = computePaginationData(plans, paginationParam);

        return new PlansResponse()
            .data(planMapper.convert(paginationData))
            .pagination(PaginationInfo.computePaginationInfo((long) plans.size(), paginationData.size(), paginationParam))
            .links(computePaginationLinks(plans.size(), paginationParam));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.CREATE }) })
    public Response createApiPlan(@Valid @NotNull CreateGenericPlan createPlan) {
        if (createPlan.getDefinitionVersion() == DefinitionVersion.V4) {
            var planV4 = (CreatePlanV4) createPlan;
            var executionContext = GraviteeContext.getExecutionContext();
            var userDetails = getAuthenticatedUserDetails();
            var output = createPlanUseCase.execute(
                new CreatePlanUseCase.Input(
                    apiId,
                    api -> planMapper.map(planV4, api),
                    flowMapper.mapToHttpV4(planV4.getFlows()),
                    AuditInfo
                        .builder()
                        .organizationId(executionContext.getOrganizationId())
                        .environmentId(executionContext.getEnvironmentId())
                        .actor(
                            AuditActor
                                .builder()
                                .userId(userDetails.getUsername())
                                .userSource(userDetails.getSource())
                                .userSourceId(userDetails.getSourceId())
                                .build()
                        )
                        .build()
                )
            );

            return Response.created(this.getLocationHeader(output.id())).entity(planMapper.map(output.plan())).build();
        } else if (createPlan.getDefinitionVersion() == DefinitionVersion.V2) {
            final io.gravitee.rest.api.model.NewPlanEntity newPlanEntity = planMapper.map((CreatePlanV2) createPlan);
            newPlanEntity.setApi(apiId);
            newPlanEntity.setType(io.gravitee.rest.api.model.PlanType.API);

            final io.gravitee.rest.api.model.PlanEntity planEntity = planServiceV2.create(
                GraviteeContext.getExecutionContext(),
                newPlanEntity
            );
            return Response.created(this.getLocationHeader(planEntity.getId())).entity(planMapper.map(planEntity)).build();
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(planInvalid()).build();
    }

    @GET
    @Path("/{planId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.READ }) })
    public Response getApiPlan(@PathParam("planId") String planId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findById(executionContext, planId);

        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        return Response.ok(planMapper.mapGenericPlan(planEntity)).build();
    }

    @PUT
    @Path("/{planId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response updateApiPlan(@PathParam("planId") String planId, @Valid @NotNull UpdateGenericPlan updatePlan) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findById(executionContext, planId);

        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        return switch (updatePlan.getDefinitionVersion()) {
            case V4 -> {
                if (planEntity instanceof PlanEntity) {
                    final UpdatePlanEntity updatePlanEntity = planMapper.map((UpdatePlanV4) updatePlan);
                    updatePlanEntity.setId(planId);
                    PlanEntity responseEntity = planServiceV4.update(executionContext, updatePlanEntity);
                    yield Response.ok(planMapper.map(responseEntity)).build();
                } else {
                    yield Response.status(Response.Status.BAD_REQUEST).entity(planInvalid(planId)).build();
                }
            }
            case FEDERATED -> {
                var toUpdate = planMapper.map((UpdatePlanFederated) updatePlan);
                toUpdate.setId(planId);

                var result = updateFederatedPlanUseCase.execute(new UpdateFederatedPlanUseCase.Input(toUpdate, getAuditInfo()));

                yield Response.ok(planMapper.mapFederated(result.updated())).build();
            }
            case V2 -> {
                if (planEntity instanceof io.gravitee.rest.api.model.PlanEntity) {
                    final io.gravitee.rest.api.model.UpdatePlanEntity updatePlanEntity = planMapper.map((UpdatePlanV2) updatePlan);
                    updatePlanEntity.setId(planId);
                    io.gravitee.rest.api.model.PlanEntity responseEntity = planServiceV2.update(executionContext, updatePlanEntity);
                    yield Response.ok(planMapper.map(responseEntity)).build();
                } else {
                    yield Response.status(Response.Status.BAD_REQUEST).entity(planInvalid(planId)).build();
                }
            }
            default -> Response.status(Response.Status.BAD_REQUEST).entity(planInvalid(planId)).build();
        };
    }

    @DELETE
    @Path("/{planId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.DELETE }) })
    public Response deleteApiPlan(@PathParam("planId") String planId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findById(executionContext, planId);

        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        planServiceV4.delete(executionContext, planId);

        return Response.noContent().build();
    }

    @POST
    @Path("/{planId}/_close")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response closeApiPlan(@PathParam("planId") String planId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findById(executionContext, planId);

        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        return Response.ok(planMapper.mapGenericPlan(planServiceV4.close(executionContext, planId))).build();
    }

    @POST
    @Path("/{planId}/_publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response publishApiPlan(@PathParam("planId") String planId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findById(executionContext, planId);

        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        if (planEntity instanceof PlanEntity) {
            return Response.ok(planMapper.map(planServiceV4.publish(executionContext, planId))).build();
        }

        return Response.ok(planMapper.map(planServiceV2.publish(executionContext, planId))).build();
    }

    @POST
    @Path("/{planId}/_deprecate")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PLAN, acls = { RolePermissionAction.UPDATE }) })
    public Response deprecateApiPlan(@PathParam("planId") String planId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GenericPlanEntity planEntity = planSearchService.findById(executionContext, planId);

        if (!planEntity.getApiId().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(planNotFoundError(planId)).build();
        }

        if (planEntity instanceof PlanEntity) {
            return Response.ok(planMapper.map(planServiceV4.deprecate(executionContext, planId))).build();
        }

        return Response.ok(planMapper.map(planServiceV2.deprecate(executionContext, planId))).build();
    }

    private GenericPlanEntity filterSensitiveData(GenericPlanEntity entity) {
        if (
            hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_GATEWAY_DEFINITION,
                entity.getApiId(),
                RolePermissionAction.READ
            ) &&
            hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_PLAN, entity.getApiId(), RolePermissionAction.READ)
        ) {
            // Return complete information if user has permission.
            return entity;
        }

        PlanEntity filtered = new PlanEntity();

        filtered.setId(entity.getId());
        filtered.setCharacteristics(entity.getCharacteristics());
        filtered.setName(entity.getName());
        filtered.setDescription(entity.getDescription());
        filtered.setOrder(entity.getOrder());
        filtered.setMode(entity.getPlanMode());
        filtered.setSecurity(entity.getPlanSecurity());
        filtered.setType(filtered.getType());
        filtered.setValidation(filtered.getValidation());
        filtered.setCommentRequired(entity.isCommentRequired());
        filtered.setCommentMessage(entity.getCommentMessage());
        filtered.setGeneralConditions(entity.getGeneralConditions());

        return filtered;
    }

    private Error planNotFoundError(String plan) {
        return new Error()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message("Plan [" + plan + "] cannot be found.")
            .putParametersItem("plan", plan)
            .technicalCode("plan.notFound");
    }

    private Error planInvalid() {
        return new Error()
            .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
            .message("Plan is not valid.")
            .technicalCode("plan.invalid");
    }

    private Error planInvalid(String plan) {
        return new Error()
            .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
            .message("Plan [" + plan + "] is not valid.")
            .putParametersItem("plan", plan)
            .technicalCode("plan.invalid");
    }
}
