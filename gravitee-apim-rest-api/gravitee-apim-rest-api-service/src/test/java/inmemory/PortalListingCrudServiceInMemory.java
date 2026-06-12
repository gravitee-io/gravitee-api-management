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
package inmemory;

import io.gravitee.apim.core.portal_listing.crud_service.PortalListingCrudService;
import io.gravitee.apim.core.portal_listing.exception.PortalListingNotFoundException;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class PortalListingCrudServiceInMemory implements PortalListingCrudService, InMemoryAlternative<PortalListing> {

    final ArrayList<PortalListing> storage = new ArrayList<>();

    @Override
    public PortalListing create(PortalListing portalListing) {
        storage.add(portalListing);
        return portalListing;
    }

    @Override
    public PortalListing update(PortalListing portalListing) {
        OptionalInt index = this.findIndex(this.storage, p -> p.getId().equals(portalListing.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), portalListing);
            return portalListing;
        }
        throw new PortalListingNotFoundException(portalListing.getId().toString());
    }

    @Override
    public Optional<PortalListing> findByIdAndEnvironmentId(PortalListingId portalListingId, String environmentId) {
        return storage
            .stream()
            .filter(p -> portalListingId.equals(p.getId()) && environmentId.equals(p.getEnvironmentId()))
            .findFirst();
    }

    @Override
    public void delete(PortalListingId portalListingId) {
        storage.removeIf(p -> portalListingId.equals(p.getId()));
    }

    @Override
    public void initWith(List<PortalListing> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PortalListing> storage() {
        return Collections.unmodifiableList(storage);
    }
}
