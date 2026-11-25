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
package io.gravitee.apim.core.portal.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalAddressDomainServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.installation.model.InstallationType;
import io.gravitee.apim.core.portal.exception.DefaultPortalNotFoundException;
import io.gravitee.apim.core.portal.exception.SeveralPortalsInEnvironmentException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultPortalDomainServiceTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final PortalId DEFAULT_PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalId AUTOMATION_PORTAL_ID = PortalId.of("22222222-2222-2222-2222-222222222222");

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final InstallationAccessQueryServiceInMemory installationAccessQueryService = new InstallationAccessQueryServiceInMemory();
    private final PortalNavigationItemsCrudServiceInMemory navigationItemCrudService = new PortalNavigationItemsCrudServiceInMemory();
    private PortalAddressDomainServiceInMemory addresses;
    private DefaultPortalDomainService cut;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        installationAccessQueryService.reset();
        navigationItemCrudService.reset();
        addresses = new PortalAddressDomainServiceInMemory(portalCrudService, installationAccessQueryService);
        cut = new DefaultPortalDomainService(portalCrudService, addresses, contentOwnership(portalCrudService));
    }

    private PortalContentOwnershipDomainService contentOwnership(PortalCrudServiceInMemory portals) {
        return new PortalContentOwnershipDomainService(portals, navigationItemCrudService);
    }

    private PortalNavigationFolder anItemOnTheZeroUuid() {
        return PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .reference(new NavigationItemReference.PortalReference(PortalId.of("00000000-0000-0000-0000-000000000000")))
            .title("Docs")
            .segment("docs")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private static Portal aPortal(PortalId id, String environmentId, String name, boolean defaultPortal, String accessPointId) {
        return Portal.of(id, environmentId, ORG_ID, name, PortalNavigationStructure.empty(), null, defaultPortal, accessPointId);
    }

    private static Portal aDefaultPortal(String accessPointId) {
        return aPortal(DEFAULT_PORTAL_ID, ENV_ID, "Developer Portal", true, accessPointId);
    }

    private static Portal anAutomationPortal(String accessPointId) {
        return aPortal(AUTOMATION_PORTAL_ID, ENV_ID, "Automation Portal", false, accessPointId);
    }

    private static AccessPoint aPortalAccessPoint(String id, String host) {
        return AccessPoint.builder()
            .id(id)
            .referenceType(AccessPoint.ReferenceType.ENVIRONMENT)
            .referenceId(ENV_ID)
            .target(AccessPoint.Target.PORTAL_INSTANCE)
            .host(host)
            .secured(true)
            .build();
    }

    /** A comparable snapshot of every stored field, since Portal equality is by id only. */
    private List<String> storedState() {
        return portalCrudService
            .storage()
            .stream()
            .map(p ->
                String.join(
                    "|",
                    p.getId().toString(),
                    p.getEnvironmentId(),
                    p.getOrganizationId(),
                    p.getName(),
                    String.valueOf(p.isDefault()),
                    String.valueOf(p.getAccessPointId())
                )
            )
            .toList();
    }

    @Nested
    class EnsureDefault {

        @Test
        void should_create_default_portal_at_the_shared_access_point_when_environment_has_none() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");

            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(portalCrudService.storage())
                .singleElement()
                .satisfies(portal -> {
                    assertThat(portal.getName()).isEqualTo("Developer Portal");
                    assertThat(portal.isDefault()).isTrue();
                    assertThat(portal.getEnvironmentId()).isEqualTo(ENV_ID);
                    assertThat(portal.getOrganizationId()).isEqualTo(ORG_ID);
                    assertThat(portal.getAccessPointId()).isNotNull();
                    assertThat(urlOf(portal)).hasToString("https://portal.acme.io/next");
                });
            assertThat(result).hasValueSatisfying(portal ->
                assertThat(portal.getId()).isEqualTo(portalCrudService.storage().getFirst().getId())
            );
        }

        @Test
        void should_create_the_default_portal_without_access_point_on_a_multi_tenant_installation() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            addresses.installationType().set(InstallationType.MULTI_TENANT);

            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(result).hasValueSatisfying(portal -> assertThat(portal.isDefault()).isTrue());
            assertThat(portalCrudService.storage()).singleElement().extracting(Portal::getAccessPointId).isNull();
            assertThat(addresses.accessPoints().storage()).isEmpty();
        }

        @Test
        void should_postpone_the_default_portal_while_its_url_cannot_be_resolved() {
            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(result).isEmpty();
            assertThat(portalCrudService.storage()).isEmpty();
        }

        @Test
        void should_create_the_postponed_default_portal_once_its_url_can_be_resolved() {
            cut.ensureDefault(ORG_ID, ENV_ID);
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");

            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(result).hasValueSatisfying(portal -> assertThat(urlOf(portal)).hasToString("https://portal.acme.io/next"));
            assertThat(portalCrudService.storage()).hasSize(1);
        }

        @Test
        void should_not_create_nor_mark_default_when_environment_has_only_an_automation_portal() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(anAutomationPortal("portal-access-point")));
            var before = storedState();

            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(result).isEmpty();
            assertThat(storedState()).isEqualTo(before);
        }

        @Test
        void should_return_existing_default_portal_unchanged() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://other.acme.io");
            portalCrudService.initWith(List.of(aDefaultPortal("portal-access-point")));
            var before = storedState();

            var first = cut.ensureDefault(ORG_ID, ENV_ID);
            var second = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(first).hasValueSatisfying(portal -> assertThat(portal.getId()).isEqualTo(DEFAULT_PORTAL_ID));
            assertThat(second).hasValueSatisfying(portal -> assertThat(portal.getId()).isEqualTo(DEFAULT_PORTAL_ID));
            assertThat(storedState()).isEqualTo(before);
        }

        @Test
        void should_create_a_single_default_portal_when_called_twice() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            cut.ensureDefault(ORG_ID, ENV_ID);
            cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(portalCrudService.storage()).hasSize(1);
        }

        @Test
        void should_only_look_at_the_given_environment() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, OTHER_ENV_ID, "Other", false, "portal-access-point")));

            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(result).hasValueSatisfying(portal -> assertThat(portal.getEnvironmentId()).isEqualTo(ENV_ID));
            assertThat(portalCrudService.storage()).hasSize(2);
        }

        @Test
        void should_return_the_default_portal_when_the_environment_has_several_portals() {
            portalCrudService.initWith(List.of(anAutomationPortal("portal-access-point"), aDefaultPortal("portal-access-point")));
            var before = storedState();

            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(result).hasValueSatisfying(portal -> assertThat(portal.getId()).isEqualTo(DEFAULT_PORTAL_ID));
            assertThat(storedState()).isEqualTo(before);
        }

        @Test
        void should_refuse_when_the_environment_has_several_portals_and_none_is_default() {
            var secondAutomationPortalId = PortalId.of("33333333-3333-3333-3333-333333333333");
            portalCrudService.initWith(
                List.of(
                    anAutomationPortal("portal-access-point"),
                    aPortal(secondAutomationPortalId, ENV_ID, "Second Automation Portal", false, "portal-access-point")
                )
            );
            var before = storedState();

            assertThatThrownBy(() -> cut.ensureDefault(ORG_ID, ENV_ID))
                .isInstanceOf(SeveralPortalsInEnvironmentException.class)
                .hasMessageContaining(ENV_ID)
                .hasMessageContaining(AUTOMATION_PORTAL_ID.toString())
                .hasMessageContaining("Automation Portal")
                .hasMessageContaining(secondAutomationPortalId.toString())
                .hasMessageContaining("Second Automation Portal");
            assertThat(storedState()).isEqualTo(before);
        }

        @Test
        void should_move_orphan_content_onto_a_default_portal_it_creates() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            var item = anItemOnTheZeroUuid();
            navigationItemCrudService.initWith(List.of(item));

            var created = cut.ensureDefault(ORG_ID, ENV_ID).orElseThrow();

            assertThat(item.getReference()).isEqualTo(new NavigationItemReference.PortalReference(created.getId()));
        }

        @Test
        void should_not_move_content_when_the_default_portal_already_exists() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(aDefaultPortal("portal-access-point")));
            var item = anItemOnTheZeroUuid();
            navigationItemCrudService.initWith(List.of(item));

            cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(item.getReference()).isEqualTo(
                new NavigationItemReference.PortalReference(PortalId.of("00000000-0000-0000-0000-000000000000"))
            );
        }

        @Test
        void should_move_nothing_while_the_default_portal_is_postponed() {
            var item = anItemOnTheZeroUuid();
            navigationItemCrudService.initWith(List.of(item));

            cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(item.getReference()).isEqualTo(
                new NavigationItemReference.PortalReference(PortalId.of("00000000-0000-0000-0000-000000000000"))
            );
        }
    }

    @Nested
    class CheckSinglePortal {

        @Test
        void should_accept_an_environment_with_one_portal_or_none() {
            portalCrudService.initWith(List.of(anAutomationPortal("portal-access-point")));

            cut.checkSinglePortal(ENV_ID);
            cut.checkSinglePortal(OTHER_ENV_ID);
        }

        @Test
        void should_name_the_environment_its_portals_and_the_fix_when_it_has_several() {
            portalCrudService.initWith(List.of(aDefaultPortal("portal-access-point"), anAutomationPortal("portal-access-point")));

            assertThatThrownBy(() -> cut.checkSinglePortal(ENV_ID))
                .isInstanceOf(SeveralPortalsInEnvironmentException.class)
                .hasMessageContaining("Environment [env-1] has 2 portals")
                .hasMessageContaining("[11111111-1111-1111-1111-111111111111] \"Developer Portal\"")
                .hasMessageContaining("[22222222-2222-2222-2222-222222222222] \"Automation Portal\"")
                .hasMessageContaining("remove");
        }
    }

    @Nested
    class FindDefault {

        @Test
        void should_return_default_portal() {
            portalCrudService.initWith(List.of(aDefaultPortal("portal-access-point")));

            assertThat(cut.findDefault(ENV_ID)).hasValueSatisfying(portal -> assertThat(portal.getId()).isEqualTo(DEFAULT_PORTAL_ID));
        }

        @Test
        void should_return_empty_when_environment_has_no_default_portal() {
            portalCrudService.initWith(List.of(anAutomationPortal("portal-access-point")));

            assertThat(cut.findDefault(ENV_ID)).isEmpty();
        }
    }

    @Nested
    class FindDefaultOrOnly {

        @Test
        void should_find_the_default_portal() {
            portalCrudService.initWith(List.of(anAutomationPortal("portal-access-point"), aDefaultPortal("portal-access-point")));

            assertThat(cut.findDefaultOrOnly(ENV_ID)).hasValueSatisfying(portal -> assertThat(portal.getId()).isEqualTo(DEFAULT_PORTAL_ID));
        }

        @Test
        void should_find_the_only_portal_when_none_is_default() {
            portalCrudService.initWith(List.of(anAutomationPortal("portal-access-point")));

            assertThat(cut.findDefaultOrOnly(ENV_ID)).hasValueSatisfying(portal ->
                assertThat(portal.getId()).isEqualTo(AUTOMATION_PORTAL_ID)
            );
        }

        @Test
        void should_find_nothing_with_several_portals_and_none_default() {
            portalCrudService.initWith(
                List.of(
                    anAutomationPortal("portal-access-point"),
                    aPortal(DEFAULT_PORTAL_ID, ENV_ID, "Second Automation Portal", false, "portal-access-point")
                )
            );

            assertThat(cut.findDefaultOrOnly(ENV_ID)).isEmpty();
        }

        @Test
        void should_find_nothing_without_portal() {
            assertThat(cut.findDefaultOrOnly(ENV_ID)).isEmpty();
        }
    }

    @Nested
    class GetDefault {

        @Test
        void should_return_default_portal() {
            portalCrudService.initWith(List.of(aDefaultPortal("portal-access-point")));

            assertThat(cut.getDefault(ENV_ID).getId()).isEqualTo(DEFAULT_PORTAL_ID);
        }

        @Test
        void should_throw_default_portal_not_found_when_none() {
            portalCrudService.initWith(List.of(anAutomationPortal("portal-access-point")));

            assertThatThrownBy(() -> cut.getDefault(ENV_ID))
                .isInstanceOf(DefaultPortalNotFoundException.class)
                .hasMessageContaining(ENV_ID);
        }
    }

    @Nested
    class LinkUnlinkedPortals {

        @Test
        void should_link_portals_without_access_point_to_the_shared_one() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(aDefaultPortal(null), anAutomationPortal(null)));

            var linked = cut.linkUnlinkedPortals(ENV_ID);

            assertThat(linked).isEqualTo(2);
            assertThat(portalCrudService.storage())
                .extracting(Portal::getAccessPointId)
                .doesNotContainNull()
                .containsOnly(portalCrudService.storage().getFirst().getAccessPointId());
            assertThat(addresses.accessPoints().storage())
                .singleElement()
                .satisfies(accessPoint -> {
                    assertThat(accessPoint.getHost()).isEqualTo("portal.acme.io");
                    assertThat(accessPoint.isSecured()).isTrue();
                    assertThat(accessPoint.getReferenceId()).isEqualTo(ENV_ID);
                });
        }

        @Test
        void should_keep_the_access_point_of_a_linked_portal() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://new.acme.io");
            addresses.accessPoints().initWith(List.of(aPortalAccessPoint("old-access-point", "old.acme.io")));
            portalCrudService.initWith(List.of(aDefaultPortal("old-access-point")));

            var linked = cut.linkUnlinkedPortals(ENV_ID);

            assertThat(linked).isZero();
            assertThat(portalCrudService.storage()).singleElement().extracting(Portal::getAccessPointId).isEqualTo("old-access-point");
            assertThat(addresses.accessPoints().storage()).extracting(AccessPoint::getId).containsExactly("old-access-point");
        }

        @Test
        void should_relink_a_portal_whose_access_point_was_deleted() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(anAutomationPortal("released-access-point")));

            var linked = cut.linkUnlinkedPortals(ENV_ID);

            assertThat(linked).isEqualTo(1);
            assertThat(addresses.accessPoints().storage())
                .singleElement()
                .satisfies(accessPoint -> {
                    assertThat(accessPoint.getHost()).isEqualTo("portal.acme.io");
                    assertThat(portalCrudService.storage())
                        .singleElement()
                        .extracting(Portal::getAccessPointId)
                        .isEqualTo(accessPoint.getId());
                });
        }

        @Test
        void should_link_no_portal_on_a_multi_tenant_installation() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            addresses.installationType().set(InstallationType.MULTI_TENANT);
            portalCrudService.initWith(List.of(aDefaultPortal(null)));

            assertThat(cut.linkUnlinkedPortals(ENV_ID)).isZero();
            assertThat(portalCrudService.storage()).singleElement().extracting(Portal::getAccessPointId).isNull();
            assertThat(addresses.accessPoints().storage()).isEmpty();
        }

        @Test
        void should_leave_portals_unlinked_while_the_portal_url_cannot_be_resolved() {
            portalCrudService.initWith(List.of(aDefaultPortal(null)));

            var linked = cut.linkUnlinkedPortals(ENV_ID);

            assertThat(linked).isZero();
            assertThat(portalCrudService.storage()).singleElement().extracting(Portal::getAccessPointId).isNull();
        }

        @Test
        void should_only_link_portals_of_the_given_environment() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, OTHER_ENV_ID, "Other", false, null)));

            assertThat(cut.linkUnlinkedPortals(ENV_ID)).isZero();
            assertThat(portalCrudService.storage()).singleElement().extracting(Portal::getAccessPointId).isNull();
        }
    }

    private URL urlOf(Portal portal) {
        return addresses.resolveUrls(portal.getEnvironmentId(), List.of(portal)).get(portal.getId());
    }
}
