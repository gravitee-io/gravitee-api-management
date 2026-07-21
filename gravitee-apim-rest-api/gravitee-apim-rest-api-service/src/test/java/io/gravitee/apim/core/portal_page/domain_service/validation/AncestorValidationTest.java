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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AncestorValidationTest {

    private static final PortalNavigationItemId FIRST_FOLDER_ID = PortalNavigationItemId.of("00000000-0000-0000-0000-000000000201");
    private static final PortalNavigationItemId SECOND_FOLDER_ID = PortalNavigationItemId.of("00000000-0000-0000-0000-000000000202");

    @Test
    void should_reject_cycle_when_checking_api_ancestors() {
        var context = cyclicContext();

        assertThatThrownBy(() -> ApiAncestorValidation.ensureNoApiInAncestors(FIRST_FOLDER_ID, context))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessage("Cyclic dependency detected in parent hierarchy.");
    }

    @Test
    void should_reject_cycle_when_resolving_api_product_ancestor() {
        var context = cyclicContext();

        assertThatThrownBy(() -> ApiProductAncestorValidation.findApiProductId(FIRST_FOLDER_ID, context))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessage("Cyclic dependency detected in parent hierarchy.");
    }

    private static CreateValidationContext cyclicContext() {
        var firstFolder = folder(FIRST_FOLDER_ID, SECOND_FOLDER_ID);
        var secondFolder = folder(SECOND_FOLDER_ID, FIRST_FOLDER_ID);
        return new CreateValidationContext(
            List.of(),
            Map.of(),
            Map.of(firstFolder.getId(), firstFolder, secondFolder.getId(), secondFolder)
        );
    }

    private static CreatePortalNavigationItem folder(PortalNavigationItemId id, PortalNavigationItemId parentId) {
        return CreatePortalNavigationItem.builder()
            .id(id)
            .type(PortalNavigationItemType.FOLDER)
            .title("Folder")
            .area(PortalArea.TOP_NAVBAR)
            .parentId(parentId)
            .build();
    }
}
