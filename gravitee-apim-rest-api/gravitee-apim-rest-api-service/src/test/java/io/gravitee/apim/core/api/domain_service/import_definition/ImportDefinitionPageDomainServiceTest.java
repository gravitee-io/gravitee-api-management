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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PageFixtures;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.NotFoundDomainException;
import io.gravitee.common.utils.TimeProvider;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportDefinitionPageDomainServiceTest {

    private static final String API_ID = "api-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String USER = "user";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId(USER).build())
        .build();
    private static final Instant INSTANT_NOW = Instant.parse("2025-11-12T10:15:30Z");

    private static final ImportDefinitionPageDomainServiceTestInitializer initializer =
        new ImportDefinitionPageDomainServiceTestInitializer();
    private ImportDefinitionPageDomainService service;

    @BeforeEach
    void setUp() {
        service = initializer.initialize();
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterEach
    void tearDown() {
        initializer.tearDown();
        TimeProvider.reset();
    }

    @Test
    void should_create_new_page() {
        var pageToCreate = PageFixtures.aPage().toBuilder().createdAt(null).updatedAt(null).build();

        assertThat(initializer.pageCrudServiceInMemory.storage()).isEmpty();

        service.upsertPages(API_ID, List.of(pageToCreate), AUDIT_INFO);

        assertThat(initializer.pageCrudServiceInMemory.storage())
            .hasSize(1)
            .first()
            .satisfies(createdPage -> {
                assertThat(createdPage.getId()).isEqualTo(pageToCreate.getId());
                assertThat(createdPage.getReferenceType()).isEqualTo(Page.ReferenceType.API);
                assertThat(createdPage.getReferenceId()).isEqualTo(API_ID);
                assertThat(createdPage.getCreatedAt()).isEqualTo(INSTANT_NOW);
                assertThat(createdPage.getUpdatedAt()).isEqualTo(INSTANT_NOW);
            });
    }

    @Test
    void should_update_page_using_stored_id_when_cross_id_matches_but_imported_id_differs() {
        var crossId = "same-cross-id";
        var storedId = "stored-page-id";
        var wrongImportedId = "generated-import-id";
        var createdAt = Instant.parse("2020-10-12T10:15:30Z");
        var existingInDb = PageFixtures.aPage()
            .toBuilder()
            .id(storedId)
            .crossId(crossId)
            .createdAt(Date.from(createdAt))
            .updatedAt(Date.from(createdAt))
            .content("old")
            .build();
        initializer.pageCrudServiceInMemory.createDocumentationPage(existingInDb);
        initializer.pageQueryServiceInMemory.initWith(List.of(existingInDb));

        var importedWithWrongId = existingInDb.toBuilder().id(wrongImportedId).content("new body").build();
        service.upsertPages(API_ID, List.of(importedWithWrongId), AUDIT_INFO);

        assertThat(initializer.pageCrudServiceInMemory.storage())
            .hasSize(1)
            .first()
            .satisfies(p -> {
                assertThat(p.getId()).isEqualTo(storedId);
                assertThat(p.getCrossId()).isEqualTo(crossId);
                assertThat(p.getContent()).isEqualTo("new body");
                assertThat(p.getUpdatedAt()).isEqualTo(INSTANT_NOW);
            });
    }

    @Test
    void should_update_new_page() {
        var createdAt = Instant.parse("2020-10-12T10:15:30Z");
        var pageToUpdate = PageFixtures.aPage().toBuilder().createdAt(Date.from(createdAt)).updatedAt(null).build();
        initializer.pageCrudServiceInMemory.createDocumentationPage(pageToUpdate);
        initializer.pageQueryServiceInMemory.initWith(List.of(pageToUpdate));

        assertThat(initializer.pageCrudServiceInMemory.storage()).hasSize(1);
        var update = "update";
        service.upsertPages(API_ID, List.of(pageToUpdate.toBuilder().content("update").build()), AUDIT_INFO);

        assertThat(initializer.pageCrudServiceInMemory.storage())
            .hasSize(1)
            .first()
            .satisfies(updatedPage -> {
                assertThat(updatedPage.getId()).isEqualTo(pageToUpdate.getId());
                assertThat(updatedPage.getReferenceType()).isEqualTo(Page.ReferenceType.API);
                assertThat(updatedPage.getReferenceId()).isEqualTo(API_ID);
                assertThat(updatedPage.getCreatedAt()).isEqualTo(createdAt);
                assertThat(updatedPage.getUpdatedAt()).isEqualTo(INSTANT_NOW);
                assertThat(updatedPage.getContent()).isEqualTo(update);
            });
    }

    @ParameterizedTest
    @EnumSource(value = Page.Type.class, names = { "FOLDER", "ROOT" })
    void should_import_child_before_its_batch_parent(Page.Type parentType) {
        var parent = importedPage("parent", parentType).toBuilder().referenceId("source-api").build();
        var child = importedPage("child", Page.Type.MARKDOWN).toBuilder().parentId(parent.getId()).build();

        service.upsertPages(API_ID, List.of(child, parent), AUDIT_INFO);

        assertThat(initializer.pageCrudServiceInMemory.storage())
            .hasSize(2)
            .allSatisfy(page -> {
                assertThat(page.getReferenceType()).isEqualTo(Page.ReferenceType.API);
                assertThat(page.getReferenceId()).isEqualTo(API_ID);
            });
        assertThat(initializer.pageCrudServiceInMemory.get(child.getId()).getParentId()).isEqualTo(parent.getId());
    }

    @Test
    void should_accept_existing_parent_owned_by_target_api() {
        var parent = importedPage("parent", Page.Type.FOLDER);
        initializer.pageCrudServiceInMemory.initWith(List.of(parent));
        initializer.pageQueryServiceInMemory.initWith(List.of(parent));
        var child = importedPage("child", Page.Type.MARKDOWN).toBuilder().parentId(parent.getId()).build();

        service.upsertPages(API_ID, List.of(child), AUDIT_INFO);

        assertThat(initializer.pageCrudServiceInMemory.get(child.getId()).getParentId()).isEqualTo(parent.getId());
    }

    @ParameterizedTest
    @CsvSource({ "API, other-api", "ENVIRONMENT, api-id", ", api-id", "API,", "API, ' '" })
    void should_reject_foreign_or_unowned_parent_before_writing_any_page(Page.ReferenceType referenceType, String referenceId) {
        var parent = importedPage("foreign-parent", Page.Type.FOLDER)
            .toBuilder()
            .referenceType(referenceType)
            .referenceId(referenceId)
            .build();
        initializer.pageCrudServiceInMemory.initWith(List.of(parent));
        initializer.pageQueryServiceInMemory.initWith(List.of(parent));
        var validPage = importedPage("valid-page", Page.Type.MARKDOWN);
        var child = importedPage("child", Page.Type.MARKDOWN).toBuilder().parentId(parent.getId()).build();

        assertThatThrownBy(() -> service.upsertPages(API_ID, List.of(validPage, child), AUDIT_INFO)).isInstanceOf(
            NotFoundDomainException.class
        );

        assertThat(initializer.pageCrudServiceInMemory.storage()).containsExactly(parent);
        assertThat(initializer.pageRevisionCrudServiceInMemory.storage()).isEmpty();
    }

    @Test
    void should_reject_parent_missing_from_batch_and_storage_before_writing_pages() {
        var validPage = importedPage("valid-page", Page.Type.MARKDOWN);
        var child = importedPage("child", Page.Type.MARKDOWN).toBuilder().parentId("missing-parent").build();

        assertThatThrownBy(() -> service.upsertPages(API_ID, List.of(validPage, child), AUDIT_INFO)).isInstanceOf(
            NotFoundDomainException.class
        );

        assertThat(initializer.pageCrudServiceInMemory.storage()).isEmpty();
        assertThat(initializer.pageRevisionCrudServiceInMemory.storage()).isEmpty();
    }

    @Test
    void should_not_allow_batch_parent_to_mask_foreign_existing_record() {
        var foreignParent = importedPage("parent", Page.Type.FOLDER).toBuilder().referenceId("other-api").build();
        initializer.pageCrudServiceInMemory.initWith(List.of(foreignParent));
        initializer.pageQueryServiceInMemory.initWith(List.of(foreignParent));
        var parent = foreignParent.toBuilder().referenceId(API_ID).build();
        var child = importedPage("child", Page.Type.MARKDOWN).toBuilder().parentId(parent.getId()).build();

        assertThatThrownBy(() -> service.upsertPages(API_ID, List.of(child, parent), AUDIT_INFO)).isInstanceOf(
            NotFoundDomainException.class
        );

        assertThat(initializer.pageCrudServiceInMemory.storage()).containsExactly(foreignParent);
        assertThat(initializer.pageRevisionCrudServiceInMemory.storage()).isEmpty();
    }

    @Test
    void should_remap_child_parent_to_stored_id_when_cross_id_matches() {
        var storedParent = importedPage("stored-parent", Page.Type.FOLDER).toBuilder().crossId("parent-cross-id").build();
        initializer.pageCrudServiceInMemory.initWith(List.of(storedParent));
        initializer.pageQueryServiceInMemory.initWith(List.of(storedParent));
        var parent = storedParent.toBuilder().id("imported-parent").build();
        var child = importedPage("child", Page.Type.MARKDOWN).toBuilder().parentId(parent.getId()).build();

        service.upsertPages(API_ID, List.of(child, parent), AUDIT_INFO);

        assertThat(initializer.pageCrudServiceInMemory.storage()).hasSize(2);
        assertThat(initializer.pageCrudServiceInMemory.get(child.getId()).getParentId()).isEqualTo(storedParent.getId());
        assertThat(initializer.pageCrudServiceInMemory.findById(parent.getId())).isEmpty();
    }

    @Test
    void should_remap_child_parent_to_stored_id_when_type_and_name_match() {
        var storedParent = importedPage("stored-parent", Page.Type.FOLDER).toBuilder().crossId(null).build();
        initializer.pageCrudServiceInMemory.initWith(List.of(storedParent));
        initializer.pageQueryServiceInMemory.initWith(List.of(storedParent));
        var parent = storedParent.toBuilder().id("imported-parent").build();
        var child = importedPage("child", Page.Type.MARKDOWN).toBuilder().parentId(parent.getId()).build();

        service.upsertPages(API_ID, List.of(child, parent), AUDIT_INFO);

        assertThat(initializer.pageCrudServiceInMemory.storage()).hasSize(2);
        assertThat(initializer.pageCrudServiceInMemory.get(child.getId()).getParentId()).isEqualTo(storedParent.getId());
    }

    @Test
    void should_preserve_translation_parent_owned_by_target_api() {
        var parent = importedPage("document", Page.Type.MARKDOWN);
        initializer.pageCrudServiceInMemory.initWith(List.of(parent));
        initializer.pageQueryServiceInMemory.initWith(List.of(parent));
        var translation = importedPage("translation", Page.Type.TRANSLATION).toBuilder().parentId(parent.getId()).build();

        service.upsertPages(API_ID, List.of(translation), AUDIT_INFO);

        assertThat(initializer.pageCrudServiceInMemory.get(translation.getId()).getParentId()).isEqualTo(parent.getId());
    }

    @Test
    void should_preserve_last_update_when_imported_pages_match_the_same_destination() {
        var stored = importedPage("stored-page", Page.Type.MARKDOWN).toBuilder().crossId("same-cross-id").build();
        initializer.pageCrudServiceInMemory.initWith(List.of(stored));
        initializer.pageQueryServiceInMemory.initWith(List.of(stored));
        var first = stored.toBuilder().id("first-import-id").content("first content").build();
        var last = stored.toBuilder().id("last-import-id").content("last content").build();

        service.upsertPages(API_ID, List.of(first, last), AUDIT_INFO);

        assertThat(initializer.pageCrudServiceInMemory.storage()).hasSize(1);
        assertThat(initializer.pageCrudServiceInMemory.get(stored.getId()).getContent()).isEqualTo(last.getContent());
    }

    @Test
    void should_not_reparent_root_page_when_another_imported_page_has_no_id() {
        var stored = importedPage("stored-folder", Page.Type.FOLDER);
        initializer.pageCrudServiceInMemory.initWith(List.of(stored));
        initializer.pageQueryServiceInMemory.initWith(List.of(stored));
        var rootPage = importedPage("root-page", Page.Type.MARKDOWN);

        service.upsertPages(API_ID, List.of(stored.toBuilder().id(null).build(), rootPage), AUDIT_INFO);

        assertThat(initializer.pageCrudServiceInMemory.get(rootPage.getId()).getParentId()).isNull();
        assertThat(initializer.pageCrudServiceInMemory.get(stored.getId()).getParentId()).isNull();
    }

    private static Page importedPage(String id, Page.Type type) {
        return PageFixtures.aPage().toBuilder().id(id).name(id).type(type).parentId(null).referenceId(API_ID).build();
    }
}
