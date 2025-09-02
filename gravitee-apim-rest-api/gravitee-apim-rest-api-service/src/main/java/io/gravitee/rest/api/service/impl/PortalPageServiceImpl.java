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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import io.gravitee.rest.api.service.PortalPageService;
import io.gravitee.rest.api.service.common.UuidString;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PortalPageServiceImpl implements PortalPageService {

    private static final String DEFAULT_PORTAL_PAGE_NAME = "Default Portal Page";

    private String defaultPortalPageContent;
    private final PortalPageRepository portalPageRepository;
    private final PortalPageContextRepository portalPageContextRepository;

    public PortalPageServiceImpl(
        @Lazy PortalPageRepository portalPageRepository,
        @Lazy PortalPageContextRepository portalPageContextRepository
    ) {
        this.portalPageRepository = portalPageRepository;
        this.portalPageContextRepository = portalPageContextRepository;
    }

    @Override
    public void createDefaultPortalHomePage(String environmentId) throws TechnicalException {
        var now = new Date();
        var homePages = portalPageContextRepository.findAllByContextTypeAndEnvironmentId(PortalPageContextType.HOMEPAGE, environmentId);
        if (homePages.isEmpty()) {
            var createPortalPage = portalPageRepository.create(
                PortalPage
                    .builder()
                    .id(UuidString.generateRandom())
                    .environmentId(environmentId)
                    .name(DEFAULT_PORTAL_PAGE_NAME)
                    .content(getDefaultPortalPageContent())
                    .createdAt(now)
                    .updatedAt(now)
                    .build()
            );
            portalPageContextRepository.create(
                PortalPageContext
                    .builder()
                    .id(UuidString.generateRandom())
                    .pageId(createPortalPage.getId())
                    .contextType(PortalPageContextType.HOMEPAGE)
                    .environmentId(environmentId)
                    .published(true)
                    .build()
            );
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
