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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalFixtures;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalListingCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalNavigationSyncDomainService;
import io.gravitee.apim.core.portal.domain_service.ValidatePortalDomainService;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.domain_service.PortalDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateOrUpdatePortalUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final PortalAutomationScopeDomainService scopeEnforcer = new PortalAutomationScopeDomainService(portalCrudService);
    private final ValidatePortalDomainService validator = new ValidatePortalDomainService(scopeEnforcer);
    private final PortalNavigationItemsCrudServiceInMemory navCrudService = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navQueryService = new PortalNavigationItemsQueryServiceInMemory(
        navCrudService.storage()
    );
    private final PortalPageContentCrudServiceInMemory pageContentCrudService = new PortalPageContentCrudServiceInMemory();
    private final PortalPageContentQueryServiceInMemory pageContentQueryService = new PortalPageContentQueryServiceInMemory();
    private final PortalListingCrudServiceInMemory portalListingCrudService = new PortalListingCrudServiceInMemory();
    private CreateOrUpdatePortalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateOrUpdatePortalUseCase(
            validator,
            portalCrudService,
            new PortalNavigationSyncDomainService(
                navQueryService,
                new AutomationManagedNavigationItemsQueryService(portalListingCrudService, pageContentQueryService),
                new NavigationSyncPlanExecutor(navCrudService, navQueryService, pageContentCrudService)
            ),
            pageContentQueryService,
            new PortalDocumentationSyncDomainService(navCrudService, navQueryService),
            scopeEnforcer
        );
    }

    @AfterEach
    void tearDown() {
        portalCrudService.reset();
        navCrudService.reset();
        pageContentCrudService.reset();
        pageContentQueryService.reset();
    }

    @Test
    void should_create_when_not_existing() {
        var portal = PortalFixtures.aPortal();

        var output = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal));

        assertThat(output.portal()).isEqualTo(portal);
        assertThat(portalCrudService.storage()).containsExactly(portal);
    }

    @Test
    void should_update_when_existing() {
        var existing = PortalFixtures.aPortal();
        portalCrudService.initWith(java.util.List.of(existing));
        var renamed = Portal.of(existing.getId(), existing.getEnvironmentId(), existing.getOrganizationId(), "Renamed Portal");

        var output = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, renamed));

        assertThat(output.portal().getName()).isEqualTo("Renamed Portal");
        assertThat(portalCrudService.storage()).hasSize(1).containsExactly(renamed);
    }

    @Test
    void should_be_idempotent_when_put_twice() {
        var portal = PortalFixtures.aPortal();

        var first = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal));
        var second = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal));

        assertThat(first.portal()).isEqualTo(second.portal());
        assertThat(portalCrudService.storage()).hasSize(1).containsExactly(portal);
    }

    @Test
    void should_not_consider_an_id_match_in_a_different_environment_as_existing() {
        var existing = PortalFixtures.aPortal();
        portalCrudService.initWith(java.util.List.of(existing));

        var otherEnvAudit = AuditInfo.builder()
            .organizationId("organization-id")
            .environmentId("other-env")
            .actor(AuditActor.builder().userId("user-id").build())
            .build();
        var inOtherEnv = Portal.of(existing.getId(), "other-env", "organization-id", "Other Env Portal");

        var output = useCase.execute(new CreateOrUpdatePortalUseCase.Input(otherEnvAudit, inOtherEnv));

        assertThat(output.portal()).isEqualTo(inOtherEnv);
        assertThat(portalCrudService.storage()).hasSize(2);
        assertThat(portalCrudService.storage().stream().map(Portal::getEnvironmentId).toList()).containsExactlyInAnyOrder(
            "environment-id",
            "other-env"
        );
    }

    @Test
    void should_sync_navigation_against_top_navbar_folders() {
        var defaultPortalId = io.gravitee.apim.core.portal.model.PortalId.of(
            io.gravitee.rest.api.service.common.HRIDToUUID.portal().context(AUDIT_INFO).hrid("default-portal").id()
        );
        var portal = Portal.of(defaultPortalId, AUDIT_INFO.environmentId(), AUDIT_INFO.organizationId(), "Default Portal");

        useCase.execute(
            new CreateOrUpdatePortalUseCase.Input(
                AUDIT_INFO,
                portal,
                List.of(new NavigationPath("/projects/alpha", "Alpha"), new NavigationPath("/projects/beta", null))
            )
        );

        assertThat(navCrudService.storage()).hasSize(3);
        assertThat(navCrudService.storage()).allMatch(item -> item.getArea() == PortalArea.TOP_NAVBAR);
        assertThat(navCrudService.storage()).allMatch(item -> item.getType() == PortalNavigationItemType.FOLDER);
        assertThat(
            navCrudService
                .storage()
                .stream()
                .map(it -> it.getTitle())
                .toList()
        ).containsExactlyInAnyOrder("projects", "Alpha", "beta");
    }

    @Test
    void should_throw_when_navigation_path_is_invalid() {
        var portal = PortalFixtures.aPortal();

        assertThatThrownBy(() ->
            useCase.execute(
                new CreateOrUpdatePortalUseCase.Input(
                    AUDIT_INFO,
                    portal,
                    List.of(new NavigationPath("/valid", null), new NavigationPath("bad-path", null))
                )
            )
        )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("navigation[1].path");
    }

    @Test
    void should_return_empty_errors_when_navigation_is_valid() {
        var portal = PortalFixtures.aPortal();

        var output = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal, List.of(new NavigationPath("/docs", null))));

        assertThat(output.errors()).isEmpty();
    }

    @Test
    void should_persist_navigation_array_on_portal_row() {
        var portal = PortalFixtures.aPortal();
        var input = List.of(new NavigationPath("/a", "A"), new NavigationPath("/b", null));

        var output = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal, input));

        assertThat(output.portal().getPortalNavigation()).extracting(NavigationPath::path).containsExactly("/a", "/b");
        assertThat(portalCrudService.storage()).hasSize(1);
        assertThat(portalCrudService.storage().get(0).getPortalNavigation()).extracting(NavigationPath::path).containsExactly("/a", "/b");
        assertThat(output.navigation()).isEqualTo(output.portal().getPortalNavigation());
    }

    @Test
    void should_use_previously_persisted_to_skip_unmanaged_folders_on_delete() {
        var portal = PortalFixtures.aPortal();
        useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal, List.of(new NavigationPath("/managed", null))));
        var unmanaged = io.gravitee.apim.core.portal_page.model.PortalNavigationFolder.builder()
            .id(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("manual")
            .segment("manual")
            .area(PortalArea.TOP_NAVBAR)
            .order(1)
            .published(true)
            .visibility(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC)
            .build();
        unmanaged.markAsRoot();
        navCrudService.initWith(List.of(unmanaged));

        useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal, List.of()));

        assertThat(navCrudService.storage()).contains(unmanaged);
        assertThat(
            navCrudService
                .storage()
                .stream()
                .anyMatch(it -> "managed".equals(it.getTitle()))
        ).isFalse();
    }

    @Test
    void should_reject_portal_when_environment_already_has_a_different_portal() {
        portalCrudService.initWith(
            List.of(
                Portal.of(
                    io.gravitee.apim.core.portal.model.PortalId.of("00000000-0000-0000-0000-0000000000b1"),
                    AUDIT_INFO.environmentId(),
                    AUDIT_INFO.organizationId(),
                    "Established"
                )
            )
        );
        var incoming = PortalFixtures.aPortal();

        assertThatThrownBy(() -> useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, incoming)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("hrid")
            .hasMessageContaining("established portal");
    }

    @Test
    void should_accept_portal_when_no_conflict_in_environment() {
        var portal = PortalFixtures.aPortal();

        var output = useCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portal));

        assertThat(output.portal()).isEqualTo(portal);
    }
}
