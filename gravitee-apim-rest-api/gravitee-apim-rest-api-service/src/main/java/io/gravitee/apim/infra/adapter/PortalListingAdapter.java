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
package io.gravitee.apim.infra.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalListingAdapter {
    PortalListingAdapter INSTANCE = Mappers.getMapper(PortalListingAdapter.class);

    TypeReference<List<PortalListingApiEntry>> APIS_TYPE = new TypeReference<>() {};

    default PortalListing toEntity(io.gravitee.repository.management.model.PortalListing portalListing) {
        if (portalListing == null) {
            return null;
        }
        return PortalListing.of(
            PortalListingId.of(portalListing.getId()),
            portalListing.getEnvironmentId(),
            portalListing.getOrganizationId(),
            PortalId.of(portalListing.getPortalId()),
            deserializeApis(portalListing.getApis())
        );
    }

    default io.gravitee.repository.management.model.PortalListing toRepository(PortalListing portalListing) {
        if (portalListing == null) {
            return null;
        }
        return io.gravitee.repository.management.model.PortalListing.builder()
            .id(portalListing.getId().toString())
            .environmentId(portalListing.getEnvironmentId())
            .organizationId(portalListing.getOrganizationId())
            .portalId(portalListing.getPortalId().toString())
            .apis(serializeApis(portalListing.getApis()))
            .build();
    }

    static String serializeApis(List<PortalListingApiEntry> apis) {
        try {
            return GraviteeJacksonMapper.getInstance().writeValueAsString(apis == null ? List.of() : apis);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize PortalListing apis", e);
        }
    }

    static List<PortalListingApiEntry> deserializeApis(String apis) {
        if (apis == null || apis.isBlank()) {
            return List.of();
        }
        try {
            return GraviteeJacksonMapper.getInstance().readValue(apis, APIS_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize PortalListing apis", e);
        }
    }
}
