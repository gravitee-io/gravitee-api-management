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
package io.gravitee.apim.infra.crud_service.portal_page;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.infra.adapter.PortalPageContentAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContentRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PortalPageContentCrudServiceImpl implements PortalPageContentCrudService {

    private String defaultPortalPageContent;
    private final PortalPageContentRepository portalPageContentRepository;
    private final PortalPageContentAdapter portalPageContentAdapter = PortalPageContentAdapter.INSTANCE;

    public PortalPageContentCrudServiceImpl(@Lazy final PortalPageContentRepository portalPageContentRepository) {
        this.portalPageContentRepository = portalPageContentRepository;
    }

    @Override
    public PortalPageContent<?> create(PortalPageContent<?> content) {
        try {
            final var repoContent = portalPageContentAdapter.toRepository(content);
            final var createdContent = portalPageContentRepository.create(repoContent);
            return portalPageContentAdapter.toEntity(createdContent);
        } catch (TechnicalException e) {
            final var errorMessage = String.format("An error occurred while creating portal page content with id %s", content.getId());
            throw new TechnicalDomainException(errorMessage, e);
        }
    }

    @Override
    public PortalPageContent<?> createDefault(String organizationId, String environmentId) {
        final var pageContentId = PortalPageContentId.random();
        final var portalPageContent = new GraviteeMarkdownPageContent(
            pageContentId,
            organizationId,
            environmentId,
            new GraviteeMarkdown(getDefaultPortalPageContent())
        );
        return this.create(portalPageContent);
    }

    @Override
    public PortalPageContent<?> update(PortalPageContent<?> content) {
        try {
            final var repoContent = portalPageContentAdapter.toRepository(content);
            final var updatedContent = portalPageContentRepository.update(repoContent);
            return portalPageContentAdapter.toEntity(updatedContent);
        } catch (TechnicalException e) {
            final var errorMessage = String.format("An error occurred while updating portal page content with id %s", content.getId());
            throw new TechnicalDomainException(errorMessage, e);
        }
    }

    @Override
    public void delete(PortalPageContentId id) {
        try {
            portalPageContentRepository.delete(id.json());
        } catch (TechnicalException e) {
            final var errorMessage = String.format("An error occurred while deleting portal page content with id %s", id);
            throw new TechnicalDomainException(errorMessage, e);
        }
    }

    private String getDefaultPortalPageContent() {
        if (defaultPortalPageContent == null) {
            try {
                final var resource = new ClassPathResource("templates/default-portal-page-content.md");
                defaultPortalPageContent = resource.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Could not load default portal page template", e);
            }
        }
        return defaultPortalPageContent;
    }
}
