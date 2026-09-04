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
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationAgent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.definition.model.v4.ApiType;
import java.util.ArrayList;
import java.util.List;
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
    private static final String AGENT_OVERVIEW_TEMPLATE = "agent-overview-page-content.md";

    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationItemDomainService portalNavigationItemDomainService;
    private final PortalPageContentCrudService portalPageContentCrudService;
    private final ApiCrudService apiCrudService;

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

        var hasChildPage = portalNavigationItemsQueryService
            .findByParentIdAndEnvironmentId(environmentId, navigationItemId)
            .stream()
            .anyMatch(PortalNavigationPage.class::isInstance);
        if (hasChildPage) {
            return false;
        }

        var content = portalPageContentCrudService.create(
            GraviteeMarkdownPageContent.create(organizationId, environmentId, loadContent(templatePath))
        );

        portalNavigationItemDomainService.create(
            organizationId,
            environmentId,
            CreatePortalNavigationItem.builder()
                .title(DEFAULT_PAGE_TITLE)
                .type(PortalNavigationItemType.PAGE)
                .area(navigationItem.getArea())
                .parentId(navigationItemId)
                .portalPageContentId(content.getId())
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .order(0)
                .published(false)
                .visibility(navigationItem.getVisibility())
                .build()
        );

        return true;
    }

    private String resolveTemplatePath(PortalNavigationItem navigationItem) {
        if (navigationItem instanceof PortalNavigationApi apiNavigationItem) {
            return apiCrudService
                .findById(apiNavigationItem.getApiId())
                .filter(api -> ApiType.MCP_PROXY == api.getType())
                .map(api -> MCP_PROXY_OVERVIEW_TEMPLATE)
                .orElse(DEFAULT_OVERVIEW_TEMPLATE);
        }
        if (navigationItem instanceof PortalNavigationApiProduct) {
            return API_PRODUCT_OVERVIEW_TEMPLATE;
        }
        // An agent is backed by an API, so it gets an overview page like one — worded for an agent consumer.
        if (navigationItem instanceof PortalNavigationAgent agentNavigationItem) {
            return apiCrudService
                .findById(agentNavigationItem.getAgentId())
                .filter(api -> ApiType.MCP_PROXY == api.getType())
                .map(api -> MCP_PROXY_OVERVIEW_TEMPLATE)
                .orElse(AGENT_OVERVIEW_TEMPLATE);
        }
        return null;
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
