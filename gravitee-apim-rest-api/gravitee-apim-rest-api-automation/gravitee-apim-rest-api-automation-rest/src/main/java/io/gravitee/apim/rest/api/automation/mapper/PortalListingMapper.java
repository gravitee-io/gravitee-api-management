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
package io.gravitee.apim.rest.api.automation.mapper;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.apim.rest.api.automation.model.PortalListingSpec;
import io.gravitee.apim.rest.api.automation.model.PortalListingState;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author GraviteeSource Team
 */
@Mapper
public interface PortalListingMapper {
    PortalListingMapper INSTANCE = Mappers.getMapper(PortalListingMapper.class);

    /** PUT path — composes the wire state from the input spec plus the use case Output (id + errors). */
    default PortalListingState toPortalListingState(
        PortalListingSpec spec,
        PortalListingId id,
        List<Validator.Error> errors,
        AuditInfo audit,
        String portalHrid
    ) {
        var state = new PortalListingState(
            id == null ? null : id.toString(),
            audit.environmentId(),
            audit.organizationId(),
            toErrors(errors),
            portalHrid
        );
        state.setHrid(spec.getHrid());
        state.setApis(spec.getApis());
        return state;
    }

    /** GET path — composes the wire state from the persisted entity. */
    default PortalListingState toPortalListingState(PortalListing portalListing, String hrid, String portalHrid) {
        var state = new PortalListingState(
            portalListing.getId() != null ? portalListing.getId().toString() : null,
            portalListing.getEnvironmentId(),
            portalListing.getOrganizationId(),
            null,
            portalHrid
        );
        state.setHrid(hrid);
        state.setApis(toWireApis(portalListing.getApis()));
        return state;
    }

    default Errors toErrors(List<Validator.Error> validationErrors) {
        if (validationErrors == null || validationErrors.isEmpty()) {
            return null;
        }
        var wire = new Errors();
        wire.setSevere(validationErrors.stream().filter(Validator.Error::isSevere).map(Validator.Error::getMessage).toList());
        wire.setWarning(validationErrors.stream().filter(Validator.Error::isWarning).map(Validator.Error::getMessage).toList());
        return wire;
    }

    default List<io.gravitee.apim.rest.api.automation.model.PortalListingApiEntry> toWireApis(List<PortalListingApiEntry> apis) {
        if (apis == null) {
            return List.of();
        }
        return apis.stream().map(this::toWire).toList();
    }

    default io.gravitee.apim.rest.api.automation.model.PortalListingApiEntry toWire(PortalListingApiEntry entry) {
        var wire = new io.gravitee.apim.rest.api.automation.model.PortalListingApiEntry();
        wire.setApiHrid(entry.apiHrid());
        wire.setLocation(entry.location());
        wire.setOrder(entry.order());
        return wire;
    }

    default List<PortalListingApiEntry> toDomainApis(List<io.gravitee.apim.rest.api.automation.model.PortalListingApiEntry> apis) {
        if (apis == null) {
            return List.of();
        }
        return apis.stream().map(this::toDomain).toList();
    }

    default PortalListingApiEntry toDomain(io.gravitee.apim.rest.api.automation.model.PortalListingApiEntry entry) {
        return new PortalListingApiEntry(entry.getApiHrid(), entry.getLocation(), entry.getOrder());
    }
}
