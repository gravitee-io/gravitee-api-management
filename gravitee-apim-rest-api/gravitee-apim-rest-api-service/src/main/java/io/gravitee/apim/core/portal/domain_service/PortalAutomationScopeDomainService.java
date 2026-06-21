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
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalAutomationScopeDomainService {

    private final PortalCrudService portalCrudService;
    private final PortalAutomationProperties properties;

    public boolean isDefaultPortal(AuditInfo auditInfo, PortalId portalId) {
        return portalCrudService.findByIdAndEnvironmentId(portalId, auditInfo.environmentId()).isPresent();
    }

    public List<Validator.Error> validate(AuditInfo auditInfo, PortalId portalId, String fieldName) {
        if (hasEstablishedPortalConflict(auditInfo, portalId)) {
            return List.of(Validator.Error.severe("%s does not match the established portal for this environment", fieldName));
        }
        return List.of();
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
