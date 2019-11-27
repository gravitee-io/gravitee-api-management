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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.SubscriptionInvalidException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private SubscriptionService subscriptionService;
    
    @Inject
    private SubscriptionMapper subscriptionMapper;
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSubscription(@Valid @NotNull(message = "Input must not be null.") SubscriptionInput subscriptionInput) {
        if(hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, subscriptionInput.getApplication(), RolePermissionAction.CREATE)) {
            NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
            newSubscriptionEntity.setApplication(subscriptionInput.getApplication());
            newSubscriptionEntity.setPlan(subscriptionInput.getPlan());
            newSubscriptionEntity.setRequest(subscriptionInput.getRequest());
            
            SubscriptionEntity createdSubscription = subscriptionService.create(newSubscriptionEntity);
            
            return Response
                    .ok(subscriptionMapper.convert(createdSubscription))
                    .build();
        }
        throw new ForbiddenAccessException();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubscriptions(@QueryParam("apiId") String apiId, @QueryParam("applicationId") String applicationId, @BeanParam PaginationParam paginationParam) {
        if(apiId == null && applicationId == null) {
            throw new SubscriptionInvalidException();
        }
        if((applicationId == null && !hasPermission(RolePermission.API_SUBSCRIPTION, apiId, RolePermissionAction.READ)) 
                || (apiId == null && !hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, applicationId, RolePermissionAction.READ))
                || (!hasPermission(RolePermission.API_SUBSCRIPTION, apiId, RolePermissionAction.READ) && !hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, applicationId, RolePermissionAction.READ))
            ) {
            throw new ForbiddenAccessException();
        }
        
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApi(apiId);
        query.setApplication(applicationId);
        
        final Page<SubscriptionEntity> pagedSubscriptions = subscriptionService.search(query, new PageableImpl(paginationParam.getPage(), paginationParam.getSize()));
        
        List<Subscription> subscriptionList = pagedSubscriptions.getContent()
                .stream()
                .map(subscriptionMapper::convert)
                .collect(Collectors.toList());
        
        
        //No pagination, because subscriptionService did it already
        return createListResponse(subscriptionList, paginationParam, false);
    }

    @Path("{subscriptionId}")
    public SubscriptionResource getSubscriptionResource() {
        return resourceContext.getResource(SubscriptionResource.class);
    }
    
}
