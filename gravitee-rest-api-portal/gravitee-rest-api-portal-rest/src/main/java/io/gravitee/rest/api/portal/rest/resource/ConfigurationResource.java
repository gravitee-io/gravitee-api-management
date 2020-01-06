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
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.portal.rest.mapper.ConfigurationMapper;
import io.gravitee.rest.api.portal.rest.mapper.IdentityProviderMapper;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Link.ResourceTypeEnum;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConfigurationResource extends AbstractResource {

    @Autowired
    private ConfigService configService;

    @Autowired
    private PageService pageService;

    @Autowired
    private SocialIdentityProviderService socialIdentityProviderService;

    @Autowired
    private ConfigurationMapper configMapper;

    @Autowired
    private IdentityProviderMapper identityProviderMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalConfiguration() {
        return Response.ok(configMapper.convert(configService.getPortalConfig())).build();
    }

    @GET
    @Path("identities")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalIdentityProviders(@BeanParam PaginationParam paginationParam) {
        List<IdentityProvider> identities = socialIdentityProviderService.findAll().stream()
                .sorted((idp1, idp2) -> String.CASE_INSENSITIVE_ORDER.compare(idp1.getName(), idp2.getName()))
                .map(identityProviderMapper::convert)
                .collect(Collectors.toList());
        return createListResponse(identities, paginationParam);
    }

    @GET
    @Path("identities/{identityProviderId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalIdentityProvider(@PathParam("identityProviderId") String identityProviderId) {
        return Response.ok(identityProviderMapper.convert(socialIdentityProviderService.findById(identityProviderId))).build();
    }

	@GET
    @Path("links")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalLinks() {
        Map<String, List<CategorizedLinks>> portalLinks = new HashMap<>();
        pageService.search(new PageQuery.Builder().type(PageType.SYSTEM_FOLDER).build()).stream()
                .filter(PageEntity::isPublished)
                .forEach(sysPage -> {
                    List<CategorizedLinks> catLinksList = new ArrayList<>();

                    // for pages under sysFolder                    
                    List<Link> links = getLinksFromFolder(sysPage);
                    if(!links.isEmpty()) {
                        CategorizedLinks catLinks = new CategorizedLinks();
                        catLinks.setCategory(sysPage.getName());
                        catLinks.setLinks(links);
                        catLinks.setRoot(true);
                        catLinksList.add(catLinks);
                    }
                    
                    // for pages into folders
                    pageService.search(new PageQuery.Builder().parent(sysPage.getId()).build()).stream()
                        .filter(PageEntity::isPublished)
                        .filter(p -> p.getType().equals("FOLDER"))
                        .forEach(folder -> {
                            List<Link> folderLinks = getLinksFromFolder(folder);
                            if(folderLinks != null && !folderLinks.isEmpty()) {
                                CategorizedLinks catLinks = new CategorizedLinks();
                                catLinks.setCategory(folder.getName());
                                catLinks.setLinks(folderLinks);
                                catLinks.setRoot(false);
                                catLinksList.add(catLinks);
                            }

                        });
                    if(!catLinksList.isEmpty()) {
                        portalLinks.put(sysPage.getName().toLowerCase(), catLinksList);
                    }
                });

        return Response
                .ok(new LinksResponse().slots(portalLinks))
                .build();
    }

    private List<Link> getLinksFromFolder(PageEntity folder) {
        return pageService.search(new PageQuery.Builder().parent(folder.getId()).build()).stream()
                .filter(PageEntity::isPublished)
                .filter(p -> !p.getType().equals("FOLDER"))
                .map(p -> {
                    if("LINK".equals(p.getType())) {
                        Link link = new Link()
                                .name(p.getName())
                                .resourceRef(p.getConfiguration().get("resourceRef"))
                                .resourceType(ResourceTypeEnum.fromValue(p.getConfiguration().get("resourceType")))
                                ;
                        String isFolderConfig = p.getConfiguration().get("isFolder");
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
    
}
