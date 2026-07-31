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

import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.PortalDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.ValidatePortalDocumentationDomainService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateOrUpdatePortalDocumentationUseCaseTest {

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

    private final PortalPageContentCrudServiceInMemory crudService = new PortalPageContentCrudServiceInMemory();
    private final PortalPageContentQueryServiceInMemory queryService = new PortalPageContentQueryServiceInMemory();
    private final PortalNavigationItemsCrudServiceInMemory navCrudService = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navQueryService = new PortalNavigationItemsQueryServiceInMemory(
        navCrudService.storage()
    );
    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final PortalAutomationScopeDomainService scopeEnforcer = new PortalAutomationScopeDomainService(portalCrudService, () -> false);
    private final ValidatePortalDocumentationDomainService validator = new ValidatePortalDocumentationDomainService(scopeEnforcer);
    private CreateOrUpdatePortalDocumentationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateOrUpdatePortalDocumentationUseCase(
            validator,
            crudService,
            queryService,
            new PortalDocumentationSyncDomainService(navCrudService, navQueryService),
            scopeEnforcer
        );
    }

    @AfterEach
    void tearDown() {
        crudService.reset();
        queryService.reset();
        navCrudService.reset();
    }

    @Test
    void should_create_when_not_existing() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/projects/alpha", 1));

        assertThat(output.id()).isEqualTo(DOC_ID);
        assertThat(output.errors()).isEmpty();
        assertThat(crudService.storage()).hasSize(1);
        var stored = crudService.storage().get(0);
        var meta = stored.getAutomationMetadata();
        assertThat(meta.name()).isEqualTo("Getting Started");
        assertThat(meta.referenceType()).isEqualTo(AutomationMetadata.ReferenceType.PORTAL);
        assertThat(meta.referenceId()).isEqualTo(PORTAL_ID.toString());
    }

    @Test
    void should_update_when_existing() {
        useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/projects/alpha", 1));
        queryService.initWith(crudService.storage());

        var output = useCase.execute(input("Renamed", PortalPageContentType.OPENAPI, "openapi: 3.0.0", "/projects/alpha/v2", 5));

        assertThat(output.id()).isEqualTo(DOC_ID);
        assertThat(crudService.storage()).hasSize(1);
        var stored = crudService.storage().get(0);
        var meta = stored.getAutomationMetadata();
        assertThat(meta.name()).isEqualTo("Renamed");
        assertThat(stored.getType()).isEqualTo(PortalPageContentType.OPENAPI);
        assertThat(meta.location()).contains("/projects/alpha/v2");
        assertThat(meta.order()).contains(5);
    }

    @Test
    void should_be_idempotent_when_put_twice() {
        var input = input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/projects/alpha", 1);

        var first = useCase.execute(input);
        queryService.initWith(crudService.storage());
        var second = useCase.execute(input);

        assertThat(first.id()).isEqualTo(second.id());
        assertThat(crudService.storage()).hasSize(1);
    }

    @Test
    void should_persist_even_when_parent_portal_does_not_exist() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/projects/alpha", 1));

        assertThat(output.errors()).isEmpty();
        assertThat(crudService.storage()).hasSize(1);
    }

    @Test
    void should_persist_with_null_location_and_order() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", null, null));

        assertThat(output.errors()).isEmpty();
        var stored = crudService.storage().get(0);
        assertThat(stored.getAutomationMetadata().location()).isEmpty();
        assertThat(stored.getAutomationMetadata().order()).isEmpty();
    }

    @Test
    void should_throw_validation_error_when_location_is_malformed() {
        var throwable = catchThrowable(() ->
            useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "projects/alpha", 1))
        );

        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        assertThat(throwable.getMessage()).contains("location");
        assertThat(crudService.storage()).isEmpty();
    }

    @Test
    void should_throw_validation_error_when_name_is_null() {
        var throwable = catchThrowable(() ->
            useCase.execute(input(null, PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/projects/alpha", 1))
        );

        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        assertThat(throwable.getMessage()).contains("name");
        assertThat(crudService.storage()).isEmpty();
    }

    @Test
    void should_throw_validation_error_when_type_is_null() {
        var throwable = catchThrowable(() -> useCase.execute(input("Getting Started", null, "# Hello", "/projects/alpha", 1)));

        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        assertThat(throwable.getMessage()).contains("type");
        assertThat(crudService.storage()).isEmpty();
    }

    @Test
    void should_throw_validation_error_when_name_is_blank() {
        var throwable = catchThrowable(() ->
            useCase.execute(input("  ", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/projects/alpha", 1))
        );

        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        assertThat(throwable.getMessage()).contains("name");
        assertThat(crudService.storage()).isEmpty();
    }

    @Test
    void should_throw_validation_error_when_content_is_null() {
        var throwable = catchThrowable(() ->
            useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, null, "/projects/alpha", 1))
        );

        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        assertThat(throwable.getMessage()).contains("content");
        assertThat(crudService.storage()).isEmpty();
    }

    @Test
    void should_reject_portal_when_environment_already_has_a_different_portal() {
        var establishedCrud = new PortalCrudServiceInMemory();
        establishedCrud.initWith(List.of(Portal.of(PORTAL_ID, AUDIT_INFO.environmentId(), AUDIT_INFO.organizationId(), "Established")));
        var restrictedEnforcer = new PortalAutomationScopeDomainService(establishedCrud, () -> false);
        var restrictedUseCase = new CreateOrUpdatePortalDocumentationUseCase(
            new ValidatePortalDocumentationDomainService(restrictedEnforcer),
            crudService,
            queryService,
            new PortalDocumentationSyncDomainService(navCrudService, navQueryService),
            restrictedEnforcer
        );
        var nonDefaultPortalId = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid("foo-portal").id());

        var throwable = catchThrowable(() ->
            restrictedUseCase.execute(
                new CreateOrUpdatePortalDocumentationUseCase.Input(
                    AUDIT_INFO,
                    DOC_ID,
                    nonDefaultPortalId,
                    "Getting Started",
                    PortalPageContentType.GRAVITEE_MARKDOWN,
                    "# Hello",
                    "/projects/alpha",
                    1
                )
            )
        );

        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
        assertThat(throwable.getMessage()).contains("portalHrid").contains("established portal");
        assertThat(crudService.storage()).isEmpty();
    }

    @Test
    void should_skip_nav_tree_materialization_for_non_default_portal_when_multiple_portals_allowed() {
        // Flag is true (validator allows non-default), but materialization must still be skipped — app is not ready for that.
        var nonDefaultPortalId = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid("foo-portal").id());
        var nonDefaultDocId = PortalPageContentId.of(
            HRIDToUUID.portalDocumentation().context(AUDIT_INFO).portal("foo-portal").hrid(DOC_HRID).id()
        );

        var output = useCase.execute(
            new CreateOrUpdatePortalDocumentationUseCase.Input(
                AUDIT_INFO,
                nonDefaultDocId,
                nonDefaultPortalId,
                "Getting Started",
                PortalPageContentType.GRAVITEE_MARKDOWN,
                "# Hello",
                "/projects/alpha",
                1
            )
        );

        assertThat(output.id()).isEqualTo(nonDefaultDocId);
        assertThat(crudService.storage()).hasSize(1);
        assertThat(navCrudService.storage()).isEmpty();
    }

    private static CreateOrUpdatePortalDocumentationUseCase.Input input(
        String name,
        PortalPageContentType type,
        String content,
        String location,
        Integer order
    ) {
        return new CreateOrUpdatePortalDocumentationUseCase.Input(AUDIT_INFO, DOC_ID, PORTAL_ID, name, type, content, location, order);
    }
}
