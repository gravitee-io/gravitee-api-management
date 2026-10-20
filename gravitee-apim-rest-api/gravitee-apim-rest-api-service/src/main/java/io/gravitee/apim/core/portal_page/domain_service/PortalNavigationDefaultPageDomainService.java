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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.definition.model.v4.ApiType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
@CustomLog
public class PortalNavigationDefaultPageDomainService {

    private static final String DEFAULT_PAGE_TITLE = "Overview";
    private static final String DEFAULT_OVERVIEW_TEMPLATE = "api-overview-page-content.md";
    private static final String MCP_PROXY_OVERVIEW_TEMPLATE = "api-overview-mcp-proxy-page-content.md";
    private static final String API_PRODUCT_OVERVIEW_TEMPLATE = "api-product-overview-page-content.md";

    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationItemDomainService portalNavigationItemDomainService;
    private final PortalPageContentCrudService portalPageContentCrudService;
    private final ApiCrudService apiCrudService;
    private final PortalNavigationItemCreationExpansionDomainService creationExpansionDomainService;
    private final PortalNavigationItemValidatorService validatorService;
    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;

    public List<PortalNavigationItemId> seedDefaultPages(
        String organizationId,
        String environmentId,
        List<PortalNavigationItemId> navigationItemIds
    ) {
        var seededNavigationItemIds = new ArrayList<PortalNavigationItemId>();

        for (var navigationItemId : navigationItemIds) {
            try {
                if (seedDefaultPage(organizationId, environmentId, navigationItemId)) {
                    seededNavigationItemIds.add(navigationItemId);
                }
            } catch (Exception e) {
                log.warn("Skipping default page seed for portal navigation item {} in environment {}", navigationItemId, environmentId, e);
            }
        }

        return List.copyOf(seededNavigationItemIds);
    }

    private boolean seedDefaultPage(String organizationId, String environmentId, PortalNavigationItemId navigationItemId) {
        var navigationItem = portalNavigationItemsQueryService.findByIdAndEnvironmentId(environmentId, navigationItemId);
        var templatePath = resolveTemplatePath(navigationItem);
        if (templatePath == null) {
            return false;
        }

        var candidate = CreatePortalNavigationItem.builder()
            .title(DEFAULT_PAGE_TITLE)
            .type(PortalNavigationItemType.PAGE)
            .area(navigationItem.getArea())
            .parentId(navigationItemId)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .order(0)
            .published(false)
            .visibility(navigationItem.getVisibility())
            .build();
        var normalized = creationExpansionDomainService.expand(List.of(candidate), environmentId).itemsToCreate().getFirst();
        if (hasDocumentation(environmentId, navigationItemId, normalized.getReference())) {
            return false;
        }

        createWithTemplate(organizationId, environmentId, normalized, templatePath);
        return true;
    }

    /**
     * Gives an API the Overview page, unpublished, for the author to review before publishing the API. The template
     * is kept as written: the portal fills in the API's details each time it displays the page.
     */
    public PortalNavigationPage createApiOverviewPage(String organizationId, String environmentId, String apiId) {
        var candidate = apiOwnedNavigationDomainService.claimForApi(
            environmentId,
            apiId,
            CreatePortalNavigationItem.builder()
                .title(DEFAULT_PAGE_TITLE)
                .type(PortalNavigationItemType.PAGE)
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .order(0)
                .visibility(PortalVisibility.PUBLIC)
                .build()
        );
        var normalized = creationExpansionDomainService.expand(List.of(candidate), environmentId).itemsToCreate().getFirst();
        return (PortalNavigationPage) createWithTemplate(organizationId, environmentId, normalized, apiOverviewTemplatePath(apiId));
    }

    private PortalNavigationItem createWithTemplate(
        String organizationId,
        String environmentId,
        CreatePortalNavigationItem page,
        String templatePath
    ) {
        var content = GraviteeMarkdownPageContent.create(organizationId, environmentId, loadContent(templatePath));
        var toCreate = page.toBuilder().portalPageContentId(content.getId()).build();

        validatorService.validateOne(toCreate, environmentId, Set.of(content.getId()), Set.of());
        portalPageContentCrudService.create(content);
        return portalNavigationItemDomainService.create(organizationId, environmentId, toCreate);
    }

    private boolean hasDocumentation(String environmentId, PortalNavigationItemId navigationItemId, NavigationItemReference reference) {
        if (reference instanceof NavigationItemReference.ApiReference api) {
            return hasApiDocumentation(environmentId, api.apiId());
        }
        return hasLocalDocumentation(environmentId, navigationItemId);
    }

    private boolean hasApiDocumentation(String environmentId, String apiId) {
        if (
            apiOwnedNavigationDomainService
                .findOwnedItems(environmentId, apiId)
                .stream()
                .anyMatch(PortalNavigationDefaultPageDomainService::isPageOrLink)
        ) {
            return true;
        }
        return apiOwnedNavigationDomainService
            .findStandaloneListings(environmentId, apiId)
            .stream()
            .anyMatch(listing -> hasLocalDocumentation(environmentId, listing.getId()));
    }

    private boolean hasLocalDocumentation(String environmentId, PortalNavigationItemId navigationItemId) {
        var toVisit = new ArrayDeque<PortalNavigationItemId>();
        var visited = new HashSet<PortalNavigationItemId>();
        toVisit.add(navigationItemId);
        while (!toVisit.isEmpty()) {
            var parentId = toVisit.pop();
            if (!visited.add(parentId)) {
                continue;
            }
            for (var child : portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(environmentId, parentId)) {
                if (!(child.getReference() instanceof NavigationItemReference.PortalReference)) {
                    continue;
                }
                if (isPageOrLink(child)) {
                    return true;
                }
                if (child instanceof PortalNavigationFolder) {
                    toVisit.push(child.getId());
                }
            }
        }
        return false;
    }

    private static boolean isPageOrLink(PortalNavigationItem item) {
        return item instanceof PortalNavigationPage || item instanceof PortalNavigationLink;
    }

    private String resolveTemplatePath(PortalNavigationItem navigationItem) {
        if (navigationItem instanceof PortalNavigationApi apiNavigationItem) {
            return apiOverviewTemplatePath(apiNavigationItem.getApiId());
        }
        if (navigationItem instanceof PortalNavigationApiProduct) {
            return API_PRODUCT_OVERVIEW_TEMPLATE;
        }
        return null;
    }

    private String apiOverviewTemplatePath(String apiId) {
        return apiCrudService
            .findById(apiId)
            .filter(api -> ApiType.MCP_PROXY == api.getType())
            .map(api -> MCP_PROXY_OVERVIEW_TEMPLATE)
            .orElse(DEFAULT_OVERVIEW_TEMPLATE);
    }

    private String loadContent(String contentPath) {
        try (
            var inputStream = PortalNavigationDefaultPageDomainService.class.getClassLoader().getResourceAsStream(
                String.format("templates/%s", contentPath)
            )
        ) {
            if (inputStream == null) {
                throw new IllegalStateException(String.format("Could not load default portal page template for %s", contentPath));
            }
            return new String(inputStream.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Could not load default portal page template", e);
        }
    }
}
