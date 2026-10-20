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
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.model.factory.PageModelFactory;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.core.validation.Validator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
@CustomLog
public class ValidatePagesDomainService implements Validator<ValidatePagesDomainService.Input> {

    private final ValidatePageSourceDomainService pageSourceValidator;
    private final ValidatePageAccessControlsDomainService accessControlsValidator;
    private final DocumentationValidationDomainService validationDomainService;
    private final PageCrudService pageCrudService;

    public record Input(AuditInfo auditInfo, String apiId, String apiHrid, Map<String, PageCRD> pages) implements Validator.Input {
        ValidatePagesDomainService.Input sanitized(Map<String, PageCRD> sanitizedPages) {
            return new ValidatePagesDomainService.Input(auditInfo, apiId, apiHrid, sanitizedPages);
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

        input.pages.forEach((key, pageCRD) -> {
            try {
                Page page = PageModelFactory.fromCRDSpec(key, pageCRD);
                page.setReferenceId(input.apiId());
                page.setHrid(key);

                if (belongsToAnotherReference(page.getId(), input.apiId())) {
                    errors.add(
                        Error.severe("invalid documentation page [%s]. Error: page id [%s] does not belong to this API", key, page.getId())
                    );
                }
                if (belongsToAnotherReference(page.getParentId(), input.apiId())) {
                    errors.add(
                        Error.severe(
                            "invalid documentation page [%s]. Error: parent page [%s] does not belong to this API",
                            key,
                            page.getParentId()
                        )
                    );
                }

                pageSourceValidator
                    .validateAndSanitize(new ValidatePageSourceDomainService.Input(input.auditInfo.organizationId(), key, page.getSource()))
                    .peek(sanitized -> page.setSource(sanitized.source()), errors::addAll);

                accessControlsValidator
                    .validateAndSanitize(new ValidatePageAccessControlsDomainService.Input(input.auditInfo, page.getAccessControls()))
                    .peek(sanitized -> page.setAccessControls(sanitized.accessControls()), errors::addAll);

                if (errors.stream().noneMatch(Error::isSevere)) {
                    Page sanitizedPage = validationDomainService.validateAndSanitizeForUpdate(
                        page,
                        input.auditInfo.organizationId(),
                        false
                    );
                    sanitizedPages.put(key, PageModelFactory.toCRDSpec(sanitizedPage));
                }

                if (page.getType() != Page.Type.ROOT && StringUtils.isEmpty(page.getName())) {
                    errors.add(Error.severe("Page [%s] is missing required property [name]", key));
                }
            } catch (Exception e) {
                errors.add(Error.severe("invalid documentation page [%s]. Error: %s", key, e.getMessage()));
            }
        });

        return Result.ofBoth(input.sanitized(sanitizedPages), errors);
    }

    // Page ids in a spec are lookup keys: an import only writes pages of the API it imports
    private boolean belongsToAnotherReference(String pageId, String apiId) {
        if (pageId == null) {
            return false;
        }
        return pageCrudService
            .findById(pageId)
            .map(existing -> existing.getReferenceType() != Page.ReferenceType.API || !Objects.equals(existing.getReferenceId(), apiId))
            .orElse(false);
    }
}
