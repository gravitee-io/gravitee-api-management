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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalCategoryIdTest {

    @Test
    void random_generates_distinct_ids() {
        assertThat(PortalCategoryId.random()).isNotEqualTo(PortalCategoryId.random());
    }

    @Test
    void of_parses_a_valid_uuid_string() {
        var id = PortalCategoryId.of("00000000-0000-0000-0000-0000000000b1");

        assertThat(id.toString()).isEqualTo("00000000-0000-0000-0000-0000000000b1");
    }

    @Test
    void of_rejects_a_non_uuid_string() {
        assertThatThrownBy(() -> PortalCategoryId.of("not-a-uuid")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void two_ids_built_from_the_same_uuid_string_are_equal() {
        var id = "00000000-0000-0000-0000-0000000000b1";

        assertThat(PortalCategoryId.of(id)).isEqualTo(PortalCategoryId.of(id));
    }
}
