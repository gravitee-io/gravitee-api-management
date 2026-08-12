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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.model.Category;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClassicCategoryToPortalCategoryMapperTest {

    @Test
    void should_map_name_to_title_and_description_and_force_visible_true() {
        var classicCategory = Category.builder()
            .id("category-id")
            .environmentId("environment-id")
            .key("classic-key")
            .name("News")
            .description("News category")
            .hidden(true)
            .order(3)
            .highlightApi("api-id")
            .picture("base64-picture")
            .background("base64-background")
            .page("documentation-page-id")
            .build();

        var result = ClassicCategoryToPortalCategoryMapper.toCreatePortalCategory(classicCategory);

        assertThat(result.getTitle()).isEqualTo("News");
        assertThat(result.getDescription()).isEqualTo("News category");
        assertThat(result.isVisible()).isTrue();
    }

    @Test
    void should_force_visible_true_even_when_classic_category_is_hidden() {
        var hiddenClassicCategory = Category.builder().name("Hidden").description("Hidden category").hidden(true).build();

        var result = ClassicCategoryToPortalCategoryMapper.toCreatePortalCategory(hiddenClassicCategory);

        assertThat(result.isVisible()).isTrue();
    }
}
