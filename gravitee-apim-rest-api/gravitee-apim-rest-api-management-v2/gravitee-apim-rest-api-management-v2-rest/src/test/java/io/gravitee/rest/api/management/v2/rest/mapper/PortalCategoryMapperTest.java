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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PortalCategoryMapperTest {

    private final PortalCategoryMapper mapper = Mappers.getMapper(PortalCategoryMapper.class);

    @Test
    void should_map_domain_portal_category_to_rest_model() {
        var domain = PortalCategory.of(
            PortalCategoryId.of("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
            "env-1",
            "Weather",
            "Weather APIs",
            true
        );

        var result = mapper.map(domain);

        assertThat(result.getId()).isEqualTo(UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"));
        assertThat(result.getTitle()).isEqualTo("Weather");
        assertThat(result.getDescription()).isEqualTo("Weather APIs");
        assertThat(result.getVisible()).isTrue();
    }

    @Test
    void should_map_list_of_domain_portal_categories() {
        var domain = PortalCategory.of(PortalCategoryId.of("f47ac10b-58cc-4372-a567-0e02b2c3d479"), "env-1", "Weather", null, true);

        var result = mapper.mapList(List.of(domain));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("Weather");
    }

    @Test
    void should_map_create_rest_model_to_domain() {
        var createPortalCategory = new io.gravitee.rest.api.management.v2.rest.model.CreatePortalCategory()
            .title("Weather")
            .description("Weather APIs")
            .visible(false);

        var result = mapper.map(createPortalCategory);

        assertThat(result.getTitle()).isEqualTo("Weather");
        assertThat(result.getDescription()).isEqualTo("Weather APIs");
        assertThat(result.isVisible()).isFalse();
    }

    @Test
    void should_map_update_rest_model_to_domain() {
        var updatePortalCategory = new io.gravitee.rest.api.management.v2.rest.model.UpdatePortalCategory()
            .title("Weather")
            .description("Weather APIs")
            .visible(false);

        var result = mapper.map(updatePortalCategory);

        assertThat(result.getTitle()).isEqualTo("Weather");
        assertThat(result.getDescription()).isEqualTo("Weather APIs");
        assertThat(result.isVisible()).isFalse();
    }
}
