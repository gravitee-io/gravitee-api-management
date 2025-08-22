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
            var updated = portalPageRepository.findById(page.id().id().toString());
            return PortalPageAdapter.toDomain(updated);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(e.getMessage(), e);
        }
    }

    @Override
    public PortalPage getById(PageId pageId) {
        try {
            var entity = portalPageRepository.findById(pageId.id().toString());
            if (entity == null) throw new PortalPageNotFoundException(pageId.id().toString());
            return PortalPageAdapter.toDomain(entity);
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
            var entity = portalPageRepository.findById(pageId.id().toString());
            return entity != null;
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(e.getMessage(), e);
        }
    }
}
