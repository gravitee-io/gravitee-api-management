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
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_page.domain_service.PortalLinkSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateOrUpdateApiLinkUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String API_ID = "00000000-0000-0000-0000-0000000000a1";

    private final PortalNavigationItemsCrudServiceInMemory navItemCrud = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navItemQuery = new PortalNavigationItemsQueryServiceInMemory(
        navItemCrud.storage()
    );

    private CreateOrUpdateApiLinkUseCase useCase;

    @BeforeEach
    void setUp() {
        navItemCrud.reset();
        useCase = new CreateOrUpdateApiLinkUseCase(
            navItemQuery,
            navigationItemValidatorService(),
            new PortalLinkSyncDomainService(navItemCrud, navItemQuery)
        );
    }

    @Test
    void materializes_an_api_attached_link_with_no_portal_in_the_environment() {
        // An API-attached link belongs to its API, so it needs no portal to exist. It renders as soon
        // as some portal's listing places the API (spec §11).
        var output = useCase.execute(input("/guides"));

        assertThat(output.errors()).isEmpty();
        assertThat(output.link().getId()).isEqualTo(PortalNavigationItemId.forApiLink(AUDIT_INFO, API_ID, "external-docs"));
        assertThat(navItemCrud.storage()).hasSize(1);
    }

    @Test
    void reports_a_severe_error_for_a_malformed_location() {
        var output = useCase.execute(input("not-absolute"));

        assertThat(output.link()).isNull();
        assertThat(output.errors()).anyMatch(Validator.Error::isSevere);
        assertThat(navItemCrud.storage()).isEmpty();
    }

    @Test
    void dry_run_validates_without_persisting() {
        var validateUseCase = new ValidateApiLinkUseCase(
            navItemQuery,
            navigationItemValidatorService(),
            new PortalLinkSyncDomainService(navItemCrud, navItemQuery)
        );

        var output = validateUseCase.execute(input("/guides"));

        assertThat(output.errors()).isEmpty();
        assertThat(output.link()).isNull();
        assertThat(navItemCrud.storage()).isEmpty();
    }

    private PortalNavigationItemValidatorService navigationItemValidatorService() {
        return new PortalNavigationItemValidatorService(
            navItemQuery,
            new PortalPageContentQueryServiceInMemory(),
            new ApiProductQueryServiceInMemory(),
            new PortalNavigationItemSourceDomainServiceInMemory()
        );
    }

    private CreateOrUpdateApiLinkUseCase.Input input(String location) {
        return new CreateOrUpdateApiLinkUseCase.Input(
            AUDIT_INFO,
            API_ID,
            "external-docs",
            "External Docs",
            "https://docs.example.com",
            location,
            0,
            null
        );
    }
}
