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
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BulkCreatePortalNavigationItemsUseCaseTest {

    private BulkCreatePortalNavigationItemUseCase useCase;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalNavigationItemValidatorService validatorService;

    @BeforeEach
    void setUp() {
        final var storage = new ArrayList<PortalNavigationItem>();

        final var crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        final var pageContentQueryService = new PortalPageContentQueryServiceInMemory();
        validatorService = new PortalNavigationItemValidatorService(queryService, pageContentQueryService);
        final var pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        final var apiCrudService = new ApiCrudServiceInMemory();

        final var domainService = new PortalNavigationItemDomainService(crudService, queryService, pageContentCrudService, apiCrudService);
        useCase = new BulkCreatePortalNavigationItemUseCase(domainService, validatorService);
        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
    }

    @Test
    void should_create_list_of_portal_navigation_items_if_validation_succeeds() {
        // Given
        final var firstItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.FOLDER)
            .title("Folder 1")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .build();
        firstItem.setParentId(PortalNavigationItemId.of(APIS_ID));

        final var secondItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.FOLDER)
            .title("Folder 2")
            .area(PortalArea.TOP_NAVBAR)
            .order(1)
            .build();
        secondItem.setParentId(PortalNavigationItemId.of(APIS_ID));

        // When
        final var output = useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(firstItem, secondItem)));

        // Then
        assertThat(output.items()).hasSize(2);
        assertThat(output.items()).extracting(PortalNavigationItem::getId).containsExactly(firstItem.getId(), secondItem.getId());

        final var items = queryService.findByParentIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(APIS_ID));
        final var createdFirstItem = items
            .stream()
            .filter(item -> item.getId().equals(firstItem.getId()))
            .findFirst();
        final var createdSecondItem = items
            .stream()
            .filter(item -> item.getId().equals(secondItem.getId()))
            .findFirst();

        assertThat(createdFirstItem).isPresent();
        assertThat(createdFirstItem.get()).satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo(firstItem.getTitle());
            assertThat(item.getArea()).isEqualTo(firstItem.getArea());
            assertThat(item.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
            assertThat(item.getPublished()).isFalse();
        });

        assertThat(createdSecondItem).isPresent();
        assertThat(createdSecondItem.get()).satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo(secondItem.getTitle());
            assertThat(item.getArea()).isEqualTo(secondItem.getArea());
            assertThat(item.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
            assertThat(item.getPublished()).isFalse();
        });
    }

    @Test
    void should_fail_when_at_least_one_item_is_invalid_during_bulk_validation() {
        // Given
        final var domainService = mock(PortalNavigationItemDomainService.class);
        final var useCase = new BulkCreatePortalNavigationItemUseCase(domainService, validatorService);

        final var validItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.FOLDER)
            .title("Folder 1")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(PortalNavigationItemId.of(APIS_ID))
            .build();

        final var invalidApiItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.API)
            .title("API without parent")
            .area(PortalArea.TOP_NAVBAR)
            .order(1)
            .apiId("api-2")
            .build();

        // When
        final var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () ->
            useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(validItem, invalidApiItem)))
        );

        // Then
        assertThat(exception.getMessage()).isEqualTo("The parentId field is required and cannot be blank.");
        verify(domainService, never()).create(anyString(), anyString(), any(CreatePortalNavigationItem.class));
    }
}
