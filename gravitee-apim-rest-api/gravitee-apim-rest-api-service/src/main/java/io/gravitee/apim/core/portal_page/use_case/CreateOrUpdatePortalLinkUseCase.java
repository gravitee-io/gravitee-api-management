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
import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import io.gravitee.apim.core.portal_page.domain_service.PortalLinkSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOrUpdatePortalLinkUseCase {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalNavigationItemValidatorService navigationItemValidatorService;
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
        var sanitizedName = input.name() != null ? input.name().trim() : null;
        var sanitizedHref = input.href() != null ? input.href().trim() : null;

        var scopeAndLocationErrors = new ArrayList<>(
            portalAutomationScopeEnforcer.validate(input.auditInfo(), input.portalId(), "portalHrid")
        );
        if (input.location() != null) {
            scopeAndLocationErrors.addAll(NavigationPathValidator.validate(input.location(), "location"));
        }
        if (!scopeAndLocationErrors.isEmpty()) {
            throw new ValidationDomainException(
                scopeAndLocationErrors.stream().map(Validator.Error::getMessage).collect(Collectors.joining(", "))
            );
        }

        var linkId = PortalNavigationItemId.forPortalLink(input.auditInfo(), input.portalId().toString(), input.linkHrid());
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(input.auditInfo().environmentId(), linkId);

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

        PortalNavigationLink link = null;
        // Skip nav-tree materialization for non-default portals — mirrors Documentation: app is not ready for that.
        if (portalAutomationScopeEnforcer.isDefaultPortal(input.auditInfo(), input.portalId())) {
            link = syncDomainService.materialize(
                input.auditInfo(),
                input.portalId().toString(),
                input.linkHrid(),
                sanitizedName,
                sanitizedHref,
                input.location(),
                input.order()
            );
        }

        return new Output(link, List.of());
    }
}
