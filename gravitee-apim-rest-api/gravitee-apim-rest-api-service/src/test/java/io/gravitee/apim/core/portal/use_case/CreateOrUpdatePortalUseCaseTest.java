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
import static org.mockito.Mockito.mock;

import fixtures.core.model.PortalFixtures;
import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalListingCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import inmemory.ThemeCrudServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationProperties;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalContentOwnershipDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalNavigationSyncDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalUrlDomainService;
import io.gravitee.apim.core.portal.domain_service.ValidatePortalDomainService;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.domain_service.PortalDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.domain_service.reconciliation.HomepageReconciler;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.theme.domain_service.CurrentThemeDomainService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import java.net.URI;
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
    private final InstallationAccessQueryServiceInMemory installationAccessQueryService = new InstallationAccessQueryServiceInMemory();
    private final PortalNavigationItemsCrudServiceInMemory navCrudService = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navQueryService = new PortalNavigationItemsQueryServiceInMemory(
        navCrudService.storage()
    );
    private final PortalPageContentCrudServiceInMemory pageContentCrudService = new PortalPageContentCrudServiceInMemory();
    private final PortalPageContentQueryServiceInMemory pageContentQueryService = new PortalPageContentQueryServiceInMemory();
    private final PortalListingCrudServiceInMemory portalListingCrudService = new PortalListingCrudServiceInMemory();
    private final ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    private final ThemeQueryServiceInMemory themeQueryService = new ThemeQueryServiceInMemory(themeCrudService);
    private CreateOrUpdatePortalUseCase useCase;

    @BeforeEach
    void setUp() {
        installationAccessQueryService.withPortalUrl(AUDIT_INFO.environmentId(), "https://portal.acme.io");
        installationAccessQueryService.withPortalUrl("other-env", "https://other.acme.io");
        useCase = aUseCase(() -> false);
    }

    private CreateOrUpdatePortalUseCase aUseCase(PortalAutomationProperties properties) {
        var scopeEnforcer = new PortalAutomationScopeDomainService(portalCrudService, properties);
        return new CreateOrUpdatePortalUseCase(
            new ValidatePortalDomainService(scopeEnforcer, new PortalUrlDomainService(installationAccessQueryService)),
            portalCrudService,
            new PortalNavigationSyncDomainService(
                navQueryService,
                new AutomationManagedNavigationItemsQueryService(portalListingCrudService, navQueryService),
                new NavigationSyncPlanExecutor(navCrudService, navQueryService, pageContentCrudService),
                mock(PortalNavigationItemValidatorService.class)
            ),
            pageContentQueryService,
            new PortalDocumentationSyncDomainService(
                navCrudService,
                navQueryService,
                new HomepageReconciler(navQueryService, navCrudService, pageContentCrudService),
                mock(PortalNavigationItemValidatorService.class)
            ),
            scopeEnforcer,
            themeCrudService,
            new CurrentThemeDomainService(themeQueryService, themeCrudService),
            new PortalUrlDomainService(installationAccessQueryService),
            new PortalContentOwnershipDomainService(portalCrudService, navCrudService, portalListingCrudService)
        );
    }

    @AfterEach
    void tearDown() {
        installationAccessQueryService.reset();
        portalCrudService.reset();
        navCrudService.reset();
        pageContentCrudService.reset();
        pageContentQueryService.reset();
    }

    private static CreateOrUpdatePortalUseCase.Input inputFor(AuditInfo auditInfo, Portal portal) {
        return inputFor(auditInfo, portal, PortalNavigationStructure.empty(), null);
    }

    private static CreateOrUpdatePortalUseCase.Input inputFor(AuditInfo auditInfo, Portal portal, PortalNavigationStructure structure) {
        return inputFor(auditInfo, portal, structure, null);
    }

    private static CreateOrUpdatePortalUseCase.Input inputFor(
        AuditInfo auditInfo,
        Portal portal,
        PortalNavigationStructure structure,
        String activeThemeHrid
    ) {
        return new CreateOrUpdatePortalUseCase.Input(auditInfo, portal.getId(), portal.getName(), structure, activeThemeHrid);
    }

    @Test
    void should_create_when_not_existing() {
        var portal = PortalFixtures.aPortal();

        var output = useCase.execute(inputFor(AUDIT_INFO, portal));

        assertThat(output.portal()).isEqualTo(portal);
        assertThat(portalCrudService.storage()).containsExactly(portal);
    }

    @Test
    void should_update_when_existing() {
        var existing = PortalFixtures.aPortal();
        portalCrudService.initWith(java.util.List.of(existing));
        var renamed = PortalFixtures.aPortal(existing.getId(), existing.getEnvironmentId(), existing.getOrganizationId(), "Renamed Portal");

        var output = useCase.execute(inputFor(AUDIT_INFO, renamed));

        assertThat(output.portal().getName()).isEqualTo("Renamed Portal");
        assertThat(portalCrudService.storage()).hasSize(1).containsExactly(renamed);
    }

    private PortalNavigationItem aLegacyFolder() {
        var folder = PortalNavigationItemFixtures.aFolder("Legacy").toBuilder().environmentId(AUDIT_INFO.environmentId()).build();
        navCrudService.initWith(List.of(folder));
        return folder;
    }

    private NavigationItemReference storedReferenceOf(PortalNavigationItem item) {
        return navCrudService
            .storage()
            .stream()
            .filter(stored -> stored.getId().equals(item.getId()))
            .findFirst()
            .orElseThrow()
            .getReference();
    }

    @Test
    void should_give_the_first_portal_of_an_environment_its_orphan_content() {
        var legacyFolder = aLegacyFolder();
        var portal = PortalFixtures.aPortal();

        useCase.execute(inputFor(AUDIT_INFO, portal));

        assertThat(storedReferenceOf(legacyFolder)).isEqualTo(new NavigationItemReference.PortalReference(portal.getId()));
    }

    @Test
    void should_give_orphan_content_to_the_default_portal_when_another_portal_is_created() throws Exception {
        var legacyFolder = aLegacyFolder();
        var defaultPortal = Portal.of(
            PortalId.of("00000000-0000-0000-0000-0000000000d1"),
            AUDIT_INFO.environmentId(),
            AUDIT_INFO.organizationId(),
            "Developer Portal",
            PortalNavigationStructure.empty(),
            null,
            true,
            URI.create("https://portal.acme.io/next").toURL()
        );
        portalCrudService.initWith(List.of(defaultPortal));

        aUseCase(() -> true).execute(inputFor(AUDIT_INFO, PortalFixtures.aPortal()));

        assertThat(storedReferenceOf(legacyFolder)).isEqualTo(new NavigationItemReference.PortalReference(defaultPortal.getId()));
    }

    @Test
    void should_leave_orphan_content_alone_when_updating_an_existing_portal() {
        var legacyFolder = aLegacyFolder();
        var existing = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(existing));

        useCase.execute(inputFor(AUDIT_INFO, existing));

        assertThat(storedReferenceOf(legacyFolder)).isEqualTo(NavigationItemReference.defaultReference());
    }

    @Test
    void should_keep_stored_default_marker_and_url_on_update() throws Exception {
        var storedUrl = URI.create("https://portal.example").toURL();
        var stored = Portal.of(
            PortalFixtures.PORTAL_ID,
            "environment-id",
            "organization-id",
            "Default Portal",
            PortalNavigationStructure.empty(),
            null,
            true,
            storedUrl
        );
        portalCrudService.initWith(List.of(stored));
        var upsert = PortalFixtures.aPortal(stored.getId(), stored.getEnvironmentId(), stored.getOrganizationId(), "Renamed Portal");

        var output = useCase.execute(inputFor(AUDIT_INFO, upsert));

        assertThat(output.portal().getName()).isEqualTo("Renamed Portal");
        assertThat(output.portal().isDefault()).isTrue();
        assertThat(output.portal().getUrl()).isEqualTo(storedUrl);
        assertThat(portalCrudService.storage())
            .singleElement()
            .satisfies(saved -> {
                assertThat(saved.getName()).isEqualTo("Renamed Portal");
                assertThat(saved.isDefault()).isTrue();
                assertThat(saved.getUrl()).isEqualTo(storedUrl);
            });
    }

    @Test
    void should_create_a_non_default_portal_with_the_resolved_url() {
        var output = useCase.execute(inputFor(AUDIT_INFO, PortalFixtures.aPortal()));

        assertThat(output.portal().isDefault()).isFalse();
        assertThat(output.portal().getUrl()).hasToString("https://portal.acme.io/next");
        assertThat(portalCrudService.storage())
            .singleElement()
            .satisfies(saved -> assertThat(saved.getUrl()).hasToString("https://portal.acme.io/next"));
    }

    @Test
    void should_refuse_a_new_portal_whose_url_cannot_be_resolved() {
        installationAccessQueryService.reset();

        assertThatThrownBy(() -> useCase.execute(inputFor(AUDIT_INFO, PortalFixtures.aPortal())))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("portal URL");
        assertThat(portalCrudService.storage()).isEmpty();
    }

    @Test
    void should_update_an_existing_portal_even_when_the_url_cannot_be_resolved() {
        var existing = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(existing));
        installationAccessQueryService.reset();

        var output = useCase.execute(
            inputFor(
                AUDIT_INFO,
                PortalFixtures.aPortal(existing.getId(), existing.getEnvironmentId(), existing.getOrganizationId(), "Renamed Portal")
            )
        );

        assertThat(output.portal().getName()).isEqualTo("Renamed Portal");
        assertThat(output.portal().getUrl()).isEqualTo(existing.getUrl());
    }

    @Test
    void should_be_idempotent_when_put_twice() {
        var portal = PortalFixtures.aPortal();

        var first = useCase.execute(inputFor(AUDIT_INFO, portal));
        var second = useCase.execute(inputFor(AUDIT_INFO, portal));

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
        var inOtherEnv = PortalFixtures.aPortal(existing.getId(), "other-env", "organization-id", "Other Env Portal");

        var output = useCase.execute(inputFor(otherEnvAudit, inOtherEnv));

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
        var portal = PortalFixtures.aPortal(defaultPortalId, AUDIT_INFO.environmentId(), AUDIT_INFO.organizationId(), "Default Portal");

        useCase.execute(
            inputFor(
                AUDIT_INFO,
                portal,
                PortalNavigationStructure.ofTopNavbar(
                    List.of(new NavigationPath("/projects/alpha", "Alpha"), new NavigationPath("/projects/beta", null))
                )
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
                inputFor(
                    AUDIT_INFO,
                    portal,
                    PortalNavigationStructure.ofTopNavbar(List.of(new NavigationPath("/valid", null), new NavigationPath("bad-path", null)))
                )
            )
        )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("structure.topNavbar[1].path");
    }

    @Test
    void should_return_empty_errors_when_navigation_is_valid() {
        var portal = PortalFixtures.aPortal();

        var output = useCase.execute(
            inputFor(AUDIT_INFO, portal, PortalNavigationStructure.ofTopNavbar(List.of(new NavigationPath("/docs", null))))
        );

        assertThat(output.errors()).isEmpty();
    }

    @Test
    void should_persist_navigation_array_on_portal_row() {
        var portal = PortalFixtures.aPortal();
        var input = PortalNavigationStructure.ofTopNavbar(List.of(new NavigationPath("/a", "A"), new NavigationPath("/b", null)));

        var output = useCase.execute(inputFor(AUDIT_INFO, portal, input));

        assertThat(output.portal().getNavigationStructure().forArea(PortalArea.TOP_NAVBAR))
            .extracting(NavigationPath::path)
            .containsExactly("/a", "/b");
        assertThat(portalCrudService.storage()).hasSize(1);
        assertThat(portalCrudService.storage().get(0).getNavigationStructure().forArea(PortalArea.TOP_NAVBAR))
            .extracting(NavigationPath::path)
            .containsExactly("/a", "/b");
        assertThat(output.structure()).isEqualTo(output.portal().getNavigationStructure());
    }

    @Test
    void should_use_previously_persisted_to_skip_unmanaged_folders_on_delete() {
        var portal = PortalFixtures.aPortal();
        useCase.execute(inputFor(AUDIT_INFO, portal, PortalNavigationStructure.ofTopNavbar(List.of(new NavigationPath("/managed", null)))));
        var unmanaged = io.gravitee.apim.core.portal_page.model.PortalNavigationFolder.builder()
            .id(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId.random())
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("manual")
            .segment("manual")
            .area(PortalArea.TOP_NAVBAR)
            .order(1)
            .published(true)
            .visibility(io.gravitee.apim.core.portal.model.PortalVisibility.PUBLIC)
            .build();
        unmanaged.markAsRoot();
        navCrudService.initWith(List.of(unmanaged));

        useCase.execute(inputFor(AUDIT_INFO, portal, PortalNavigationStructure.ofTopNavbar(List.of())));

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
                PortalFixtures.aPortal(
                    io.gravitee.apim.core.portal.model.PortalId.of("00000000-0000-0000-0000-0000000000b1"),
                    AUDIT_INFO.environmentId(),
                    AUDIT_INFO.organizationId(),
                    "Established"
                )
            )
        );
        var incoming = PortalFixtures.aPortal();

        assertThatThrownBy(() -> useCase.execute(inputFor(AUDIT_INFO, incoming)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("hrid")
            .hasMessageContaining("established portal");
    }

    @Test
    void should_accept_portal_when_no_conflict_in_environment() {
        var portal = PortalFixtures.aPortal();

        var output = useCase.execute(inputFor(AUDIT_INFO, portal));

        assertThat(output.portal()).isEqualTo(portal);
    }

    private static Theme aTheme(String id, ThemeType type, boolean enabled) {
        return Theme.builder()
            .id(id)
            .name("name")
            .type(type)
            .referenceId(AUDIT_INFO.environmentId())
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .enabled(enabled)
            .build();
    }

    @Test
    void should_wire_active_theme_and_enable_it_when_active_theme_hrid_set() {
        var themeHrid = "brand-theme";
        var themeId = io.gravitee.rest.api.service.common.HRIDToUUID.portalTheme().context(AUDIT_INFO).hrid(themeHrid).id();
        var otherThemeId = "11111111-1111-1111-1111-111111111111";
        themeCrudService.initWith(
            List.of(aTheme(themeId, ThemeType.PORTAL_NEXT, false), aTheme(otherThemeId, ThemeType.PORTAL_NEXT, true))
        );
        var portal = PortalFixtures.aPortal();

        var output = useCase.execute(inputFor(AUDIT_INFO, portal, PortalNavigationStructure.empty(), themeHrid));

        assertThat(output.portal().getActiveThemeId()).isEqualTo(themeId);
        assertThat(themeCrudService.storage())
            .filteredOn(t -> t.getId().equals(themeId))
            .singleElement()
            .extracting(t -> t.isEnabled())
            .isEqualTo(true);
        assertThat(themeCrudService.storage())
            .filteredOn(t -> t.getId().equals(otherThemeId))
            .singleElement()
            .extracting(t -> t.isEnabled())
            .isEqualTo(false);
    }

    @Test
    void should_clear_active_theme_and_disable_previous_when_hrid_not_provided() {
        var existingActiveThemeId = "22222222-2222-2222-2222-222222222222";
        var portal = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(portal.withActiveThemeId(existingActiveThemeId)));
        themeCrudService.initWith(List.of(aTheme(existingActiveThemeId, ThemeType.PORTAL_NEXT, true)));

        var output = useCase.execute(inputFor(AUDIT_INFO, portal));

        assertThat(output.portal().getActiveThemeId()).isNull();
        assertThat(themeCrudService.storage())
            .filteredOn(t -> t.getId().equals(existingActiveThemeId))
            .singleElement()
            .extracting(Theme::isEnabled)
            .isEqualTo(false);
    }

    @Test
    void should_reactivate_another_existing_theme_when_active_theme_is_cleared() {
        var clearedThemeId = "22222222-2222-2222-2222-222222222222";
        var otherThemeId = "33333333-3333-3333-3333-333333333333";
        var portal = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(portal.withActiveThemeId(clearedThemeId)));
        themeCrudService.initWith(
            List.of(aTheme(clearedThemeId, ThemeType.PORTAL_NEXT, true), aTheme(otherThemeId, ThemeType.PORTAL_NEXT, false))
        );

        var output = useCase.execute(inputFor(AUDIT_INFO, portal));

        assertThat(output.portal().getActiveThemeId()).isNull();
        assertThat(themeCrudService.storage())
            .filteredOn(t -> t.getId().equals(clearedThemeId))
            .singleElement()
            .extracting(Theme::isEnabled)
            .isEqualTo(false);
        assertThat(themeCrudService.storage())
            .filteredOn(t -> t.getId().equals(otherThemeId))
            .singleElement()
            .extracting(Theme::isEnabled)
            .isEqualTo(true);
    }
}
