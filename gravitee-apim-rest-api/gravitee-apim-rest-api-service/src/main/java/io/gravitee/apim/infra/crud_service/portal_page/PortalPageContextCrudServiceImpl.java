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
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContextCrudService;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.infra.adapter.PortalPageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PortalPageContextCrudServiceImpl implements PortalPageContextCrudService {

    private final PortalPageContextRepository portalPageContextRepository;
    private final PortalPageAdapter pageAdapter = PortalPageAdapter.INSTANCE;

    public PortalPageContextCrudServiceImpl(@Lazy PortalPageContextRepository portalPageContextRepository) {
        this.portalPageContextRepository = portalPageContextRepository;
    }

    @Override
    public List<PageId> findAllIdsByContextTypeAndEnvironmentId(PortalViewContext contextType, String environmentId) {
        var repoCtx = PortalPageContextType.valueOf(contextType.name());
        try {
            return portalPageContextRepository
                .findAllByContextTypeAndEnvironmentId(repoCtx, environmentId)
                .stream()
                .map(PortalPageContext::getPageId)
                .map(PageId::of)
                .toList();
        } catch (io.gravitee.repository.exceptions.TechnicalException e) {
            throw new TechnicalDomainException("Something went wrong while trying to find portal page contexts", e);
        }
    }

    @Override
    public PortalPageView findByPageId(PageId pageId) {
        return pageAdapter.map(portalPageContextRepository.findByPageId(pageId.toString()));
    }

    @Override
    public void update(PageId pageId, PortalPageView toUpdate) {
        var item = pageAdapter.map(pageId, toUpdate);
        try {
            portalPageContextRepository.updateByPageId(item);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Something went wrong while trying to update portal page context", e);
        }
    }
}
