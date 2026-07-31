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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.validation.LinkUrlValidator;
import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

/**
 * Validates Portal Link input. Format checks only — no reference-existence checks
 * (parent portal). Missing parent is tolerated as orphan per the orphan-tolerance design;
 * the link materializes once the missing CRD applies.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidatePortalLinkDomainService implements Validator<ValidatePortalLinkDomainService.Input> {

    private final PortalAutomationScopeDomainService portalAutomationScopeEnforcer;

    public record Input(AuditInfo auditInfo, PortalId portalId, String name, String href, String location, Integer order) implements
        Validator.Input {}

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<>(portalAutomationScopeEnforcer.validate(input.auditInfo(), input.portalId(), "portalHrid"));

        var sanitizedName = input.name() != null ? input.name().trim() : null;
        if (sanitizedName == null || sanitizedName.isBlank()) {
            errors.add(Error.severe("name must not be blank"));
        }

        var sanitizedHref = input.href() != null ? input.href().trim() : null;
        if (sanitizedHref == null || sanitizedHref.isBlank()) {
            errors.add(Error.severe("href must not be blank"));
        } else if (!LinkUrlValidator.isWellFormedAbsoluteUrl(sanitizedHref)) {
            errors.add(Error.severe("href must be a well-formed absolute URL: %s", sanitizedHref));
        }
        if (input.location() != null) {
            errors.addAll(NavigationPathValidator.validate(input.location(), "location"));
        }

        var sanitized = new Input(input.auditInfo(), input.portalId(), sanitizedName, sanitizedHref, input.location(), input.order());
        return Result.ofBoth(sanitized, errors);
    }
}
