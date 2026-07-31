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

import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.ValidatePortalLinkDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidatePortalLinkUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String PORTAL_HRID = "default-portal";
    private static final PortalId PORTAL_ID = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid(PORTAL_HRID).id());
    private static final String LINK_HRID = "external-docs";

    private ValidatePortalLinkUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ValidatePortalLinkUseCase(
            new ValidatePortalLinkDomainService(new PortalAutomationScopeDomainService(new PortalCrudServiceInMemory(), () -> false))
        );
    }

    @Test
    void should_return_no_errors_and_no_link_for_well_formed_input() {
        var output = useCase.execute(input("External Docs", "https://docs.example.com", "/projects/alpha", 1));

        assertThat(output.errors()).isEmpty();
        assertThat(output.link()).isNull();
    }

    @Test
    void should_not_block_when_referenced_portal_does_not_exist() {
        var output = useCase.execute(input("External Docs", "https://docs.example.com", "/projects/alpha", 1));

        assertThat(output.errors()).isEmpty();
    }

    @Test
    void should_surface_href_format_error() {
        var output = useCase.execute(input("External Docs", "not-a-url", "/projects/alpha", 1));

        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("href"));
    }

    @Test
    void should_surface_blank_name_error() {
        var output = useCase.execute(input("  ", "https://docs.example.com", "/projects/alpha", 1));

        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("name"));
    }

    @Test
    void should_surface_location_format_error() {
        var output = useCase.execute(input("External Docs", "https://docs.example.com", "projects/alpha", 1));

        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("location"));
    }

    private static CreateOrUpdatePortalLinkUseCase.Input input(String name, String href, String location, Integer order) {
        return new CreateOrUpdatePortalLinkUseCase.Input(AUDIT_INFO, PORTAL_ID, LINK_HRID, name, href, location, order);
    }
}
