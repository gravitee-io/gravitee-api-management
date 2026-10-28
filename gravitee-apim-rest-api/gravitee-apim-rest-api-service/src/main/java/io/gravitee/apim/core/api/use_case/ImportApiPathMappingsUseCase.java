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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ApiPathMappingsDomainService;
import io.gravitee.apim.core.api.domain_service.OpenApiPathMappingsExtractor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class ImportApiPathMappingsUseCase {

    private final ApiPathMappingsDomainService apiPathMappingsDomainService;
    private final PageCrudService pageCrudService;
    private final OpenApiPathMappingsExtractor openApiPathMappingsExtractor;

    public record Input(String apiId, String pageId, boolean overwrite, AuditInfo auditInfo) {}

    public record Output(Set<String> pathMappings) {}

    public Output execute(Input input) {
        var api = apiPathMappingsDomainService.loadEditableHttpV4Api(input.apiId(), input.auditInfo());
        var page = pageCrudService.findById(input.pageId()).orElseThrow(() -> new PageNotFoundException(input.pageId()));
        if (page.getReferenceType() != Page.ReferenceType.API || !input.apiId().equals(page.getReferenceId())) {
            throw new PageNotFoundException(input.pageId());
        }
        if (page.getType() != Page.Type.SWAGGER) {
            throw new ValidationDomainException("Page must be a SWAGGER documentation page");
        }

        var extracted = openApiPathMappingsExtractor.extract(page.getContent());
        Set<String> toStore;
        if (input.overwrite()) {
            toStore = new LinkedHashSet<>(extracted);
        } else {
            toStore = new LinkedHashSet<>(apiPathMappingsDomainService.currentMappings(api));
            toStore.addAll(extracted);
        }
        return new Output(apiPathMappingsDomainService.replace(api, toStore, input.auditInfo()));
    }
}
