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
package io.gravitee.apim.core.portal_listing.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates Portal Listing input. Format checks only — no reference-existence checks.
 *
 * @author GraviteeSource Team
 */
@DomainService
public class ValidatePortalListingDomainService implements Validator<ValidatePortalListingDomainService.Input> {

    public record Input(AuditInfo auditInfo, PortalListingId listingId, PortalId portalId, List<PortalListingApiEntry> apis) implements
        Validator.Input {}

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();
        List<PortalListingApiEntry> apis = input.apis() == null ? List.of() : input.apis();
        for (int i = 0; i < apis.size(); i++) {
            errors.addAll(NavigationPathValidator.validate(apis.get(i).location(), "apis[" + i + "].location"));
        }
        return Result.ofBoth(input, errors);
    }
}
