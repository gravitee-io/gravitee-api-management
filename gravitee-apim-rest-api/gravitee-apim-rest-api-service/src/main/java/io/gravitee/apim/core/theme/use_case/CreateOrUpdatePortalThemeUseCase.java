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
package io.gravitee.apim.core.theme.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.domain_service.ThemeDomainService;
import io.gravitee.apim.core.theme.domain_service.ValidateThemeDomainService;
import io.gravitee.apim.core.theme.model.NewTheme;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeAutomationMetadata;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.model.UpdateTheme;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOrUpdatePortalThemeUseCase {

    private final ValidateThemeDomainService validator;
    private final ThemeDomainService themeDomainService;
    private final ThemeCrudService themeCrudService;

    public record Output(Theme theme, List<Validator.Error> errors) {}

    public Output execute(ValidateThemeDomainService.Input input) {
        var validation = validator.validateAndSanitize(input);

        validation
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(errors.stream().map(Validator.Error::getMessage).collect(Collectors.joining(", ")));
            });

        var warnings = validation.warning().orElseGet(List::of);
        var sanitized = validation.value().orElseThrow(() -> new ValidationDomainException("Unable to sanitize theme"));

        var envId = sanitized.auditInfo().environmentId();
        var themeId = HRIDToUUID.portalTheme().context(sanitized.auditInfo()).hrid(sanitized.themeHrid()).id();
        var automationMetadata = new ThemeAutomationMetadata(sanitized.themeHrid());
        var existing = themeCrudService.findByIdAndEnvironmentId(themeId, envId);

        Theme saved;
        if (existing.isPresent()) {
            var toUpdate = UpdateTheme.builder()
                .id(themeId)
                .type(ThemeType.PORTAL_NEXT)
                .name(sanitized.name())
                .definitionPortalNext(sanitized.definitionPortalNext())
                .enabled(existing.get().isEnabled())
                .logo(sanitized.logo())
                .optionalLogo(sanitized.optionalLogo())
                .favicon(sanitized.favicon())
                .backgroundImage(sanitized.backgroundImage())
                .automationMetadata(automationMetadata)
                .build();
            saved = themeDomainService.update(toUpdate, existing.get());
        } else {
            var toCreate = NewTheme.builder()
                .name(sanitized.name())
                .type(ThemeType.PORTAL_NEXT)
                .referenceType(Theme.ReferenceType.ENVIRONMENT)
                .referenceId(envId)
                .definitionPortalNext(sanitized.definitionPortalNext())
                .enabled(false)
                .logo(sanitized.logo())
                .optionalLogo(sanitized.optionalLogo())
                .favicon(sanitized.favicon())
                .backgroundImage(sanitized.backgroundImage())
                .automationMetadata(automationMetadata)
                .build();
            saved = themeDomainService.create(toCreate, themeId);
        }

        return new Output(saved, warnings);
    }
}
