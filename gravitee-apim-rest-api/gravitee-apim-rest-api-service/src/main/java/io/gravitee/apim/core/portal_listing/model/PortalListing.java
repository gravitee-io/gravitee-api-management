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
package io.gravitee.apim.core.portal_listing.model;

import io.gravitee.apim.core.portal.model.PortalId;
import jakarta.annotation.Nonnull;
import java.util.List;
import lombok.Getter;

@Getter
public class PortalListing {

    @Nonnull
    private final PortalListingId id;

    @Nonnull
    private final String environmentId;

    @Nonnull
    private final String organizationId;

    @Nonnull
    private final PortalId portalId;

    @Nonnull
    private final List<PortalListingApiEntry> apis;

    private PortalListing(
        PortalListingId id,
        String environmentId,
        String organizationId,
        PortalId portalId,
        List<PortalListingApiEntry> apis
    ) {
        this.id = id;
        this.environmentId = environmentId;
        this.organizationId = organizationId;
        this.portalId = portalId;
        this.apis = List.copyOf(apis);
    }

    public static PortalListing of(
        PortalListingId id,
        String environmentId,
        String organizationId,
        PortalId portalId,
        List<PortalListingApiEntry> apis
    ) {
        return new PortalListing(id, environmentId, organizationId, portalId, apis);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalListing that = (PortalListing) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
