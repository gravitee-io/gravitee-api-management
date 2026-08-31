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
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import io.gravitee.apim.core.portal_listing.model.PortalListingId;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Validates Portal Listing input. Format checks, plus API-reference-existence checks — the referenced
 * portal itself is not existence-checked (that reference is allowed to arrive out of order).
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidatePortalListingDomainService implements Validator<ValidatePortalListingDomainService.Input> {

    private final PortalAutomationScopeDomainService portalAutomationScopeEnforcer;
    private final ApiCrudService apiCrudService;

    public record Input(AuditInfo auditInfo, PortalListingId listingId, PortalId portalId, List<PortalListingApiEntry> apis) implements
        Validator.Input {}

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();
        errors.addAll(portalAutomationScopeEnforcer.validate(input.auditInfo(), input.portalId(), "portalHrid"));
        List<PortalListingApiEntry> apis = input.apis() == null ? List.of() : input.apis();
        var apiIds = apis
            .stream()
            .map(entry -> entry.apiId(input.auditInfo()))
            .toList();
        Set<String> existingApiIds = apiCrudService.findByIds(apiIds).stream().map(Api::getId).collect(Collectors.toSet());
        for (int i = 0; i < apis.size(); i++) {
            var entry = apis.get(i);
            errors.addAll(NavigationPathValidator.validate(entry.location(), "apis[" + i + "].location"));
            if (!existingApiIds.contains(apiIds.get(i))) {
                errors.add(Error.severe("apis[%d]: API [%s] not found", i, entry.apiHrid()));
            }
        }
        return Result.ofBoth(input, errors);
    }
}
