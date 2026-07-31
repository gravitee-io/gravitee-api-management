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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.PortalLinkSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.ValidatePortalLinkDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOrUpdatePortalLinkUseCase {

    private final ValidatePortalLinkDomainService validator;
    private final PortalLinkSyncDomainService syncDomainService;
    private final PortalAutomationScopeDomainService portalAutomationScopeEnforcer;

    public record Input(
        AuditInfo auditInfo,
        PortalId portalId,
        String linkHrid,
        String name,
        String href,
        String location,
        Integer order
    ) {}

    public record Output(PortalNavigationLink link, List<Validator.Error> errors) {}

    public Output execute(Input input) {
        var validation = validator.validateAndSanitize(
            new ValidatePortalLinkDomainService.Input(
                input.auditInfo(),
                input.portalId(),
                input.name(),
                input.href(),
                input.location(),
                input.order()
            )
        );

        validation
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(errors.stream().map(Validator.Error::getMessage).collect(Collectors.joining(", ")));
            });

        var warnings = validation.warning().orElseGet(List::of);
        var sanitized = validation.value().orElseThrow(() -> new ValidationDomainException("Unable to sanitize portal link"));

        PortalNavigationLink link = null;
        // Skip nav-tree materialization for non-default portals — mirrors Documentation: app is not ready for that.
        if (portalAutomationScopeEnforcer.isDefaultPortal(input.auditInfo(), sanitized.portalId())) {
            link = syncDomainService.materialize(
                input.auditInfo(),
                sanitized.portalId().toString(),
                input.linkHrid(),
                sanitized.name(),
                sanitized.href(),
                sanitized.location(),
                sanitized.order()
            );
        }

        return new Output(link, warnings);
    }
}
