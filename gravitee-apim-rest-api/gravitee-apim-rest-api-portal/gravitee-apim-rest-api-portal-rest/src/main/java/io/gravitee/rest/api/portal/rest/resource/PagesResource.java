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

import io.gravitee.apim.core.portal_page.use_case.GetHomepageUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.portal.rest.mapper.PageMapper;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.PortalHomepage;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.portal.rest.utils.HttpHeadersUtil;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.AccessControlService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PagesResource extends AbstractResource {

    @Inject
    private PageMapper pageMapper;

    @Inject
    private PageService pageService;

    @Inject
    private AccessControlService accessControlService;

    @Inject
    private GetHomepageUseCase getHomepageUseCase;

    @Inject
    private PortalPageRepository portalPageRepository;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getPages(
        @HeaderParam("Accept-Language") String acceptLang,
        @BeanParam PaginationParam paginationParam,
        @QueryParam("homepage") Boolean homepage,
        @QueryParam("parent") String parent
    ) {
        final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        Stream<Page> pageStream = pageService
            .search(
                GraviteeContext.getCurrentEnvironment(),
                new PageQuery.Builder().homepage(homepage).published(true).build(),
                acceptedLocale
            )
            .stream()
            .filter(pageEntity -> accessControlService.canAccessPageFromPortal(executionContext, pageEntity))
            .map(pageMapper::convert)
            .map(this::addPageLink);

        final List<Page> pages;
        if (parent != null) {
            pages = new ArrayList<>();
            final Map<String, Page> pagesMap = pageStream.collect(Collectors.toMap(Page::getId, page -> page));
            pagesMap
                .values()
                .forEach(page -> {
                    List<String> ancestors = this.getAncestors(pagesMap, page);
                    if (ancestors.contains(parent)) {
                        pages.add(page);
                    }
                });
        } else {
            pages = pageStream.collect(Collectors.toList());
        }
        pages.sort(Comparator.comparingInt(Page::getOrder));
        return createListResponse(executionContext, pages, paginationParam);
    }

    private List<String> getAncestors(Map<String, Page> pages, Page page) {
        final List<String> ancestors = new ArrayList<>();
        final String parentId = page.getParent();
        if (parentId == null) {
            return ancestors;
        }
        ancestors.add(parentId);
        final Page parentPage = pages.get(parentId);
        if (parentPage != null) {
            ancestors.addAll(getAncestors(pages, parentPage));
        }

        return ancestors;
    }

    @Path("{pageId}")
    public PageResource getPageResource() {
        return resourceContext.getResource(PageResource.class);
    }

    @GET
    @Path("_homepage")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getHomepage() {
        final String envId = GraviteeContext.getCurrentEnvironment();
        try {
            var output = getHomepageUseCase.execute(new GetHomepageUseCase.Input(envId));
            var pageWithView = output.page();
            if (!pageWithView.viewDetails().published()) {
                return Response.status(404).build();
            }

            var page = pageWithView.page();
            var pageId = page.id().toString();

            String name = null;
            Date createdAt = null;
            Date updatedAt = null;
            try {
                var repoPageOpt = portalPageRepository.findById(pageId);
                if (repoPageOpt.isPresent()) {
                    var repoPage = repoPageOpt.get();
                    name = repoPage.getName();
                    createdAt = repoPage.getCreatedAt();
                    updatedAt = repoPage.getUpdatedAt();
                }
            } catch (Exception e) {
                LOGGER.error("An error occurs while retrieving page information from repository", e);
            }

            PortalHomepage response = new PortalHomepage()
                .setId(pageId)
                .setName(name)
                .setCreatedAt(createdAt)
                .setUpdatedAt(updatedAt)
                .setContent(page.pageContent().content())
                .setType("GRAVITEE_MARKDOWN");

            return Response.ok(response).build();
        } catch (Exception e) {
            LOGGER.error("Page not found", e);
            return Response.status(404).build();
        }
    }

    private Page addPageLink(Page page) {
        return page.links(
            pageMapper.computePageLinks(
                PortalApiLinkHelper.pagesURL(uriInfo.getBaseUriBuilder(), page.getId()),
                PortalApiLinkHelper.pagesURL(uriInfo.getBaseUriBuilder(), page.getParent())
            )
        );
    }
}
