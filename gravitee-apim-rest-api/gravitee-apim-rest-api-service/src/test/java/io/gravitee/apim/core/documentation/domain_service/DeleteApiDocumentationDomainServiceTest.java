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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PageRevisionCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.PageAuditEvent;
import io.gravitee.apim.core.documentation.exception.ApiFolderNotEmptyException;
import io.gravitee.apim.core.documentation.exception.ApiPageInvalidReferenceTypeException;
import io.gravitee.apim.core.documentation.exception.ApiPageUsedAsGeneralConditionException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.search.model.IndexablePage;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeleteApiDocumentationDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final Api API = Api.builder().id("api-id").definitionVersion(DefinitionVersion.V4).build();
    public static final String PAGE_ID = "page-id";
    public static final String FOLDER_ID = "folder-id";

    private DeleteApiDocumentationDomainService cut;
    private UpdateApiDocumentationDomainService updateApiDocumentationDomainService;
    private final PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    private final PageRevisionCrudServiceInMemory pageRevisionCrudService = new PageRevisionCrudServiceInMemory();
    private final PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory();
    private final PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final IndexerInMemory indexer = new IndexerInMemory();

    @BeforeEach
    void setUp() {
        updateApiDocumentationDomainService =
            new UpdateApiDocumentationDomainService(
                pageCrudService,
                pageRevisionCrudService,
                new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
                indexer
            );
        cut =
            new DeleteApiDocumentationDomainService(
                pageCrudService,
                pageQueryService,
                new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
                updateApiDocumentationDomainService,
                planQueryService,
                indexer
            );
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(pageCrudService, pageRevisionCrudService, pageQueryService, planQueryService, auditCrudService, userCrudService)
            .forEach(InMemoryAlternative::reset);
    }

    @Nested
    class Validation {

        @Test
        void should_throw_if_page_to_delete_not_found() {
            assertThatThrownBy(() -> cut.delete(API, PAGE_ID, AUDIT_INFO)).isInstanceOf(PageNotFoundException.class);
        }

        @Test
        void should_throw_if_page_to_delete_has_not_api_reference_type() {
            pageCrudService.initWith(List.of(Page.builder().id(PAGE_ID).referenceType(Page.ReferenceType.ENVIRONMENT).build()));
            assertThatThrownBy(() -> cut.delete(API, PAGE_ID, AUDIT_INFO)).isInstanceOf(ApiPageInvalidReferenceTypeException.class);
        }

        @Test
        void should_throw_if_page_used_as_general_condition() {
            pageCrudService.initWith(
                List.of(Page.builder().id(PAGE_ID).referenceType(Page.ReferenceType.API).type(Page.Type.MARKDOWN).build())
            );
            planQueryService.initWith(
                List.of(
                    PlanFixtures
                        .aPlanHttpV4()
                        .toBuilder()
                        .id("plan-1")
                        .apiId(API.getId())
                        .generalConditions(PAGE_ID)
                        .build()
                        .setPlanStatus(PlanStatus.PUBLISHED)
                )
            );
            assertThatThrownBy(() -> cut.delete(API, PAGE_ID, AUDIT_INFO)).isInstanceOf(ApiPageUsedAsGeneralConditionException.class);
        }

        @Test
        void should_throw_if_deleting_non_empty_folder() {
            final Page folder = Page
                .builder()
                .id(FOLDER_ID)
                .referenceId(API.getId())
                .referenceType(Page.ReferenceType.API)
                .type(Page.Type.FOLDER)
                .build();
            final Page page = Page
                .builder()
                .id(PAGE_ID)
                .referenceId(API.getId())
                .referenceType(Page.ReferenceType.API)
                .parentId(FOLDER_ID)
                .type(Page.Type.MARKDOWN)
                .build();
            pageCrudService.initWith(List.of(folder, page));
            pageQueryService.initWith(List.of(folder, page));
            planQueryService.initWith(
                List.of(
                    PlanFixtures
                        .aPlanHttpV4()
                        .toBuilder()
                        .id("plan-1")
                        .apiId(API.getId())
                        .generalConditions(PAGE_ID)
                        .build()
                        .setPlanStatus(PlanStatus.PUBLISHED)
                )
            );

            assertThatThrownBy(() -> cut.delete(API, FOLDER_ID, AUDIT_INFO)).isInstanceOf(ApiFolderNotEmptyException.class);
        }
    }

    @Nested
    class Deletion {

        static final Page BASE_PAGE = Page
            .builder()
            .id(PAGE_ID)
            .referenceId(API.getId())
            .referenceType(Page.ReferenceType.API)
            .type(Page.Type.MARKDOWN)
            .build();

        @Test
        void should_delete_and_update_order_of_pages() {
            final Page first = pageWithIdAndOrder("first", 0);
            final Page second = pageWithIdAndOrder("second", 1).toBuilder().type(Page.Type.FOLDER).build();
            final Page third = pageWithIdAndOrder("third", 2);
            final Page fourth = pageWithIdAndOrder("fourth", 3);

            final List<Page> storedPages = List.of(first, second, third, fourth);
            pageCrudService.initWith(storedPages);
            pageQueryService.initWith(storedPages);

            // Should be able to delete folder
            cut.delete(API, second.getId(), AUDIT_INFO);

            // Hack to synchronize pageCrudService and pageQueryService
            syncPageStorage();

            assertThat(pageQueryService.searchByApiId(API.getId()))
                .hasSize(3)
                .extracting(Page::getId, Page::getOrder)
                .containsExactly(tuple("first", 0), tuple("third", 1), tuple("fourth", 2));

            // should be able to delete first page
            cut.delete(API, first.getId(), AUDIT_INFO);

            // Hack to synchronize pageCrudService and pageQueryService
            syncPageStorage();

            assertThat(pageQueryService.searchByApiId(API.getId()))
                .hasSize(2)
                .extracting(Page::getId, Page::getOrder)
                .containsExactly(tuple("third", 0), tuple("fourth", 1));

            // should be able to delete last page
            cut.delete(API, fourth.getId(), AUDIT_INFO);

            // Hack to synchronize pageCrudService and pageQueryService
            syncPageStorage();

            assertThat(pageQueryService.searchByApiId(API.getId()))
                .hasSize(1)
                .extracting(Page::getId, Page::getOrder)
                .containsExactly(tuple("third", 0));

            // should be able to delete remaining page
            cut.delete(API, third.getId(), AUDIT_INFO);

            // Hack to synchronize pageCrudService and pageQueryService
            syncPageStorage();

            assertThat(pageQueryService.searchByApiId(API.getId())).isEmpty();

            assertThat(
                auditCrudService
                    .storage()
                    .stream()
                    .filter(auditEntity -> auditEntity.getEvent().equals(PageAuditEvent.PAGE_DELETED.name()))
                    .toList()
            )
                .hasSize(4);
        }

        @Test
        void should_remove_from_index() {
            pageCrudService.initWith(List.of(BASE_PAGE));
            pageQueryService.initWith(List.of(BASE_PAGE));
            indexer.initWith(List.of(new IndexablePage(BASE_PAGE)));
            cut.delete(API, BASE_PAGE.getId(), AUDIT_INFO);
            assertThat(indexer.storage()).isEmpty();
        }

        private void syncPageStorage() {
            pageQueryService.initWith(pageCrudService.storage());
        }

        private static Page pageWithIdAndOrder(String id, int order) {
            return BASE_PAGE.toBuilder().id(id).updatedAt(new Date()).order(order).build();
        }
    }
}
