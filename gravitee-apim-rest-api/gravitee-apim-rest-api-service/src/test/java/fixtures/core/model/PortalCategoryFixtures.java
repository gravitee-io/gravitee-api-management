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
package fixtures.core.model;

import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_category.model.UpdatePortalCategory;

public class PortalCategoryFixtures {

    private PortalCategoryFixtures() {}

    public static final PortalCategoryId PORTAL_CATEGORY_ID = PortalCategoryId.of("00000000-0000-0000-0000-0000000000b1");

    public static PortalCategory aPortalCategory() {
        return PortalCategory.of(PORTAL_CATEGORY_ID, "environment-id", "News", "News category", true);
    }

    public static UpdatePortalCategory anUpdatePortalCategory() {
        return UpdatePortalCategory.builder().title("Updated title").description("Updated description").visible(false).build();
    }
}
