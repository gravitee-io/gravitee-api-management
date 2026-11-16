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
package io.gravitee.apim.core.portal.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Validates Portal input. Format checks only — no reference-existence checks.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidatePortalDomainService implements Validator<ValidatePortalDomainService.Input> {

    private final PortalAutomationScopeDomainService portalAutomationScopeEnforcer;
    private final PortalUrlDomainService portalUrlDomainService;

    public record Input(AuditInfo auditInfo, PortalId portalId, PortalNavigationStructure structure) implements Validator.Input {}

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();
        errors.addAll(portalAutomationScopeEnforcer.validatePortal(input.auditInfo(), input.portalId(), "hrid"));
        if (isNewPortalWithoutResolvableUrl(input)) {
            errors.add(
                Error.severe(
                    "Portal [%s] cannot be created: the portal URL of environment [%s] is not set or is not an absolute http(s) URL without credentials",
                    input.portalId(),
                    input.auditInfo().environmentId()
                )
            );
        }
        var structure = input.structure() == null ? PortalNavigationStructure.empty() : input.structure();
        structure.areas().forEach((area, paths) -> errors.addAll(validateArea(area, paths)));
        return Result.ofBoth(input, errors);
    }

    /** A portal is only created with a real URL; an existing one keeps its stored URL. */
    private boolean isNewPortalWithoutResolvableUrl(Input input) {
        return (
            !portalAutomationScopeEnforcer.portalExistsInEnvironment(input.auditInfo(), input.portalId()) &&
            portalUrlDomainService.resolveDefaultPortalUrl(input.auditInfo().environmentId()).isEmpty()
        );
    }

    private static List<Error> validateArea(PortalArea area, List<NavigationPath> paths) {
        var errors = new ArrayList<Error>();
        var fieldPrefix = "structure." + area.wireName();
        for (int i = 0; i < paths.size(); i++) {
            errors.addAll(NavigationPathValidator.validate(paths.get(i).path(), fieldPrefix + "[" + i + "].path"));
        }
        return errors;
    }
}
