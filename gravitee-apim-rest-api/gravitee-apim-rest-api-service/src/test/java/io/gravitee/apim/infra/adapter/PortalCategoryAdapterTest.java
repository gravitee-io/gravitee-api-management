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

import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalCategoryAdapterTest {

    private final PortalCategoryAdapter adapter = PortalCategoryAdapter.INSTANCE;

    @Nested
    class ToModel {

        @Test
        void maps_all_fields() {
            var repository = io.gravitee.repository.management.model.PortalCategory.builder()
                .id("00000000-0000-0000-0000-0000000000b1")
                .environmentId("env")
                .title("News")
                .description("News category")
                .visible(true)
                .build();

            var domain = adapter.toModel(repository);

            assertThat(domain.getId()).isEqualTo(PortalCategoryId.of("00000000-0000-0000-0000-0000000000b1"));
            assertThat(domain.getEnvironmentId()).isEqualTo("env");
            assertThat(domain.getTitle()).isEqualTo("News");
            assertThat(domain.getDescription()).isEqualTo("News category");
            assertThat(domain.isVisible()).isTrue();
        }

        @Test
        void returns_null_for_null_input() {
            assertThat(adapter.toModel(null)).isNull();
        }
    }

    @Nested
    class ToRepository {

        @Test
        void maps_all_fields() {
            var domain = PortalCategory.of(
                PortalCategoryId.of("00000000-0000-0000-0000-0000000000b1"),
                "env",
                "News",
                "News category",
                true
            );

            var repository = adapter.toRepository(domain);

            assertThat(repository.getId()).isEqualTo("00000000-0000-0000-0000-0000000000b1");
            assertThat(repository.getEnvironmentId()).isEqualTo("env");
            assertThat(repository.getTitle()).isEqualTo("News");
            assertThat(repository.getDescription()).isEqualTo("News category");
            assertThat(repository.isVisible()).isTrue();
        }

        @Test
        void returns_null_for_null_input() {
            assertThat(adapter.toRepository(null)).isNull();
        }
    }

    @Test
    void round_trip_through_repository_and_back() {
        var domain = PortalCategory.of(PortalCategoryId.of("00000000-0000-0000-0000-0000000000b1"), "env", "News", "News category", true);

        var roundTripped = adapter.toModel(adapter.toRepository(domain));

        assertThat(roundTripped).isEqualTo(domain);
        assertThat(roundTripped.getTitle()).isEqualTo(domain.getTitle());
        assertThat(roundTripped.getDescription()).isEqualTo(domain.getDescription());
        assertThat(roundTripped.isVisible()).isEqualTo(domain.isVisible());
    }
}
