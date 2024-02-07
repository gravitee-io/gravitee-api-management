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

import static fixtures.core.model.ApiFixtures.aMessageApiV4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.*;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.DocumentationValidationDomainService;
import io.gravitee.apim.core.documentation.domain_service.HomepageDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageNameException;
import io.gravitee.apim.core.documentation.exception.InvalidPageParentException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.sanitizer.HtmlSanitizerImpl;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.PageContentUnsafeException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ApiCreateDocumentationPageUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String API_ID = "api-id";
    private static final String PARENT_ID = "parent-id";
    private static final String PAGE_ID = "page-id";

    private static final Api API_MESSAGE_V4 = aMessageApiV4().toBuilder().id(API_ID).build();

    private final PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    private final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    private final PageRevisionCrudServiceInMemory pageRevisionCrudService = new PageRevisionCrudServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final DocumentationValidationDomainService documentationValidationDomainService = new DocumentationValidationDomainService(
        new HtmlSanitizerImpl(),
        new NoopTemplateResolverDomainService(),
        apiCrudService
    );
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    CreateApiDocumentationDomainService createApiDocumentationDomainService;
    ApiCreateDocumentationPageUseCase apiCreateDocumentationPageUsecase;

    @BeforeEach
    void setUp() {
        UuidString.overrideGenerator(() -> PAGE_ID);

        createApiDocumentationDomainService =
            new CreateApiDocumentationDomainService(
                pageCrudService,
                pageRevisionCrudService,
                new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor())
            );
        apiCreateDocumentationPageUsecase =
            new ApiCreateDocumentationPageUseCase(
                createApiDocumentationDomainService,
                new ApiDocumentationDomainService(pageQueryService, planQueryService),
                new HomepageDomainService(pageQueryService, pageCrudService),
                pageCrudService,
                pageQueryService,
                documentationValidationDomainService
            );
        apiCrudService.initWith(List.of(API_MESSAGE_V4));
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(pageQueryService, pageCrudService, pageRevisionCrudService, auditCrudService, userCrudService)
            .forEach(InMemoryAlternative::reset);
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @Nested
    class CreateMarkdownTests {

        @Test
        void should_create_markdown() {
            var parentPage = Page
                .builder()
                .id(PARENT_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .parentId("")
                .name("parent")
                .type(Page.Type.FOLDER)
                .build();
            pageCrudService.initWith(List.of(parentPage));

            var res = apiCreateDocumentationPageUsecase.execute(
                ApiCreateDocumentationPageUseCase.Input
                    .builder()
                    .page(
                        Page
                            .builder()
                            .type(Page.Type.MARKDOWN)
                            .name("new page ")
                            .content("nice content")
                            .homepage(false)
                            .visibility(Page.Visibility.PRIVATE)
                            .parentId(PARENT_ID)
                            .order(1)
                            .referenceType(Page.ReferenceType.API)
                            .referenceId(API_ID)
                            .build()
                    )
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.createdPage())
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("name", "new page")
                .hasFieldOrPropertyWithValue("type", Page.Type.MARKDOWN)
                .hasFieldOrPropertyWithValue("content", "nice content")
                .hasFieldOrPropertyWithValue("homepage", false)
                .hasFieldOrPropertyWithValue("visibility", Page.Visibility.PRIVATE)
                .hasFieldOrPropertyWithValue("parentId", "parent-id")
                .hasFieldOrPropertyWithValue("order", 0);

            var savedPage = pageCrudService
                .storage()
                .stream()
                .filter(page -> page.getId().equals(res.createdPage().getId()))
                .toList()
                .get(0);
            assertThat(savedPage).isEqualTo(res.createdPage());
        }

        @Test
        void should_create_audit() {
            apiCreateDocumentationPageUsecase.execute(
                ApiCreateDocumentationPageUseCase.Input
                    .builder()
                    .page(
                        Page
                            .builder()
                            .type(Page.Type.MARKDOWN)
                            .name("new page")
                            .content("nice content")
                            .homepage(false)
                            .visibility(Page.Visibility.PRIVATE)
                            .parentId(PARENT_ID)
                            .order(1)
                            .referenceType(Page.ReferenceType.API)
                            .referenceId(API_ID)
                            .build()
                    )
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var audit = auditCrudService.storage().get(0);
            assertThat(audit)
                .isNotNull()
                .hasFieldOrPropertyWithValue("referenceId", "api-id")
                .hasFieldOrPropertyWithValue("event", "PAGE_CREATED");
        }

        @Test
        void should_create_a_page_revision() {
            apiCreateDocumentationPageUsecase.execute(
                ApiCreateDocumentationPageUseCase.Input
                    .builder()
                    .page(
                        Page
                            .builder()
                            .type(Page.Type.MARKDOWN)
                            .name("new page")
                            .content("nice content")
                            .homepage(false)
                            .visibility(Page.Visibility.PRIVATE)
                            .parentId(PARENT_ID)
                            .order(1)
                            .referenceType(Page.ReferenceType.API)
                            .referenceId(API_ID)
                            .build()
                    )
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var pageRevision = pageRevisionCrudService.storage().get(0);
            assertThat(pageRevision)
                .isNotNull()
                .hasFieldOrPropertyWithValue("pageId", PAGE_ID)
                .hasFieldOrPropertyWithValue("contributor", null)
                .hasFieldOrPropertyWithValue("name", "new page")
                .hasFieldOrPropertyWithValue("content", "nice content");
        }

        @Test
        void should_change_homepage_to_new_page() {
            final String EXISTING_PAGE_ID = "existing-homepage-id";
            var existingHomepage = Page
                .builder()
                .id(EXISTING_PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .parentId("")
                .name("existing homepage")
                .homepage(true)
                .build();
            pageCrudService.initWith(List.of(existingHomepage));
            pageQueryService.initWith(List.of(existingHomepage));

            var res = apiCreateDocumentationPageUsecase.execute(
                ApiCreateDocumentationPageUseCase.Input
                    .builder()
                    .page(
                        Page
                            .builder()
                            .type(Page.Type.MARKDOWN)
                            .name("new page")
                            .content("nice content")
                            .homepage(true)
                            .visibility(Page.Visibility.PRIVATE)
                            .parentId("")
                            .order(1)
                            .referenceType(Page.ReferenceType.API)
                            .referenceId(API_ID)
                            .build()
                    )
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.createdPage()).isNotNull().hasFieldOrPropertyWithValue("homepage", true);

            var formerHomepage = pageCrudService.storage().stream().filter(p -> p.getId().equals(EXISTING_PAGE_ID)).toList().get(0);
            assertThat(formerHomepage).isNotNull().hasFieldOrPropertyWithValue("homepage", false);
        }

        @Test
        void should_give_new_markdown_highest_count() {
            final String EXISTING_PAGE_ID = "existing-page-id";
            final String PARENT_ID = "123";
            var existingParent = Page
                .builder()
                .id(PARENT_ID)
                .type(Page.Type.FOLDER)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .name("existing parent")
                .order(1)
                .build();

            var existingPage = Page
                .builder()
                .id(EXISTING_PAGE_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId(API_ID)
                .parentId(PARENT_ID)
                .name("existing page")
                .homepage(true)
                .order(99)
                .build();
            pageCrudService.initWith(List.of(existingPage, existingParent));
            pageQueryService.initWith(List.of(existingPage, existingParent));

            var res = apiCreateDocumentationPageUsecase.execute(
                ApiCreateDocumentationPageUseCase.Input
                    .builder()
                    .page(
                        Page
                            .builder()
                            .type(Page.Type.MARKDOWN)
                            .name("new page")
                            .content("nice content")
                            .visibility(Page.Visibility.PRIVATE)
                            .parentId(PARENT_ID)
                            .referenceType(Page.ReferenceType.API)
                            .referenceId(API_ID)
                            .build()
                    )
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.createdPage()).isNotNull().hasFieldOrPropertyWithValue("order", 100);
        }

        @Test
        void should_not_add_missing_parent() {
            var res = apiCreateDocumentationPageUsecase.execute(
                ApiCreateDocumentationPageUseCase.Input
                    .builder()
                    .page(
                        Page
                            .builder()
                            .type(Page.Type.MARKDOWN)
                            .name("new page")
                            .content("nice content")
                            .homepage(false)
                            .visibility(Page.Visibility.PRIVATE)
                            .parentId(PARENT_ID)
                            .order(1)
                            .referenceType(Page.ReferenceType.API)
                            .referenceId(API_ID)
                            .build()
                    )
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.createdPage()).isNotNull().hasFieldOrPropertyWithValue("parentId", null);
        }

        @Test
        void should_ignore_empty_parent_id() {
            var res = apiCreateDocumentationPageUsecase.execute(
                ApiCreateDocumentationPageUseCase.Input
                    .builder()
                    .page(
                        Page
                            .builder()
                            .type(Page.Type.MARKDOWN)
                            .name("new page")
                            .content("nice content")
                            .homepage(false)
                            .visibility(Page.Visibility.PRIVATE)
                            .parentId("")
                            .order(1)
                            .referenceType(Page.ReferenceType.API)
                            .referenceId(API_ID)
                            .build()
                    )
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.createdPage().getParentId()).isNull();
        }

        @Test
        void should_throw_error_if_markdown_unsafe() {
            assertThatThrownBy(() ->
                    apiCreateDocumentationPageUsecase.execute(
                        ApiCreateDocumentationPageUseCase.Input
                            .builder()
                            .page(
                                Page
                                    .builder()
                                    .type(Page.Type.MARKDOWN)
                                    .name("new page")
                                    .content(getNotSafe())
                                    .homepage(false)
                                    .visibility(Page.Visibility.PRIVATE)
                                    .parentId(PARENT_ID)
                                    .order(1)
                                    .referenceType(Page.ReferenceType.API)
                                    .referenceId(API_ID)
                                    .build()
                            )
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(PageContentUnsafeException.class);
        }

        @Test
        void should_throw_error_if_parent_is_not_folder() {
            var parentPage = Page
                .builder()
                .id(PARENT_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .parentId("")
                .name("parent")
                .build();
            pageCrudService.initWith(List.of(parentPage));

            assertThatThrownBy(() ->
                    apiCreateDocumentationPageUsecase.execute(
                        ApiCreateDocumentationPageUseCase.Input
                            .builder()
                            .page(
                                Page
                                    .builder()
                                    .type(Page.Type.MARKDOWN)
                                    .name("new page")
                                    .visibility(Page.Visibility.PRIVATE)
                                    .parentId(PARENT_ID)
                                    .order(1)
                                    .referenceType(Page.ReferenceType.API)
                                    .referenceId(API_ID)
                                    .build()
                            )
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(InvalidPageParentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = { "  " })
        @NullAndEmptySource
        void should_throw_error_if_page_name_is_null_or_empty(String name) {
            var parentPage = Page
                .builder()
                .id(PARENT_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .parentId("")
                .name("parent")
                .type(Page.Type.FOLDER)
                .build();
            pageCrudService.initWith(List.of(parentPage));

            assertThatThrownBy(() ->
                    apiCreateDocumentationPageUsecase.execute(
                        ApiCreateDocumentationPageUseCase.Input
                            .builder()
                            .page(
                                Page
                                    .builder()
                                    .type(Page.Type.MARKDOWN)
                                    .name(name)
                                    .visibility(Page.Visibility.PRIVATE)
                                    .parentId(PARENT_ID)
                                    .order(1)
                                    .referenceType(Page.ReferenceType.API)
                                    .referenceId(API_ID)
                                    .build()
                            )
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(InvalidPageNameException.class);
        }

        @Test
        void should_throw_error_if_name_not_unique() {
            var parentFolder = Page
                .builder()
                .id(PARENT_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .parentId("")
                .name("parent")
                .build();
            var subPage = Page
                .builder()
                .id("sub-page-id")
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.MARKDOWN)
                .parentId(PARENT_ID)
                .name("sub-page")
                .build();
            pageCrudService.initWith(List.of(parentFolder, subPage));
            pageQueryService.initWith(List.of(parentFolder, subPage));

            assertThatThrownBy(() ->
                    apiCreateDocumentationPageUsecase.execute(
                        ApiCreateDocumentationPageUseCase.Input
                            .builder()
                            .page(
                                Page
                                    .builder()
                                    .id("sub-page-id")
                                    .referenceType(Page.ReferenceType.API)
                                    .referenceId("api-id")
                                    .type(Page.Type.MARKDOWN)
                                    .parentId(PARENT_ID)
                                    .name("sub-page")
                                    .visibility(Page.Visibility.PRIVATE)
                                    .build()
                            )
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Name already exists with the same parent and type: sub-page");
        }
    }

    @Nested
    class CreateFolderTests {

        @Test
        void should_create_folder() {
            var parentPage = Page
                .builder()
                .id(PARENT_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .parentId("")
                .name("parent")
                .type(Page.Type.FOLDER)
                .build();
            pageCrudService.initWith(List.of(parentPage));

            var res = apiCreateDocumentationPageUsecase.execute(
                ApiCreateDocumentationPageUseCase.Input
                    .builder()
                    .page(
                        Page
                            .builder()
                            .type(Page.Type.FOLDER)
                            .name("new page")
                            .visibility(Page.Visibility.PRIVATE)
                            .parentId(PARENT_ID)
                            .referenceType(Page.ReferenceType.API)
                            .referenceId(API_ID)
                            .build()
                    )
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            assertThat(res.createdPage())
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("name", "new page")
                .hasFieldOrPropertyWithValue("type", Page.Type.FOLDER)
                .hasFieldOrPropertyWithValue("content", null)
                .hasFieldOrPropertyWithValue("homepage", false)
                .hasFieldOrPropertyWithValue("visibility", Page.Visibility.PRIVATE)
                .hasFieldOrPropertyWithValue("parentId", "parent-id")
                .hasFieldOrPropertyWithValue("order", 0)
                .hasFieldOrPropertyWithValue("hidden", true);

            var savedPage = pageCrudService
                .storage()
                .stream()
                .filter(page -> page.getId().equals(res.createdPage().getId()))
                .toList()
                .get(0);
            assertThat(savedPage).isEqualTo(res.createdPage());
        }

        @Test
        void should_create_audit() {
            apiCreateDocumentationPageUsecase.execute(
                ApiCreateDocumentationPageUseCase.Input
                    .builder()
                    .page(
                        Page
                            .builder()
                            .type(Page.Type.FOLDER)
                            .name("new page")
                            .visibility(Page.Visibility.PRIVATE)
                            .parentId(PARENT_ID)
                            .order(1)
                            .referenceType(Page.ReferenceType.API)
                            .referenceId(API_ID)
                            .build()
                    )
                    .auditInfo(AUDIT_INFO)
                    .build()
            );

            var audit = auditCrudService.storage().get(0);
            assertThat(audit)
                .isNotNull()
                .hasFieldOrPropertyWithValue("referenceId", "api-id")
                .hasFieldOrPropertyWithValue("event", "PAGE_CREATED");
        }

        @Test
        void should_throw_error_if_parent_is_not_folder() {
            var parentPage = Page
                .builder()
                .id(PARENT_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .parentId("")
                .name("parent")
                .build();
            pageCrudService.initWith(List.of(parentPage));

            assertThatThrownBy(() ->
                    apiCreateDocumentationPageUsecase.execute(
                        ApiCreateDocumentationPageUseCase.Input
                            .builder()
                            .page(
                                Page
                                    .builder()
                                    .type(Page.Type.FOLDER)
                                    .name("new page")
                                    .visibility(Page.Visibility.PRIVATE)
                                    .parentId(PARENT_ID)
                                    .order(1)
                                    .referenceType(Page.ReferenceType.API)
                                    .referenceId(API_ID)
                                    .build()
                            )
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(InvalidPageParentException.class);
        }

        @Test
        void should_throw_error_if_name_not_unique() {
            var parentFolder = Page
                .builder()
                .id(PARENT_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .parentId("")
                .name("parent")
                .build();
            var subFolder = Page
                .builder()
                .id("sub-page-id")
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .type(Page.Type.FOLDER)
                .parentId(PARENT_ID)
                .name("sub-page")
                .build();
            pageCrudService.initWith(List.of(parentFolder, subFolder));
            pageQueryService.initWith(List.of(parentFolder, subFolder));

            assertThatThrownBy(() ->
                    apiCreateDocumentationPageUsecase.execute(
                        ApiCreateDocumentationPageUseCase.Input
                            .builder()
                            .page(
                                Page
                                    .builder()
                                    .id("sub-page-id")
                                    .referenceType(Page.ReferenceType.API)
                                    .referenceId("api-id")
                                    .type(Page.Type.FOLDER)
                                    .parentId(PARENT_ID)
                                    .name("sub-page")
                                    .visibility(Page.Visibility.PRIVATE)
                                    .build()
                            )
                            .auditInfo(AUDIT_INFO)
                            .build()
                    )
                )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Name already exists with the same parent and type: sub-page");
        }
    }

    private String getNotSafe() {
        String html = "";
        html += "<script src=\"/external.jpg\" />";
        html += "<div onClick=\"alert('test');\" style=\"margin: auto\">onclick alert<div>";

        return html;
    }
}
