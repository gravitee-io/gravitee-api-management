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
package io.gravitee.apim.infra.query_service.portal_page;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.apim.infra.adapter.PortalPageContentAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContentRepository;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PortalPageContentQueryServiceImpl implements PortalPageContentQueryService {

    private final PortalPageContentRepository portalPageContentRepository;
    private final PortalPageContentAdapter portalPageContentAdapter = PortalPageContentAdapter.INSTANCE;

    public PortalPageContentQueryServiceImpl(@Lazy final PortalPageContentRepository portalPageContentRepository) {
        this.portalPageContentRepository = portalPageContentRepository;
    }

    @Override
    public Optional<PortalPageContent<?>> findById(PortalPageContentId id) {
        try {
            return portalPageContentRepository.findById(id.json()).map(portalPageContentAdapter::toEntity);
        } catch (TechnicalException e) {
            String errorMessage = String.format("An error occurred while finding portal page content by id %s", id.json());
            throw new TechnicalDomainException(errorMessage, e);
        }
    }
}
