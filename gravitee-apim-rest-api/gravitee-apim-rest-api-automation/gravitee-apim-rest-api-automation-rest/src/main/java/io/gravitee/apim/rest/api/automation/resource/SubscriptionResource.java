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
package io.gravitee.apim.rest.api.automation.resource;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.model.SubscriptionState;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionResource extends AbstractResource {

    @Inject
    private SubscriptionCrudService subscriptionCrudService;

    @Inject
    private ApiCrudService apiCrudService;

    @Inject
    private ApplicationCrudService applicationCrudService;

    @Inject
    private PlanCrudService planCrudService;

    @PathParam("hrid")
    private String hrid;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public Response getSubscriptionByHRID(@QueryParam("legacy") boolean legacy) {
        var executionContext = GraviteeContext.getExecutionContext();
        try {
            SubscriptionState subscriptionState = new SubscriptionState();
            subscriptionState.setHrid(hrid);
            subscriptionState.setEnvironmentId(executionContext.getEnvironmentId());
            subscriptionState.setOrganizationId(executionContext.getOrganizationId());

            SubscriptionEntity subscriptionEntity = subscriptionCrudService.get(
                legacy ? hrid : IdBuilder.builder(executionContext, hrid).buildId()
            );
            subscriptionState.setId(subscriptionEntity.getId());

            Api api = apiCrudService.get(subscriptionEntity.getApiId());
            subscriptionState.setApiHrid(api.getHrid());

            BaseApplicationEntity application = applicationCrudService.findById(
                subscriptionEntity.getApplicationId(),
                executionContext.getEnvironmentId()
            );
            subscriptionState.setApplicationHrid(application.getHrid());

            Plan plan = planCrudService.getById(subscriptionEntity.getPlanId());
            subscriptionState.setPlanHrid(plan.getHrid());

            subscriptionState.setStartingAt(subscriptionEntity.getStartingAt().toOffsetDateTime());
            subscriptionState.setEndingAt(
                subscriptionEntity.getEndingAt() != null ? subscriptionEntity.getEndingAt().toOffsetDateTime() : null
            );
            return Response.ok(subscriptionState).build();
        } catch (TechnicalManagementException e) {
            throw new HRIDNotFoundException(hrid);
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response deleteSubscriptionByHrid(@QueryParam("legacy") boolean legacy) {
        var executionContext = GraviteeContext.getExecutionContext();

        try {
            SubscriptionEntity subscriptionEntity = subscriptionCrudService.get(
                legacy ? hrid : IdBuilder.builder(executionContext, hrid).buildId()
            );
            subscriptionCrudService.delete(subscriptionEntity.getId());
        } catch (TechnicalDomainException e) {
            throw new HRIDNotFoundException(hrid);
        }
        return Response.noContent().build();
    }
}
