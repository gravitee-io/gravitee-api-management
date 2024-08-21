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
package io.gravitee.apim.infra.crud_service.portal_menu_link;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_menu_link.crud_service.PortalMenuLinkCrudService;
import io.gravitee.apim.core.portal_menu_link.exception.PortalMenuLinkNotFoundException;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.infra.adapter.PortalMenuLinkAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalMenuLinkRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PortalMenuLinkCrudServiceImpl implements PortalMenuLinkCrudService {

    private final PortalMenuLinkRepository portalMenuLinkRepository;
    private final PortalMenuLinkAdapter portalMenuLinkAdapter;

    public PortalMenuLinkCrudServiceImpl(
        @Lazy PortalMenuLinkRepository portalMenuLinkRepository,
        PortalMenuLinkAdapter portalMenuLinkAdapter
    ) {
        this.portalMenuLinkRepository = portalMenuLinkRepository;
        this.portalMenuLinkAdapter = portalMenuLinkAdapter;
    }

    @Override
    public PortalMenuLink create(PortalMenuLink portalMenuLink) {
        try {
            var result = portalMenuLinkRepository.create(portalMenuLinkAdapter.toRepository(portalMenuLink));
            return portalMenuLinkAdapter.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to create a PortalMenuLink with id: %s", portalMenuLink.getId()),
                e
            );
        }
    }

    @Override
    public PortalMenuLink update(PortalMenuLink portalMenuLink) {
        try {
            var result = portalMenuLinkRepository.update(portalMenuLinkAdapter.toRepository(portalMenuLink));
            return portalMenuLinkAdapter.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to update a PortalMenuLink with id: %s", portalMenuLink.getId()),
                e
            );
        }
    }

    @Override
    public PortalMenuLink getByIdAndEnvironmentId(String portalMenuLinkId, String environmentId) {
        try {
            return portalMenuLinkRepository
                .findByIdAndEnvironmentId(portalMenuLinkId, environmentId)
                .map(portalMenuLinkAdapter::toEntity)
                .orElseThrow(() -> new PortalMenuLinkNotFoundException(portalMenuLinkId));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurred while trying to find a PortalMenuLink with id (%s) in environment (%s)",
                    portalMenuLinkId,
                    environmentId
                ),
                e
            );
        }
    }
}
