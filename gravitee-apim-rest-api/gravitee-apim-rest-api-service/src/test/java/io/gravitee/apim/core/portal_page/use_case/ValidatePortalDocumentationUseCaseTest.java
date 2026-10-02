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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.PortalDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.domain_service.ValidatePortalDocumentationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.reconciliation.HomepageReconciler;
import io.gravitee.apim.core.portal_page.exception.HomepageAlreadyExistsException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidatePortalDocumentationUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String PORTAL_HRID = "default-portal";
    private static final String DOC_HRID = "getting-started";
    private static final PortalId PORTAL_ID = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid(PORTAL_HRID).id());
    private static final PortalPageContentId DOC_ID = PortalPageContentId.of(
        HRIDToUUID.portalDocumentation().context(AUDIT_INFO).portal(PORTAL_HRID).hrid(DOC_HRID).id()
    );

    private final PortalPageContentCrudServiceInMemory contentCrudService = new PortalPageContentCrudServiceInMemory();
    private final PortalNavigationItemsCrudServiceInMemory navCrudService = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navQueryService = new PortalNavigationItemsQueryServiceInMemory(
        navCrudService.storage()
    );
    private PortalDocumentationSyncDomainService syncDomainService;
    private ValidatePortalDocumentationUseCase useCase;

    @BeforeEach
    void setUp() {
        syncDomainService = new PortalDocumentationSyncDomainService(
            navCrudService,
            navQueryService,
            new HomepageReconciler(navQueryService, navCrudService, contentCrudService),
            new PortalNavigationItemValidatorService(
                navQueryService,
                PortalPageContentQueryServiceInMemory.sharing(contentCrudService.storage()),
                new ApiProductQueryServiceInMemory(),
                new PortalNavigationItemSourceDomainServiceInMemory()
            )
        );
        useCase = new ValidatePortalDocumentationUseCase(
            new ValidatePortalDocumentationDomainService(
                new PortalAutomationScopeDomainService(new PortalCrudServiceInMemory(), () -> false)
            ),
            syncDomainService
        );
    }

    @AfterEach
    void tearDown() {
        contentCrudService.reset();
        navCrudService.reset();
    }

    @Test
    void should_return_documentation_id_and_no_errors_for_well_formed_input() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/projects/alpha", 1));

        assertThat(output.id()).isEqualTo(DOC_ID);
        assertThat(output.errors()).isEmpty();
    }

    @Test
    void should_not_block_when_referenced_portal_does_not_exist() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/projects/alpha", 1));

        assertThat(output.errors()).isEmpty();
    }

    @Test
    void should_surface_location_format_error() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "projects/alpha", 1));

        assertThat(output.errors()).isNotEmpty();
        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("location"));
    }

    @Test
    void should_surface_blank_name_error() {
        var output = useCase.execute(input("  ", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/projects/alpha", 1));

        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("name"));
    }

    @Test
    void should_surface_null_type_error() {
        var output = useCase.execute(input("Getting Started", null, "# Hello", "/projects/alpha", 1));

        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("type"));
    }

    @Test
    void should_surface_null_content_error() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, null, "/projects/alpha", 1));

        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("content"));
    }

    @Test
    void should_reject_a_second_homepage_without_persisting_anything() {
        syncDomainService.materialize(AUDIT_INFO, homepageContent("home-1", "Home"), PortalArea.HOMEPAGE, null);
        var storageBefore = List.copyOf(navCrudService.storage());

        assertThatThrownBy(() -> useCase.execute(homepageInput("home-2", "Home 2"))).isInstanceOf(HomepageAlreadyExistsException.class);

        assertThat(navCrudService.storage()).containsExactlyInAnyOrderElementsOf(storageBefore);
    }

    @Test
    void should_allow_moving_an_existing_page_to_a_different_area_without_persisting_anything() {
        syncDomainService.materialize(
            AUDIT_INFO,
            pageContent(DOC_ID, "Getting Started", "/projects/alpha", 1),
            PortalArea.TOP_NAVBAR,
            null
        );
        var storageBefore = List.copyOf(navCrudService.storage());

        var output = useCase.execute(
            new CreateOrUpdatePortalDocumentationUseCase.Input(
                AUDIT_INFO,
                DOC_ID,
                PORTAL_ID,
                "Getting Started",
                PortalPageContentType.GRAVITEE_MARKDOWN,
                "# New content",
                null,
                0,
                PortalArea.HOMEPAGE,
                null
            )
        );

        assertThat(output.errors()).isEmpty();
        assertThat(navCrudService.storage()).containsExactlyInAnyOrderElementsOf(storageBefore);
    }

    @Test
    void should_reject_moving_an_existing_page_to_a_conflicting_homepage_without_persisting_anything() {
        syncDomainService.materialize(AUDIT_INFO, homepageContent("home-1", "Home"), PortalArea.HOMEPAGE, null);
        syncDomainService.materialize(
            AUDIT_INFO,
            pageContent(DOC_ID, "Getting Started", "/projects/alpha", 1),
            PortalArea.TOP_NAVBAR,
            null
        );
        var storageBefore = List.copyOf(navCrudService.storage());

        assertThatThrownBy(() ->
            useCase.execute(
                new CreateOrUpdatePortalDocumentationUseCase.Input(
                    AUDIT_INFO,
                    DOC_ID,
                    PORTAL_ID,
                    "Getting Started",
                    PortalPageContentType.GRAVITEE_MARKDOWN,
                    "# New content",
                    null,
                    0,
                    PortalArea.HOMEPAGE,
                    null
                )
            )
        ).isInstanceOf(HomepageAlreadyExistsException.class);

        assertThat(navCrudService.storage()).containsExactlyInAnyOrderElementsOf(storageBefore);
    }

    private static CreateOrUpdatePortalDocumentationUseCase.Input homepageInput(String hrid, String name) {
        var id = PortalPageContentId.of(HRIDToUUID.portalDocumentation().context(AUDIT_INFO).portal(PORTAL_HRID).hrid(hrid).id());
        return new CreateOrUpdatePortalDocumentationUseCase.Input(
            AUDIT_INFO,
            id,
            PORTAL_ID,
            name,
            PortalPageContentType.GRAVITEE_MARKDOWN,
            "# Hello",
            null,
            0,
            PortalArea.HOMEPAGE,
            null
        );
    }

    private static GraviteeMarkdownPageContent homepageContent(String hrid, String name) {
        var id = PortalPageContentId.of(HRIDToUUID.portalDocumentation().context(AUDIT_INFO).portal(PORTAL_HRID).hrid(hrid).id());
        return pageContent(id, name, null, 0);
    }

    private static GraviteeMarkdownPageContent pageContent(PortalPageContentId id, String name, String location, Integer order) {
        var meta = new AutomationMetadata(
            AutomationMetadata.ReferenceType.PORTAL,
            PORTAL_ID.toString(),
            name,
            Optional.ofNullable(location),
            Optional.ofNullable(order)
        );
        return new GraviteeMarkdownPageContent(
            id,
            AUDIT_INFO.organizationId(),
            AUDIT_INFO.environmentId(),
            GraviteeMarkdown.of("# Hello"),
            meta
        );
    }

    private static CreateOrUpdatePortalDocumentationUseCase.Input input(
        String name,
        PortalPageContentType type,
        String content,
        String location,
        Integer order
    ) {
        return new CreateOrUpdatePortalDocumentationUseCase.Input(
            AUDIT_INFO,
            DOC_ID,
            PORTAL_ID,
            name,
            type,
            content,
            location,
            order,
            null,
            null
        );
    }
}
