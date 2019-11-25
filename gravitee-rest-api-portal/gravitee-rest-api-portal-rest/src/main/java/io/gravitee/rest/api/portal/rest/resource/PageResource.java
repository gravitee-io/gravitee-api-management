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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.portal.rest.mapper.PageMapper;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.List;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageResource extends AbstractResource {

    @Inject
    private PageMapper pageMapper;

    @Inject
    private PageService pageService;

    @Inject
    private GroupService groupService;

    private static final String INCLUDE_CONTENT = "content";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPageByPageId(@PathParam("pageId") String pageId, @QueryParam("include") List<String> include) {

        PageEntity pageEntity = pageService.findById(pageId);

        if (isDisplayable(pageEntity.isPublished(), pageEntity.getExcludedGroups())) {
            if (!isAuthenticated() && pageEntity.getMetadata() != null) {
                pageEntity.getMetadata().clear();
            }
            pageService.transformWithTemplate(pageEntity, null);
            
            Page page = pageMapper.convert(pageEntity);
            
            if (include.contains(INCLUDE_CONTENT)) {
                page.setContent(pageEntity.getContent());
            }
            
            page.setLinks(pageMapper.computePageLinks(
                    PortalApiLinkHelper.pagesURL(uriInfo.getBaseUriBuilder(), pageId),
                    PortalApiLinkHelper.pagesURL(uriInfo.getBaseUriBuilder(), page.getParent())
                    ));
            return Response.ok(page).build();
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    @GET
    @Path("content")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPageContentByPageId(@PathParam("pageId") String pageId) {

        PageEntity pageEntity = pageService.findById(pageId);

        if (isDisplayable(pageEntity.isPublished(), pageEntity.getExcludedGroups())) {
            pageService.transformWithTemplate(pageEntity, null);
            return Response.ok(pageEntity.getContent()).build();
        } else {
            throw new UnauthorizedAccessException();
        }
    }

    private boolean isDisplayable(boolean isPagePublished, List<String> excludedGroups) {
        return isPagePublished
                && groupService.isUserAuthorizedToAccessPortalData(excludedGroups, getAuthenticatedUserOrNull());
    }
}
