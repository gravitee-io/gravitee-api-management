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
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.installation.model.InstallationType;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalHost;
import io.gravitee.apim.core.portal.model.PortalHostTemplate;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalAddressDomainServiceTest {

    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final PortalId PORTAL_ID = PortalId.of("00000000-0000-0000-0000-000000000001");
    private static final PortalId OTHER_PORTAL_ID = PortalId.of("00000000-0000-0000-0000-000000000002");

    private final InstallationAccessQueryServiceInMemory installation = new InstallationAccessQueryServiceInMemory();
    private final PortalCrudServiceInMemory portals = new PortalCrudServiceInMemory();
    private PortalAddressDomainServiceInMemory cut;

    @BeforeEach
    void setUp() {
        installation.reset();
        portals.reset();
        installation.withPortalUrl(ENV_ID, "http://localhost:4100");
        cut = new PortalAddressDomainServiceInMemory(portals, installation);
    }

    @Nested
    class NewPortalAccessPoint {

        @Test
        void should_share_the_environment_access_point_when_no_template_is_configured() {
            var first = cut.newPortalAccessPoint(ENV_ID, "Partners");
            var second = cut.newPortalAccessPoint(ENV_ID, "Docs");

            assertThat(first).isPresent().isEqualTo(second);
            assertThat(cut.accessPoints().storage())
                .singleElement()
                .satisfies(accessPoint -> {
                    assertThat(accessPoint.getHost()).isEqualTo("localhost:4100");
                    assertThat(accessPoint.isSecured()).isFalse();
                    assertThat(accessPoint.getTarget()).isEqualTo(AccessPoint.Target.PORTAL_INSTANCE);
                    assertThat(accessPoint.getReferenceType()).isEqualTo(AccessPoint.ReferenceType.ENVIRONMENT);
                    assertThat(accessPoint.getReferenceId()).isEqualTo(ENV_ID);
                });
        }

        @Test
        void should_create_an_access_point_on_a_generated_host_keeping_the_port() {
            installation.withPortalHostTemplate(PortalHostTemplate.parse("{portal-slug}.localhost"));

            var accessPointId = cut.newPortalAccessPoint(ENV_ID, "Partners Portal");

            assertThat(cut.accessPoints().storage())
                .singleElement()
                .satisfies(accessPoint -> {
                    assertThat(accessPoint.getId()).isEqualTo(accessPointId.orElseThrow());
                    assertThat(accessPoint.getHost()).isEqualTo("partners-portal.localhost:4100");
                    assertThat(accessPoint.isSecured()).isFalse();
                });
        }

        @Test
        void should_drop_the_default_https_port_from_a_generated_host() {
            installation.withPortalUrl(ENV_ID, "https://portal.example.com/developers");
            installation.withPortalHostTemplate(PortalHostTemplate.parse("{portal-slug}.example.com"));

            cut.newPortalAccessPoint(ENV_ID, "Partners");

            assertThat(cut.accessPoints().storage())
                .singleElement()
                .satisfies(accessPoint -> {
                    assertThat(accessPoint.getHost()).isEqualTo("partners.example.com");
                    assertThat(accessPoint.isSecured()).isTrue();
                });
        }

        @Test
        void should_add_a_numeric_suffix_while_any_access_point_uses_the_host() {
            installation.withPortalHostTemplate(PortalHostTemplate.parse("{portal-slug}.localhost"));
            cut
                .accessPoints()
                .initWith(
                    List.of(
                        anAccessPoint("taken-1", OTHER_ENV_ID, AccessPoint.Target.PORTAL_INSTANCE, "partners.localhost:4100"),
                        anAccessPoint("taken-2", ENV_ID, AccessPoint.Target.CONSOLE, "partners-2.localhost:4100")
                    )
                );

            var accessPointId = cut.newPortalAccessPoint(ENV_ID, "Partners").orElseThrow();

            assertThat(cut.accessPoints().storage())
                .filteredOn(accessPoint -> accessPoint.getId().equals(accessPointId))
                .singleElement()
                .extracting(AccessPoint::getHost)
                .isEqualTo("partners-3.localhost:4100");
        }

        @Test
        void should_fall_back_to_a_fixed_slug_when_the_name_has_no_usable_character() {
            installation.withPortalHostTemplate(PortalHostTemplate.parse("{portal-slug}.localhost"));

            cut.newPortalAccessPoint(ENV_ID, "!!!");

            assertThat(cut.accessPoints().storage()).singleElement().extracting(AccessPoint::getHost).isEqualTo("portal.localhost:4100");
        }

        @Test
        void should_shorten_a_long_name_to_a_valid_dns_label() {
            installation.withPortalHostTemplate(PortalHostTemplate.parse("{portal-slug}.localhost"));

            cut.newPortalAccessPoint(ENV_ID, "a".repeat(39) + " " + "b".repeat(30));

            assertThat(cut.accessPoints().storage())
                .singleElement()
                .extracting(AccessPoint::getHost)
                .isEqualTo("a".repeat(39) + ".localhost:4100");
        }

        @Test
        void should_create_no_access_point_on_a_multi_tenant_installation() {
            installation.withPortalHostTemplate(PortalHostTemplate.parse("{portal-slug}.localhost"));
            cut.installationType().set(InstallationType.MULTI_TENANT);

            assertThat(cut.newPortalAccessPoint(ENV_ID, "Partners")).isEmpty();
            assertThat(cut.accessPoints().storage()).isEmpty();
        }

        @Test
        void should_create_no_shared_access_point_on_a_multi_tenant_installation() {
            cut.installationType().set(InstallationType.MULTI_TENANT);

            assertThat(cut.newPortalAccessPoint(ENV_ID, "Partners")).isEmpty();
            assertThat(cut.sharedAccessPoint(ENV_ID)).isEmpty();
            assertThat(cut.accessPoints().storage()).isEmpty();
        }

        @Test
        void should_return_empty_when_the_environment_has_no_usable_portal_url() {
            installation.withPortalUrl(ENV_ID, null);
            installation.withPortalHostTemplate(PortalHostTemplate.parse("{portal-slug}.localhost"));

            assertThat(cut.newPortalAccessPoint(ENV_ID, "Partners")).isEmpty();
            assertThat(cut.accessPoints().storage()).isEmpty();
        }
    }

    @Nested
    class ResolveUrls {

        @Test
        void should_keep_the_environment_portal_path_for_a_portal_at_the_shared_access_point() {
            installation.withPortalUrl(ENV_ID, "https://acme.com/portal");
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("shared", ENV_ID, "acme.com", true)));

            assertThat(cut.resolveUrls(ENV_ID, List.of(aPortal(PORTAL_ID, ENV_ID, "shared")))).hasEntrySatisfying(PORTAL_ID, url ->
                assertThat(url).hasToString("https://acme.com/portal/next")
            );
        }

        @Test
        void should_build_the_url_from_the_access_point_alone_whatever_the_environment_portal_path() {
            installation.withPortalUrl(ENV_ID, "https://portal.example.com/developers");
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("partners", ENV_ID, "partners.example.com:8443", true)));
            var portal = aPortal(PORTAL_ID, ENV_ID, "partners");

            assertThat(cut.resolveUrls(ENV_ID, List.of(portal)))
                .containsOnlyKeys(PORTAL_ID)
                .hasEntrySatisfying(PORTAL_ID, url -> assertThat(url).hasToString("https://partners.example.com:8443/next"));
        }

        @Test
        void should_keep_a_portal_on_its_host_without_the_path_of_a_new_environment_portal_url() {
            installation.withPortalUrl(ENV_ID, "https://new.acme.io/dev");
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("old-shared", ENV_ID, "old.acme.io", true)));

            assertThat(cut.resolveUrls(ENV_ID, List.of(aPortal(PORTAL_ID, ENV_ID, "old-shared")))).hasEntrySatisfying(PORTAL_ID, url ->
                assertThat(url).hasToString("https://old.acme.io/next")
            );
        }

        @Test
        void should_use_next_when_the_environment_has_no_usable_portal_url() {
            installation.withPortalUrl(ENV_ID, null);
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("partners", ENV_ID, "partners.localhost:4100", false)));

            assertThat(cut.resolveUrls(ENV_ID, List.of(aPortal(PORTAL_ID, ENV_ID, "partners")))).hasEntrySatisfying(PORTAL_ID, url ->
                assertThat(url).hasToString("http://partners.localhost:4100/next")
            );
        }

        @Test
        void should_give_no_url_to_a_portal_without_a_usable_access_point() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("elsewhere", OTHER_ENV_ID, "partners.localhost:4100", false)));

            assertThat(
                cut.resolveUrls(ENV_ID, List.of(aPortal(PORTAL_ID, ENV_ID, null), aPortal(OTHER_PORTAL_ID, ENV_ID, "elsewhere")))
            ).isEmpty();
        }
    }

    @Nested
    class FollowPortalUrl {

        private static final PortalId PARTNERS_PORTAL_ID = PortalId.of("00000000-0000-0000-0000-000000000003");

        @BeforeEach
        void setUp() {
            cut
                .accessPoints()
                .initWith(
                    List.of(
                        aPortalAccessPoint("previous-shared", ENV_ID, "localhost:4100", false),
                        aPortalAccessPoint("partners", ENV_ID, "partners.localhost:4100", false)
                    )
                );
            portals.initWith(
                List.of(
                    aPortal(PORTAL_ID, ENV_ID, "previous-shared"),
                    aPortal(OTHER_PORTAL_ID, ENV_ID, "previous-shared"),
                    aPortal(PARTNERS_PORTAL_ID, ENV_ID, "partners")
                )
            );
        }

        @Test
        void should_move_the_portals_at_the_previous_shared_access_point_to_the_new_portal_url_host() {
            installation.withPortalUrl(ENV_ID, "https://portal.acme.io/developers");

            var moved = cut.followPortalUrl(ENV_ID, Optional.of(url("http://localhost:4100/next")));

            assertThat(moved).isEqualTo(2);
            var newShared = cut
                .accessPoints()
                .storage()
                .stream()
                .filter(accessPoint -> accessPoint.getHost().equals("portal.acme.io"))
                .findFirst();
            assertThat(newShared).hasValueSatisfying(accessPoint -> assertThat(accessPoint.isSecured()).isTrue());
            assertThat(cut.accessPoints().storage()).extracting(AccessPoint::getId).doesNotContain("previous-shared").contains("partners");
            assertThat(portals.storage())
                .filteredOn(portal -> !portal.getId().equals(PARTNERS_PORTAL_ID))
                .extracting(Portal::getAccessPointId)
                .containsOnly(newShared.orElseThrow().getId());
            assertThat(cut.resolveUrls(ENV_ID, portals.storage()))
                .hasEntrySatisfying(PORTAL_ID, url -> assertThat(url).hasToString("https://portal.acme.io/developers/next"))
                .hasEntrySatisfying(PARTNERS_PORTAL_ID, url -> assertThat(url).hasToString("http://partners.localhost:4100/next"));
        }

        @Test
        void should_keep_the_portals_when_only_the_path_of_the_portal_url_changes() {
            installation.withPortalUrl(ENV_ID, "http://localhost:4100/developers");

            assertThat(cut.followPortalUrl(ENV_ID, Optional.of(url("http://localhost:4100/next")))).isZero();
            assertThat(portals.storage()).extracting(Portal::getAccessPointId).containsOnly("previous-shared", "partners");
        }

        @Test
        void should_keep_the_portals_without_a_previous_portal_url() {
            installation.withPortalUrl(ENV_ID, "https://portal.acme.io");

            assertThat(cut.followPortalUrl(ENV_ID, Optional.empty())).isZero();
            assertThat(cut.accessPoints().storage()).extracting(AccessPoint::getId).containsOnly("previous-shared", "partners");
        }

        @Test
        void should_keep_the_portals_while_the_new_portal_url_cannot_be_resolved() {
            installation.withPortalUrl(ENV_ID, null);

            assertThat(cut.followPortalUrl(ENV_ID, Optional.of(url("http://localhost:4100/next")))).isZero();
            assertThat(portals.storage()).extracting(Portal::getAccessPointId).containsOnly("previous-shared", "partners");
        }

        @Test
        void should_move_nothing_on_a_multi_tenant_installation() {
            installation.withPortalUrl(ENV_ID, "https://portal.acme.io");
            cut.installationType().set(InstallationType.MULTI_TENANT);

            assertThat(cut.followPortalUrl(ENV_ID, Optional.of(url("http://localhost:4100/next")))).isZero();
            assertThat(cut.accessPoints().storage()).extracting(AccessPoint::getId).containsOnly("previous-shared", "partners");
        }

        @SneakyThrows
        private static URL url(String value) {
            return URI.create(value).toURL();
        }
    }

    @Nested
    class ValidateCustomUrl {

        @Test
        void should_keep_scheme_host_and_port_with_a_lowercase_host() {
            assertThat(cut.validateCustomUrl(PORTAL_ID, "HTTPS://Developers.Acme.io:8443/")).hasToString("https://developers.acme.io:8443");
        }

        @Test
        void should_accept_the_host_the_portal_already_has() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("own", ENV_ID, "developers.acme.io", true)));
            portals.initWith(List.of(aPortal(PORTAL_ID, ENV_ID, "own")));

            assertThat(cut.validateCustomUrl(PORTAL_ID, "https://developers.acme.io")).hasToString("https://developers.acme.io");
        }

        @Test
        void should_refuse_a_host_used_by_a_portal_of_another_environment() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("taken", OTHER_ENV_ID, "developers.acme.io", true)));
            portals.initWith(List.of(aPortal(OTHER_PORTAL_ID, OTHER_ENV_ID, "taken")));

            assertThatThrownBy(() -> cut.validateCustomUrl(PORTAL_ID, "https://DEVELOPERS.acme.io"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("developers.acme.io")
                .extracting("technicalCode")
                .isEqualTo("portal.url.exists");
        }

        @Test
        void should_refuse_a_host_used_by_an_access_point_that_serves_no_portal() {
            cut.accessPoints().initWith(List.of(anAccessPoint("console", ENV_ID, AccessPoint.Target.CONSOLE, "developers.acme.io")));

            assertThatThrownBy(() -> cut.validateCustomUrl(PORTAL_ID, "https://developers.acme.io"))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("developers.acme.io")
                .extracting("technicalCode")
                .isEqualTo("portal.url.exists");
        }

        @Test
        void should_refuse_the_host_of_a_portal_access_point_no_portal_is_served_at() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("orphan", ENV_ID, "developers.acme.io", true)));

            assertThatThrownBy(() -> cut.validateCustomUrl(PORTAL_ID, "https://developers.acme.io"))
                .isInstanceOf(ValidationDomainException.class)
                .extracting("technicalCode")
                .isEqualTo("portal.url.exists");
        }

        @ParameterizedTest
        @ValueSource(
            strings = {
                "",
                "developers.acme.io",
                "/portal",
                "ftp://developers.acme.io",
                "https://user:secret@developers.acme.io",
                "https://developers.acme.io/portal",
                "https://developers.acme.io?a=b",
                "https://developers.acme.io#top",
                "https://developers acme.io",
            }
        )
        void should_refuse_a_url_an_access_point_cannot_serve(String url) {
            assertThatThrownBy(() -> cut.validateCustomUrl(PORTAL_ID, url)).isInstanceOf(ValidationDomainException.class);
        }
    }

    @Nested
    class FindByAddress {

        private final PortalHost host = PortalHost.parse("partners.localhost:4100").orElseThrow();

        @Test
        void should_find_the_only_portal_on_the_host_in_any_environment() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("partners", OTHER_ENV_ID, "partners.localhost:4100", false)));
            portals.initWith(List.of(aPortal(PORTAL_ID, OTHER_ENV_ID, "partners")));

            assertThat(cut.findByAddress(host, null)).hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(PORTAL_ID));
        }

        @Test
        void should_ignore_a_portal_outside_the_scoped_environment() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("partners", OTHER_ENV_ID, "partners.localhost:4100", false)));
            portals.initWith(List.of(aPortal(PORTAL_ID, OTHER_ENV_ID, "partners")));

            assertThat(cut.findByAddress(host, ENV_ID)).isEmpty();
        }

        @Test
        void should_resolve_a_host_shared_across_environments_inside_the_scoped_one() {
            cut
                .accessPoints()
                .initWith(
                    List.of(
                        aPortalAccessPoint("mine", ENV_ID, "partners.localhost:4100", false),
                        aPortalAccessPoint("theirs", OTHER_ENV_ID, "partners.localhost:4100", false)
                    )
                );
            portals.initWith(List.of(aPortal(PORTAL_ID, ENV_ID, "mine"), aPortal(OTHER_PORTAL_ID, OTHER_ENV_ID, "theirs")));

            assertThat(cut.findByAddress(host, ENV_ID)).hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(PORTAL_ID));
        }

        @Test
        void should_find_nothing_when_portals_share_the_access_point() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("shared", ENV_ID, "partners.localhost:4100", false)));
            portals.initWith(List.of(aPortal(PORTAL_ID, ENV_ID, "shared"), aPortal(OTHER_PORTAL_ID, ENV_ID, "shared")));

            assertThat(cut.findByAddress(host, null)).isEmpty();
        }

        @Test
        void should_ignore_an_access_point_of_another_target() {
            cut.accessPoints().initWith(List.of(anAccessPoint("console", ENV_ID, AccessPoint.Target.CONSOLE, "partners.localhost:4100")));
            portals.initWith(List.of(aPortal(PORTAL_ID, ENV_ID, "console")));

            assertThat(cut.findByAddress(host, null)).isEmpty();
        }
    }

    @Nested
    class OwnOrigin {

        @Test
        void should_return_the_origin_of_the_portal_own_access_point() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("partners", ENV_ID, "partners.localhost:4100", false)));

            assertThat(cut.ownOrigin(aPortal(PORTAL_ID, ENV_ID, "partners"))).contains("http://partners.localhost:4100");
        }

        @Test
        void should_return_a_secured_origin_for_a_secured_access_point() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("partners", ENV_ID, "partners-portal.com", true)));

            assertThat(cut.ownOrigin(aPortal(PORTAL_ID, ENV_ID, "partners"))).contains("https://partners-portal.com");
        }

        @Test
        void should_return_nothing_for_the_access_point_at_the_environment_portal_url() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("shared", ENV_ID, "localhost:4100", false)));

            assertThat(cut.ownOrigin(aPortal(PORTAL_ID, ENV_ID, "shared"))).isEmpty();
        }

        @Test
        void should_return_nothing_for_a_portal_without_access_point() {
            assertThat(cut.ownOrigin(aPortal(PORTAL_ID, ENV_ID, null))).isEmpty();
        }

        @Test
        void should_return_nothing_when_the_environment_portal_url_cannot_be_resolved() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("partners", OTHER_ENV_ID, "partners.localhost:4100", false)));

            assertThat(cut.ownOrigin(aPortal(PORTAL_ID, OTHER_ENV_ID, "partners"))).isEmpty();
        }
    }

    @Nested
    class Release {

        @Test
        void should_delete_the_access_point_no_portal_is_served_at_anymore() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("partners", ENV_ID, "partners.localhost:4100", false)));

            cut.release(aPortal(PORTAL_ID, ENV_ID, "partners"));

            assertThat(cut.accessPoints().storage()).isEmpty();
        }

        @Test
        void should_keep_an_access_point_another_portal_is_served_at() {
            cut.accessPoints().initWith(List.of(aPortalAccessPoint("shared", ENV_ID, "localhost:4100", false)));
            portals.initWith(List.of(aPortal(OTHER_PORTAL_ID, ENV_ID, "shared")));

            cut.release(aPortal(PORTAL_ID, ENV_ID, "shared"));

            assertThat(cut.accessPoints().storage()).extracting(AccessPoint::getId).containsExactly("shared");
        }
    }

    private static Portal aPortal(PortalId id, String environmentId, String accessPointId) {
        return Portal.of(id, environmentId, "org-1", "Portal " + id, PortalNavigationStructure.empty(), null, false, accessPointId);
    }

    private static AccessPoint aPortalAccessPoint(String id, String environmentId, String host, boolean secured) {
        return anAccessPoint(id, environmentId, AccessPoint.Target.PORTAL_INSTANCE, host).toBuilder().secured(secured).build();
    }

    private static AccessPoint anAccessPoint(String id, String environmentId, AccessPoint.Target target, String host) {
        return AccessPoint.builder()
            .id(id)
            .referenceType(AccessPoint.ReferenceType.ENVIRONMENT)
            .referenceId(environmentId)
            .target(target)
            .host(host)
            .build();
    }
}
