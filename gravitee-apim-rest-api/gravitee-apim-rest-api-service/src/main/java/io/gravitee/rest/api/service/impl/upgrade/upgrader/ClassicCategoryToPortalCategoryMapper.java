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

import io.gravitee.apim.core.portal_category.model.CreatePortalCategory;
import io.gravitee.repository.management.model.Category;

/**
 * Maps a Classic portal {@link Category} to a Portal Next {@link CreatePortalCategory} for
 * {@link ClassicCategoriesMigrationUpgrader}. Picture and documentation page link have no Portal
 * Next equivalent and are dropped; migrated categories always start visible, regardless of the
 * Classic category's hidden flag.
 *
 * @author GraviteeSource Team
 */
public final class ClassicCategoryToPortalCategoryMapper {

    private ClassicCategoryToPortalCategoryMapper() {}

    public static CreatePortalCategory toCreatePortalCategory(Category classicCategory) {
        return CreatePortalCategory.builder()
            .title(classicCategory.getName())
            .description(classicCategory.getDescription())
            .visible(true)
            .build();
    }
}
