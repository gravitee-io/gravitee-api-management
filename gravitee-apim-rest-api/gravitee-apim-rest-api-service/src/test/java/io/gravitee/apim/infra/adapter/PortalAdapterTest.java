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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalAdapterTest {

    private final PortalAdapter adapter = PortalAdapter.INSTANCE;

    @Nested
    class PortalNavigationSerialization {

        @Test
        void round_trip_through_repository_and_back() {
            var portal = Portal.of(
                PortalId.of("11111111-1111-1111-1111-111111111111"),
                "env",
                "org",
                "Portal",
                List.of(new NavigationPath("/a", "A"), new NavigationPath("/a/b", null))
            );

            var roundTripped = adapter.toEntity(adapter.toRepository(portal));

            assertThat(roundTripped.getPortalNavigation()).extracting(NavigationPath::path).containsExactly("/a", "/a/b");
            assertThat(roundTripped.getPortalNavigation().get(0).displayName()).isEqualTo("A");
            assertThat(roundTripped.getPortalNavigation().get(1).displayName()).isNull();
        }

        @Test
        void serialize_omits_absent_display_name() {
            String json = adapter.serializePortalNavigation(List.of(new NavigationPath("/a", null)));

            assertThat(json).isEqualTo("[{\"path\":\"/a\"}]");
        }

        @Test
        void serialize_includes_display_name_when_present() {
            String json = adapter.serializePortalNavigation(List.of(new NavigationPath("/a", "A")));

            assertThat(json).isEqualTo("[{\"path\":\"/a\",\"displayName\":\"A\"}]");
        }

        @Test
        void serialize_null_when_input_is_null() {
            assertThat(adapter.serializePortalNavigation(null)).isNull();
        }

        @Test
        void serialize_null_when_input_is_empty() {
            assertThat(adapter.serializePortalNavigation(List.of())).isNull();
        }

        @Test
        void deserialize_null_string_to_empty_list() {
            assertThat(adapter.deserializePortalNavigation(null)).isEmpty();
        }

        @Test
        void deserialize_blank_string_to_empty_list() {
            assertThat(adapter.deserializePortalNavigation("   ")).isEmpty();
        }

        @Test
        void deserialize_json_array_to_navigation_paths() {
            var result = adapter.deserializePortalNavigation("[{\"path\":\"/x\",\"displayName\":\"X\"},{\"path\":\"/y\"}]");

            assertThat(result).extracting(NavigationPath::path).containsExactly("/x", "/y");
            assertThat(result.get(0).displayName()).isEqualTo("X");
            assertThat(result.get(1).displayName()).isNull();
        }

        @Test
        void deserialize_malformed_json_throws() {
            assertThatThrownBy(() -> adapter.deserializePortalNavigation("{not json}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid portal navigation JSON");
        }
    }

    @Nested
    class PortalConversion {

        @Test
        void to_repository_maps_basic_fields() {
            var portal = Portal.of(PortalId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "env", "org", "Portal");

            var repo = adapter.toRepository(portal);

            assertThat(repo.getId()).isEqualTo("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            assertThat(repo.getEnvironmentId()).isEqualTo("env");
            assertThat(repo.getOrganizationId()).isEqualTo("org");
            assertThat(repo.getName()).isEqualTo("Portal");
            assertThat(repo.getPortalNavigation()).isNull();
        }

        @Test
        void to_entity_returns_null_for_null_input() {
            assertThat(adapter.toEntity(null)).isNull();
        }

        @Test
        void to_repository_returns_null_for_null_input() {
            assertThat(adapter.toRepository(null)).isNull();
        }
    }
}
