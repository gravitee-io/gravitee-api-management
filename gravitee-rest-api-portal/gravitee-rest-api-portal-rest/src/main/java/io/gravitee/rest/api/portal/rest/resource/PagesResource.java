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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.portal.rest.mapper.PageMapper;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.PagesResponse;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PageService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PagesResource extends AbstractResource {

    @Context
    private UriInfo uriInfo;
    
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
    public Response getPages(@DefaultValue(PAGE_QUERY_PARAM_DEFAULT) @QueryParam("page") Integer page, @DefaultValue(SIZE_QUERY_PARAM_DEFAULT) @QueryParam("size") Integer size, @QueryParam("homepage") Boolean homepage, @QueryParam("root") Boolean root, @QueryParam("parent") String parent) {

            List<Page> pages = pageService
                    .search(new PageQuery.Builder()
                            .homepage(homepage)
                            .parent(parent)
                            .rootParent(root)
                            .build())
                    .stream()
                    .filter(pageEntity -> isDisplayable(pageEntity.isPublished(), pageEntity.getExcludedGroups()))
                    .map(pageMapper::convert)
                    .map(pageResult -> pageResult.content(null))
                    .collect(Collectors.toList());
            
            int totalItems = pages.size();
            
            pages = this.paginateResultList(pages, page, size);
            
            PagesResponse response = new PagesResponse()
                    .data(pages)
                    .links(this.computePaginatedLinks(uriInfo, page, size, totalItems))
                    ;
            
            return Response.ok(response).build();
    }

    @Path("{pageId}")
    public PageResource getPageResource() {
        return resourceContext.getResource(PageResource.class);
    }

    private boolean isDisplayable(boolean isPagePublished, List<String> excludedGroups) {
        return isPagePublished
                && groupService.isUserAuthorizedToAccessPortalData(excludedGroups, getAuthenticatedUserOrNull());

    }
}
