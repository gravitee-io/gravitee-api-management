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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContextCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalPagesDomainService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateDefaultPortalHomepageUseCase {

    private static final String DEFAULT_TEMPLATE_PATH = "templates/default-portal-page.md";

    private final PortalPageContextCrudService portalPageContextCrudService;
    private final PortalPagesDomainService portalPagesDomainService;

    public void execute(String environmentId) {
        var homePageIds = portalPageContextCrudService.findAllIdsByContextTypeAndEnvironmentId(PortalViewContext.HOMEPAGE, environmentId);
        if (homePageIds.isEmpty()) {
            var content = new GraviteeMarkdown(loadDefaultPortalPageContent());
            var page = PortalPage.create(content);
            var view = new PortalPageView(PortalViewContext.HOMEPAGE, true);
            portalPagesDomainService.createWithContext(page, view, environmentId);
        }
    }

    protected String loadDefaultPortalPageContent() {
        try (var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_TEMPLATE_PATH)) {
            if (is == null) {
                throw new IllegalStateException("Could not load default portal page template: " + DEFAULT_TEMPLATE_PATH);
            }
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Could not load default portal page template", e);
        }
    }
}
