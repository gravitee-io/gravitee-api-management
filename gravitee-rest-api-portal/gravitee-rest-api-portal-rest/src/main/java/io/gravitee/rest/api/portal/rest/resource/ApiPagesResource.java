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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.portal.rest.mapper.PageMapper;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPagesResource extends AbstractResource {

    @Inject
    private PageMapper pageMapper;
    
    @Inject
    private PageService pageService;

    @Inject
    private GroupService groupService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPagesByApiId(@PathParam("apiId") String apiId, @BeanParam PaginationParam paginationParam, @QueryParam("homepage") Boolean homepage, @QueryParam("parent") String parent) {
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        if (userApis.stream().anyMatch(a->a.getId().equals(apiId))) {
            
            final ApiEntity apiEntity = apiService.findById(apiId);
            List<Page> pages = pageService
                    .search(new PageQuery.Builder()
                            .api(apiId)
                            .homepage(homepage)
                            .parent(parent)
                            .build())
                    .stream()
                    .filter(pageEntity -> isDisplayable(apiEntity, pageEntity.isPublished(), pageEntity.getExcludedGroups()))
                    .map(pageMapper::convert)
                    .map(pageResult -> pageResult.content(null))
                    .collect(Collectors.toList());
            
            return createListResponse(pages, paginationParam);
        }
        throw new ApiNotFoundException(apiId);
    }

    @Path("{pageId}")
    public ApiPageResource getApiPageResource() {
        return resourceContext.getResource(ApiPageResource.class);
    }

    private boolean isDisplayable(ApiEntity api, boolean isPagePublished, List<String> excludedGroups) {
        return pageService.isDisplayable(api, isPagePublished, getAuthenticatedUserOrNull()) &&
                        groupService.isUserAuthorizedToAccessApiData(api, excludedGroups, getAuthenticatedUserOrNull());

    }
}
