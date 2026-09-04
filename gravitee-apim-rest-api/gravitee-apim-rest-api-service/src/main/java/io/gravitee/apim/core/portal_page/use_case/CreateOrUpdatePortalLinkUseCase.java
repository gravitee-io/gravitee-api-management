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
import io.gravitee.apim.core.exception.AbstractDomainException;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import io.gravitee.apim.core.portal_page.domain_service.PortalLinkSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        Integer order,
        PortalVisibility visibility
    ) {}

    public record Output(PortalNavigationLink link, List<Validator.Error> errors) {}

    /**
     * Severe errors are reported back in {@link Output#errors()} rather than thrown, so the caller
     * receives the structured {@code errors.severe[]} array described by the {@code Errors} schema
     * instead of a flat message. Nothing is materialized when any severe error is present, and
     * {@code PortalLinksResource.createOrUpdate} turns that into a {@code 400} on the apply path.
     */
    public Output execute(Input input) {
        var sanitizedName = input.name() != null ? input.name().trim() : null;
        var sanitizedHref = input.href() != null ? input.href().trim() : null;

        var errors = new ArrayList<>(portalAutomationScopeEnforcer.validate(input.auditInfo(), input.portalId(), "portalHrid"));
        if (input.location() != null) {
            errors.addAll(NavigationPathValidator.validate(input.location(), "location"));
        }

        var linkId = PortalNavigationItemId.forPortalLink(input.auditInfo(), input.portalId().toString(), input.linkHrid());
        var existing = navigationItemsQueryService.findByIdAndEnvironmentId(input.auditInfo().environmentId(), linkId);
        var parentId = syncDomainService.parentIdFor(input.auditInfo(), input.portalId().toString(), input.location());

        // The shared validator throws on its first failing rule, so at most one error surfaces here.
        try {
            if (existing instanceof PortalNavigationLink existingLink) {
                var toUpdate = UpdatePortalNavigationItem.builder()
                    .type(PortalNavigationItemType.LINK)
                    .title(sanitizedName)
                    .url(sanitizedHref)
                    .parentId(parentId)
                    .visibility(input.visibility())
                    .published(true)
                    .build();
                navigationItemValidatorService.validateToUpdate(toUpdate, existingLink);
            } else {
                var toCreate = CreatePortalNavigationItem.builder()
                    .id(linkId)
                    .type(PortalNavigationItemType.LINK)
                    .title(sanitizedName)
                    .url(sanitizedHref)
                    .area(PortalArea.TOP_NAVBAR)
                    .parentId(parentId)
                    .visibility(input.visibility())
                    .published(true)
                    .automationMetadata(
                        new AutomationMetadata(
                            AutomationMetadata.ReferenceType.PORTAL,
                            input.portalId().toString(),
                            null,
                            Optional.ofNullable(input.location()),
                            Optional.empty()
                        )
                    )
                    .build();
                navigationItemValidatorService.validateOne(toCreate, input.auditInfo().environmentId());
            }
            if (portalAutomationScopeEnforcer.portalExistsInEnvironment(input.auditInfo(), input.portalId())) {
                syncDomainService.validateForConflicts(input.auditInfo(), input.portalId().toString(), input.linkHrid(), input.location());
            } else {
                errors.add(Validator.Error.severe("the portal to attach the link to does not exist in this environment"));
            }
        } catch (AbstractDomainException e) {
            errors.add(Validator.Error.severe("%s", e.getMessage()));
        }

        if (errors.stream().anyMatch(Validator.Error::isSevere)) {
            return new Output(null, errors);
        }

        var link = syncDomainService.materialize(
            input.auditInfo(),
            input.portalId().toString(),
            input.linkHrid(),
            sanitizedName,
            sanitizedHref,
            input.location(),
            input.order(),
            input.visibility()
        );

        return new Output(link, errors);
    }
}
