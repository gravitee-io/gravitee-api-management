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
import io.gravitee.apim.core.exception.AbstractDomainException;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

/**
 * Dry-run companion to {@link CreateOrUpdatePortalLinkUseCase} — runs the exact same checks but
 * never materializes and never throws: every problem is reported back as a severe {@link Validator.Error}
 * in the {@code Output}, since a dry run's whole point is to preview problems, not fail the call.
 */
@UseCase
@RequiredArgsConstructor
public class ValidatePortalLinkUseCase {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalNavigationItemValidatorService navigationItemValidatorService;
    private final PortalAutomationScopeDomainService portalAutomationScopeEnforcer;

    public CreateOrUpdatePortalLinkUseCase.Output execute(CreateOrUpdatePortalLinkUseCase.Input input) {
        var sanitizedName = input.name() != null ? input.name().trim() : null;
        var sanitizedHref = input.href() != null ? input.href().trim() : null;

        var errors = new ArrayList<>(portalAutomationScopeEnforcer.validate(input.auditInfo(), input.portalId(), "portalHrid"));
        if (input.location() != null) {
            errors.addAll(NavigationPathValidator.validate(input.location(), "location"));
        }

        var linkId = PortalNavigationItemId.forPortalLink(input.auditInfo(), input.portalId().toString(), input.linkHrid());
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(input.auditInfo().environmentId(), linkId);

        try {
            if (existing instanceof PortalNavigationLink existingLink) {
                var toUpdate = UpdatePortalNavigationItem.builder()
                    .type(PortalNavigationItemType.LINK)
                    .title(sanitizedName)
                    .url(sanitizedHref)
                    .build();
                navigationItemValidatorService.validateToUpdate(toUpdate, existingLink);
            } else {
                var toCreate = CreatePortalNavigationItem.builder()
                    .id(linkId)
                    .type(PortalNavigationItemType.LINK)
                    .title(sanitizedName)
                    .url(sanitizedHref)
                    .build();
                navigationItemValidatorService.validateOne(toCreate, input.auditInfo().environmentId());
            }
        } catch (AbstractDomainException e) {
            errors.add(Validator.Error.severe("%s", e.getMessage()));
        }

        return new CreateOrUpdatePortalLinkUseCase.Output(null, errors);
    }
}
