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
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.mapper.PageMapper;
import io.gravitee.rest.api.portal.rest.mapper.PlanMapper;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Link.ResourceTypeEnum;
import io.gravitee.rest.api.portal.rest.utils.HttpHeadersUtil;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiMapper apiMapper;
    @Inject
    private PageMapper pageMapper;
    @Inject
    private PlanMapper planMapper;

    @Inject
    private PageService pageService;
    @Inject
    private PlanService planService;
    @Inject
    private GroupService groupService;

    private static final String INCLUDE_PAGES = "pages";
    private static final String INCLUDE_PLANS = "plans";

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getApiByApiId(@PathParam("apiId") String apiId, @QueryParam("include") List<String> include) {
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {

            ApiEntity apiEntity = apiService.findById(apiId);
            Api api = apiMapper.convert(apiEntity);

            if (include.contains(INCLUDE_PAGES)) {
                List<Page> pages = pageService.search(new PageQuery.Builder().api(apiId).build()).stream()
                        .filter(page -> isDisplayable(apiEntity, page.isPublished(), page.getExcludedGroups()))
                        .map(pageMapper::convert).collect(Collectors.toList());
                api.setPages(pages);
            }
            if (include.contains(INCLUDE_PLANS)) {
                String user = getAuthenticatedUserOrNull();
                List<Plan> plans = planService.findByApi(apiId).stream()
                        .filter(plan -> PlanStatus.PUBLISHED.equals(plan.getStatus()))
                        .filter(plan -> (isAuthenticated() || groupService.isUserAuthorizedToAccessApiData(apiEntity,
                                plan.getExcludedGroups(), getAuthenticatedUserOrNull())))
                        .sorted(Comparator.comparingInt(PlanEntity::getOrder)).map(p -> planMapper.convert(p, user))
                        .collect(Collectors.toList());
                api.setPlans(plans);
            }

            api.links(apiMapper.computeApiLinks(PortalApiLinkHelper.apisURL(uriInfo.getBaseUriBuilder(), api.getId())));

            return Response.ok(api).build();
        }
        throw new ApiNotFoundException(apiId);
    }

    private boolean isDisplayable(ApiEntity api, boolean isPagePublished, List<String> excludedGroups) {
        return (isAuthenticated()) || (pageService.isDisplayable(api, isPagePublished, getAuthenticatedUserOrNull())
                && groupService.isUserAuthorizedToAccessApiData(api, excludedGroups, getAuthenticatedUserOrNull()));

    }

    @GET
    @Path("picture")
    @Produces({ MediaType.WILDCARD, MediaType.APPLICATION_JSON })
    public Response getPictureByApiId(@Context Request request, @PathParam("apiId") String apiId) {
        Collection<ApiEntity> userApis = apiService.findPublishedByUser(getAuthenticatedUserOrNull());
        if (userApis.stream().anyMatch(a -> a.getId().equals(apiId))) {

            InlinePictureEntity image = apiService.getPicture(apiId);

            return createPictureReponse(request, image);
        }
        throw new ApiNotFoundException(apiId);
    }

    @GET
    @Path("links")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiLinks(@HeaderParam("Accept-Language") String acceptLang, @PathParam("apiId") String apiId) {
        final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);
        Map<String, List<CategorizedLinks>> apiLinks = new HashMap<>();
        pageService.search(new PageQuery.Builder().api(apiId).type(PageType.SYSTEM_FOLDER).build(), acceptedLocale).stream()
                .filter(PageEntity::isPublished)
                .forEach(sysPage -> {
                    List<CategorizedLinks> catLinksList = new ArrayList<>();

                    // for pages under sysFolder                    
                    List<Link> links = getLinksFromFolder(sysPage, apiId, acceptedLocale);
                    if(!links.isEmpty()) {
                        CategorizedLinks catLinks = new CategorizedLinks();
                        catLinks.setCategory(sysPage.getName());
                        catLinks.setLinks(links);
                        catLinks.setRoot(true);
                        catLinksList.add(catLinks);
                    }
                    
                    // for pages into folders
                    pageService.search(new PageQuery.Builder().api(apiId).parent(sysPage.getId()).build(), acceptedLocale).stream()
                        .filter(PageEntity::isPublished)
                        .filter(p -> p.getType().equals("FOLDER"))
                        .forEach(folder -> {
                            List<Link> folderLinks = getLinksFromFolder(folder, apiId, acceptedLocale);
                            if(folderLinks != null && !folderLinks.isEmpty()) {
                                CategorizedLinks catLinks = new CategorizedLinks();
                                catLinks.setCategory(folder.getName());
                                catLinks.setLinks(folderLinks);
                                catLinks.setRoot(false);
                                catLinksList.add(catLinks);
                            }

                        });
                    if(!catLinksList.isEmpty()) {
                        apiLinks.put(sysPage.getName().toLowerCase(), catLinksList);
                    }
                });

        return Response
                .ok(new LinksResponse().slots(apiLinks))
                .build();
    }

    private List<Link> getLinksFromFolder(PageEntity folder, String apiId, String acceptedLocale) {
        return pageService.search(new PageQuery.Builder().api(apiId).parent(folder.getId()).build(), acceptedLocale).stream()
                .filter(PageEntity::isPublished)
                .filter(p -> !p.getType().equals("FOLDER"))
                .map(p -> {
                    if("LINK".equals(p.getType())) {
                        Link link = new Link()
                                .name(p.getName())
                                .resourceRef(p.getContent())
                                .resourceType(ResourceTypeEnum.fromValue(p.getConfiguration().get(PageConfigurationKeys.LINK_RESOURCE_TYPE)))
                                ;
                        String isFolderConfig = p.getConfiguration().get(PageConfigurationKeys.LINK_IS_FOLDER);
                        if(isFolderConfig != null && !isFolderConfig.isEmpty()) {
                            link.setFolder(Boolean.valueOf(isFolderConfig));
                        }

                        return link;
                    } else {
                        return new Link()
                                .name(p.getName())
                                .resourceRef(p.getId())
                                .resourceType(ResourceTypeEnum.PAGE)
                                ;
                    }
                })
                .collect(Collectors.toList());
    }
    
    @Path("metrics")
    public ApiMetricsResource getApiMetricsResource() {
        return resourceContext.getResource(ApiMetricsResource.class);
    }

    @Path("pages")
    public ApiPagesResource getApiPagesResource() {
        return resourceContext.getResource(ApiPagesResource.class);
    }

    @Path("plans")
    public ApiPlansResource getApiPlansResource() {
        return resourceContext.getResource(ApiPlansResource.class);
    }

    @Path("ratings")
    public ApiRatingsResource getRatingResource() {
        return resourceContext.getResource(ApiRatingsResource.class);
    }

    @Path("subscribers")
    public ApiSubscribersResource getApiSubscribersResource() {
        return resourceContext.getResource(ApiSubscribersResource.class);
    }
}
