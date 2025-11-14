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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.exception.ParentAreaMismatchException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentTypeMismatchException;
import io.gravitee.apim.core.portal_page.exception.PortalPageSpecificationException;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.ArrayList;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreatePortalNavigationItemUseCaseTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final String APIS_ID = "00000000-0000-0000-0000-000000000001";
    private static final String PAGE11_ID = "00000000-0000-0000-0000-000000000007";
    private static final String NON_EXISTENT_ID = "6c2c004d-c4f2-4a2b-b2c3-857a4dfcc842";

    private CreatePortalNavigationItemUseCase useCase;
    private PortalNavigationItemsQueryServiceInMemory queryService;

    @BeforeEach
    void setUp() {
        final var storage = new ArrayList<PortalNavigationItem>();
        final PortalNavigationItemsCrudServiceInMemory crudService = new PortalNavigationItemsCrudServiceInMemory(storage);

        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        useCase = new CreatePortalNavigationItemUseCase(crudService, queryService);
        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
    }

    @Test
    void should_create_top_level_navigation_item_when_parent_id_is_null() {
        // Given
        final var portalNavigationItem = new PortalNavigationFolder(
            PortalNavigationItemId.random(),
            ORG_ID,
            ENV_ID,
            "title",
            PortalArea.TOP_NAVBAR,
            0
        );

        // When
        useCase.execute(new CreatePortalNavigationItemUseCase.Input(portalNavigationItem));

        // Then
        final var result = queryService.findTopLevelItemsByEnvironmentIdAndPortalArea(
            portalNavigationItem.getEnvironmentId(),
            PortalArea.TOP_NAVBAR
        );
        assertThat(result).extracting(PortalNavigationItem::getId).contains(portalNavigationItem.getId());
    }

    @Test
    void should_create_nested_navigation_item_when_parent_id_is_not_null() {
        // Given
        final var portalNavigationItem = new PortalNavigationFolder(
            PortalNavigationItemId.random(),
            ORG_ID,
            ENV_ID,
            "title",
            PortalArea.TOP_NAVBAR,
            0
        );
        portalNavigationItem.setParentId(PortalNavigationItemId.of(APIS_ID));

        // When
        useCase.execute(new CreatePortalNavigationItemUseCase.Input(portalNavigationItem));

        // Then
        final var result = queryService.findByParentIdAndEnvironmentId(
            portalNavigationItem.getEnvironmentId(),
            PortalNavigationItemId.of(APIS_ID)
        );
        assertThat(result).extracting(PortalNavigationItem::getId).contains(portalNavigationItem.getId());
    }

    @Test
    void should_fail_when_parent_is_not_found() {
        // Given
        final var portalNavigationItem = new PortalNavigationFolder(
            PortalNavigationItemId.random(),
            ORG_ID,
            ENV_ID,
            "title",
            PortalArea.TOP_NAVBAR,
            0
        );
        portalNavigationItem.setParentId(PortalNavigationItemId.of(NON_EXISTENT_ID));

        // When
        final ThrowingRunnable throwing = () -> useCase.execute(new CreatePortalNavigationItemUseCase.Input(portalNavigationItem));

        // Then
        Exception exception = assertThrows(ParentNotFoundException.class, throwing);
        assertThat(exception.getMessage()).isEqualTo("Parent item with id %s does not exist", NON_EXISTENT_ID);
    }

    @Test
    void should_fail_when_parent_is_not_folder() {
        // Given
        final var portalNavigationItem = new PortalNavigationFolder(
            PortalNavigationItemId.random(),
            ORG_ID,
            ENV_ID,
            "title",
            PortalArea.TOP_NAVBAR,
            0
        );
        portalNavigationItem.setParentId(PortalNavigationItemId.of(PAGE11_ID));

        // When
        final ThrowingRunnable throwing = () -> useCase.execute(new CreatePortalNavigationItemUseCase.Input(portalNavigationItem));

        // Then
        Exception exception = assertThrows(ParentTypeMismatchException.class, throwing);
        assertThat(exception.getMessage()).isEqualTo("Parent item with id %s is not a folder", PAGE11_ID);
    }

    @Test
    void should_fail_when_parent_is_in_different_area() {
        // Given
        final var portalNavigationItem = new PortalNavigationFolder(
            PortalNavigationItemId.random(),
            ORG_ID,
            ENV_ID,
            "title",
            PortalArea.HOMEPAGE,
            0
        );
        portalNavigationItem.setParentId(PortalNavigationItemId.of(APIS_ID));

        // When
        final ThrowingRunnable throwing = () -> useCase.execute(new CreatePortalNavigationItemUseCase.Input(portalNavigationItem));

        // Then
        Exception exception = assertThrows(ParentAreaMismatchException.class, throwing);
        assertThat(exception.getMessage()).isEqualTo("Parent item with id %s belongs to a different area than the child item", APIS_ID);
    }
}
