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

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemHasChildrenException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class DeletePortalNavigationItemUseCaseTest {

    private final PortalNavigationItemsCrudServiceInMemory portalNavigationItemsCrudService =
        new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService =
        new PortalNavigationItemsQueryServiceInMemory(portalNavigationItemsCrudService.storage());
    private final PortalPageContentCrudServiceInMemory portalPageContentCrudService = new PortalPageContentCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private DeletePortalNavigationItemUseCase deletePortalNavigationItemUseCase;

    @BeforeEach
    void setUp() {
        var domainService = new PortalNavigationItemDomainService(
            portalNavigationItemsCrudService,
            portalNavigationItemsQueryService,
            portalPageContentCrudService,
            apiCrudService
        );

        deletePortalNavigationItemUseCase = new DeletePortalNavigationItemUseCase(domainService, portalNavigationItemsQueryService);
    }

    @AfterEach
    void afterEach() {
        portalNavigationItemsCrudService.reset();
        portalNavigationItemsQueryService.reset();
    }

    @Test
    void should_delete() {
        // Given
        var toDelete = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "page11", null);
        portalNavigationItemsCrudService.initWith(List.of(toDelete));
        portalNavigationItemsQueryService.initWith(List.of(toDelete));

        // When
        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                toDelete.getId()
            )
        );

        // Then
        assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
    }

    @Test
    void should_reorder_when_delete() {
        // Given
        PortalNavigationPage page1 = PortalNavigationItemFixtures.aPage("p1", null).toBuilder().order(1).build();
        PortalNavigationPage page2 = PortalNavigationItemFixtures.aPage("p2", null).toBuilder().order(2).build();
        PortalNavigationPage page3 = PortalNavigationItemFixtures.aPage("p3", null).toBuilder().order(3).build();
        PortalNavigationPage page4 = PortalNavigationItemFixtures.aPage("p4", null).toBuilder().order(4).build();
        PortalNavigationPage page5 = PortalNavigationItemFixtures.aPage("p5", null).toBuilder().order(5).build();
        portalNavigationItemsCrudService.initWith(List.of(page1, page2, page3, page4, page5));
        portalNavigationItemsQueryService.initWith(List.copyOf(portalNavigationItemsCrudService.storage()));

        // When
        // perform delete
        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                page2.getId()
            )
        );

        // Then
        assertThat(portalNavigationItemsCrudService.storage()).hasSize(4);
        var expected = Map.of(page1.getId(), 1, page3.getId(), 2, page4.getId(), 3, page5.getId(), 4);
        assertThat(
            portalNavigationItemsCrudService
                .storage()
                .stream()
                .collect(Collectors.toMap(PortalNavigationItem::getId, PortalNavigationItem::getOrder))
        )
            .containsOnlyKeys(expected.keySet())
            .containsAllEntriesOf(expected);
    }

    @Test
    void should_throw_not_found_exception_when_item_to_delete_does_not_exist() {
        // Given
        var toKeep = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "page11", null);
        portalNavigationItemsCrudService.initWith(List.of(toKeep));
        portalNavigationItemsQueryService.initWith(List.of(toKeep));

        // When
        var throwable = Assertions.catchThrowable(() ->
            deletePortalNavigationItemUseCase.execute(
                new DeletePortalNavigationItemUseCase.Input(
                    PortalNavigationItemFixtures.ORG_ID,
                    PortalNavigationItemFixtures.ENV_ID,
                    PortalNavigationItemId.of(PortalNavigationItemFixtures.PAGE12_ID)
                )
            )
        );

        // Then
        Assertions.assertThat(throwable)
            .isInstanceOf(PortalNavigationItemNotFoundException.class)
            .hasMessage("Portal navigation item not found")
            .extracting(ex -> ((PortalNavigationItemNotFoundException) ex).getId())
            .isEqualTo(PortalNavigationItemFixtures.PAGE12_ID);
    }

    @Test
    void should_delete_page_with_content() {
        // Given
        var pageContent = portalPageContentCrudService.createDefault(
            PortalNavigationItemFixtures.ORG_ID,
            PortalNavigationItemFixtures.ENV_ID
        );
        var toDelete = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE11_ID, "page11", null)
            .toBuilder()
            .portalPageContentId(pageContent.getId())
            .build();
        portalNavigationItemsCrudService.initWith(List.of(toDelete));
        portalNavigationItemsQueryService.initWith(List.of(toDelete));
        portalPageContentCrudService.initWith(List.of(pageContent));

        // When
        deletePortalNavigationItemUseCase.execute(
            new DeletePortalNavigationItemUseCase.Input(
                PortalNavigationItemFixtures.ORG_ID,
                PortalNavigationItemFixtures.ENV_ID,
                toDelete.getId()
            )
        );

        // Then
        assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_throw_when_parent() {
        // Given
        var parent = PortalNavigationItemFixtures.aFolder("Parent");
        var child1 = PortalNavigationItemFixtures.aPage("Child 1", parent.getId());
        var child2 = PortalNavigationItemFixtures.aFolder("Child 2", parent.getId());
        var grandChild = PortalNavigationItemFixtures.aPage("Grand Child", child2.getId());
        portalNavigationItemsCrudService.initWith(List.of(parent, child1, child2, grandChild));
        portalNavigationItemsQueryService.initWith(List.of(parent, child1, child2, grandChild));

        // When
        var throwable = Assertions.catchThrowable(() ->
            deletePortalNavigationItemUseCase.execute(
                new DeletePortalNavigationItemUseCase.Input(
                    PortalNavigationItemFixtures.ORG_ID,
                    PortalNavigationItemFixtures.ENV_ID,
                    parent.getId()
                )
            )
        );

        // Then
        Assertions.assertThat(throwable).isInstanceOf(PortalNavigationItemHasChildrenException.class).hasMessageContaining("has children");
    }
}
