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

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.*;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.search.model.IndexablePage;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;

public class CreateApiDocumentationDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String PARENT_ID = "parent-id";
    private static final String PAGE_ID = "page-id";
    private static final Date DATE = new Date();

    private final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    private final PageRevisionCrudServiceInMemory pageRevisionCrudService = new PageRevisionCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final IndexerInMemory indexer = new IndexerInMemory();

    CreateApiDocumentationDomainService createApiDocumentationDomainService;

    @BeforeEach
    void setUp() {
        createApiDocumentationDomainService = new CreateApiDocumentationDomainService(
            pageCrudService,
            pageRevisionCrudService,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            indexer
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(pageCrudService, pageRevisionCrudService, auditCrudService, userCrudService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class MarkdownPage {

        @Test
        void should_create_markdown() {
            var parentPage = Page.builder()
                .id(PARENT_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .parentId("")
                .name("parent")
                .build();
            pageCrudService.initWith(List.of(parentPage));

            var res = createApiDocumentationDomainService.createPage(
                Page.builder()
                    .id(PAGE_ID)
                    .createdAt(DATE)
                    .updatedAt(DATE)
                    .type(Page.Type.MARKDOWN)
                    .name("new page")
                    .content("nice content")
                    .homepage(false)
                    .visibility(Page.Visibility.PRIVATE)
                    .parentId("parent-id")
                    .referenceId("api-id")
                    .referenceType(Page.ReferenceType.API)
                    .order(1)
                    .build(),
                AUDIT_INFO
            );

            assertThat(res)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("name", "new page")
                .hasFieldOrPropertyWithValue("type", Page.Type.MARKDOWN)
                .hasFieldOrPropertyWithValue("content", "nice content")
                .hasFieldOrPropertyWithValue("homepage", false)
                .hasFieldOrPropertyWithValue("visibility", Page.Visibility.PRIVATE)
                .hasFieldOrPropertyWithValue("parentId", PARENT_ID)
                .hasFieldOrPropertyWithValue("order", 1);

            assertThat(res.getCreatedAt()).isNotNull().isEqualTo(res.getUpdatedAt());
        }

        @Test
        void should_create_an_audit() {
            createApiDocumentationDomainService.createPage(
                Page.builder()
                    .id(PAGE_ID)
                    .createdAt(DATE)
                    .updatedAt(DATE)
                    .type(Page.Type.MARKDOWN)
                    .name("new page")
                    .content("nice content")
                    .homepage(false)
                    .visibility(Page.Visibility.PRIVATE)
                    .parentId("parent-id")
                    .referenceId("api-id")
                    .referenceType(Page.ReferenceType.API)
                    .order(1)
                    .build(),
                AUDIT_INFO
            );
            var audit = auditCrudService.storage().get(0);
            assertThat(audit)
                .isNotNull()
                .hasFieldOrPropertyWithValue("referenceId", "api-id")
                .hasFieldOrPropertyWithValue("event", "PAGE_CREATED");
        }

        @Test
        void should_create_a_page_revision() {
            createApiDocumentationDomainService.createPage(
                Page.builder()
                    .id(PAGE_ID)
                    .createdAt(DATE)
                    .updatedAt(DATE)
                    .type(Page.Type.MARKDOWN)
                    .name("new page")
                    .content("nice content")
                    .homepage(false)
                    .visibility(Page.Visibility.PRIVATE)
                    .parentId("parent-id")
                    .referenceId("api-id")
                    .referenceType(Page.ReferenceType.API)
                    .order(1)
                    .build(),
                AUDIT_INFO
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
        void shouldIndexPage() {
            var page = Page.builder()
                .id(PAGE_ID)
                .createdAt(DATE)
                .updatedAt(DATE)
                .type(Page.Type.MARKDOWN)
                .name("new page")
                .content("nice content")
                .homepage(false)
                .visibility(Page.Visibility.PRIVATE)
                .parentId("parent-id")
                .referenceId("api-id")
                .referenceType(Page.ReferenceType.API)
                .order(1)
                .build();

            createApiDocumentationDomainService.createPage(page, AUDIT_INFO);

            assertThat(indexer.storage()).contains(new IndexablePage(page));
        }
    }

    @Nested
    class Folder {

        @Test
        void should_create_folder() {
            var parentPage = Page.builder()
                .id(PARENT_ID)
                .referenceType(Page.ReferenceType.API)
                .referenceId("api-id")
                .parentId("")
                .name("parent")
                .build();
            pageCrudService.initWith(List.of(parentPage));

            var res = createApiDocumentationDomainService.createPage(
                Page.builder()
                    .id(PAGE_ID)
                    .createdAt(DATE)
                    .updatedAt(DATE)
                    .type(Page.Type.FOLDER)
                    .name("new page")
                    .visibility(Page.Visibility.PRIVATE)
                    .parentId("parent-id")
                    .referenceId("api-id")
                    .referenceType(Page.ReferenceType.API)
                    .order(1)
                    .build(),
                AUDIT_INFO
            );

            assertThat(res)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", PAGE_ID)
                .hasFieldOrPropertyWithValue("name", "new page")
                .hasFieldOrPropertyWithValue("type", Page.Type.FOLDER)
                .hasFieldOrPropertyWithValue("content", null)
                .hasFieldOrPropertyWithValue("homepage", false)
                .hasFieldOrPropertyWithValue("visibility", Page.Visibility.PRIVATE)
                .hasFieldOrPropertyWithValue("parentId", PARENT_ID)
                .hasFieldOrPropertyWithValue("order", 1);

            assertThat(res.getCreatedAt()).isNotNull().isEqualTo(res.getUpdatedAt());

            assertThat(pageRevisionCrudService.storage().size()).isEqualTo(0);

            var audit = auditCrudService.storage().get(0);
            assertThat(audit)
                .isNotNull()
                .hasFieldOrPropertyWithValue("referenceId", "api-id")
                .hasFieldOrPropertyWithValue("event", "PAGE_CREATED");
        }

        @Test
        void should_create_audit() {
            createApiDocumentationDomainService.createPage(
                Page.builder()
                    .id(PAGE_ID)
                    .createdAt(DATE)
                    .updatedAt(DATE)
                    .type(Page.Type.FOLDER)
                    .name("new page")
                    .visibility(Page.Visibility.PRIVATE)
                    .parentId("parent-id")
                    .referenceId("api-id")
                    .referenceType(Page.ReferenceType.API)
                    .order(1)
                    .build(),
                AUDIT_INFO
            );

            var audit = auditCrudService.storage().get(0);
            assertThat(audit)
                .isNotNull()
                .hasFieldOrPropertyWithValue("referenceId", "api-id")
                .hasFieldOrPropertyWithValue("event", "PAGE_CREATED");
        }

        @Test
        void should_not_create_page_revision() {
            createApiDocumentationDomainService.createPage(
                Page.builder()
                    .id(PAGE_ID)
                    .createdAt(DATE)
                    .updatedAt(DATE)
                    .type(Page.Type.FOLDER)
                    .name("new page")
                    .visibility(Page.Visibility.PRIVATE)
                    .parentId("parent-id")
                    .referenceId("api-id")
                    .referenceType(Page.ReferenceType.API)
                    .order(1)
                    .build(),
                AUDIT_INFO
            );

            assertThat(pageRevisionCrudService.storage().size()).isEqualTo(0);
        }
    }
}
