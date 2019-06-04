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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.mapper.PlanMapper;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.model.PlansResponse;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPlansResource extends AbstractResource {

    @Context
    private UriInfo uriInfo;
    
    @Inject
    private PlanMapper planMapper;
    
    @Inject
    private PlanService planService;

    @Inject
    private GroupService groupService;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiPlansByApiId(@PathParam("apiId") String apiId, @DefaultValue(PAGE_QUERY_PARAM_DEFAULT) @QueryParam("page") Integer page, @DefaultValue(SIZE_QUERY_PARAM_DEFAULT) @QueryParam("size") Integer size, @QueryParam("status") String status) {
        ApiEntity apiEntity = apiService.findById(apiId);

        if (Visibility.PUBLIC.equals(apiEntity.getVisibility())) {
            
            List<Plan> plans = planService.findByApi(apiId).stream()
                    .filter(plan -> (
                            (status!=null && status.equalsIgnoreCase(plan.getStatus().name())) ||
                            status==null
                    ))
                    .filter(plan -> (
                            isAuthenticated() || 
                            groupService.isUserAuthorizedToAccessApiData(apiEntity, plan.getExcludedGroups(), getAuthenticatedUserOrNull())
                    ))
                    .sorted(Comparator.comparingInt(PlanEntity::getOrder))
                    .map(planMapper::convert)
                    .collect(Collectors.toList());
            
            int totalItems = plans.size();

            plans = this.paginateResultList(plans, page, size);
            
            PlansResponse response = new PlansResponse()
                    .data(plans)
                    .links(this.computePaginatedLinks(uriInfo, page, size, totalItems))
                    ;
            
            return Response.ok(response).build();
        }

        throw new ForbiddenAccessException();
    }
}
