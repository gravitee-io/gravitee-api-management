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

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
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
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ApiSubscriptionResource extends AbstractResource {

    @Inject
    private SubscriptionCrudService subscriptionCrudService;

    @Inject
    private ApplicationCrudService applicationCrudService;

    @Inject
    private PlanCrudService planCrudService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public Response getSubscriptionByHRID(
        @PathParam("apiHrid") String apiHrid,
        @PathParam("hrid") String hrid,
        @QueryParam("legacy") boolean legacy
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        try {
            String subscriptionId = legacy ? hrid : IdBuilder.builder(executionContext, apiHrid).withExtraId(hrid).buildId();
            SubscriptionEntity subscriptionEntity = subscriptionCrudService.get(subscriptionId);

            if (legacy && !subscriptionEntity.getApiId().equals(apiHrid)) {
                throw new SubscriptionNotFoundException(apiHrid);
            }

            SubscriptionState subscriptionState = new SubscriptionState();
            subscriptionState.setId(subscriptionId);
            subscriptionState.setHrid(hrid);
            subscriptionState.setEnvironmentId(executionContext.getEnvironmentId());
            subscriptionState.setOrganizationId(executionContext.getOrganizationId());
            subscriptionState.setApiHrid(apiHrid);

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
        } catch (SubscriptionNotFoundException e) {
            log.debug("Subscription not found for hrid: {}, apiHrid {}, operation: get", hrid, apiHrid);
            throw new HRIDNotFoundException(hrid);
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response deleteSubscriptionByHrid(
        @PathParam("apiHrid") String apiHrid,
        @PathParam("hrid") String hrid,
        @QueryParam("legacyID") boolean legacyID,
        @QueryParam("legacyApiID") boolean legacyApiID
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        try {
            String subscriptionId = legacyID ? hrid : IdBuilder.builder(executionContext, apiHrid).withExtraId(hrid).buildId();
            SubscriptionEntity subscriptionEntity = subscriptionCrudService.get(subscriptionId);

            if (legacyApiID && !subscriptionEntity.getApiId().equals(apiHrid)) {
                throw new SubscriptionNotFoundException(apiHrid);
            }

            subscriptionCrudService.delete(subscriptionEntity.getId());
        } catch (SubscriptionNotFoundException e) {
            log.debug("Subscription not found for hrid: {}, apiHrid {}, operation: delete", hrid, apiHrid);
            throw new HRIDNotFoundException(hrid);
        }
        return Response.noContent().build();
    }
}
