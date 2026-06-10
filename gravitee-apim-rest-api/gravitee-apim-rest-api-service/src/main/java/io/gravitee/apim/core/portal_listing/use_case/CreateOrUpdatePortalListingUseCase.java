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
package io.gravitee.apim.core.portal_listing.use_case;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.crud_service.PortalListingCrudService;
import io.gravitee.apim.core.portal_listing.domain_service.ValidatePortalListingDomainService;
import io.gravitee.apim.core.portal_listing.model.PortalListing;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateOrUpdatePortalListingUseCase {

    private final ValidatePortalListingDomainService validator;
    private final PortalListingCrudService portalListingCrudService;

    public record Input(AuditInfo auditInfo, PortalListingId listingId, PortalId portalId, List<PortalListingApiEntry> apis) {}

    public record Output(PortalListingId id, List<Validator.Error> errors) {}

    public Output execute(Input input) {
        var validation = validator.validateAndSanitize(
            new ValidatePortalListingDomainService.Input(input.auditInfo(), input.listingId(), input.portalId(), input.apis())
        );

        validation
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(errors.stream().map(Validator.Error::getMessage).collect(Collectors.joining(", ")));
            });

        var warnings = validation.warning().orElseGet(List::of);

        var listing = PortalListing.of(
            input.listingId(),
            input.auditInfo().environmentId(),
            input.auditInfo().organizationId(),
            input.portalId(),
            input.apis() == null ? List.of() : input.apis()
        );

        var existing = portalListingCrudService.findByIdAndEnvironmentId(input.listingId(), input.auditInfo().environmentId());
        var saved = existing.isPresent() ? portalListingCrudService.update(listing) : portalListingCrudService.create(listing);

        return new Output(saved.getId(), warnings);
    }
}
