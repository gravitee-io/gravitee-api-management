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
import io.gravitee.apim.core.portal_listing.crud_service.PortalListingCrudService;
import io.gravitee.apim.core.portal_listing.exception.PortalListingNotFoundException;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeletePortalListingUseCase {

    private final PortalListingCrudService portalListingCrudService;

    public record Input(AuditInfo auditInfo, PortalListingId listingId) {}

    public void execute(Input input) {
        portalListingCrudService
            .findByIdAndEnvironmentId(input.listingId(), input.auditInfo().environmentId())
            .orElseThrow(() -> new PortalListingNotFoundException(input.listingId().toString()));
        portalListingCrudService.delete(input.listingId());
    }
}
