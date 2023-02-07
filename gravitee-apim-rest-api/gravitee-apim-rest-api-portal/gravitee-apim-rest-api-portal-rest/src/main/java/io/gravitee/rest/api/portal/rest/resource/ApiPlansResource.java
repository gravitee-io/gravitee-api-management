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

import static io.gravitee.rest.api.model.permissions.RolePermission.API_PLAN;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static java.util.Collections.emptyList;

import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.portal.rest.mapper.PlanMapper;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPlansResource extends AbstractResource {

    @Inject
    private PlanMapper planMapper;

    @Inject
    private PlanSearchService planSearchService;

    @Inject
    private GroupService groupService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiPlansByApiId(@PathParam("apiId") String apiId, @BeanParam PaginationParam paginationParam) {
        String username = getAuthenticatedUserOrNull();

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), apiId)) {
            GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, apiId);

            if (Visibility.PUBLIC.equals(genericApiEntity.getVisibility()) || hasPermission(executionContext, API_PLAN, apiId, READ)) {
                List<Plan> plans = planSearchService
                    .findByApi(executionContext, apiId)
                    .stream()
                    .filter(plan -> PlanStatus.PUBLISHED.equals(plan.getPlanStatus()))
                    .filter(plan -> groupService.isUserAuthorizedToAccessApiData(genericApiEntity, plan.getExcludedGroups(), username))
                    .sorted(Comparator.comparingInt(GenericPlanEntity::getOrder))
                    .map(p -> planMapper.convert(p))
                    .collect(Collectors.toList());

                return createListResponse(executionContext, plans, paginationParam);
            } else {
                return createListResponse(executionContext, emptyList(), paginationParam);
            }
        }
        throw new ApiNotFoundException(apiId);
    }
}
