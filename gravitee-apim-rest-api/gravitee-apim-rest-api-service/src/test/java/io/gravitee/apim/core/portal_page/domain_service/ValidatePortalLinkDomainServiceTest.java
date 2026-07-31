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
package io.gravitee.apim.core.portal_page.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.PortalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidatePortalLinkDomainServiceTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId("DEFAULT").environmentId("DEFAULT").build();
    private static final PortalId PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");

    private ValidatePortalLinkDomainService service;

    @BeforeEach
    void setUp() {
        var portalCrudService = new PortalCrudServiceInMemory();
        service = new ValidatePortalLinkDomainService(new PortalAutomationScopeDomainService(portalCrudService, () -> true));
    }

    @Test
    void should_accept_a_valid_link() {
        var result = service.validateAndSanitize(
            new ValidatePortalLinkDomainService.Input(
                AUDIT_INFO,
                PORTAL_ID,
                "External Docs",
                "https://docs.example.com",
                "/projects/alpha",
                3
            )
        );

        assertThat(result.severe()).isEmpty();
        assertThat(result.value()).isPresent();
    }

    @Test
    void should_reject_a_blank_name() {
        var result = service.validateAndSanitize(
            new ValidatePortalLinkDomainService.Input(AUDIT_INFO, PORTAL_ID, "  ", "https://docs.example.com", "/projects/alpha", 3)
        );

        assertThat(result.severe()).isPresent();
    }

    @Test
    void should_reject_a_blank_href() {
        var result = service.validateAndSanitize(
            new ValidatePortalLinkDomainService.Input(AUDIT_INFO, PORTAL_ID, "External Docs", "  ", "/projects/alpha", 3)
        );

        assertThat(result.severe()).isPresent();
    }

    @Test
    void should_reject_a_href_that_is_not_a_well_formed_absolute_url() {
        var result = service.validateAndSanitize(
            new ValidatePortalLinkDomainService.Input(AUDIT_INFO, PORTAL_ID, "External Docs", "not-a-url", "/projects/alpha", 3)
        );

        assertThat(result.severe()).isPresent();
    }

    @Test
    void should_reject_a_location_that_does_not_start_with_a_slash() {
        var result = service.validateAndSanitize(
            new ValidatePortalLinkDomainService.Input(
                AUDIT_INFO,
                PORTAL_ID,
                "External Docs",
                "https://docs.example.com",
                "projects/alpha",
                3
            )
        );

        assertThat(result.severe()).isPresent();
    }

    @Test
    void should_accept_a_null_location() {
        var result = service.validateAndSanitize(
            new ValidatePortalLinkDomainService.Input(AUDIT_INFO, PORTAL_ID, "External Docs", "https://docs.example.com", null, 3)
        );

        assertThat(result.severe()).isEmpty();
    }

    @Test
    void should_trim_the_name() {
        var result = service.validateAndSanitize(
            new ValidatePortalLinkDomainService.Input(
                AUDIT_INFO,
                PORTAL_ID,
                "  External Docs  ",
                "https://docs.example.com",
                "/projects/alpha",
                3
            )
        );

        assertThat(result.severe()).isEmpty();
        assertThat(result.value()).isPresent();
        assertThat(result.value().get().name()).isEqualTo("External Docs");
    }

    @Test
    void should_trim_the_href() {
        var result = service.validateAndSanitize(
            new ValidatePortalLinkDomainService.Input(
                AUDIT_INFO,
                PORTAL_ID,
                "External Docs",
                "  https://docs.example.com  ",
                "/projects/alpha",
                3
            )
        );

        assertThat(result.severe()).isEmpty();
        assertThat(result.value()).isPresent();
        assertThat(result.value().get().href()).isEqualTo("https://docs.example.com");
    }

    @Test
    void should_accept_a_mailto_href() {
        var result = service.validateAndSanitize(
            new ValidatePortalLinkDomainService.Input(
                AUDIT_INFO,
                PORTAL_ID,
                "Contact Us",
                "mailto:support@example.com",
                "/projects/alpha",
                3
            )
        );

        assertThat(result.severe()).isEmpty();
        assertThat(result.value()).isPresent();
        assertThat(result.value().get().href()).isEqualTo("mailto:support@example.com");
    }
}
