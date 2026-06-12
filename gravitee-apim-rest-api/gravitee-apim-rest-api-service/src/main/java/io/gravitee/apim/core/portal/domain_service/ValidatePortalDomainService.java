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
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates Portal input. Format checks only — no reference-existence checks.
 *
 * @author GraviteeSource Team
 */
@DomainService
public class ValidatePortalDomainService implements Validator<ValidatePortalDomainService.Input> {

    public record Input(AuditInfo auditInfo, Portal portal, List<NavigationPath> navigation) implements Validator.Input {}

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();
        List<NavigationPath> navigation = input.navigation() == null ? List.of() : input.navigation();
        for (int i = 0; i < navigation.size(); i++) {
            errors.addAll(NavigationPathValidator.validate(navigation.get(i).path(), "navigation[" + i + "].path"));
        }
        return Result.ofBoth(input, errors);
    }
}
