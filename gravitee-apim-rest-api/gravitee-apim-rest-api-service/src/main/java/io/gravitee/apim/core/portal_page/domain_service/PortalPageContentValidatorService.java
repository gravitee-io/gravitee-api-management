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
import io.gravitee.apim.core.portal_page.exception.PortalPageContentTooLargeException;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalPageContentValidatorService {

    private static final int MAX_CONTENT_SIZE_IN_MEGA_BYTES = 10;
    private static final int MAX_CONTENT_SIZE_IN_BYTES = MAX_CONTENT_SIZE_IN_MEGA_BYTES * 1024 * 1024;

    private final List<PortalPageContentValidator> validators;

    public void validateForUpdate(PortalPageContent<?> existingContent, UpdatePortalPageContent updateContent) {
        validators
            .stream()
            .filter(validator -> validator.appliesTo(existingContent))
            .forEach(validator -> validator.validate(existingContent, updateContent));
    }

    /**
     * Deliberately not part of {@link #validateForUpdate}: the cap constrains the Management API update
     * endpoint (file import) only, so Automation API flows keep accepting existing definitions unchanged.
     */
    public void validateContentSize(String content) {
        if (content != null && content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_SIZE_IN_BYTES) {
            throw new PortalPageContentTooLargeException(MAX_CONTENT_SIZE_IN_MEGA_BYTES);
        }
    }
}
