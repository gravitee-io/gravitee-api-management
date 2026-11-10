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
package io.gravitee.apim.core.portal_listing.crud_service;

import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PortalListingCrudService {
    PortalListing create(PortalListing portalListing);

    PortalListing update(PortalListing portalListing);

    Optional<PortalListing> findByIdAndEnvironmentId(PortalListingId portalListingId, String environmentId);

    List<PortalListing> findAllByPortalIdAndEnvironmentId(PortalId portalId, String environmentId);

    /**
     * Points every listing of the environment whose portal is outside {@code existingPortalIds} at {@code portalId}.
     *
     * @param existingPortalIds the portals of the environment, never empty
     * @return the number of listings changed
     */
    long updateOrphanPortalIds(String environmentId, Collection<PortalId> existingPortalIds, PortalId portalId);

    void delete(PortalListingId portalListingId);
}
