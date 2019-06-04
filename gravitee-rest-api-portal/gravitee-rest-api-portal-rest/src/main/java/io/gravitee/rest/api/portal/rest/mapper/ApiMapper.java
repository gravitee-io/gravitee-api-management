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
package io.gravitee.rest.api.portal.rest.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApiLinks;
import io.gravitee.rest.api.portal.rest.model.RatingSummary;
import io.gravitee.rest.api.service.EntrypointService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.SubscriptionService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */

@Component
public class ApiMapper {
    
    @Autowired
    private RatingService ratingService;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private EntrypointService entrypointService;
    
    @Autowired
    private ParameterService parameterService;
    
    public Api convert(ApiEntity api) {
        final Api apiItem = new Api();

        apiItem.setDescription(api.getDescription());
        
        List<String> entrypoints = getSpecificEntrypoints(api, entrypointService);
        String defaultEntrypoint = getDefaultEntrypoint(api, parameterService);
        if(defaultEntrypoint != null) {
            entrypoints.add(defaultEntrypoint);
        }
        apiItem.setEntrypoints(entrypoints);
        
        apiItem.setId(api.getId());
        if(api.getLabels() != null) {
            apiItem.setLabels(new ArrayList<String>(api.getLabels()));
        } else {
            apiItem.setLabels(new ArrayList<String>());
        }
        apiItem.setName(api.getName());
        
        apiItem.setPages(null);
        apiItem.setPlans(null);
        
        if (ratingService.isEnabled()) {
            final RatingSummaryEntity ratingSummaryEntity = ratingService.findSummaryByApi(api.getId());
            RatingSummary ratingSummary = new RatingSummary()
                    .average(ratingSummaryEntity.getAverageRate())
                    .count(BigDecimal.valueOf(ratingSummaryEntity.getNumberOfRatings()))
                    ;
            apiItem.setRatingSummary(ratingSummary);
        }
        
        SubscriptionQuery query = new SubscriptionQuery();
        query.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED));
        query.setApi(api.getId());
        Collection<SubscriptionEntity> subscriptions = subscriptionService.search(query);
        apiItem.setSubscribed(subscriptions != null && !subscriptions.isEmpty());
        
        if(api.getTags() != null) {
            apiItem.setTags(new ArrayList<String>(api.getTags()));
        } else {
            apiItem.setTags(new ArrayList<String>());
        }
        apiItem.setVersion(api.getVersion());
        if(api.getViews() != null) {
            apiItem.setViews(new ArrayList<String>(api.getViews()));
        } else {
            apiItem.setViews(new ArrayList<String>());
        }
        
        return apiItem;
    }

    private String getDefaultEntrypoint(ApiEntity api, ParameterService parameterService) {
        String defaultEntrypoint = null;
        List<String> params = parameterService.findAll(Key.PORTAL_ENTRYPOINT);
        if(params != null && !params.isEmpty()) {
            defaultEntrypoint = params.get(0);
            if(api.getProxy() != null) {
                defaultEntrypoint += api.getProxy().getContextPath();
            }
        }
        return defaultEntrypoint;
    }

    private List<String> getSpecificEntrypoints(ApiEntity api, EntrypointService entrypointService) {
        return entrypointService.findAll()
                .stream()
                .map(e -> {
                    if(api.getProxy() != null) {
                        return e.getValue() + api.getProxy().getContextPath();
                    } else {
                        return e.getValue();
                    }
                })
                .collect(Collectors.toList());
    }

    public ApiLinks computeApiLinks(String basePath) {
        ApiLinks apiLinks = new ApiLinks();
        apiLinks.setPages(basePath+"/pages");
        apiLinks.setPicture(basePath+"/picture");
        apiLinks.setPlans(basePath+"/plans");
        apiLinks.setRatings(basePath+"/ratings");
        apiLinks.setSelf(basePath);
        
        return apiLinks;
    }
}
