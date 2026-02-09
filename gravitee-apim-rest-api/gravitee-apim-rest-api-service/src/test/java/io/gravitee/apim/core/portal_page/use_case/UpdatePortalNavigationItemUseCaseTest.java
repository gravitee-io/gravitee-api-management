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

import static fixtures.core.model.PortalNavigationItemFixtures.API1_FOLDER_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.API1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.APIS_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdatePortalNavigationItemUseCaseTest {

    private UpdatePortalNavigationItemUseCase useCase;
    private PortalNavigationItemsCrudServiceInMemory crudService;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalNavigationItemValidatorService validatorService;
    private PortalNavigationItemDomainService domainService;
    private PortalPageContentCrudServiceInMemory pageContentCrudService;
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    @BeforeEach
    void setUp() {
        final var storage = new ArrayList<PortalNavigationItem>();
        crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        validatorService = mock(PortalNavigationItemValidatorService.class);
        domainService = new PortalNavigationItemDomainService(crudService, queryService, pageContentCrudService, apiCrudService);
        useCase = new UpdatePortalNavigationItemUseCase(queryService, validatorService, domainService);

        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
    }

    @Test
    void should_update_title_when_item_exists_and_validation_succeeds() {
        // Given an existing PAGE item
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(PAGE11_ID));
        assertThat(existing).isNotNull();
        var originalId = existing.getId();

        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.PAGE) // must match existing type
            .title("  New Title  ")
            .order(1)
            .published(true)
            .visibility(existing.getVisibility())
            .build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(originalId.toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        // When
        var output = useCase.execute(input);

        // Then: validator called with provided payload and existing entity
        verify(validatorService).validateToUpdate(any(UpdatePortalNavigationItem.class), any(PortalNavigationItem.class));

        // And storage updated with trimmed title
        var updated = queryService.findByIdAndEnvironmentId(ENV_ID, originalId);
        assertThat(updated).isNotNull();
        assertThat(updated.getTitle()).isEqualTo("New Title");

        // And output contains the updated item
        assertThat(output.updatedItem()).isNotNull();
        assertThat(output.updatedItem().getId()).isEqualTo(originalId);
        assertThat(output.updatedItem().getTitle()).isEqualTo("New Title");
        assertThat(output.updatedItem().getPublished()).isTrue();
        assertThat(output.updatedItem().getVisibility()).isEqualTo(existing.getVisibility());
    }

    @Test
    void should_throw_when_item_does_not_exist() {
        // Given
        var nonExistingId = PortalNavigationItemId.random();
        var toUpdate = UpdatePortalNavigationItem.builder().type(PortalNavigationItemType.PAGE).title("Whatever").build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(nonExistingId.toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        // When / Then
        assertThrows(PortalNavigationItemNotFoundException.class, () -> useCase.execute(input));
    }

    @Test
    void should_propagate_validator_exception_and_not_change_storage() {
        // Given existing item
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(PAGE11_ID));
        assertThat(existing).isNotNull();
        var originalTitle = existing.getTitle();

        // Make validator throw
        doThrow(new IllegalArgumentException("Invalid data"))
            .when(validatorService)
            .validateToUpdate(any(UpdatePortalNavigationItem.class), any(PortalNavigationItem.class));

        var toUpdate = UpdatePortalNavigationItem.builder().type(PortalNavigationItemType.PAGE).title("New Title").build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(existing.getId().toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(input));

        // And ensure storage unchanged
        var after = queryService.findByIdAndEnvironmentId(ENV_ID, existing.getId());
        assertThat(after.getTitle()).isEqualTo(originalTitle);
    }

    @Test
    void should_throw_parentId_not_found_when_parent_does_not_exist() {
        // Given existing item
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(PAGE11_ID));
        assertThat(existing).isNotNull();
        var originalTitle = existing.getTitle();

        // Given a non-existing parent ID
        var nonExistingParentId = PortalNavigationItemId.random();
        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.PAGE)
            .title("New Title")
            .parentId(nonExistingParentId)
            .build();

        // Make validator throw ParentNotFoundException
        doThrow(new ParentNotFoundException(nonExistingParentId.toString()))
            .when(validatorService)
            .validateToUpdate(any(UpdatePortalNavigationItem.class), any(PortalNavigationItem.class));

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(existing.getId().toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        // When / Then
        assertThrows(ParentNotFoundException.class, () -> useCase.execute(input));

        // And ensure storage unchanged
        var after = queryService.findByIdAndEnvironmentId(ENV_ID, existing.getId());
        assertThat(after.getTitle()).isEqualTo(originalTitle);
    }

    @Test
    void should_publish_an_unpublished_page() {
        // Given an existing PAGE item
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(PAGE11_ID));
        assertThat(existing).isNotNull();
        var originalId = existing.getId();

        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.PAGE) // must match existing type
            .title("  New Title  ")
            .order(1)
            .published(true)
            .visibility(existing.getVisibility())
            .build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(originalId.toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        // When
        var output = useCase.execute(input);

        // Then: validator called with provided payload and existing entity
        verify(validatorService).validateToUpdate(any(UpdatePortalNavigationItem.class), any(PortalNavigationItem.class));

        // And storage updated with trimmed title
        var updated = queryService.findByIdAndEnvironmentId(ENV_ID, originalId);
        assertThat(updated).isNotNull();
        assertThat(updated.getTitle()).isEqualTo("New Title");

        // And output contains the updated item
        assertThat(output.updatedItem()).isNotNull();
        assertThat(output.updatedItem().getId()).isEqualTo(originalId);
        assertThat(output.updatedItem().getTitle()).isEqualTo("New Title");
        assertThat(output.updatedItem().getPublished()).isTrue();
        assertThat(output.updatedItem().getVisibility()).isEqualTo(existing.getVisibility());
    }

    @Test
    void should_change_visibility_to_private() {
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(PAGE11_ID));
        assertThat(existing).isNotNull();
        var originalId = existing.getId();

        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.PAGE) // must match existing type
            .title(existing.getTitle())
            .order(existing.getOrder())
            .published(existing.getPublished())
            .visibility(PortalVisibility.PRIVATE)
            .build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(originalId.toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        // When
        var output = useCase.execute(input);

        // Then: validator called with provided payload and existing entity
        verify(validatorService).validateToUpdate(any(UpdatePortalNavigationItem.class), any(PortalNavigationItem.class));

        // And storage updated with trimmed title
        var updated = queryService.findByIdAndEnvironmentId(ENV_ID, originalId);
        assertThat(updated).isNotNull();

        // And output contains the updated item
        assertThat(output.updatedItem()).isNotNull();
        assertThat(output.updatedItem().getId()).isEqualTo(originalId);
        assertThat(output.updatedItem().getTitle()).isEqualTo(existing.getTitle());
        assertThat(output.updatedItem().getPublished()).isEqualTo(existing.getPublished());
        assertThat(output.updatedItem().getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
    }

    @Test
    void should_update_order() {
        // Given an existing PAGE item
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(PAGE11_ID));
        assertThat(existing).isNotNull();
        var originalId = existing.getId();

        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.PAGE) // must match existing type
            .title(existing.getTitle())
            .order(2)
            .published(existing.getPublished())
            .visibility(existing.getVisibility())
            .build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(originalId.toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        // When
        var output = useCase.execute(input);

        // Then: validator called with provided payload and existing entity
        verify(validatorService).validateToUpdate(any(UpdatePortalNavigationItem.class), any(PortalNavigationItem.class));

        // And storage updated with new order
        var updated = queryService.findByIdAndEnvironmentId(ENV_ID, originalId);
        assertThat(updated).isNotNull();
        assertThat(updated.getOrder()).isEqualTo(2);

        // And output contains the updated item
        assertThat(output.updatedItem()).isNotNull();
        assertThat(output.updatedItem().getId()).isEqualTo(originalId);
        assertThat(output.updatedItem().getOrder()).isEqualTo(2);
    }

    @Test
    void should_fail_when_api_item_has_null_parent_id() {
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID));
        assertThat(existing).isNotNull();

        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.API)
            .title("Title")
            .parentId(null)
            .published(existing.getPublished())
            .visibility(existing.getVisibility())
            .build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(existing.getId().toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        useCase = buildUseCaseWithRealValidator();

        var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));
        assertThat(exception.getMessage()).isEqualTo("The parentId field is required and cannot be blank.");
    }

    private UpdatePortalNavigationItemUseCase buildUseCaseWithRealValidator() {
        var pageContentQueryService = new PortalPageContentQueryServiceInMemory();
        var realValidator = new PortalNavigationItemValidatorService(queryService, pageContentQueryService);
        return new UpdatePortalNavigationItemUseCase(queryService, realValidator, domainService);
    }
}
