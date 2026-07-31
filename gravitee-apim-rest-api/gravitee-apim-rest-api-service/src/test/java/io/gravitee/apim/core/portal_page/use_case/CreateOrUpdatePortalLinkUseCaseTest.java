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

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.PortalLinkSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateOrUpdatePortalLinkUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String PORTAL_HRID = "default-portal";
    private static final PortalId PORTAL_ID = PortalId.of(HRIDToUUID.portal().context(AUDIT_INFO).hrid(PORTAL_HRID).id());
    private static final String LINK_HRID = "external-docs";

    private final PortalNavigationItemsCrudServiceInMemory navCrudService = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navQueryService = new PortalNavigationItemsQueryServiceInMemory(
        navCrudService.storage()
    );
    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final PortalAutomationScopeDomainService scopeEnforcer = new PortalAutomationScopeDomainService(portalCrudService, () -> false);
    private final PortalNavigationItemValidatorService navigationItemValidatorService = new PortalNavigationItemValidatorService(
        navQueryService,
        new PortalPageContentQueryServiceInMemory(),
        new ApiProductQueryServiceInMemory(),
        new PortalNavigationItemSourceDomainServiceInMemory()
    );
    private CreateOrUpdatePortalLinkUseCase useCase;

    @BeforeEach
    void setUp() {
        portalCrudService.initWith(
            List.of(Portal.of(PORTAL_ID, AUDIT_INFO.environmentId(), AUDIT_INFO.organizationId(), "Default Portal"))
        );
        useCase = new CreateOrUpdatePortalLinkUseCase(
            navQueryService,
            navigationItemValidatorService,
            new PortalLinkSyncDomainService(navCrudService, navQueryService),
            scopeEnforcer
        );
    }

    @AfterEach
    void tearDown() {
        navCrudService.reset();
        portalCrudService.reset();
    }

    @Test
    void should_create_a_link_when_valid() {
        var output = useCase.execute(input("External Docs", "https://docs.example.com", "/projects/alpha", 3));

        assertThat(output.errors()).isEmpty();
        assertThat(output.link().getTitle()).isEqualTo("External Docs");
        assertThat(output.link().getUrl()).isEqualTo("https://docs.example.com");
        assertThat(output.link().getOrder()).isEqualTo(3);
        assertThat(navCrudService.storage()).hasSize(1);
    }

    @Test
    void should_update_an_existing_link() {
        useCase.execute(input("External Docs", "https://docs.example.com", "/projects/alpha", 1));

        var output = useCase.execute(input("Renamed", "https://renamed.example.com", "/projects/beta", 2));

        assertThat(output.link().getTitle()).isEqualTo("Renamed");
        assertThat(output.link().getUrl()).isEqualTo("https://renamed.example.com");
        assertThat(output.link().getOrder()).isEqualTo(2);
        assertThat(navCrudService.storage()).hasSize(1);
    }

    @Test
    void should_be_idempotent_when_put_twice() {
        var request = input("External Docs", "https://docs.example.com", "/projects/alpha", 3);

        var first = useCase.execute(request);
        var second = useCase.execute(request);

        assertThat(first.link().getId()).isEqualTo(second.link().getId());
        assertThat(navCrudService.storage()).hasSize(1);
    }

    @Test
    void should_report_severe_error_and_materialize_nothing_when_href_is_invalid() {
        var output = useCase.execute(input("External Docs", "not-a-url", "/projects/alpha", 3));

        assertThat(output.link()).isNull();
        assertThat(output.errors()).anyMatch(Validator.Error::isSevere);
        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("url"));
        assertThat(navCrudService.storage()).isEmpty();
    }

    @Test
    void should_report_severe_error_and_materialize_nothing_when_name_is_blank() {
        var output = useCase.execute(input("  ", "https://docs.example.com", "/projects/alpha", 3));

        assertThat(output.link()).isNull();
        assertThat(output.errors()).anyMatch(Validator.Error::isSevere);
        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("title"));
        assertThat(navCrudService.storage()).isEmpty();
    }

    @Test
    void should_report_severe_error_and_materialize_nothing_when_location_is_malformed() {
        var output = useCase.execute(input("External Docs", "https://docs.example.com", "projects/alpha", 3));

        assertThat(output.link()).isNull();
        assertThat(output.errors()).anyMatch(Validator.Error::isSevere);
        assertThat(output.errors())
            .extracting(Validator.Error::getMessage)
            .anyMatch(m -> m.contains("location"));
        assertThat(navCrudService.storage()).isEmpty();
    }

    @Test
    void should_skip_materialization_when_parent_portal_does_not_exist() {
        portalCrudService.reset();

        var output = useCase.execute(input("External Docs", "https://docs.example.com", "/projects/alpha", 3));

        assertThat(output.errors()).isEmpty();
        assertThat(output.link()).isNull();
        assertThat(navCrudService.storage()).isEmpty();
    }

    private static CreateOrUpdatePortalLinkUseCase.Input input(String name, String href, String location, Integer order) {
        return new CreateOrUpdatePortalLinkUseCase.Input(AUDIT_INFO, PORTAL_ID, LINK_HRID, name, href, location, order);
    }
}
