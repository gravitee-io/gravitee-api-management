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
package io.gravitee.apim.core.portal.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalTest {

    private static final PortalId PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalNavigationStructure STRUCTURE = PortalNavigationStructure.ofTopNavbar(
        List.of(new NavigationPath("/a", "A"))
    );

    private static Portal aDefaultPortalWithAccessPoint() {
        return Portal.of(PORTAL_ID, "env", "org", "Portal", PortalNavigationStructure.empty(), null, true, "access-point");
    }

    @Nested
    class DefaultOrOnly {

        private final Portal defaultPortal = Portal.createDefault("env", "org", "Default", "access-point");
        private final Portal other = Portal.create("env", "org", "Partners", "access-point");
        private final Portal another = Portal.create("env", "org", "Internal", "access-point");

        @Test
        void should_pick_the_default_portal_among_others() {
            assertThat(Portal.defaultOrOnly(List.of(other, defaultPortal, another))).contains(defaultPortal);
        }

        @Test
        void should_pick_the_only_portal_when_it_is_not_the_default_one() {
            assertThat(Portal.defaultOrOnly(List.of(other))).contains(other);
        }

        @Test
        void should_pick_none_among_several_portals_without_a_default_one() {
            assertThat(Portal.defaultOrOnly(List.of(other, another))).isEmpty();
        }

        @Test
        void should_pick_none_without_portals() {
            assertThat(Portal.defaultOrOnly(List.of())).isEmpty();
        }
    }

    @Nested
    class Factories {

        @Test
        void create_default_builds_a_default_portal_with_the_given_access_point() {
            var portal = Portal.createDefault("env", "org", "Developer Portal", "access-point");

            assertThat(portal.getId()).isNotNull();
            assertThat(portal.getName()).isEqualTo("Developer Portal");
            assertThat(portal.isDefault()).isTrue();
            assertThat(portal.getAccessPointId()).isEqualTo("access-point");
        }

        @Test
        void create_builds_a_non_default_portal_with_the_given_access_point() {
            var portal = Portal.create("env", "org", "Portal", "access-point");

            assertThat(portal.isDefault()).isFalse();
            assertThat(portal.getAccessPointId()).isEqualTo("access-point");
        }

        @Test
        void create_and_create_default_refuse_a_missing_access_point() {
            assertThatThrownBy(() -> Portal.createDefault("env", "org", "Developer Portal", null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Portal.create("env", "org", "Portal", null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void create_with_an_id_builds_a_non_default_portal_with_that_id_and_access_point() {
            var portal = Portal.create(PORTAL_ID, "env", "org", "Portal", "access-point");

            assertThat(portal.getId()).isEqualTo(PORTAL_ID);
            assertThat(portal.getName()).isEqualTo("Portal");
            assertThat(portal.isDefault()).isFalse();
            assertThat(portal.getNavigationStructure().isEmpty()).isTrue();
            assertThat(portal.getAccessPointId()).isEqualTo("access-point");
        }

        @Test
        void create_with_an_id_refuses_a_missing_access_point() {
            assertThatThrownBy(() -> Portal.create(PORTAL_ID, "env", "org", "Portal", null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void create_from_console_builds_a_non_default_portal_created_from_the_console() {
            var portal = Portal.createFromConsole("env", "org", "Partners", "partners-access-point");

            assertThat(portal.getId()).isNotNull();
            assertThat(portal.getEnvironmentId()).isEqualTo("env");
            assertThat(portal.getOrganizationId()).isEqualTo("org");
            assertThat(portal.getName()).isEqualTo("Partners");
            assertThat(portal.isDefault()).isFalse();
            assertThat(portal.isCreatedFromConsole()).isTrue();
            assertThat(portal.getActiveThemeId()).isNull();
            assertThat(portal.getNavigationStructure().isEmpty()).isTrue();
            assertThat(portal.getAccessPointId()).isEqualTo("partners-access-point");
        }

        @Test
        void create_from_console_refuses_a_missing_access_point() {
            assertThatThrownBy(() -> Portal.createFromConsole("env", "org", "Partners", null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void other_factories_build_portals_not_created_from_the_console() {
            assertThat(Portal.createDefault("env", "org", "Developer Portal", "access-point").isCreatedFromConsole()).isFalse();
            assertThat(Portal.create("env", "org", "Portal", "access-point").isCreatedFromConsole()).isFalse();
            assertThat(Portal.create(PORTAL_ID, "env", "org", "Portal", "access-point").isCreatedFromConsole()).isFalse();
            assertThat(aDefaultPortalWithAccessPoint().isCreatedFromConsole()).isFalse();
        }

        @Test
        void full_of_keeps_the_created_from_console_marker() {
            var portal = Portal.of(PORTAL_ID, "env", "org", "Portal", STRUCTURE, "theme-1", false, true, "access-point");

            assertThat(portal.isDefault()).isFalse();
            assertThat(portal.isCreatedFromConsole()).isTrue();
            assertThat(portal.getAccessPointId()).isEqualTo("access-point");
        }

        @Test
        void full_of_keeps_every_persisted_value() {
            var portal = Portal.of(PORTAL_ID, "env", "org", "Portal", STRUCTURE, "theme-1", true, "access-point");

            assertThat(portal.getId()).isEqualTo(PORTAL_ID);
            assertThat(portal.getEnvironmentId()).isEqualTo("env");
            assertThat(portal.getOrganizationId()).isEqualTo("org");
            assertThat(portal.getName()).isEqualTo("Portal");
            assertThat(portal.getNavigationStructure()).isEqualTo(STRUCTURE);
            assertThat(portal.getActiveThemeId()).isEqualTo("theme-1");
            assertThat(portal.isDefault()).isTrue();
            assertThat(portal.getAccessPointId()).isEqualTo("access-point");
        }
    }

    @Nested
    class Update {

        @Test
        void update_changes_name_navigation_and_active_theme_and_keeps_default_marker_and_access_point() {
            var portal = aDefaultPortalWithAccessPoint();

            portal.update("Renamed", STRUCTURE, "theme-2");

            assertThat(portal.getName()).isEqualTo("Renamed");
            assertThat(portal.getNavigationStructure()).isEqualTo(STRUCTURE);
            assertThat(portal.getActiveThemeId()).isEqualTo("theme-2");
            assertThat(portal.isDefault()).isTrue();
            assertThat(portal.getAccessPointId()).isEqualTo("access-point");
        }

        @Test
        void update_with_null_navigation_sets_an_empty_structure() {
            var portal = Portal.of(PORTAL_ID, "env", "org", "Portal", STRUCTURE, null, false, "access-point");

            portal.update("Portal", null, null);

            assertThat(portal.getNavigationStructure().isEmpty()).isTrue();
        }
    }

    @Nested
    class NameFilter {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = { "", "  " })
        void should_keep_every_portal_without_a_query(String query) {
            assertThat(Portal.nameFilter(query)).accepts(named("Developer Portal"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "partner", "PARTNER", " Partner " })
        void should_keep_a_portal_whose_name_contains_the_query_ignoring_case(String query) {
            assertThat(Portal.nameFilter(query)).accepts(named("Partners extranet"));
        }

        @Test
        void should_reject_a_portal_whose_name_does_not_contain_the_query() {
            assertThat(Portal.nameFilter("partner")).rejects(named("Developer Portal"));
        }

        private static Portal named(String name) {
            return Portal.create("env", "org", name, "access-point");
        }
    }

    @Nested
    class LinkAccessPoint {

        @Test
        void should_link_another_access_point() {
            var portal = aDefaultPortalWithAccessPoint();

            portal.linkAccessPoint("other-access-point");

            assertThat(portal.getAccessPointId()).isEqualTo("other-access-point");
            assertThat(portal.isDefault()).isTrue();
            assertThat(portal.getName()).isEqualTo("Portal");
        }

        @Test
        void should_reject_a_null_access_point() {
            var portal = aDefaultPortalWithAccessPoint();

            assertThatThrownBy(() -> portal.linkAccessPoint(null)).isInstanceOf(NullPointerException.class);
            assertThat(portal.getAccessPointId()).isEqualTo("access-point");
        }
    }

    @Nested
    @SuppressWarnings("deprecation")
    class Withers {

        @Test
        void with_navigation_structure_keeps_default_marker_and_access_point() {
            var portal = aDefaultPortalWithAccessPoint();

            var copy = portal.withNavigationStructure(STRUCTURE);

            assertThat(copy.isDefault()).isTrue();
            assertThat(copy.getAccessPointId()).isEqualTo("access-point");
        }

        @Test
        void withers_keep_the_created_from_console_marker() {
            var portal = Portal.createFromConsole("env", "org", "Partners", "partners-access-point");

            assertThat(portal.withNavigationStructure(STRUCTURE).isCreatedFromConsole()).isTrue();
            assertThat(portal.withActiveThemeId("theme-1").isCreatedFromConsole()).isTrue();
        }

        @Test
        void with_active_theme_id_keeps_default_marker_and_access_point() {
            var portal = aDefaultPortalWithAccessPoint();

            var copy = portal.withActiveThemeId("theme-1");

            assertThat(copy.isDefault()).isTrue();
            assertThat(copy.getAccessPointId()).isEqualTo("access-point");
        }
    }
}
