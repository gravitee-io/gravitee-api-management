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
package io.gravitee.apim.core.portal_page.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_documentation.exception.PortalDocumentationNotFoundException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeletePortalDocumentationUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final PortalPageContentId DOC_ID = PortalPageContentId.of("00000000-0000-0000-0000-0000000000c1");

    private final PortalPageContentCrudServiceInMemory crudService = new PortalPageContentCrudServiceInMemory();
    private final PortalPageContentQueryServiceInMemory queryService = new PortalPageContentQueryServiceInMemory();
    private DeletePortalDocumentationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeletePortalDocumentationUseCase(crudService, queryService);
    }

    @AfterEach
    void tearDown() {
        crudService.reset();
        queryService.reset();
    }

    @Test
    void should_delete_documentation() {
        var pageContent = aPageContent();
        crudService.initWith(List.of(pageContent));
        queryService.initWith(List.of(pageContent));

        useCase.execute(new DeletePortalDocumentationUseCase.Input(AUDIT_INFO, DOC_ID));

        assertThat(crudService.storage()).isEmpty();
    }

    @Test
    void should_throw_when_missing() {
        var throwable = catchThrowable(() -> useCase.execute(new DeletePortalDocumentationUseCase.Input(AUDIT_INFO, DOC_ID)));

        assertThat(throwable).isInstanceOf(PortalDocumentationNotFoundException.class);
    }

    @Test
    void should_throw_when_page_content_has_no_automation_metadata() {
        var pageContent = new GraviteeMarkdownPageContent(
            DOC_ID,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("# Hello")
        );
        crudService.initWith(List.of(pageContent));
        queryService.initWith(List.of(pageContent));

        var throwable = catchThrowable(() -> useCase.execute(new DeletePortalDocumentationUseCase.Input(AUDIT_INFO, DOC_ID)));

        assertThat(throwable).isInstanceOf(PortalDocumentationNotFoundException.class);
    }

    private static GraviteeMarkdownPageContent aPageContent() {
        return new GraviteeMarkdownPageContent(
            DOC_ID,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("# Hello"),
            new AutomationMetadata(
                AutomationMetadata.ReferenceType.PORTAL,
                "00000000-0000-0000-0000-0000000000a1",
                "Getting Started",
                Optional.of("/projects/alpha"),
                Optional.of(1)
            )
        );
    }
}
