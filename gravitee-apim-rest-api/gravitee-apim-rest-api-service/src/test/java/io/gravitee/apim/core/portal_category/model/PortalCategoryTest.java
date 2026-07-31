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
package io.gravitee.apim.core.portal_category.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalCategoryTest {

    @Nested
    class Create {

        @Test
        void generates_a_random_id() {
            var first = PortalCategory.create("env", "News", "News category", true);
            var second = PortalCategory.create("env", "News", "News category", true);

            assertThat(first.getId()).isNotNull().isNotEqualTo(second.getId());
        }

        @Test
        void sets_all_fields() {
            var category = PortalCategory.create("env", "News", "News category", true);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(category.getEnvironmentId()).isEqualTo("env");
                soft.assertThat(category.getTitle()).isEqualTo("News");
                soft.assertThat(category.getDescription()).isEqualTo("News category");
                soft.assertThat(category.isVisible()).isTrue();
            });
        }
    }

    @Nested
    class Of {

        @Test
        void reconstitutes_with_the_given_id() {
            var id = PortalCategoryId.random();

            var category = PortalCategory.of(id, "env", "News", "News category", false);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(category.getId()).isEqualTo(id);
                soft.assertThat(category.getEnvironmentId()).isEqualTo("env");
                soft.assertThat(category.getTitle()).isEqualTo("News");
                soft.assertThat(category.getDescription()).isEqualTo("News category");
                soft.assertThat(category.isVisible()).isFalse();
            });
        }
    }

    @Nested
    class Update {

        @Test
        void mutates_title_description_and_visible_in_place() {
            var id = PortalCategoryId.random();
            var category = PortalCategory.of(id, "env", "News", "News category", true);

            category.update(UpdatePortalCategory.builder().title("Updated").description("Updated description").visible(false).build());

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(category.getId()).isEqualTo(id);
                soft.assertThat(category.getEnvironmentId()).isEqualTo("env");
                soft.assertThat(category.getTitle()).isEqualTo("Updated");
                soft.assertThat(category.getDescription()).isEqualTo("Updated description");
                soft.assertThat(category.isVisible()).isFalse();
            });
        }
    }

    @Nested
    class EqualsAndHashCode {

        @Test
        void same_instance_is_equal_to_itself() {
            var category = PortalCategory.create("env", "News", "News category", true);

            assertThat(category).isEqualTo(category);
        }

        @Test
        void is_not_equal_to_null() {
            var category = PortalCategory.create("env", "News", "News category", true);

            assertThat(category).isNotEqualTo(null);
        }

        @Test
        void is_not_equal_to_a_different_type() {
            var category = PortalCategory.create("env", "News", "News category", true);

            assertThat(category).isNotEqualTo("not a portal category");
        }

        @Test
        void two_categories_with_the_same_id_are_equal_even_if_other_fields_differ() {
            var id = PortalCategoryId.random();
            var first = PortalCategory.of(id, "env", "News", "News category", true);
            var second = PortalCategory.of(id, "other-env", "Other", "Other description", false);

            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
        }

        @Test
        void two_categories_with_different_ids_are_not_equal() {
            var first = PortalCategory.create("env", "News", "News category", true);
            var second = PortalCategory.create("env", "News", "News category", true);

            assertThat(first).isNotEqualTo(second);
        }
    }
}
