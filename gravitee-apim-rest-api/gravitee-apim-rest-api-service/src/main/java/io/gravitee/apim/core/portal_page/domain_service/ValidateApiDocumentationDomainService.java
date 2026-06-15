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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;

/**
 * Validates API Documentation input. Format checks only — no reference-existence checks
 * (parent API). Missing parent is tolerated as orphan per the orphan-tolerance design;
 * the doc materializes once the missing CRD applies.
 *
 * @author GraviteeSource Team
 */
@DomainService
public class ValidateApiDocumentationDomainService implements Validator<ValidateApiDocumentationDomainService.Input> {

    public record Input(
        AuditInfo auditInfo,
        PortalPageContentId portalPageContentId,
        String apiId,
        String name,
        PortalPageContentType type,
        String content,
        String location,
        Integer order
    ) implements Validator.Input {}

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();

        if (input.name() == null || input.name().isBlank()) {
            errors.add(Error.severe("name must not be blank"));
        }
        if (input.type() == null) {
            errors.add(Error.severe("type must not be null"));
        }
        if (input.content() == null) {
            errors.add(Error.severe("content must not be null"));
        }
        if (input.location() != null) {
            errors.addAll(NavigationPathValidator.validate(input.location(), "location"));
        }

        return Result.ofBoth(input, errors);
    }
}
