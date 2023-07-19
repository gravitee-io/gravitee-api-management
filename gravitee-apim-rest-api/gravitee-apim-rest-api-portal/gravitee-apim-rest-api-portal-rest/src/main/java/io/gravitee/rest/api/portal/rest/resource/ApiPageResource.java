/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.portal.rest.mapper.PageMapper;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.portal.rest.utils.HttpHeadersUtil;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ApiPageResource extends AbstractResource {

    private static final String INCLUDE_CONTENT = "content";

    @Inject
    private PageMapper pageMapper;

    @Inject
    private PageService pageService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getPageByApiIdAndPageId(
        @HeaderParam("Accept-Language") String acceptLang,
        @PathParam("apiId") String apiId,
        @PathParam("pageId") String pageId,
        @QueryParam("include") List<String> include
    ) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, apiId);

        if (accessControlService.canAccessApiFromPortal(executionContext, genericApiEntity)) {
            final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);
            PageEntity pageEntity = pageService.findById(pageId, acceptedLocale);
            if (accessControlService.canAccessApiPageFromPortal(executionContext, genericApiEntity, pageEntity)) {
                pageService.transformSwagger(executionContext, pageEntity, genericApiEntity);
                if (!isAuthenticated() && pageEntity.getMetadata() != null) {
                    pageEntity.getMetadata().clear();
                }
                Page page = pageMapper.convert(uriInfo.getBaseUriBuilder(), apiId, pageEntity);
                if (include.contains(INCLUDE_CONTENT)) {
                    page.setContent(pageEntity.getContent());
                }
                page.setLinks(
                    pageMapper.computePageLinks(
                        PortalApiLinkHelper.apiPagesURL(uriInfo.getBaseUriBuilder(), apiId, pageId),
                        PortalApiLinkHelper.apiPagesURL(uriInfo.getBaseUriBuilder(), apiId, page.getParent())
                    )
                );
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
    @RequirePortalAuth
    public Response getPageContentByApiIdAndPageId(@PathParam("apiId") String apiId, @PathParam("pageId") String pageId) {
        final ApiQuery apiQuery = new ApiQuery();
        apiQuery.setIds(Collections.singletonList(apiId));
        GenericApiEntity genericApiEntity = apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), apiId);
        if (accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), genericApiEntity)) {
            PageEntity pageEntity = pageService.findById(pageId, null);
            if (accessControlService.canAccessApiPageFromPortal(GraviteeContext.getExecutionContext(), genericApiEntity, pageEntity)) {
                pageService.transformSwagger(GraviteeContext.getExecutionContext(), pageEntity, genericApiEntity);
                return Response.ok(pageEntity.getContent()).build();
            } else {
                throw new UnauthorizedAccessException();
            }
        }
        throw new ApiNotFoundException(apiId);
    }
}
