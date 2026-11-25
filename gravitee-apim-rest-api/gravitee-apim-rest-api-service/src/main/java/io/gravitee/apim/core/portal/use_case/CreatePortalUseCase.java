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
package io.gravitee.apim.core.portal.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.domain_service.PortalAddressDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalNameDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalQuotaDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalSummaryDomainService;
import io.gravitee.apim.core.portal.exception.PortalLimitReachedException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalSummary;
import io.gravitee.apim.core.portal_page.domain_service.PortalStarterContentDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreatePortalUseCase {

    private final PortalNameDomainService portalNameDomainService;
    private final PortalQuotaDomainService portalQuotaDomainService;
    private final PortalAddressDomainService portalAddressDomainService;
    private final PortalCrudService portalCrudService;
    private final PortalStarterContentDomainService portalStarterContentDomainService;
    private final PortalSummaryDomainService portalSummaryDomainService;

    public record Input(AuditInfo auditInfo, String name, @Nullable String url) {}

    public record Output(PortalSummary portal) {}

    /** The payload is checked before the limit, so a form shows its errors next to the fields first. */
    public Output execute(Input input) {
        var organizationId = input.auditInfo().organizationId();
        var environmentId = input.auditInfo().environmentId();
        var name = portalNameDomainService.validateAndNormalize(input.name());
        if (input.url() != null) {
            throw new ValidationDomainException(
                "A portal URL cannot be chosen yet: leave it out to serve the portal at the environment's portal URL"
            );
        }

        var portals = portalCrudService.findByEnvironmentId(environmentId);
        ensureDefaultPortal(portals);
        var quota = portalQuotaDomainService.quota(organizationId, portals.size());
        if (quota.used() >= quota.limit()) {
            throw new PortalLimitReachedException(quota.limit());
        }
        portalNameDomainService.ensureUnique(environmentId, name, null);
        // On a multi-tenant installation, Cloud owns the access points: the portal gets none.
        var accessPointId = portalAddressDomainService.sharedAccessPoint(environmentId);
        if (accessPointId.isEmpty() && portalAddressDomainService.assignsAccessPoints()) {
            throw new ValidationDomainException("The environment has no valid portal URL to serve the portal at");
        }

        var created = portalCrudService.create(Portal.createFromConsole(environmentId, organizationId, name, accessPointId.orElse(null)));
        seedOrUndo(organizationId, environmentId, created);
        return new Output(portalSummaryDomainService.summarize(environmentId, List.of(created)).getFirst());
    }

    /**
     * Without a default portal, the environment's lone portal is taken for an Automation API one, so a console portal
     * stored first would stay the only portal and never get a default one next to it.
     */
    private static void ensureDefaultPortal(List<Portal> environmentPortals) {
        if (environmentPortals.stream().anyMatch(Portal::isDefault)) {
            return;
        }
        throw new ValidationDomainException(
            environmentPortals.isEmpty()
                ? "The environment has no default portal yet: set its portal URL first"
                : "The environment's portals are managed through the Automation API"
        );
    }

    /**
     * Nothing is transactional: a portal left half seeded would hold its name and a place in the license limit, with
     * nothing to seed it again, so the portal and what was seeded are deleted before the failure is reported.
     */
    private void seedOrUndo(String organizationId, String environmentId, Portal created) {
        var owner = new NavigationItemReference.PortalReference(created.getId());
        try {
            portalStarterContentDomainService.seed(organizationId, environmentId, owner);
        } catch (RuntimeException seedingFailure) {
            // Kept when its items cannot be deleted: left to no portal, moveOrphans would give them to the default one
            if (undo(seedingFailure, () -> portalStarterContentDomainService.deleteAll(environmentId, owner))) {
                undo(seedingFailure, () -> portalCrudService.delete(created.getId()));
            }
            throw seedingFailure;
        }
    }

    private static boolean undo(RuntimeException seedingFailure, Runnable step) {
        try {
            step.run();
            return true;
        } catch (RuntimeException undoFailure) {
            seedingFailure.addSuppressed(undoFailure);
            return false;
        }
    }
}
