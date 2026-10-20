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
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference.ApiReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OwnerChangeRuleTest {

    private static final ApiReference API = new ApiReference("api-a");

    private final OwnerChangeRule rule = new OwnerChangeRule();

    private final PortalNavigationItem section = PortalNavigationItemFixtures.aFolder("Section");
    private final PortalNavigationItem page = PortalNavigationItemFixtures.aPage("Guide", null).toBuilder().reference(API).build();

    @Test
    void rejects_leaving_an_api_that_has_two_standalone_listings() {
        var ctx = contextOf(section, page, aListingUnder(section), aListingUnder(section));

        assertThatThrownBy(() -> rule.validate(leavingForThePortal(), page, ctx))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("api-a")
            .hasMessageContaining("listed in more than one place");
    }

    @Test
    void accepts_leaving_an_api_whose_second_listing_is_inside_an_api_product() {
        var product = PortalNavigationItemFixtures.anApiProduct(
            PortalNavigationItemId.random().json(),
            "Product",
            section.getId(),
            "product-ref"
        );
        var productFolder = PortalNavigationItemFixtures.aFolder("Product APIs", product.getId());
        var ctx = contextOf(section, page, product, productFolder, aListingUnder(section), aListingUnder(productFolder));

        assertThatCode(() -> rule.validate(leavingForThePortal(), page, ctx)).doesNotThrowAnyException();
    }

    @Test
    void does_not_count_the_listings_of_another_api() {
        var listingOfAnotherApi = PortalNavigationItemFixtures.anApi(
            PortalNavigationItemId.random().json(),
            "Other API",
            section.getId(),
            "api-b"
        );
        var ctx = contextOf(section, page, aListingUnder(section), listingOfAnotherApi);

        assertThatCode(() -> rule.validate(leavingForThePortal(), page, ctx)).doesNotThrowAnyException();
    }

    private static PortalNavigationItem aListingUnder(PortalNavigationItem parent) {
        return PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "Listing", parent.getId(), API.apiId());
    }

    private UpdatePortalNavigationItem leavingForThePortal() {
        return UpdatePortalNavigationItem.builder()
            .type(page.getType())
            .title(page.getTitle())
            .parentId(section.getId())
            .reference(NavigationItemReference.defaultReference())
            .build();
    }

    private static UpdateValidationContext contextOf(PortalNavigationItem... items) {
        var all = List.of(items);
        return new UpdateValidationContext(
            all,
            all.stream().collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity())),
            Map.of(),
            Map.of(),
            List.of()
        );
    }
}
