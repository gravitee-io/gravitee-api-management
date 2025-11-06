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

import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.util.stream.Stream;

public class ImportDefinitionMetadataDomainServiceTestInitializer {

    // In Memory
    public final ApiMetadataQueryServiceInMemory apiMetadataQueryServiceInMemory = new ApiMetadataQueryServiceInMemory();
    public final AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    public final MetadataCrudServiceInMemory metadataCrudServiceInMemory = new MetadataCrudServiceInMemory();
    public final UserCrudServiceInMemory userCrudServiceInMemory = new UserCrudServiceInMemory();

    // Domain Services
    private final ApiMetadataDomainService apiMetadataDomainService;

    ImportDefinitionMetadataDomainServiceTestInitializer() {
        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudServiceInMemory, new JacksonJsonDiffProcessor());

        this.apiMetadataDomainService = new ApiMetadataDomainService(
            metadataCrudServiceInMemory,
            apiMetadataQueryServiceInMemory,
            auditDomainService
        );
    }

    ImportDefinitionMetadataDomainService initialize() {
        return new ImportDefinitionMetadataDomainService(apiMetadataDomainService);
    }

    void tearDown() {
        Stream.of(apiMetadataQueryServiceInMemory, auditCrudServiceInMemory, metadataCrudServiceInMemory, userCrudServiceInMemory).forEach(
            InMemoryAlternative::reset
        );
    }
}
