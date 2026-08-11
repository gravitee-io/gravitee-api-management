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
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalNavigationSyncDomainService;
import io.gravitee.apim.core.portal.domain_service.ValidatePortalDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal_page.domain_service.PortalDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOrUpdatePortalUseCase {

    private final ValidatePortalDomainService validator;
    private final PortalCrudService portalCrudService;
    private final PortalNavigationSyncDomainService portalNavigationSyncDomainService;
    private final PortalPageContentQueryService portalPageContentQueryService;
    private final PortalDocumentationSyncDomainService portalDocumentationSyncDomainService;
    private final PortalAutomationScopeDomainService portalAutomationScopeEnforcer;

    public record Input(AuditInfo auditInfo, Portal portal, PortalNavigationStructure structure) {
        public Input(AuditInfo auditInfo, Portal portal) {
            this(auditInfo, portal, PortalNavigationStructure.empty());
        }
    }

    public record Output(Portal portal, PortalNavigationStructure structure, List<Validator.Error> errors) {}

    public Output execute(Input input) {
        var validation = validator.validateAndSanitize(
            new ValidatePortalDomainService.Input(input.auditInfo(), input.portal(), input.structure())
        );

        validation
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(errors.stream().map(Validator.Error::getMessage).collect(Collectors.joining(", ")));
            });

        var warnings = validation.warning().orElseGet(List::of);

        var sanitized = validation.value().orElseThrow(() -> new ValidationDomainException("Unable to sanitize portal"));
        var existing = portalCrudService.findByIdAndEnvironmentId(sanitized.portal().getId(), input.auditInfo().environmentId());
        var previouslyPersisted = existing.map(Portal::getNavigationStructure).orElseGet(PortalNavigationStructure::empty);
        portalNavigationSyncDomainService.validateForConflicts(
            input.auditInfo(),
            sanitized.portal().getId(),
            previouslyPersisted,
            sanitized.structure()
        );
        var portalToSave = sanitized.portal().withNavigationStructure(sanitized.structure());
        var saved = existing.isPresent() ? portalCrudService.update(portalToSave) : portalCrudService.create(portalToSave);
        var isDefault = portalAutomationScopeEnforcer.isDefaultPortal(input.auditInfo(), saved.getId());
        // Skip nav-tree materialization for non-default portals — app is not ready for that.
        if (isDefault) {
            portalNavigationSyncDomainService.sync(input.auditInfo(), saved.getId(), previouslyPersisted, sanitized.structure());
            portalPageContentQueryService
                .findByReference(input.auditInfo().environmentId(), AutomationMetadata.ReferenceType.PORTAL, saved.getId().toString())
                .forEach(pc -> portalDocumentationSyncDomainService.materialize(input.auditInfo(), pc));
        }
        return new Output(saved, saved.getNavigationStructure(), warnings);
    }
}
