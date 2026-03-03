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
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.infra.adapter.PortalPageContentAdapter;
import io.gravitee.apim.infra.crud_service.portal_page.adapter.DefaultPortalPageContentProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContentRepository;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PortalPageContentCrudServiceImpl implements PortalPageContentCrudService {

    private final PortalPageContentRepository portalPageContentRepository;
    private final List<DefaultPortalPageContentProvider> defaultContentProviders;
    private final PortalPageContentAdapter portalPageContentAdapter = PortalPageContentAdapter.INSTANCE;

    public PortalPageContentCrudServiceImpl(
        @Lazy final PortalPageContentRepository portalPageContentRepository,
        List<DefaultPortalPageContentProvider> defaultContentProviders
    ) {
        this.portalPageContentRepository = portalPageContentRepository;
        this.defaultContentProviders = defaultContentProviders;
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
    public PortalPageContent<?> createDefault(String organizationId, String environmentId, PortalPageContentType contentType) {
        final var portalPageContent = defaultContentProviders
            .stream()
            .filter(provider -> provider.appliesTo(contentType))
            .findFirst()
            .map(provider -> provider.provide(organizationId, environmentId))
            .orElseThrow(() -> new IllegalArgumentException("No default content provider for type: " + contentType));
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
}
