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
import io.gravitee.apim.core.theme.domain_service.ValidateThemeDomainService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeAutomationMetadata;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ValidatePortalThemeUseCase {

    private final ValidateThemeDomainService validator;

    public CreateOrUpdatePortalThemeUseCase.Output execute(ValidateThemeDomainService.Input input) {
        var result = validator.validateAndSanitize(input);
        var sanitized = result.value().orElse(input);
        List<Validator.Error> errors = result.errors().orElseGet(List::of);
        return new CreateOrUpdatePortalThemeUseCase.Output(toTheme(sanitized), errors);
    }

    private static Theme toTheme(ValidateThemeDomainService.Input sanitized) {
        var id = sanitized.themeHrid() == null || sanitized.themeHrid().isBlank()
            ? null
            : HRIDToUUID.portalTheme().context(sanitized.auditInfo()).hrid(sanitized.themeHrid()).id();
        var automationMetadata = sanitized.themeHrid() == null || sanitized.themeHrid().isBlank()
            ? null
            : new ThemeAutomationMetadata(sanitized.themeHrid());
        return Theme.builder()
            .id(id)
            .name(sanitized.name())
            .type(ThemeType.PORTAL_NEXT)
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .referenceId(sanitized.auditInfo().environmentId())
            .definitionPortalNext(sanitized.definitionPortalNext())
            .enabled(false)
            .logo(sanitized.logo())
            .optionalLogo(sanitized.optionalLogo())
            .favicon(sanitized.favicon())
            .backgroundImage(sanitized.backgroundImage())
            .automationMetadata(automationMetadata)
            .build();
    }
}
