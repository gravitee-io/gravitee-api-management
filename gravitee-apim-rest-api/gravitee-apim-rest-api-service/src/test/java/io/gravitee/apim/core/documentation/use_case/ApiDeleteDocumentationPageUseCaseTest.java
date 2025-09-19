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
package io.gravitee.apim.core.documentation.use_case;

import static org.assertj.core.api.Assertions.*;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PageRevisionCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.domain_service.DeleteApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.DefinitionVersion;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiDeleteDocumentationPageUseCaseTest {

    private static final Api API = Api.builder().id("api-id").definitionVersion(DefinitionVersion.V4).build();
    public static final String PAGE_ID = "page-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    private final PageRevisionCrudServiceInMemory pageRevisionCrudService = new PageRevisionCrudServiceInMemory();
    private final PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final IndexerInMemory indexer = new IndexerInMemory();

    private ApiDeleteDocumentationPageUseCase cut;

    @BeforeEach
    void setUp() {
        final AuditDomainService auditDomainService = new AuditDomainService(
            auditCrudService,
            userCrudService,
            new JacksonJsonDiffProcessor()
        );
        cut = new ApiDeleteDocumentationPageUseCase(
            new DeleteApiDocumentationDomainService(
                pageCrudService,
                pageQueryService,
                auditDomainService,
                new UpdateApiDocumentationDomainService(pageCrudService, pageRevisionCrudService, auditDomainService, indexer),
                planQueryService,
                indexer
            ),
            apiCrudService
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(pageCrudService, pageRevisionCrudService, pageQueryService, planQueryService, auditCrudService, userCrudService).forEach(
            InMemoryAlternative::reset
        );
    }

    @Test
    void should_throw_if_api_not_found() {
        assertThatThrownBy(() -> cut.execute(new ApiDeleteDocumentationPageUseCase.Input(API.getId(), PAGE_ID, AUDIT_INFO))).isInstanceOf(
            ApiNotFoundException.class
        );
    }

    @Test
    void should_delete_page() {
        final Page page = Page.builder()
            .id(PAGE_ID)
            .referenceId(API.getId())
            .referenceType(Page.ReferenceType.API)
            .type(Page.Type.MARKDOWN)
            .build();
        pageCrudService.initWith(List.of(page));
        pageQueryService.initWith(List.of(page));
        apiCrudService.initWith(List.of(API));
        assertThat(pageCrudService.findById(PAGE_ID)).isPresent();
        cut.execute(new ApiDeleteDocumentationPageUseCase.Input(API.getId(), PAGE_ID, AUDIT_INFO));
        assertThat(pageCrudService.findById(PAGE_ID)).isEmpty();
    }
}
