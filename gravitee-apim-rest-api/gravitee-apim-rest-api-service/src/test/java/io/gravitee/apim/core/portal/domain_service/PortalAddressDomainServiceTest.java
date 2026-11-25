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

import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalAddressDomainServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    class SharedAccessPoint {

        @Test
        void should_share_one_access_point_between_the_environment_portals() {
            var first = cut.sharedAccessPoint(ENV_ID);
            var second = cut.sharedAccessPoint(ENV_ID);

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
        void should_return_empty_when_the_environment_has_no_usable_portal_url() {
            installation.withPortalUrl(ENV_ID, null);

            assertThat(cut.sharedAccessPoint(ENV_ID)).isEmpty();
            assertThat(cut.accessPoints().storage()).isEmpty();
        }
    }

    @Nested
    class ResolveUrls {

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
