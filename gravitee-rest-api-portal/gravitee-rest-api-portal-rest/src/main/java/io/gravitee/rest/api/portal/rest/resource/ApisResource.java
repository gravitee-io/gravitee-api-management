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

import static io.gravitee.rest.api.model.Visibility.PUBLIC;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED;
import static java.util.Collections.singletonList;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.RatingSummaryEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.CategoryApiQuery;
import io.gravitee.rest.api.portal.rest.resource.param.ApisParam;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TopApiService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;
    
    @Inject
    private ApiMapper apiMapper;
    
    @Inject
    private ApplicationService applicationService;
    
    @Inject
    private SubscriptionService subscriptionService;
    
    @Inject
    private RatingService ratingService;

    @Inject
    private TopApiService topApiService;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApis(@BeanParam PaginationParam paginationParam, @BeanParam ApisParam apisParam) {
        final ApiQuery apiQuery = new ApiQuery();
        apiQuery.setLifecycleStates(singletonList(PUBLISHED));
        apiQuery.setContextPath(apisParam.getContextPath());
        apiQuery.setLabel(apisParam.getLabel());
        apiQuery.setName(apisParam.getName());
        apiQuery.setTag(apisParam.getTag());
        apiQuery.setVersion(apisParam.getVersion());
        apiQuery.setView(apisParam.getView());
        
        Collection<ApiEntity> apis;
        if (isAuthenticated()) {
            apis = apiService.findByUser(getAuthenticatedUser(), apiQuery);
        } else {
            apiQuery.setVisibility(PUBLIC);
            apis = apiService.search(apiQuery);
        }

        FilteredApi filteredApis = filterByCategory(apis, apisParam.getCategory());
        
        List<Api> apisList= filteredApis.getFilteredApis().stream()
                .map(apiMapper::convert)
                .map(this::addApiLinks)
                .collect(Collectors.toList());
        
        return createListResponse(apisList, paginationParam, uriInfo, filteredApis.getMetadata());
    }

    private FilteredApi filterByCategory(Collection<ApiEntity> apis, CategoryApiQuery category) {
        if(category != null) {
            switch (category) {
                case MINE:
                    if (isAuthenticated()) {
                        return getCurrentUserSubscribedApis(apis);
                    }
                    break;
                    
                case STARRED:
                    if(ratingService.isEnabled()) {
                        return getRatedApis(apis);
                    }
                    break;
                    
                case TRENDINGS:
                    return getApisOrderByNumberOfSubscriptions(apis);
                
                case FEATURED:
                    return getTopApis(apis);
                    
                default:
                    break;
            }
        }
        
        //No category was applied but at least, the list is ordered
        return new FilteredApi(
                apis.stream()
                .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                .collect(Collectors.toList())
                ,null
                );
    }

    private FilteredApi getTopApis(Collection<ApiEntity> apis) {
        List<String> topApiIdList = topApiService.findAll().stream().map(TopApiEntity::getApi).collect(Collectors.toList());
        return new FilteredApi(
                apis.stream()
                    .filter(api->topApiIdList.contains(api.getId()))
                    .sorted((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()))
                    .collect(Collectors.toList())
                , null
                );
    }

    protected FilteredApi getApisOrderByNumberOfSubscriptions(Collection<ApiEntity> apis) {
        //find all subscribed apis
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApis(apis.stream().map(ApiEntity::getId).collect(Collectors.toList()));
        
        //group by apis
        Map<String, Long> subscribedApiWithCount = subscriptionService.search(subscriptionQuery).stream()
            .collect(Collectors.groupingBy(SubscriptionEntity::getApi, Collectors.counting()));
        
        //link an api with its nb of subscritions
        Map<ApiEntity, Long> apisWithCount = new HashMap<>();
        Map<String,  Map<String, String>> apisMetadata = new HashMap<>();
        Map<String, String> subscriptionsMetadata = new HashMap<>();
        apisMetadata.put("subscriptions", subscriptionsMetadata);
        apis.forEach(api -> {
            Long apiSubscriptionsCount = subscribedApiWithCount.get(api.getId());
            if(apiSubscriptionsCount != null) {
                //creation of a map which will be sorted to retrieve apis in the right order
                apisWithCount.put(api, apiSubscriptionsCount);
                
                //creation of a metadata map
                subscriptionsMetadata.put(api.getId(), apiSubscriptionsCount.toString());
            }
        });
        
        //order the list
        return new FilteredApi(
                apisWithCount.entrySet().stream()
                  .sorted(Map.Entry.<ApiEntity, Long>comparingByValue().reversed()
                          .thenComparing(Map.Entry.<ApiEntity, Long>comparingByKey(Comparator.comparing(ApiEntity::getName))))
                  .map(Map.Entry::getKey)
                  .collect(Collectors.toList())
                , apisMetadata);
        
    }

    protected FilteredApi getRatedApis(Collection<ApiEntity> apis) {
        //keep apis with ratings
        Map<ApiEntity, RatingSummaryEntity> ratings = new HashMap<>();
        //APIPortal: should create a specific service to retrieve all the information in one call to the repository
        apis.forEach(api-> {
            RatingSummaryEntity apiRatingSummary = ratingService.findSummaryByApi(api.getId());
            if(apiRatingSummary != null && apiRatingSummary.getNumberOfRatings()>0) {
                ratings.put(api, apiRatingSummary);
            }
        });
        
        //sort apis by ratings, nb of ratings, and name
        return new FilteredApi(
                ratings.entrySet().stream()
                  .sorted( (e1, e2) -> {
                    RatingSummaryEntity o1 = e1.getValue();
                    RatingSummaryEntity o2 = e2.getValue();
                    int averageRateComparaison = Double.compare(o2.getAverageRate(), o1.getAverageRate());
                    if(averageRateComparaison != 0) {
                        return averageRateComparaison;
                    }
                    int nbRatingsComparaison = Integer.compare(o2.getNumberOfRatings(), o1.getNumberOfRatings());
                    if(nbRatingsComparaison != 0) {
                        return nbRatingsComparaison;
                    }
                    return String.CASE_INSENSITIVE_ORDER.compare(e1.getKey().getName(), e2.getKey().getName());
                  })
                  .map(Map.Entry::getKey)
                  .collect(Collectors.toList())
                , null);
    }

    protected FilteredApi getCurrentUserSubscribedApis(Collection<ApiEntity> apis) {
        //get Current user applications
        List<String> currentUserApplicationsId = applicationService.findByUser(getAuthenticatedUser()).stream().map(ApplicationListItem::getId).collect(Collectors.toList());
        
        //find all subscribed apis for these applications
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApplications(currentUserApplicationsId);
        List<String> subscribedApis = subscriptionService.search(subscriptionQuery).stream().map(SubscriptionEntity::getApi).distinct().collect(Collectors.toList());
        
        //filter apis list with subscribed apis list
        return new FilteredApi(
                apis.stream()
                .filter(api-> subscribedApis.contains(api.getId()))
                .sorted((a1,a2) -> String.CASE_INSENSITIVE_ORDER.compare(a1.getName(), a2.getName()))
                .collect(Collectors.toList())
                , null);
    }

    private Api addApiLinks(Api api) {
        String basePath = uriInfo.getAbsolutePathBuilder().path(api.getId()).build().toString();
        return api.links(apiMapper.computeApiLinks(basePath));
    }
    
    
    @Path("{apiId}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }
    
    private class FilteredApi {
        Collection<ApiEntity> filteredApis;
        Map<String, Map<String, String>> metadata;
        
        
        public FilteredApi(Collection<ApiEntity> filteredApis, Map<String, Map<String, String>> metadata) {
            super();
            this.filteredApis = filteredApis;
            this.metadata = metadata;
        }
        
        public Collection<ApiEntity> getFilteredApis() {
            return filteredApis;
        }
        public Map<String, Map<String, String>> getMetadata() {
            return metadata;
        }
    }
}
