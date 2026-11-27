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
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.LicenseFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.NavigationPortalDomainServiceInMemory;
import inmemory.PortalAddressDomainServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.installation.model.InstallationType;
import io.gravitee.apim.core.portal.domain_service.PortalLogoDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalNameDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalQuotaDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalSummaryDomainService;
import io.gravitee.apim.core.portal.exception.PortalLimitReachedException;
import io.gravitee.apim.core.portal.exception.PortalNameAlreadyExistsException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalStarterContentDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.node.api.license.LicenseManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreatePortalUseCaseTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId(ORG_ID)
        .environmentId(ENV_ID)
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final InstallationAccessQueryServiceInMemory installationAccessQueryService = new InstallationAccessQueryServiceInMemory();
    private final LicenseManager licenseManager = mock(LicenseManager.class);
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private FailingPortalPageContentCrudService pageContentCrudService;
    private FailingPortalNavigationItemsCrudService navigationItemsCrudService;
    private PortalStarterContentDomainService starterContentDomainService;
    private PortalAddressDomainServiceInMemory addresses;
    private CreatePortalUseCase cut;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        installationAccessQueryService.reset();
        installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
        givenLicenseLimit(10);

        var navigationItems = new ArrayList<PortalNavigationItem>();
        navigationItemsCrudService = new FailingPortalNavigationItemsCrudService(navigationItems);
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory(navigationItems);
        pageContentCrudService = new FailingPortalPageContentCrudService();
        var navigationPortalDomainService = new NavigationPortalDomainServiceInMemory(portalCrudService);
        starterContentDomainService = new PortalStarterContentDomainService(
            new PortalNavigationItemDomainService(
                navigationItemsCrudService,
                navigationItemsQueryService,
                pageContentCrudService,
                PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
                new ApiCrudServiceInMemory(),
                new PortalNavigationItemSourceDomainServiceInMemory(),
                new ApiOwnedNavigationDomainService(navigationItemsQueryService, navigationItemsCrudService),
                navigationPortalDomainService
            ),
            pageContentCrudService,
            navigationItemsQueryService,
            navigationItemsCrudService
        );
        addresses = new PortalAddressDomainServiceInMemory(portalCrudService, installationAccessQueryService);
        cut = new CreatePortalUseCase(
            new PortalNameDomainService(portalCrudService),
            new PortalQuotaDomainService(licenseManager),
            addresses,
            portalCrudService,
            starterContentDomainService,
            new PortalSummaryDomainService(
                new PortalLogoDomainService(new ThemeQueryServiceInMemory()),
                navigationItemsQueryService,
                addresses
            )
        );
    }

    private void givenLicenseLimit(int limit) {
        when(licenseManager.getOrganizationLicenseOrPlatform(ORG_ID)).thenReturn(
            LicenseFixtures.aLicenseWithAttribute("apim-portals-max", limit)
        );
    }

    private Portal givenDefaultPortal() {
        return portalCrudService.create(Portal.createDefault(ENV_ID, ORG_ID, "Developer Portal", "default-portal-access-point"));
    }

    private Portal givenAutomationPortal(String environmentId, String name) {
        return portalCrudService.create(Portal.create(environmentId, ORG_ID, name, "automation-access-point"));
    }

    private CreatePortalUseCase.Output create(String name, String url) {
        return cut.execute(new CreatePortalUseCase.Input(AUDIT_INFO, name, url));
    }

    @Test
    void should_create_a_portal_that_is_not_the_default_one() {
        givenDefaultPortal();

        var output = create("  Partners  ", null);

        var created = output.portal().portal();
        assertThat(created.getName()).isEqualTo("Partners");
        assertThat(created.isDefault()).isFalse();
        assertThat(created.isCreatedFromConsole()).isTrue();
        assertThat(created.getEnvironmentId()).isEqualTo(ENV_ID);
        assertThat(created.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(created.getActiveThemeId()).isNull();
        assertThat(output.portal().publishedApisCount()).isZero();
        assertThat(portalCrudService.findByIdAndEnvironmentId(created.getId(), ENV_ID)).hasValueSatisfying(stored -> {
            assertThat(stored.getName()).isEqualTo("Partners");
            assertThat(stored.isDefault()).isFalse();
            assertThat(stored.isCreatedFromConsole()).isTrue();
        });
        assertThat(portalCrudService.findDefaultByEnvironmentId(ENV_ID)).hasValueSatisfying(defaultPortal ->
            assertThat(defaultPortal.getName()).isEqualTo("Developer Portal")
        );
    }

    @Test
    void should_serve_the_portal_at_the_environment_shared_access_point() {
        givenDefaultPortal();
        var created = create("Partners", null).portal().portal();

        assertThat(created.getAccessPointId()).isNotNull();
        assertThat(addresses.accessPoints().storage())
            .singleElement()
            .satisfies(accessPoint -> {
                assertThat(accessPoint.getId()).isEqualTo(created.getAccessPointId());
                assertThat(accessPoint.getHost()).isEqualTo("portal.acme.io");
            });
        assertThat(addresses.resolveUrls(ENV_ID, List.of(created))).hasEntrySatisfying(created.getId(), url ->
            assertThat(url).hasToString("https://portal.acme.io/next")
        );
    }

    @Test
    void should_create_a_portal_without_access_point_on_a_multi_tenant_installation() {
        givenDefaultPortal();
        addresses.installationType().set(InstallationType.MULTI_TENANT);

        var created = create("Partners", null).portal().portal();

        assertThat(created.getAccessPointId()).isNull();
        assertThat(addresses.accessPoints().storage()).isEmpty();
    }

    @Test
    void should_refuse_when_the_environment_portal_url_cannot_be_resolved() {
        givenDefaultPortal();
        installationAccessQueryService.reset();

        assertThatThrownBy(() -> create("Partners", null))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("URL");
        assertThat(portalCrudService.storage()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://partners.example.com", "not a url", "" })
    void should_refuse_a_url_until_portal_urls_can_be_chosen(String url) {
        givenDefaultPortal();

        assertThatThrownBy(() -> create("Partners", url))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("cannot be chosen yet");
        assertThat(portalCrudService.storage()).extracting(Portal::getName).containsExactly("Developer Portal");
    }

    @Test
    void should_seed_the_starter_content_of_the_new_portal_and_leave_the_default_portal_unchanged() {
        var defaultPortal = new NavigationItemReference.PortalReference(givenDefaultPortal().getId());
        starterContentDomainService.seed(ORG_ID, ENV_ID, defaultPortal);
        var defaultItemsBefore = itemsOf(defaultPortal).stream().map(PortalNavigationItem::getId).toList();

        var created = new NavigationItemReference.PortalReference(create("Partners", null).portal().portal().getId());

        assertThat(itemsOf(created))
            .extracting(PortalNavigationItem::getTitle, PortalNavigationItem::getType)
            .containsExactlyInAnyOrder(
                tuple("Guides", PortalNavigationItemType.FOLDER),
                tuple("Getting started", PortalNavigationItemType.PAGE),
                tuple("Core concepts", PortalNavigationItemType.FOLDER),
                tuple("Authentication", PortalNavigationItemType.PAGE),
                tuple("Making your first API call", PortalNavigationItemType.PAGE),
                tuple("APIs", PortalNavigationItemType.FOLDER),
                tuple("Docs", PortalNavigationItemType.LINK),
                tuple("Home Page", PortalNavigationItemType.PAGE)
            );
        assertThat(
            navigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(ENV_ID, PortalArea.HOMEPAGE, created)
        )
            .extracting(PortalNavigationItem::getTitle)
            .containsExactly("Home Page");
        assertThat(itemsOf(defaultPortal)).extracting(PortalNavigationItem::getId).containsExactlyInAnyOrderElementsOf(defaultItemsBefore);
    }

    @Test
    void should_undo_the_portal_and_the_starter_content_created_before_seeding_fails() {
        var defaultPortal = new NavigationItemReference.PortalReference(givenDefaultPortal().getId());
        starterContentDomainService.seed(ORG_ID, ENV_ID, defaultPortal);
        var itemsBefore = List.copyOf(navigationItemsQueryService.storage());
        var contentsBefore = List.copyOf(pageContentCrudService.storage());
        // The "Guides" folder is created before the first page content, so the failure comes part way
        pageContentCrudService.failAfter(0);

        assertThatThrownBy(() -> create("Partners", null)).hasMessage("page content storage unavailable");

        assertThat(portalCrudService.storage()).extracting(Portal::getName).containsExactly("Developer Portal");
        assertThat(navigationItemsQueryService.storage()).containsExactlyInAnyOrderElementsOf(itemsBefore);
        assertThat(itemsOf(defaultPortal)).hasSize(itemsBefore.size());
        assertThat(pageContentCrudService.storage()).containsExactlyInAnyOrderElementsOf(contentsBefore);
    }

    @Test
    void should_undo_the_pages_already_created_when_seeding_fails_after_them() {
        givenDefaultPortal();
        // "Getting started" gets its content and its page, then the content of "Authentication" fails
        pageContentCrudService.failAfter(1);

        assertThatThrownBy(() -> create("Partners", null)).hasMessage("page content storage unavailable");

        assertThat(portalCrudService.storage()).extracting(Portal::getName).containsExactly("Developer Portal");
        assertThat(navigationItemsQueryService.storage()).isEmpty();
        assertThat(pageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_delete_the_content_of_a_page_that_cannot_be_created() {
        givenDefaultPortal();
        navigationItemsCrudService.failCreatingPages();

        assertThatThrownBy(() -> create("Partners", null)).hasMessage("navigation item storage unavailable");

        assertThat(navigationItemsQueryService.storage()).isEmpty();
        assertThat(pageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_keep_the_portal_with_its_items_when_its_seeded_items_cannot_be_deleted() {
        givenDefaultPortal();
        pageContentCrudService.failAfter(0);
        navigationItemsCrudService.failDeleting();

        assertThatThrownBy(() -> create("Partners", null))
            .hasMessage("page content storage unavailable")
            .satisfies(failure ->
                assertThat(failure.getSuppressed()).extracting(Throwable::getMessage).containsExactly("navigation item storage unavailable")
            );

        // Deleting the portal would leave its items to a portal that no longer exists, for moveOrphans to give to another one
        var kept = portalCrudService
            .storage()
            .stream()
            .filter(portal -> portal.getName().equals("Partners"))
            .findFirst()
            .orElseThrow();
        assertThat(itemsOf(new NavigationItemReference.PortalReference(kept.getId())))
            .extracting(PortalNavigationItem::getTitle)
            .containsExactly("Guides");
    }

    @Test
    void should_report_the_page_failure_when_its_content_cannot_be_deleted_either() {
        givenDefaultPortal();
        navigationItemsCrudService.failCreatingPages();
        pageContentCrudService.failDeleting();

        assertThatThrownBy(() -> create("Partners", null))
            .hasMessage("navigation item storage unavailable")
            .satisfies(failure ->
                assertThat(failure.getSuppressed()).extracting(Throwable::getMessage).contains("page content storage unavailable")
            );
    }

    @Test
    void should_refuse_when_the_environment_has_no_default_portal_yet() {
        assertThatThrownBy(() -> create("Partners", null))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("no default portal yet");
        assertThat(portalCrudService.storage()).isEmpty();
    }

    @Test
    void should_refuse_when_the_environment_portals_are_managed_through_the_automation_api() {
        givenAutomationPortal(ENV_ID, "Automation Portal");

        assertThatThrownBy(() -> create("Partners", null))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Automation API");
        assertThat(portalCrudService.storage()).extracting(Portal::getName).containsExactly("Automation Portal");
    }

    @Test
    void should_refuse_at_the_license_limit_of_one_with_the_default_portal() {
        givenLicenseLimit(1);
        givenDefaultPortal();

        assertThatThrownBy(() -> create("Partners", null))
            .isInstanceOfSatisfying(PortalLimitReachedException.class, e -> {
                assertThat(e.getTechnicalCode()).isEqualTo("portal.limitReached");
                assertThat(e.getParameters()).isEqualTo(Map.of("limit", "1"));
            })
            .hasMessage("The license allows 1 portal in this environment");
        assertThat(portalCrudService.storage()).hasSize(1);
    }

    @Test
    void should_refuse_at_the_license_limit_counting_automation_api_portals() {
        givenDefaultPortal();
        IntStream.range(0, 9).forEach(i -> givenAutomationPortal(ENV_ID, "Automation " + i));

        assertThatThrownBy(() -> create("Partners", null))
            .isInstanceOfSatisfying(PortalLimitReachedException.class, e -> assertThat(e.getParameters()).isEqualTo(Map.of("limit", "10")))
            .hasMessage("The license allows 10 portals in this environment");
        assertThat(portalCrudService.storage()).hasSize(10);
    }

    @Test
    void should_create_the_last_portal_the_license_allows() {
        givenDefaultPortal();
        IntStream.range(0, 8).forEach(i -> givenAutomationPortal(ENV_ID, "Automation " + i));

        create("Partners", null);

        assertThat(portalCrudService.findByEnvironmentId(ENV_ID)).hasSize(10);
    }

    @Test
    void should_refuse_an_empty_name() {
        assertThatThrownBy(() -> create("", null))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("name");
        assertThatThrownBy(() -> create(null, null))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("name");
        assertThat(portalCrudService.storage()).isEmpty();
    }

    @Test
    void should_refuse_a_blank_name() {
        assertThatThrownBy(() -> create(" \t ", null))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("blank");
        assertThat(portalCrudService.storage()).isEmpty();
    }

    @Test
    void should_refuse_a_name_used_in_the_environment_ignoring_case() {
        givenDefaultPortal();

        assertThatThrownBy(() -> create(" developer PORTAL ", null)).isInstanceOf(PortalNameAlreadyExistsException.class);
        assertThat(portalCrudService.storage()).hasSize(1);
    }

    @Test
    void should_accept_a_name_used_in_another_environment() {
        givenDefaultPortal();
        givenAutomationPortal(OTHER_ENV_ID, "Partners");

        var created = create("Partners", null).portal().portal();

        assertThat(created.getName()).isEqualTo("Partners");
        assertThat(created.getEnvironmentId()).isEqualTo(ENV_ID);
    }

    @Test
    void should_report_an_invalid_payload_before_the_limit() {
        givenLicenseLimit(1);
        givenDefaultPortal();

        assertThatThrownBy(() -> create("", null))
            .isInstanceOf(ValidationDomainException.class)
            .isNotInstanceOf(PortalLimitReachedException.class);
        assertThatThrownBy(() -> create("Partners", "ftp://x"))
            .isInstanceOf(ValidationDomainException.class)
            .isNotInstanceOf(PortalLimitReachedException.class);
    }

    @Test
    void should_report_the_limit_before_a_name_already_used() {
        givenLicenseLimit(1);
        givenDefaultPortal();

        assertThatThrownBy(() -> create("Developer Portal", null)).isInstanceOf(PortalLimitReachedException.class);
    }

    private List<PortalNavigationItem> itemsOf(NavigationItemReference.PortalReference portal) {
        return navigationItemsQueryService
            .storage()
            .stream()
            .filter(item -> portal.equals(item.getReference()))
            .toList();
    }

    /** Fails every page content creation after the given number of successful ones. */
    private static class FailingPortalPageContentCrudService extends PortalPageContentCrudServiceInMemory {

        private int createsLeft = Integer.MAX_VALUE;
        private boolean failDeleting;

        void failAfter(int successfulCreates) {
            createsLeft = successfulCreates;
        }

        void failDeleting() {
            failDeleting = true;
        }

        @Override
        public void delete(PortalPageContentId id) {
            if (failDeleting) {
                throw new IllegalStateException("page content storage unavailable");
            }
            super.delete(id);
        }

        @Override
        public PortalPageContent<?> create(PortalPageContent<?> content) {
            if (createsLeft-- <= 0) {
                throw new IllegalStateException("page content storage unavailable");
            }
            return super.create(content);
        }
    }

    private static class FailingPortalNavigationItemsCrudService extends PortalNavigationItemsCrudServiceInMemory {

        private boolean failCreatingPages;
        private boolean failDeleting;

        FailingPortalNavigationItemsCrudService(List<PortalNavigationItem> storage) {
            super(storage);
        }

        void failCreatingPages() {
            failCreatingPages = true;
        }

        void failDeleting() {
            failDeleting = true;
        }

        @Override
        public PortalNavigationItem create(PortalNavigationItem portalNavigationItem) {
            if (failCreatingPages && portalNavigationItem.getType() == PortalNavigationItemType.PAGE) {
                throw new IllegalStateException("navigation item storage unavailable");
            }
            return super.create(portalNavigationItem);
        }

        @Override
        public void deleteByIds(List<PortalNavigationItemId> ids) {
            if (failDeleting) {
                throw new IllegalStateException("navigation item storage unavailable");
            }
            super.deleteByIds(ids);
        }
    }
}
