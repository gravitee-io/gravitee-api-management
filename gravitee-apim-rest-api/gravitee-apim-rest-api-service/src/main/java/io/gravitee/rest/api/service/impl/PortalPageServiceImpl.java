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
package io.gravitee.rest.api.service.impl;

import io.gravitee.apim.core.portal_page.crud_service.PortalPageContextCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageCrudService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.rest.api.service.PortalPageService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PortalPageServiceImpl implements PortalPageService {

    private static final String DEFAULT_PORTAL_PAGE_NAME = "Default Portal Page";

    private String defaultPortalPageContent;
    private final PortalPageCrudService portalPageCrudService;
    private final PortalPageContextCrudService portalPageContextCrudService;

    public PortalPageServiceImpl(
        @Lazy PortalPageCrudService portalPageCrudService,
        @Lazy PortalPageContextCrudService portalPageContextCrudService
    ) {
        this.portalPageCrudService = portalPageCrudService;
        this.portalPageContextCrudService = portalPageContextCrudService;
    }

    @Override
    public void createDefaultPortalHomePage(String environmentId) {
        var homePageIds = portalPageContextCrudService.findAllIdsByContextTypeAndEnvironmentId(PortalViewContext.HOMEPAGE, environmentId);
        if (homePageIds.isEmpty()) {
            var createdPage = portalPageCrudService.create(PortalPage.create(new GraviteeMarkdown(getDefaultPortalPageContent())));
            portalPageContextCrudService.create(createdPage.getId(), new PortalPageView(PortalViewContext.HOMEPAGE, true), environmentId);
        }
    }

    private String getDefaultPortalPageContent() {
        if (defaultPortalPageContent == null) {
            try {
                var resource = new ClassPathResource("templates/default-portal-page.md");
                defaultPortalPageContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Could not load default portal page template", e);
            }
        }
        return defaultPortalPageContent;
    }
}
