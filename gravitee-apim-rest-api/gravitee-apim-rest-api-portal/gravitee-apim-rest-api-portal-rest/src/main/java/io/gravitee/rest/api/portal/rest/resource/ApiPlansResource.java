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
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.portal.rest.mapper.PlanMapper;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.portal.rest.utils.HttpHeadersUtil;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPlansResource extends AbstractResource {

    @Inject
    private PlanMapper planMapper;

    @Inject
    private PlanService planService;

    @Inject
    private GroupService groupService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiPlansByApiId(@PathParam("apiId") String apiId, @BeanParam PaginationParam paginationParam) {
        String username = getAuthenticatedUserOrNull();

        final ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(Collections.singletonList(apiId));

        Collection<ApiEntity> userApis = apiService.findPublishedByUser(username, apiQuery);
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {
            ApiEntity apiEntity = apiService.findById(apiId);

            if (Visibility.PUBLIC.equals(apiEntity.getVisibility()) || hasPermission(API_PLAN, apiId, READ)) {
                List<Plan> plans = planService
                    .findByApi(apiId)
                    .stream()
                    .filter(plan -> PlanStatus.PUBLISHED.equals(plan.getStatus()))
                    .filter(plan -> groupService.isUserAuthorizedToAccessApiData(apiEntity, plan.getExcludedGroups(), username))
                    .sorted(Comparator.comparingInt(PlanEntity::getOrder))
                    .map(p -> planMapper.convert(p))
                    .collect(Collectors.toList());

                return createListResponse(plans, paginationParam);
            } else {
                return createListResponse(emptyList(), paginationParam);
            }
        }
        throw new ApiNotFoundException(apiId);
    }
}
