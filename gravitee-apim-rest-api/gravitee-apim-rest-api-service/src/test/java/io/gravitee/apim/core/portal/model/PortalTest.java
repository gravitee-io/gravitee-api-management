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

import java.net.URI;
import java.net.URL;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalTest {

    private static final PortalId PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalNavigationStructure STRUCTURE = PortalNavigationStructure.ofTopNavbar(
        List.of(new NavigationPath("/a", "A"))
    );

    @SneakyThrows
    private static URL url(String value) {
        return URI.create(value).toURL();
    }

    private static Portal aDefaultPortalWithUrl() {
        return Portal.of(PORTAL_ID, "env", "org", "Portal", PortalNavigationStructure.empty(), null, true, url("https://portal.example"));
    }

    @Nested
    class Factories {

        @Test
        void create_default_builds_a_default_portal_with_the_given_url() {
            var portal = Portal.createDefault("env", "org", "Developer Portal", url("https://portal.example/next"));

            assertThat(portal.getId()).isNotNull();
            assertThat(portal.getName()).isEqualTo("Developer Portal");
            assertThat(portal.isDefault()).isTrue();
            assertThat(portal.getUrl()).isEqualTo(url("https://portal.example/next"));
        }

        @Test
        void create_builds_a_non_default_portal_with_the_given_url() {
            var portal = Portal.create("env", "org", "Portal", url("https://portal.example/next"));

            assertThat(portal.isDefault()).isFalse();
            assertThat(portal.getUrl()).isEqualTo(url("https://portal.example/next"));
        }

        @Test
        void create_and_create_default_refuse_a_missing_url() {
            assertThatThrownBy(() -> Portal.createDefault("env", "org", "Developer Portal", null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Portal.create("env", "org", "Portal", null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void of_builds_a_non_default_portal_with_the_placeholder_url() {
            var portal = Portal.of(PORTAL_ID, "env", "org", "Portal");

            assertThat(portal.isDefault()).isFalse();
            assertThat(portal.getUrl()).isEqualTo(Portal.URL_PLACEHOLDER);
        }

        @Test
        void placeholder_url_is_the_to_be_implemented_example() {
            assertThat(Portal.URL_PLACEHOLDER).hasToString("https://example.com/to-be-implemented");
        }

        @Test
        void full_of_keeps_every_persisted_value() {
            var portal = Portal.of(PORTAL_ID, "env", "org", "Portal", STRUCTURE, "theme-1", true, url("https://portal.example"));

            assertThat(portal.getId()).isEqualTo(PORTAL_ID);
            assertThat(portal.getEnvironmentId()).isEqualTo("env");
            assertThat(portal.getOrganizationId()).isEqualTo("org");
            assertThat(portal.getName()).isEqualTo("Portal");
            assertThat(portal.getNavigationStructure()).isEqualTo(STRUCTURE);
            assertThat(portal.getActiveThemeId()).isEqualTo("theme-1");
            assertThat(portal.isDefault()).isTrue();
            assertThat(portal.getUrl()).isEqualTo(url("https://portal.example"));
        }
    }

    @Nested
    class Update {

        @Test
        void update_changes_name_navigation_and_active_theme_and_keeps_default_marker_and_url() {
            var portal = aDefaultPortalWithUrl();

            portal.update("Renamed", STRUCTURE, "theme-2");

            assertThat(portal.getName()).isEqualTo("Renamed");
            assertThat(portal.getNavigationStructure()).isEqualTo(STRUCTURE);
            assertThat(portal.getActiveThemeId()).isEqualTo("theme-2");
            assertThat(portal.isDefault()).isTrue();
            assertThat(portal.getUrl()).isEqualTo(url("https://portal.example"));
        }

        @Test
        void update_with_null_navigation_sets_an_empty_structure() {
            var portal = Portal.of(PORTAL_ID, "env", "org", "Portal", STRUCTURE);

            portal.update("Portal", null, null);

            assertThat(portal.getNavigationStructure().isEmpty()).isTrue();
        }
    }

    @Nested
    class ChangeUrl {

        @Test
        void should_change_url() {
            var portal = aDefaultPortalWithUrl();

            portal.changeUrl(url("https://portal.acme.io/next"));

            assertThat(portal.getUrl()).hasToString("https://portal.acme.io/next");
            assertThat(portal.isDefault()).isTrue();
            assertThat(portal.getName()).isEqualTo("Portal");
        }

        @Test
        void should_reject_null_url() {
            var portal = aDefaultPortalWithUrl();

            assertThatThrownBy(() -> portal.changeUrl(null)).isInstanceOf(NullPointerException.class);
            assertThat(portal.getUrl()).hasToString("https://portal.example");
        }
    }

    @Nested
    @SuppressWarnings("deprecation")
    class Withers {

        @Test
        void with_navigation_structure_keeps_default_marker_and_url() {
            var portal = aDefaultPortalWithUrl();

            var copy = portal.withNavigationStructure(STRUCTURE);

            assertThat(copy.isDefault()).isTrue();
            assertThat(copy.getUrl()).isEqualTo(url("https://portal.example"));
        }

        @Test
        void with_active_theme_id_keeps_default_marker_and_url() {
            var portal = aDefaultPortalWithUrl();

            var copy = portal.withActiveThemeId("theme-1");

            assertThat(copy.isDefault()).isTrue();
            assertThat(copy.getUrl()).isEqualTo(url("https://portal.example"));
        }
    }
}
