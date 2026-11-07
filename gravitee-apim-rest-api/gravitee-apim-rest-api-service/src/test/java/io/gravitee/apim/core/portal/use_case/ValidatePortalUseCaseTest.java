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
package io.gravitee.apim.core.portal.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalUrlDomainService;
import io.gravitee.apim.core.portal.domain_service.ValidatePortalDomainService;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidatePortalUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String PORTAL_HRID = "default-portal";
    private static final PortalId PORTAL_ID = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid(PORTAL_HRID).id());
    private static final String PORTAL_NAME = "Default Portal";

    private final InstallationAccessQueryServiceInMemory installationAccessQueryService = new InstallationAccessQueryServiceInMemory();
    private final ValidatePortalDomainService validator = new ValidatePortalDomainService(
        new PortalAutomationScopeDomainService(new PortalCrudServiceInMemory(), () -> false),
        new PortalUrlDomainService(installationAccessQueryService)
    );
    private ValidatePortalUseCase useCase;

    @BeforeEach
    void setUp() {
        installationAccessQueryService.reset();
        installationAccessQueryService.withPortalUrl(AUDIT_INFO.environmentId(), "https://portal.acme.io");
        useCase = new ValidatePortalUseCase(validator);
    }

    @Test
    void should_report_a_new_portal_whose_url_cannot_be_resolved_by_its_id_and_name() {
        installationAccessQueryService.reset();

        var output = useCase.execute(
            new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, PORTAL_ID, PORTAL_NAME, PortalNavigationStructure.empty(), null)
        );

        assertThat(output.portalId()).isEqualTo(PORTAL_ID);
        assertThat(output.name()).isEqualTo(PORTAL_NAME);
        assertThat(output.errors()).anyMatch(e -> e.getMessage().contains("portal URL"));
    }

    @Test
    void should_return_portal_and_no_errors_for_well_formed_input() {
        var output = useCase.execute(
            new CreateOrUpdatePortalUseCase.Input(
                AUDIT_INFO,
                PORTAL_ID,
                PORTAL_NAME,
                PortalNavigationStructure.ofTopNavbar(
                    List.of(new NavigationPath("/docs", null), new NavigationPath("/docs/getting-started", null))
                ),
                null
            )
        );

        assertThat(output.portalId()).isEqualTo(PORTAL_ID);
        assertThat(output.name()).isEqualTo(PORTAL_NAME);
        assertThat(output.errors()).isEmpty();
    }

    @Test
    void should_surface_path_format_errors_with_indexed_field_name() {
        var output = useCase.execute(
            new CreateOrUpdatePortalUseCase.Input(
                AUDIT_INFO,
                PORTAL_ID,
                PORTAL_NAME,
                PortalNavigationStructure.ofTopNavbar(List.of(new NavigationPath("/valid", null), new NavigationPath("bad-path", null))),
                null
            )
        );

        assertThat(output.errors()).anyMatch(e -> e.getMessage().contains("structure.topNavbar[1].path"));
    }

    @Test
    void should_echo_navigation_in_output() {
        var nav = List.of(new NavigationPath("/docs", null));
        var structure = PortalNavigationStructure.ofTopNavbar(nav);
        var output = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, PORTAL_ID, PORTAL_NAME, structure, null));

        assertThat(output.structure().forArea(PortalArea.TOP_NAVBAR)).containsExactlyElementsOf(nav);
        assertThat(output.errors()).isEmpty();
    }
}
