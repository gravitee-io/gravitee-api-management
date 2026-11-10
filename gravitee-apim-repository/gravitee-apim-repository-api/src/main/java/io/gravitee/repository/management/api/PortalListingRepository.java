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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.PortalListing;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author GraviteeSource Team
 */
public interface PortalListingRepository extends CrudRepository<PortalListing, String> {
    Optional<PortalListing> findByIdAndEnvironmentId(final String portalListingId, final String environmentId) throws TechnicalException;
    List<PortalListing> findAllByPortalIdAndEnvironmentId(final String portalId, final String environmentId) throws TechnicalException;
    /** @return the distinct portal ids the environment's listings reference */
    Set<String> findPortalIdsByEnvironmentId(final String environmentId) throws TechnicalException;
    /**
     * Points every listing of the environment whose portal is one of {@code fromPortalIds} at
     * {@code toPortalId}, and changes nothing else on those listings.
     *
     * @return the number of listings changed, 0 when {@code fromPortalIds} is empty
     */
    long updatePortalIds(final String environmentId, final Collection<String> fromPortalIds, final String toPortalId)
        throws TechnicalException;
    void deleteByEnvironmentId(final String environmentId) throws TechnicalException;
    void deleteByOrganizationId(final String organizationId) throws TechnicalException;
}
