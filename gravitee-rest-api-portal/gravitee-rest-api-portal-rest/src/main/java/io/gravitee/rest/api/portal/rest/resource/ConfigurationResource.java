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
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PageConfigurationKeys;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.configuration.application.ApplicationGrantTypeEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypesEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.portal.rest.mapper.ConfigurationMapper;
import io.gravitee.rest.api.portal.rest.mapper.IdentityProviderMapper;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Link.ResourceTypeEnum;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.utils.HttpHeadersUtil;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.Hook;
import io.swagger.annotations.ApiOperation;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
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
    @Autowired
    private ApplicationTypeService applicationTypeService;

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
    public Response getPortalLinks(@HeaderParam("Accept-Language") String acceptLang) {
        final String acceptedLocale = HttpHeadersUtil.getFirstAcceptedLocaleName(acceptLang);
        Map<String, List<CategorizedLinks>> portalLinks = new HashMap<>();
        pageService.search(new PageQuery.Builder().type(PageType.SYSTEM_FOLDER).build(), acceptedLocale).stream()
                .filter(PageEntity::isPublished)
                .forEach(sysPage -> {
                    List<CategorizedLinks> catLinksList = new ArrayList<>();

                    // for pages under sysFolder
                    List<Link> links = getLinksFromFolder(sysPage, acceptedLocale);
                    if (!links.isEmpty()) {
                        CategorizedLinks catLinks = new CategorizedLinks();
                        catLinks.setCategory(sysPage.getName());
                        catLinks.setLinks(links);
                        catLinks.setRoot(true);
                        catLinksList.add(catLinks);
                    }

                    // for pages into folders
                    pageService.search(new PageQuery.Builder().parent(sysPage.getId()).build(), acceptedLocale).stream()
                            .filter(PageEntity::isPublished)
                            .filter(p -> p.getType().equals("FOLDER"))
                            .forEach(folder -> {
                                List<Link> folderLinks = getLinksFromFolder(folder, acceptedLocale);
                                if (folderLinks != null && !folderLinks.isEmpty()) {
                                    CategorizedLinks catLinks = new CategorizedLinks();
                                    catLinks.setCategory(folder.getName());
                                    catLinks.setLinks(folderLinks);
                                    catLinks.setRoot(false);
                                    catLinksList.add(catLinks);
                                }

                            });
                    if (!catLinksList.isEmpty()) {
                        portalLinks.put(sysPage.getName().toLowerCase(), catLinksList);
                    }
                });

        return Response
                .ok(new LinksResponse().slots(portalLinks))
                .build();
    }

    private List<Link> getLinksFromFolder(PageEntity folder, String acceptedLocale) {
        return pageService.search(new PageQuery.Builder().parent(folder.getId()).build(), acceptedLocale).stream()
                .filter(PageEntity::isPublished)
                .filter(p -> !p.getType().equals("FOLDER"))
                .map(p -> {
                    if ("LINK".equals(p.getType())) {
                        String relatedPageId = p.getContent();
                        Link link = new Link()
                                .name(p.getName())
                                .resourceRef(relatedPageId)
                                .resourceType(ResourceTypeEnum.fromValue(p.getConfiguration().get(PageConfigurationKeys.LINK_RESOURCE_TYPE)));
                        String isFolderConfig = p.getConfiguration().get(PageConfigurationKeys.LINK_IS_FOLDER);
                        if (isFolderConfig != null && !isFolderConfig.isEmpty()) {
                            link.setFolder(Boolean.valueOf(isFolderConfig));
                        }

                        return link;
                    } else {
                        return new Link()
                                .name(p.getName())
                                .resourceRef(p.getId())
                                .resourceType(ResourceTypeEnum.PAGE);
                    }
                })
                .collect(Collectors.toList());
    }

    @GET
    @Path("applications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEnabledApplicationTypes() {
        try {
            ApplicationTypesEntity enabledApplicationTypes = applicationTypeService.getEnabledApplicationTypes();
            return Response
                    .ok(convert(enabledApplicationTypes))
                    .build();
        } catch (TechnicalException e) {
            return Response.ok().build();
        }
    }

    private ConfigurationApplicationsResponse convert(ApplicationTypesEntity enabledApplicationTypes) {
        ConfigurationApplicationsResponse configurationApplicationsResponse = new ConfigurationApplicationsResponse();
        List<ApplicationType> types = enabledApplicationTypes.getData().stream().map(applicationTypeEntity -> {

            ApplicationType applicationType = new ApplicationType();
            applicationType.setAllowedGrantTypes(convert(applicationTypeEntity.getAllowed_grant_types()));
            applicationType.setDefaultGrantTypes(convert(applicationTypeEntity.getDefault_grant_types()));
            applicationType.setMandatoryGrantTypes(convert(applicationTypeEntity.getMandatory_grant_types()));
            applicationType.setId(applicationTypeEntity.getId());
            applicationType.setName(applicationTypeEntity.getName());
            applicationType.setDescription(applicationTypeEntity.getDescription());
            applicationType.setRequiresRedirectUris(applicationTypeEntity.getRequires_redirect_uris());
            return applicationType;
        }).collect(Collectors.toList());

        configurationApplicationsResponse.setData(types);
        return configurationApplicationsResponse;
    }

    private List<ApplicationGrantType> convert(List<ApplicationGrantTypeEntity> allowedGrantTypes) {

        return allowedGrantTypes.stream().map(applicationGrantTypeEntity -> {
            ApplicationGrantType applicationGrantType = new ApplicationGrantType();
            applicationGrantType.setCode(applicationGrantTypeEntity.getCode());
            applicationGrantType.setName(applicationGrantTypeEntity.getName());
            applicationGrantType.setType(applicationGrantTypeEntity.getType());
            applicationGrantType.setResponsesTypes(applicationGrantTypeEntity.getResponses_types());
            return applicationGrantType;
        }).collect(Collectors.toList());
    }

}
