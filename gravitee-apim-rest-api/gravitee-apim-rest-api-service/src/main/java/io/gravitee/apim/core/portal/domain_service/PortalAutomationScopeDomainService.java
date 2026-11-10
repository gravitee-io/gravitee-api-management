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
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalAutomationScopeDomainService {

    private final PortalCrudService portalCrudService;
    private final PortalAutomationProperties properties;

    /**
     * Whether a portal with this id is present in this environment — nothing about it being the
     * environment's established or "default" one. Whether a <em>second</em> portal may exist at all is
     * a separate rule, enforced by {@link #validate}.
     */
    public boolean portalExistsInEnvironment(AuditInfo auditInfo, PortalId portalId) {
        return portalCrudService.findByIdAndEnvironmentId(portalId, auditInfo.environmentId()).isPresent();
    }

    public List<Validator.Error> validate(AuditInfo auditInfo, PortalId portalId, String fieldName) {
        if (hasEstablishedPortalConflict(auditInfo, portalId)) {
            return List.of(Validator.Error.severe("%s does not match the established portal for this environment", fieldName));
        }
        return List.of();
    }

    /**
     * {@link #validate} for a write of the portal itself, the only one allowed to take over a lone default portal:
     * a listing, a page or a link naming another portal has nothing to replace it with.
     */
    public List<Validator.Error> validatePortal(AuditInfo auditInfo, PortalId portalId, String fieldName) {
        return findReplaceableDefault(auditInfo, portalId).isPresent() ? List.of() : validate(auditInfo, portalId, fieldName);
    }

    /**
     * The default portal that the first Automation API portal of this environment takes over from.
     *
     * @return the environment's only portal, when it is the default one and is not {@code portalId}; the default
     *     marker is enough to recognize it, since an Automation API portal is never default
     */
    public Optional<Portal> findReplaceableDefault(AuditInfo auditInfo, PortalId portalId) {
        var portals = portalCrudService.findByEnvironmentId(auditInfo.environmentId());
        if (portals.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(portals.getFirst())
            .filter(Portal::isDefault)
            .filter(portal -> !portal.getId().equals(portalId));
    }

    private boolean hasEstablishedPortalConflict(AuditInfo auditInfo, PortalId portalId) {
        if (properties.allowMultiplePortalPerEnv()) {
            return false;
        }
        return portalCrudService
            .findByEnvironmentId(auditInfo.environmentId())
            .stream()
            .anyMatch(p -> !p.getId().equals(portalId));
    }
}
