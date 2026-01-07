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
package io.gravitee.apim.core.api.domain_service.import_definition;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.audit.model.AuditInfo;
import java.util.Set;
import lombok.CustomLog;

@DomainService
class ImportDefinitionMetadataDomainService {

    private final ApiMetadataDomainService apiMetadataDomainService;

    ImportDefinitionMetadataDomainService(ApiMetadataDomainService apiMetadataDomainService) {
        this.apiMetadataDomainService = apiMetadataDomainService;
    }

    void upsertMetadata(String apiId, Set<NewApiMetadata> metadataSet, AuditInfo auditInfo) {
        if (metadataSet != null) {
            apiMetadataDomainService.saveApiMetadata(
                apiId,
                metadataSet
                    .stream()
                    .map(importMetadata ->
                        ApiMetadata.builder()
                            .apiId(apiId)
                            .key(importMetadata.getKey())
                            .format(importMetadata.getFormat())
                            .name(importMetadata.getName())
                            .value(importMetadata.getValue())
                            .defaultValue(importMetadata.getDefaultValue())
                            .build()
                    )
                    .toList(),
                auditInfo
            );
        }
    }
}
