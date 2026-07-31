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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TypeConsistencyRuleTest {

    private final TypeConsistencyRule rule = new TypeConsistencyRule();

    @Test
    void should_accept_api_product_update_type() {
        var existing = PortalNavigationItemFixtures.anApiProduct();
        var update = UpdatePortalNavigationItem.builder().type(PortalNavigationItemType.API_PRODUCT).build();

        assertThatCode(() -> rule.validate(update, existing, UpdateValidationContext.empty())).doesNotThrowAnyException();
    }

    @Test
    void should_reject_api_product_type_change() {
        var existing = PortalNavigationItemFixtures.anApiProduct();
        var update = UpdatePortalNavigationItem.builder().type(PortalNavigationItemType.FOLDER).build();

        assertThatThrownBy(() -> rule.validate(update, existing, UpdateValidationContext.empty()))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessage("Navigation item type cannot be changed or is mismatched (expected API_PRODUCT, got FOLDER).");
    }
}
