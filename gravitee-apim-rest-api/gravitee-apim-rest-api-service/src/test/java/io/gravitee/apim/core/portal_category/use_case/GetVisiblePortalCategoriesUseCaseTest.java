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
package io.gravitee.apim.core.portal_category.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalCategoryQueryServiceInMemory;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetVisiblePortalCategoriesUseCaseTest {

    private static final String ENVIRONMENT_ID = "env-1";

    private PortalCategoryQueryServiceInMemory portalCategoryQueryServiceInMemory;
    private GetVisiblePortalCategoriesUseCase useCase;

    @BeforeEach
    void setUp() {
        portalCategoryQueryServiceInMemory = new PortalCategoryQueryServiceInMemory();
        useCase = new GetVisiblePortalCategoriesUseCase(portalCategoryQueryServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        portalCategoryQueryServiceInMemory.reset();
    }

    @Test
    void should_return_only_visible_categories_for_environment() {
        portalCategoryQueryServiceInMemory.initWith(
            List.of(
                PortalCategory.of(PortalCategoryId.random(), ENVIRONMENT_ID, "Weather", null, true),
                PortalCategory.of(PortalCategoryId.random(), ENVIRONMENT_ID, "Hidden Category", null, false),
                PortalCategory.of(PortalCategoryId.random(), "other-env", "Banking", null, true)
            )
        );

        var output = useCase.execute(new GetVisiblePortalCategoriesUseCase.Input(ENVIRONMENT_ID));

        assertThat(output.portalCategories()).extracting(PortalCategory::getTitle).containsExactly("Weather");
    }

    @Test
    void should_return_empty_list_when_no_visible_categories() {
        portalCategoryQueryServiceInMemory.initWith(
            List.of(PortalCategory.of(PortalCategoryId.random(), ENVIRONMENT_ID, "Hidden Category", null, false))
        );

        var output = useCase.execute(new GetVisiblePortalCategoriesUseCase.Input(ENVIRONMENT_ID));

        assertThat(output.portalCategories()).isEmpty();
    }
}
