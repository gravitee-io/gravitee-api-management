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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.documentation.exception.InvalidPageNameException;
import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.core.sanitizer.SanitizeResult;
import io.gravitee.rest.api.service.exceptions.PageContentUnsafeException;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DocumentationValidationDomainService {

    private final HtmlSanitizer htmlSanitizer;
    private final TemplateResolverDomainService templateResolverDomainService;
    private final ApiCrudService apiCrudService;

    public String sanitizeDocumentationName(String name) {
        if (null == name || name.trim().isEmpty()) {
            throw new InvalidPageNameException();
        }
        return name.trim();
    }

    public void validateContent(String content, String apiId) {
        this.validateContentIsSafe(content);
        this.validateTemplate(content, apiId);
    }

    public void validateContentIsSafe(String content) {
        final SanitizeResult sanitizeInfos = htmlSanitizer.isSafe(content);
        if (!sanitizeInfos.isSafe()) {
            throw new PageContentUnsafeException(sanitizeInfos.getRejectedMessage());
        }
    }

    public void validateTemplate(String pageContent, String apiId) {
        this.templateResolverDomainService.resolveTemplate(pageContent, Map.of("api", this.apiCrudService.get(apiId)));
    }
}
