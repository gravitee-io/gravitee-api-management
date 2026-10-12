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
package io.gravitee.apim.core.portal_page.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.portal_page.domain_service.DocumentationDestination.Parent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference.ApiReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DocumentationDestinationTest {

    private static final PortalNavigationItemId PARENT_ID = PortalNavigationItemId.random();
    private static final NavigationItemReference PORTAL = NavigationItemReference.defaultReference();
    private static final ApiReference API = new ApiReference("api-a");

    @Test
    void should_belong_to_the_portal_with_no_parent() {
        assertThat(DocumentationDestination.under(null)).isEqualTo(new DocumentationDestination(null, null, null));
    }

    @Test
    void should_belong_to_the_api_under_its_listing_and_take_no_stored_parent() {
        var listing = new Parent(PARENT_ID, PortalNavigationItemType.API, PORTAL, "api-a", false);

        assertThat(DocumentationDestination.under(listing)).isEqualTo(new DocumentationDestination(API, null, PARENT_ID));
    }

    @Test
    void should_belong_to_the_api_under_a_folder_it_owns() {
        var folder = new Parent(PARENT_ID, PortalNavigationItemType.FOLDER, API, null, false);

        assertThat(DocumentationDestination.under(folder)).isEqualTo(new DocumentationDestination(API, PARENT_ID, null));
    }

    @Test
    void should_belong_to_the_portal_under_a_portal_folder() {
        var folder = new Parent(PARENT_ID, PortalNavigationItemType.FOLDER, PORTAL, null, false);

        assertThat(DocumentationDestination.under(folder)).isEqualTo(new DocumentationDestination(null, PARENT_ID, null));
    }

    @Test
    void should_belong_to_the_portal_under_a_listing_placed_in_an_api_product() {
        var listing = new Parent(PARENT_ID, PortalNavigationItemType.API, PORTAL, "api-a", true);

        assertThat(DocumentationDestination.under(listing)).isEqualTo(new DocumentationDestination(null, PARENT_ID, null));
    }

    @Test
    void should_belong_to_the_portal_under_a_folder_placed_in_an_api_product() {
        var folder = new Parent(PARENT_ID, PortalNavigationItemType.FOLDER, API, null, true);

        assertThat(DocumentationDestination.under(folder)).isEqualTo(new DocumentationDestination(null, PARENT_ID, null));
    }

    @Test
    void should_belong_to_the_portal_under_a_listing_that_names_no_api() {
        var listing = new Parent(PARENT_ID, PortalNavigationItemType.API, PORTAL, null, false);

        assertThat(DocumentationDestination.under(listing)).isEqualTo(new DocumentationDestination(null, PARENT_ID, null));
    }

    @ParameterizedTest
    @EnumSource(value = PortalNavigationItemType.class, names = { "PAGE", "LINK", "API_PRODUCT" })
    void should_not_take_the_owner_of_a_parent_that_is_not_a_folder(PortalNavigationItemType type) {
        var parent = new Parent(PARENT_ID, type, API, null, false);

        assertThat(DocumentationDestination.under(parent)).isEqualTo(new DocumentationDestination(null, PARENT_ID, null));
    }
}
