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
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.search.model.IndexablePage;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiPublishDocumentationPageUseCaseTest {

    private final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    private final PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final PageRevisionCrudServiceInMemory pageRevisionCrudService = new PageRevisionCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final IndexerInMemory indexer = new IndexerInMemory();

    private ApiPublishDocumentationPageUseCase useCase;
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String API_ID = "api-id";
    private static final String PAGE_ID = "page-id";
    private static final Date DATE = new Date();

    @BeforeEach
    void setUp() {
        var apiDocumentationDomainService = new ApiDocumentationDomainService(pageQueryService, planQueryService);
        var updateDocumentationDomainService = new UpdateApiDocumentationDomainService(
            pageCrudService,
            pageRevisionCrudService,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            indexer
        );
        useCase = new ApiPublishDocumentationPageUseCase(
            apiDocumentationDomainService,
            updateDocumentationDomainService,
            apiCrudService,
            pageCrudService
        );
    }

    @AfterEach
    void tearDown() {
        pageCrudService.reset();
        pageQueryService.reset();
        apiCrudService.reset();
        indexer.reset();
    }

    @Test
    void should_publish_markdown() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID)
                    .type(Page.Type.MARKDOWN)
                    .published(false)
                    .createdAt(DATE)
                    .build()
            )
        );
        var res = useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, PAGE_ID, AUDIT_INFO)).page();
        assertThat(res).isNotNull().hasFieldOrPropertyWithValue("id", PAGE_ID).hasFieldOrPropertyWithValue("published", true);
    }

    @Test
    void should_publish_folder() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID)
                    .type(Page.Type.FOLDER)
                    .published(false)
                    .createdAt(DATE)
                    .build()
            )
        );
        var res = useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, PAGE_ID, AUDIT_INFO)).page();
        assertThat(res)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", PAGE_ID)
            .hasFieldOrPropertyWithValue("published", true)
            .hasFieldOrPropertyWithValue("hidden", true);
    }

    @Test
    void should_publish_folder_and_has_published_children() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID)
                    .type(Page.Type.FOLDER)
                    .published(false)
                    .createdAt(DATE)
                    .build(),
                Page.builder().id("child").parentId(PAGE_ID).published(true).build()
            )
        );
        var res = useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, PAGE_ID, AUDIT_INFO)).page();
        assertThat(res)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", PAGE_ID)
            .hasFieldOrPropertyWithValue("published", true)
            .hasFieldOrPropertyWithValue("hidden", false);
    }

    @Test
    void should_create_audit() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID)
                    .type(Page.Type.MARKDOWN)
                    .published(false)
                    .createdAt(DATE)
                    .build()
            )
        );
        useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, PAGE_ID, AUDIT_INFO)).page();
        assertThat(auditCrudService.storage().get(0)).isNotNull().hasFieldOrPropertyWithValue("event", "PAGE_UPDATED");
    }

    @Test
    void should_index_if_markdown() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID)
                    .type(Page.Type.MARKDOWN)
                    .published(false)
                    .createdAt(DATE)
                    .build()
            )
        );
        useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, PAGE_ID, AUDIT_INFO)).page();
        assertThat(indexer.storage()).extracting("id").contains(PAGE_ID);
    }

    @Test
    void should_throw_error_if_page_already_published() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID)
                    .type(Page.Type.MARKDOWN)
                    .published(true)
                    .createdAt(DATE)
                    .build()
            )
        );
        assertThatThrownBy(() -> useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, PAGE_ID, AUDIT_INFO))).isInstanceOf(
            ValidationDomainException.class
        );
    }

    @Test
    void should_throw_error_if_api_does_not_exist() {
        initPageServices(
            List.of(
                Page.builder()
                    .id("page#1")
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID)
                    .type(Page.Type.MARKDOWN)
                    .createdAt(DATE)
                    .build()
            )
        );
        assertThatThrownBy(() -> useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, PAGE_ID, AUDIT_INFO))).isInstanceOf(
            ApiNotFoundException.class
        );
    }

    @Test
    void should_throw_error_if_page_not_associated_to_api_by_id() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.API)
                    .referenceId(API_ID + "-bad")
                    .type(Page.Type.MARKDOWN)
                    .published(false)
                    .createdAt(DATE)
                    .build()
            )
        );
        assertThatThrownBy(() -> useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, PAGE_ID, AUDIT_INFO))).isInstanceOf(
            ValidationDomainException.class
        );
    }

    @Test
    void should_throw_error_if_page_not_associated_to_api_by_reference_type() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(
            List.of(
                Page.builder()
                    .id(PAGE_ID)
                    .referenceType(Page.ReferenceType.ENVIRONMENT)
                    .referenceId(API_ID)
                    .type(Page.Type.MARKDOWN)
                    .build()
            )
        );
        assertThatThrownBy(() -> useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, PAGE_ID, AUDIT_INFO))).isInstanceOf(
            ValidationDomainException.class
        );
    }

    @Test
    void should_throw_error_if_page_does_not_exist() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        initPageServices(List.of());
        assertThatThrownBy(() -> useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, PAGE_ID, AUDIT_INFO))).isInstanceOf(
            PageNotFoundException.class
        );
    }

    @Test
    void should_throw_error_if_page_id_is_root() {
        initApiServices(List.of(Api.builder().id(API_ID).build()));
        assertThatThrownBy(() -> useCase.execute(new ApiPublishDocumentationPageUseCase.Input(API_ID, "ROOT", AUDIT_INFO))).isInstanceOf(
            PageNotFoundException.class
        );
    }

    private void initPageServices(List<Page> pages) {
        pageCrudService.initWith(pages);
        pageQueryService.initWith(pages);
    }

    private void initApiServices(List<Api> apis) {
        apiCrudService.initWith(apis);
    }
}
