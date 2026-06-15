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

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_page.domain_service.ValidateApiDocumentationDomainService;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateApiDocumentationUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String API_HRID = "pets-api";
    private static final String DOC_HRID = "getting-started";
    private static final String API_ID = HRIDToUUID.api().context(AUDIT_INFO).hrid(API_HRID).id();
    private static final PortalPageContentId DOC_ID = PortalPageContentId.of(
        HRIDToUUID.apiDocumentation().context(AUDIT_INFO).api(API_HRID).hrid(DOC_HRID).id()
    );

    private ValidateApiDocumentationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ValidateApiDocumentationUseCase(new ValidateApiDocumentationDomainService());
    }

    @Test
    void should_return_documentation_id_and_no_errors_for_well_formed_input() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/getting-started", 1));

        assertThat(output.id()).isEqualTo(DOC_ID);
        assertThat(output.errors()).isEmpty();
    }

    @Test
    void should_not_block_when_referenced_api_does_not_exist() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/getting-started", 1));

        assertThat(output.errors()).isEmpty();
    }

    @Test
    void should_surface_location_format_error() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "getting-started", 1));

        assertThat(output.errors()).isNotEmpty();
        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("location"));
    }

    @Test
    void should_surface_blank_name_error() {
        var output = useCase.execute(input("  ", PortalPageContentType.GRAVITEE_MARKDOWN, "# Hello", "/getting-started", 1));

        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("name"));
    }

    @Test
    void should_surface_null_type_error() {
        var output = useCase.execute(input("Getting Started", null, "# Hello", "/getting-started", 1));

        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("type"));
    }

    @Test
    void should_surface_null_content_error() {
        var output = useCase.execute(input("Getting Started", PortalPageContentType.GRAVITEE_MARKDOWN, null, "/getting-started", 1));

        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("content"));
    }

    private static CreateOrUpdateApiDocumentationUseCase.Input input(
        String name,
        PortalPageContentType type,
        String content,
        String location,
        Integer order
    ) {
        return new CreateOrUpdateApiDocumentationUseCase.Input(AUDIT_INFO, DOC_ID, API_ID, name, type, content, location, order);
    }
}
