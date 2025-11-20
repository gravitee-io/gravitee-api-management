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
package io.gravitee.apim.core.portal_page.use_case;

import static fixtures.core.model.PortalNavigationItemFixtures.APIS_ID;
import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListPortalNavigationItemsUseCaseTest {

    private static final String ENV_ID = "env-id";

    private ListPortalNavigationItemsUseCase useCase;

    @BeforeEach
    void setUp() {
        PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory();
        useCase = new ListPortalNavigationItemsUseCase(queryService);

        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
    }

    @Test
    void should_return_top_level_navigation_items_when_parent_id_is_null_and_load_children_is_true() {
        // Given
        // Setup data for APIs, Guides, Support

        // When
        var result = useCase.execute(new ListPortalNavigationItemsUseCase.Input(ENV_ID, PortalArea.TOP_NAVBAR, Optional.empty(), true));

        // Then
        assertThat(result.items())
            .hasSize(9)
            .extracting(PortalNavigationItem::getTitle)
            .containsExactly("APIs", "Example Link", "Guides", "Support", "Overview", "Getting Started", "Category1", "page11", "page12");
    }

    @Test
    void should_return_direct_children_when_parent_id_is_apis_and_load_children_is_false() {
        // Given
        // Setup data

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                Optional.of(PortalNavigationItemId.of(APIS_ID)),
                false
            )
        );

        // Then
        assertThat(result.items())
            .hasSize(3)
            .extracting(PortalNavigationItem::getTitle)
            .containsExactly("Overview", "Getting Started", "Category1");
    }

    @Test
    void should_return_direct_children_and_grandchildren_when_parent_id_is_apis_and_load_children_is_true() {
        // Given
        // Setup data

        // When
        var result = useCase.execute(
            new ListPortalNavigationItemsUseCase.Input(ENV_ID, PortalArea.TOP_NAVBAR, Optional.of(PortalNavigationItemId.of(APIS_ID)), true)
        );

        // Then
        assertThat(result.items())
            .hasSize(5)
            .extracting(PortalNavigationItem::getTitle)
            .containsExactly("Overview", "Getting Started", "Category1", "page11", "page12");
    }
}
