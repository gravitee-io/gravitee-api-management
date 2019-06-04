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

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;
import io.gravitee.rest.api.portal.rest.model.SubscriptionsResponse;
import io.gravitee.rest.api.service.SubscriptionService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;
    
    @Inject
    private SubscriptionService subscriptionService;
    
    @Inject
    private SubscriptionMapper subscriptionMapper;
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSubscription(@Valid SubscriptionInput subscriptionInput) {
        NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
        newSubscriptionEntity.setApplication(subscriptionInput.getApplication());
        newSubscriptionEntity.setPlan(subscriptionInput.getPlan());
        newSubscriptionEntity.setRequest(subscriptionInput.getRequest());
        
        SubscriptionEntity createdSubscription = subscriptionService.create(newSubscriptionEntity);
        
        return Response
                .ok(subscriptionMapper.convert(createdSubscription))
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubscriptions(@QueryParam("api") String api, @QueryParam("application") String application, @DefaultValue(PAGE_QUERY_PARAM_DEFAULT) @QueryParam("page") Integer page, @DefaultValue(SIZE_QUERY_PARAM_DEFAULT) @QueryParam("size") Integer size) {
        
        //APIPortal: check if currentUuser is allowed to manage this application / api ?
        
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApi(api);
        query.setApplication(application);
        
        final Page<SubscriptionEntity> pagedSubscriptions = subscriptionService.search(query, new PageableImpl(page, size));
        
        List<Subscription> subscriptionList = pagedSubscriptions.getContent()
                .stream()
                .map(subscriptionMapper::convert)
                .collect(Collectors.toList());
        
        
        int totalItems = subscriptionList.size();
        
        subscriptionList = this.paginateResultList(subscriptionList, page, size);

        SubscriptionsResponse subscriptionResponse = new SubscriptionsResponse()
                .data(subscriptionList)
                .links(this.computePaginatedLinks(uriInfo, page, size, totalItems))
                ;
        
        return Response
                .ok(subscriptionResponse)
                .build();
    }

    @Path("{subscriptionId}")
    public SubscriptionResource getSubscriptionResource() {
        return resourceContext.getResource(SubscriptionResource.class);
    }
    
}
