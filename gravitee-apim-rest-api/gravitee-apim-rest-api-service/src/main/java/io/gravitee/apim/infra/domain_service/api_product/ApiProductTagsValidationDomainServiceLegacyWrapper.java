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
package io.gravitee.apim.infra.domain_service.api_product;

import io.gravitee.apim.core.api_product.domain_service.ApiProductTagsValidationDomainService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ApiProductTagsValidationDomainServiceLegacyWrapper implements ApiProductTagsValidationDomainService {

    private final TagsValidationService tagsValidationService;

    @Override
    public void validateAndSanitize(String organizationId, String environmentId, Set<String> existingTags, Set<String> newTags) {
        tagsValidationService.validateAndSanitize(new ExecutionContext(organizationId, environmentId), existingTags, newTags);
    }
}
