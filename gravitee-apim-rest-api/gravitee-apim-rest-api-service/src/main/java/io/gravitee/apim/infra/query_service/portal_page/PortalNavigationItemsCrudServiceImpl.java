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
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.infra.adapter.PortalNavigationItemAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PortalNavigationItemsCrudServiceImpl implements PortalNavigationItemCrudService {

    private final PortalNavigationItemRepository portalNavigationItemRepository;
    private final PortalNavigationItemAdapter portalNavigationItemAdapter = PortalNavigationItemAdapter.INSTANCE;

    public PortalNavigationItemsCrudServiceImpl(@Lazy final PortalNavigationItemRepository portalNavigationItemRepository) {
        this.portalNavigationItemRepository = portalNavigationItemRepository;
    }

    @Override
    public PortalNavigationItem create(PortalNavigationItem portalNavigationItem) {
        try {
            final var repoItem = portalNavigationItemAdapter.toRepository(portalNavigationItem);
            portalNavigationItemRepository.create(repoItem);
            return portalNavigationItem;
        } catch (TechnicalException e) {
            final var errorMessage = String.format(
                "An error occurred while creating portal navigation item with id %s and environmentId %s",
                portalNavigationItem.getId(),
                portalNavigationItem.getEnvironmentId()
            );
            throw new TechnicalDomainException(errorMessage, e);
        }
    }

    @Override
    public PortalNavigationItem update(PortalNavigationItem portalNavigationItem) {
        try {
            final var repoItem = portalNavigationItemAdapter.toRepository(portalNavigationItem);
            portalNavigationItemRepository.update(repoItem);
            return portalNavigationItem;
        } catch (TechnicalException e) {
            final var errorMessage = String.format(
                "An error occurred while updating portal navigation item with id %s and environmentId %s",
                portalNavigationItem.getId(),
                portalNavigationItem.getEnvironmentId()
            );
            throw new TechnicalDomainException(errorMessage, e);
        }
    }

    @Override
    public void delete(PortalNavigationItemId portalNavigationItemId) {
        try {
            portalNavigationItemRepository.delete(portalNavigationItemId.toString());
        } catch (TechnicalException e) {
            final var errorMessage = String.format(
                "An error occurred while deleting portal navigation item with id %s",
                portalNavigationItemId
            );
            throw new TechnicalDomainException(errorMessage, e);
        }
    }
}
