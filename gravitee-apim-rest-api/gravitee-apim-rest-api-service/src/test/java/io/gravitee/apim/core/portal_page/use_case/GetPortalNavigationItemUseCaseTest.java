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
import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE12_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalNavigationItemUseCaseTest {

    private static final String ENVIRONMENT_ID = ENV_ID;

    private GetPortalNavigationItemUseCase useCase;

    @BeforeEach
    void setUp() {
        PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory();
        useCase = new GetPortalNavigationItemUseCase(queryService);

        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
    }

    @Test
    void should_return_portal_navigation_item_when_found() {
        // Given
        final var input = new GetPortalNavigationItemUseCase.Input(
            PortalNavigationItemId.of(APIS_ID),
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forPortal(false)
        );

        // When
        final var output = useCase.execute(input);

        // Then
        assertThat(output.portalNavigationItem()).isNotNull();
        assertThat(output.portalNavigationItem().getId()).isEqualTo(PortalNavigationItemId.of(APIS_ID));
        assertThat(output.portalNavigationItem().getTitle()).isEqualTo("APIs");
    }

    @Test
    void should_throw_when_item_not_found() {
        // Given
        final var unknownId = PortalNavigationItemId.of("00000000-0000-0000-0000-000000000099");
        final var input = new GetPortalNavigationItemUseCase.Input(
            unknownId,
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forPortal(false)
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }

    @Test
    void should_throw_when_item_exists_in_different_environment() {
        // Given
        final var input = new GetPortalNavigationItemUseCase.Input(
            PortalNavigationItemId.of(APIS_ID),
            "different-env",
            PortalNavigationItemViewerContext.forPortal(false)
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }

    @Test
    void should_throw_when_item_not_visible_in_portal() {
        // Given
        final var input = new GetPortalNavigationItemUseCase.Input(
            PortalNavigationItemId.of(PAGE11_ID),
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forPortal(true)
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }

    @Test
    void should_throw_when_item_is_private_and_user_is_not_authenticated() {
        // Given
        final var input = new GetPortalNavigationItemUseCase.Input(
            PortalNavigationItemId.of(PAGE12_ID),
            ENVIRONMENT_ID,
            PortalNavigationItemViewerContext.forPortal(false)
        );

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found");
    }
}
