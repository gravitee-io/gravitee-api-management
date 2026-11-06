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
import io.gravitee.apim.core.portal.domain_service.ValidatePortalDomainService;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ValidatePortalUseCase {

    private final ValidatePortalDomainService validator;

    /** What a dry run reports: nothing is created, so there is no portal, only what was declared. */
    public record Output(
        PortalId portalId,
        String name,
        PortalNavigationStructure structure,
        String activeThemeHrid,
        List<Validator.Error> errors
    ) {}

    public Output execute(CreateOrUpdatePortalUseCase.Input input) {
        var declared = new ValidatePortalDomainService.Input(input.auditInfo(), input.portalId(), input.structure());
        var result = validator.validateAndSanitize(declared);
        List<Validator.Error> errors = result.errors().orElseGet(List::of);
        var sanitized = result.value().orElse(declared);
        return new Output(sanitized.portalId(), input.name(), sanitized.structure(), input.activeThemeHrid(), errors);
    }
}
