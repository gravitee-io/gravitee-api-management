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
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.infra.adapter.PortalNavigationItemAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PortalNavigationItemsCrudServiceImpl implements PortalNavigationItemCrudService {

    /** Each miss means another writer touched the row meanwhile; contention on one item is rare and brief. */
    private static final int MAX_FETCH_STATE_ATTEMPTS = 5;

    private final PortalNavigationItemRepository portalNavigationItemRepository;
    private final PortalNavigationItemAdapter portalNavigationItemAdapter = PortalNavigationItemAdapter.INSTANCE;

    public PortalNavigationItemsCrudServiceImpl(@Lazy final PortalNavigationItemRepository portalNavigationItemRepository) {
        this.portalNavigationItemRepository = portalNavigationItemRepository;
    }

    @Override
    public PortalNavigationItem create(PortalNavigationItem portalNavigationItem) {
        try {
            final var repoItem = portalNavigationItemAdapter.toRepository(portalNavigationItem);
            final var createdItem = portalNavigationItemRepository.create(repoItem);
            return portalNavigationItemAdapter.toEntity(createdItem);
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
            final var updatedItem = portalNavigationItemRepository.update(repoItem);
            return portalNavigationItemAdapter.toEntity(updatedItem);
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
    public Optional<PortalNavigationItem> updateSourceFetchState(
        PortalNavigationItemId id,
        PortalNavigationItemSource fetchedSource,
        PortalNavigationItemSource.FetchState fetchState
    ) {
        try {
            for (int attempt = 0; attempt < MAX_FETCH_STATE_ATTEMPTS; attempt++) {
                final var stored = portalNavigationItemRepository.findById(id.json()).orElse(null);
                if (stored == null) {
                    return Optional.empty();
                }
                final var storedSource = portalNavigationItemAdapter.sourceFromRepository(stored);
                if (storedSource == null || !storedSource.sameOriginAs(fetchedSource)) {
                    return Optional.of(portalNavigationItemAdapter.toEntity(stored));
                }
                final var configuration = portalNavigationItemAdapter.configurationWithFetchState(stored.getConfiguration(), fetchState);
                if (
                    portalNavigationItemRepository.updateConfigurationIfUnchanged(stored.getId(), stored.getConfiguration(), configuration)
                ) {
                    stored.setConfiguration(configuration);
                    return Optional.of(portalNavigationItemAdapter.toEntity(stored));
                }
            }
            throw new TechnicalDomainException(
                String.format(
                    "Unable to persist the fetch state of portal navigation item with id %s: it keeps being updated concurrently",
                    id
                )
            );
        } catch (TechnicalException e) {
            final var errorMessage = String.format(
                "An error occurred while updating the fetch state of portal navigation item with id %s",
                id
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

    @Override
    public void deleteByIds(List<PortalNavigationItemId> ids) {
        try {
            portalNavigationItemRepository.deleteByIds(ids.stream().map(PortalNavigationItemId::toString).collect(Collectors.toList()));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while bulk deleting portal navigation items", e);
        }
    }
}
