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
package io.gravitee.apim.core.documentation.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.factory.PageModelFactory;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
@Slf4j
public class ValidatePagesDomainService implements Validator<ValidatePagesDomainService.Input> {

    private final ValidatePageSourceDomainService pageSourceValidator;
    private final DocumentationValidationDomainService validationDomainService;

    public record Input(String organisationId, String apiId, Map<String, PageCRD> pages) implements Validator.Input {
        ValidatePagesDomainService.Input sanitized(Map<String, PageCRD> sanitizedPages) {
            return new ValidatePagesDomainService.Input(organisationId, apiId, sanitizedPages);
        }
    }

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        if (input.pages() == null || input.pages().isEmpty()) {
            log.debug("no pages to validate and sanitize");
            return Result.ofValue(input);
        }
        log.debug("validating pages");

        List<Error> errors = new ArrayList<>();
        Map<String, PageCRD> sanitizedPages = new HashMap<>();

        input.pages.forEach((k, v) -> {
            try {
                Page page = PageModelFactory.fromCRDSpec(v);
                page.setReferenceId(input.apiId());

                pageSourceValidator
                    .validateAndSanitize(new ValidatePageSourceDomainService.Input(k, page.getSource()))
                    .peek(sanitized -> page.setSource(sanitized.source()), errors::addAll);

                Page sanitizedPage = validationDomainService.validateAndSanitizeForUpdate(page, input.organisationId, false);
                sanitizedPages.put(k, PageModelFactory.toCRDSpec(sanitizedPage));
            } catch (Exception e) {
                errors.add(Error.severe("invalid documentation page [%s]. Error: %s", k, e.getMessage()));
            }
        });

        return Result.ofBoth(input.sanitized(sanitizedPages), errors);
    }
}
