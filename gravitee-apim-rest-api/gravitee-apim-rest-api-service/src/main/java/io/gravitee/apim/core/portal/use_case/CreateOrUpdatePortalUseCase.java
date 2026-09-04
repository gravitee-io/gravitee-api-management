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
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.domain_service.CurrentThemeDomainService;
import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeAutomationMetadata;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import java.util.Objects;
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
    private final ThemeCrudService themeCrudService;
    private final CurrentThemeDomainService currentThemeDomainService;

    public record Input(AuditInfo auditInfo, Portal portal, PortalNavigationStructure structure, String activeThemeHrid) {
        public Input(AuditInfo auditInfo, Portal portal) {
            this(auditInfo, portal, PortalNavigationStructure.empty(), null);
        }

        public Input(AuditInfo auditInfo, Portal portal, PortalNavigationStructure structure) {
            this(auditInfo, portal, structure, null);
        }
    }

    public record Output(Portal portal, PortalNavigationStructure structure, String activeThemeHrid, List<Validator.Error> errors) {
        public Output(Portal portal, PortalNavigationStructure structure, List<Validator.Error> errors) {
            this(portal, structure, null, errors);
        }
    }

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
        var resolvedActiveThemeId = resolveActiveThemeId(input, existing.map(Portal::getActiveThemeId).orElse(null));
        var portalToSave = sanitized.portal().withNavigationStructure(sanitized.structure()).withActiveThemeId(resolvedActiveThemeId);
        var saved = existing.isPresent() ? portalCrudService.update(portalToSave) : portalCrudService.create(portalToSave);
        var portalExists = portalAutomationScopeEnforcer.portalExistsInEnvironment(input.auditInfo(), saved.getId());
        if (portalExists) {
            portalNavigationSyncDomainService.sync(input.auditInfo(), saved.getId(), previouslyPersisted, sanitized.structure());
            portalPageContentQueryService
                .findByReference(input.auditInfo().environmentId(), AutomationMetadata.ReferenceType.PORTAL, saved.getId().toString())
                .forEach(pc -> portalDocumentationSyncDomainService.materialize(input.auditInfo(), pc));
        }
        var activeThemeHrid = reverseResolveActiveThemeHrid(input, saved.getActiveThemeId());
        return new Output(saved, saved.getNavigationStructure(), activeThemeHrid, warnings);
    }

    private String reverseResolveActiveThemeHrid(Input input, String activeThemeId) {
        if (activeThemeId == null) {
            return null;
        }
        if (input.activeThemeHrid() != null) {
            return input.activeThemeHrid();
        }
        return themeCrudService
            .findByIdAndEnvironmentId(activeThemeId, input.auditInfo().environmentId())
            .map(Theme::getAutomationMetadata)
            .map(ThemeAutomationMetadata::hrid)
            .orElse(null);
    }

    private String resolveActiveThemeId(Input input, String currentActiveThemeId) {
        var targetThemeId = input.activeThemeHrid() == null
            ? null
            : HRIDToUUID.portalTheme().context(input.auditInfo()).hrid(input.activeThemeHrid()).id();
        if (Objects.equals(targetThemeId, currentActiveThemeId)) {
            return targetThemeId;
        }
        var environmentId = input.auditInfo().environmentId();
        if (targetThemeId == null) {
            themeCrudService.findByIdAndEnvironmentId(currentActiveThemeId, environmentId).ifPresent(currentThemeDomainService::deactivate);
            return null;
        }
        var target = themeCrudService
            .findByIdAndEnvironmentId(targetThemeId, environmentId)
            .orElseThrow(() -> new ThemeNotFoundException(input.activeThemeHrid()));
        currentThemeDomainService.activate(target);
        return targetThemeId;
    }
}
