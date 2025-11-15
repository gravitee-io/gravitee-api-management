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

import fixtures.core.model.PageFixtures;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
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
}
