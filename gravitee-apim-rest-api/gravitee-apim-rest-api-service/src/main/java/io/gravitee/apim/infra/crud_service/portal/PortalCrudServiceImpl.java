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
package io.gravitee.apim.infra.crud_service.portal;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.infra.adapter.PortalAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalRepository;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PortalCrudServiceImpl implements PortalCrudService {

    private final PortalRepository portalRepository;
    private static final PortalAdapter portalAdapter = PortalAdapter.INSTANCE;

    public PortalCrudServiceImpl(@Lazy PortalRepository portalRepository) {
        this.portalRepository = portalRepository;
    }

    @Override
    public Portal create(Portal portal) {
        try {
            var result = portalRepository.create(portalAdapter.toRepository(portal));
            return portalAdapter.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to create a Portal with id: %s", portal.getId()),
                e
            );
        }
    }

    @Override
    public Portal update(Portal portal) {
        try {
            var result = portalRepository.update(portalAdapter.toRepository(portal));
            return portalAdapter.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to update a Portal with id: %s", portal.getId()),
                e
            );
        }
    }

    @Override
    public Optional<Portal> findByIdAndEnvironmentId(PortalId portalId, String environmentId) {
        try {
            return portalRepository.findByIdAndEnvironmentId(portalId.toString(), environmentId).map(portalAdapter::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to find a Portal with id (%s) in environment (%s)", portalId, environmentId),
                e
            );
        }
    }

    @Override
    public void delete(PortalId portalId) {
        try {
            portalRepository.delete(portalId.toString());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to delete the Portal with id: %s", portalId),
                e
            );
        }
    }
}
