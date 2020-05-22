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
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.mapper.PageMapper;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.utils.HttpHeadersUtil;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.Collection;
import java.util.List;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPageResource extends AbstractResource {

    @Inject
    private PageMapper pageMapper;

    @Inject
    private PageService pageService;

    @Inject
    private GroupService groupService;

    private static final String INCLUDE_CONTENT = "content";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPageByApiIdAndPageId(
            @HeaderParam("Accept-Language") String acceptLang,
            @PathParam("apiId") String apiId,
            @PathParam("pageId") String pageId,
            @QueryParam("include") List<String> include) {
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {
            final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);
            final ApiEntity apiEntity = apiService.findById(apiId);

            PageEntity pageEntity = pageService.findById(pageId, acceptedLocale);
            pageService.transformSwagger(pageEntity, apiId);

            if (isDisplayable(apiEntity, pageEntity.getExcludedGroups(), pageEntity.getType())) {
                
                if (!isAuthenticated() && pageEntity.getMetadata() != null) {
                    pageEntity.getMetadata().clear();
                }
                
                Page page = pageMapper.convert(pageEntity);
                
                if (include.contains(INCLUDE_CONTENT)) {
                    page.setContent(pageEntity.getContent());
                }
                
                page.setLinks(pageMapper.computePageLinks(
                        PortalApiLinkHelper.apiPagesURL(uriInfo.getBaseUriBuilder(), apiId, pageId),
                        PortalApiLinkHelper.apiPagesURL(uriInfo.getBaseUriBuilder(), apiId, page.getParent())
                        ));
                return Response.ok(page).build();
            } else {
                throw new UnauthorizedAccessException();
            }
        }
        throw new ApiNotFoundException(apiId);
    }

    @GET
    @Path("content")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPageContentByApiIdAndPageId(@PathParam("apiId") String apiId,
            @PathParam("pageId") String pageId) {
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {

            final ApiEntity apiEntity = apiService.findById(apiId);

            PageEntity pageEntity = pageService.findById(pageId, null);
            pageService.transformSwagger(pageEntity, apiId);

            if (isDisplayable(apiEntity, pageEntity.getExcludedGroups(), pageEntity.getType())) {
                return Response.ok(pageEntity.getContent()).build();
            } else {
                throw new UnauthorizedAccessException();
            }
        }
        throw new ApiNotFoundException(apiId);
    }

    private boolean isDisplayable(ApiEntity api, List<String> excludedGroups, String pageType) {
        return groupService.isUserAuthorizedToAccessApiData(api, excludedGroups, getAuthenticatedUserOrNull())
                && !"SYSTEM_FOLDER".equals(pageType);

    }
}
