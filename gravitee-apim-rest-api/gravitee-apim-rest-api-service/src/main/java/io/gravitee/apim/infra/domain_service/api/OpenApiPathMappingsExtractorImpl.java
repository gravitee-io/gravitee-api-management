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
package io.gravitee.apim.infra.domain_service.api;

import io.gravitee.apim.core.api.domain_service.OpenApiPathMappingsExtractor;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.infra.converter.oai.OAIPathMappings;
import io.gravitee.rest.api.service.impl.swagger.parser.OAIParser;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OpenApiPathMappingsExtractorImpl implements OpenApiPathMappingsExtractor {

    @Override
    public List<String> extract(String openApiContent) {
        if (openApiContent == null || openApiContent.isBlank()) {
            throw new ValidationDomainException("Swagger page content must be inline");
        }
        if (UrlSanitizerUtils.isUrl(openApiContent)) {
            throw new ValidationDomainException("Swagger page content must be inline");
        }
        try {
            var options = new ParseOptions();
            options.setResolve(false);
            options.setResolveFully(false);
            var descriptor = new OAIParser().parse(openApiContent, options);
            if (descriptor == null || descriptor.getSpecification() == null) {
                throw new ValidationDomainException("Invalid OpenAPI specification");
            }
            return OAIPathMappings.from(descriptor.getSpecification());
        } catch (ValidationDomainException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationDomainException("Invalid OpenAPI specification");
        }
    }
}
