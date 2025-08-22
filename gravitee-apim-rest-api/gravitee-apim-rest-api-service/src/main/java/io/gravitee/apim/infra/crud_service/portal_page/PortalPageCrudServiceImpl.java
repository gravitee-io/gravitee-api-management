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
import io.gravitee.apim.core.portal_page.crud_service.PortalPageCrudService;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.infra.adapter.PortalPageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.rest.api.service.exceptions.PortalPageNotFoundException;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PortalPageCrudServiceImpl implements PortalPageCrudService {

    private final PortalPageRepository portalPageRepository;

    public PortalPageCrudServiceImpl(@Lazy PortalPageRepository portalPageRepository) {
        this.portalPageRepository = portalPageRepository;
    }

    @Override
    public PortalPage create(PortalPage page) {
        try {
            var entity = PortalPageAdapter.toEntity(page, List.of());
            var created = portalPageRepository.create(entity);
            return PortalPageAdapter.toDomain(created);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(e.getMessage(), e);
        }
    }

    @Override
    public PortalPage byPortalViewContext(PortalViewContext portalViewContext) {
        try {
            var pages = portalPageRepository.findByContext(portalViewContext.name());
            if (pages == null || pages.isEmpty()) throw new PortalPageNotFoundException(portalViewContext.name());
            return PortalPageAdapter.toDomain(pages.stream().findFirst().orElse(null));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(e.getMessage(), e);
        }
    }

    @Override
    public PortalPage setPortalViewContextPage(PortalViewContext portalViewContext, PortalPage page) {
        try {
            portalPageRepository.assignContext(page.id().id().toString(), portalViewContext.name());
            var updatedOpt = portalPageRepository.findById(page.id().id().toString());
            if (updatedOpt.isEmpty()) throw new PortalPageNotFoundException(page.id().id().toString());
            return PortalPageAdapter.toDomain(updatedOpt.get());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(e.getMessage(), e);
        }
    }

    @Override
    public PortalPage getById(PageId pageId) {
        try {
            var entityOpt = portalPageRepository.findById(pageId.id().toString());
            if (entityOpt.isEmpty()) throw new PortalPageNotFoundException(pageId.id().toString());
            return PortalPageAdapter.toDomain(entityOpt.get());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(e.getMessage(), e);
        }
    }

    @Override
    public boolean portalViewContextExists(PortalViewContext key) {
        try {
            var pages = portalPageRepository.findByContext(key.name());
            return pages != null && !pages.isEmpty();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(e.getMessage(), e);
        }
    }

    @Override
    public boolean pageIdExists(PageId pageId) {
        try {
            var entityOpt = portalPageRepository.findById(pageId.id().toString());
            return entityOpt.isPresent();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(e.getMessage(), e);
        }
    }
}
