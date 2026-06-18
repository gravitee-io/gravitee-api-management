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
package io.gravitee.apim.infra.crud_service.portal_listing;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.crud_service.PortalListingCrudService;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.infra.adapter.PortalListingAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalListingRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PortalListingCrudServiceImpl implements PortalListingCrudService {

    private final PortalListingRepository portalListingRepository;
    private static final PortalListingAdapter portalListingAdapter = PortalListingAdapter.INSTANCE;

    public PortalListingCrudServiceImpl(@Lazy PortalListingRepository portalListingRepository) {
        this.portalListingRepository = portalListingRepository;
    }

    @Override
    public PortalListing create(PortalListing portalListing) {
        try {
            var result = portalListingRepository.create(portalListingAdapter.toRepository(portalListing));
            return portalListingAdapter.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to create a Portal Listing with id: %s", portalListing.getId()),
                e
            );
        }
    }

    @Override
    public PortalListing update(PortalListing portalListing) {
        try {
            var result = portalListingRepository.update(portalListingAdapter.toRepository(portalListing));
            return portalListingAdapter.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to update a Portal Listing with id: %s", portalListing.getId()),
                e
            );
        }
    }

    @Override
    public Optional<PortalListing> findByIdAndEnvironmentId(PortalListingId portalListingId, String environmentId) {
        try {
            return portalListingRepository
                .findByIdAndEnvironmentId(portalListingId.toString(), environmentId)
                .map(portalListingAdapter::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurred while trying to find a Portal Listing with id (%s) in environment (%s)",
                    portalListingId,
                    environmentId
                ),
                e
            );
        }
    }

    @Override
    public List<PortalListing> findAllByPortalIdAndEnvironmentId(PortalId portalId, String environmentId) {
        try {
            return portalListingRepository
                .findAllByPortalIdAndEnvironmentId(portalId.toString(), environmentId)
                .stream()
                .map(portalListingAdapter::toEntity)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurred while trying to find Portal Listings for portal (%s) in environment (%s)",
                    portalId,
                    environmentId
                ),
                e
            );
        }
    }

    @Override
    public void delete(PortalListingId portalListingId) {
        try {
            portalListingRepository.delete(portalListingId.toString());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to delete the Portal Listing with id: %s", portalListingId),
                e
            );
        }
    }
}
