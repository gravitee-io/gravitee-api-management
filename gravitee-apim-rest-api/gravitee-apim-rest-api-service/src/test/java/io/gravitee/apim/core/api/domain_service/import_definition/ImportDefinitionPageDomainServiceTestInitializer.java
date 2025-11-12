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

import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PageRevisionCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.util.stream.Stream;

public class ImportDefinitionPageDomainServiceTestInitializer {

    // In Memory
    public final AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    public final PageCrudServiceInMemory pageCrudServiceInMemory = new PageCrudServiceInMemory();
    public final PageQueryServiceInMemory pageQueryServiceInMemory = new PageQueryServiceInMemory();
    public final PageRevisionCrudServiceInMemory pageRevisionCrudServiceInMemory = new PageRevisionCrudServiceInMemory();
    public final PlanQueryServiceInMemory planQueryServiceInMemory = new PlanQueryServiceInMemory();
    public final UserCrudServiceInMemory userCrudServiceInMemory = new UserCrudServiceInMemory();

    // Domain Services
    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;
    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;

    ImportDefinitionPageDomainServiceTestInitializer() {
        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudServiceInMemory, new JacksonJsonDiffProcessor());
        apiDocumentationDomainService = new ApiDocumentationDomainService(pageQueryServiceInMemory, planQueryServiceInMemory);
        createApiDocumentationDomainService = new CreateApiDocumentationDomainService(
            pageCrudServiceInMemory,
            pageRevisionCrudServiceInMemory,
            auditDomainService,
            new IndexerInMemory()
        );
        updateApiDocumentationDomainService = new UpdateApiDocumentationDomainService(
            pageCrudServiceInMemory,
            pageRevisionCrudServiceInMemory,
            auditDomainService,
            new IndexerInMemory()
        );
    }

    ImportDefinitionPageDomainService initialize() {
        return new ImportDefinitionPageDomainService(
            apiDocumentationDomainService,
            createApiDocumentationDomainService,
            updateApiDocumentationDomainService
        );
    }

    void tearDown() {
        Stream.of(
            auditCrudServiceInMemory,
            pageCrudServiceInMemory,
            pageQueryServiceInMemory,
            planQueryServiceInMemory,
            userCrudServiceInMemory
        ).forEach(InMemoryAlternative::reset);
    }
}
