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
import static fixtures.core.model.PortalNavigationItemFixtures.API2_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.APIS_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.API_PRODUCT_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.CATEGORY1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.LINK1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdatePortalNavigationItemUseCaseTest {

    private UpdatePortalNavigationItemUseCase useCase;
    private PortalNavigationItemsCrudServiceInMemory crudService;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalNavigationItemValidatorService validatorService;
    private PortalNavigationItemDomainService domainService;
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private PortalPageContentCrudServiceInMemory pageContentCrudService;
    private PortalNavigationItemSourceDomainServiceInMemory sourceDomainService;

    @BeforeEach
    void setUp() {
        final var storage = new ArrayList<PortalNavigationItem>();
        crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);

        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        PortalPageContentQueryServiceInMemory pageContentQueryService = PortalPageContentQueryServiceInMemory.sharing(
            pageContentCrudService.storage()
        );

        // A single instance shared by the three collaborators, so that a test can observe what the
        // validation was given and in which order masking and merging happened.
        sourceDomainService = new PortalNavigationItemSourceDomainServiceInMemory();

        validatorService = new PortalNavigationItemValidatorService(
            queryService,
            pageContentQueryService,
            apiProductQueryService,
            sourceDomainService
        );
        domainService = new PortalNavigationItemDomainService(
            crudService,
            queryService,
            pageContentCrudService,
            PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
            apiCrudService,
            sourceDomainService
        );
        useCase = new UpdatePortalNavigationItemUseCase(queryService, validatorService, domainService, sourceDomainService);

        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
    }

    @Test
    void should_move_api_below_product_when_api_belongs_to_product() {
        var productReferenceId = "00000000-0000-0000-0000-000000000019";
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id(productReferenceId).environmentId(ENV_ID).apiIds(Set.of("api-1")).build())
        );
        var product = PortalNavigationItemFixtures.anApiProduct(
            API_PRODUCT_ID,
            "Product",
            PortalNavigationItemId.of(APIS_ID),
            productReferenceId
        );
        queryService.storage().add(product);
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID));
        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.API)
            .title(existing.getTitle())
            .order(0)
            .parentId(product.getId())
            .published(existing.getPublished())
            .visibility(existing.getVisibility())
            .build();

        var output = useCase.execute(
            UpdatePortalNavigationItemUseCase.Input.builder()
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .navigationItemId(existing.getId().toString())
                .updatePortalNavigationItem(toUpdate)
                .build()
        );

        assertThat(output.updatedItem().getParentId()).isEqualTo(product.getId());
    }

    @Test
    void should_reject_moving_api_below_product_when_api_does_not_belong_to_product() {
        var productReferenceId = "00000000-0000-0000-0000-000000000019";
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id(productReferenceId).environmentId(ENV_ID).apiIds(Set.of("other-api")).build())
        );
        var product = PortalNavigationItemFixtures.anApiProduct(
            API_PRODUCT_ID,
            "Product",
            PortalNavigationItemId.of(APIS_ID),
            productReferenceId
        );
        queryService.storage().add(product);
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID));
        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.API)
            .title(existing.getTitle())
            .order(0)
            .parentId(product.getId())
            .published(existing.getPublished())
            .visibility(existing.getVisibility())
            .build();

        assertThrows(InvalidPortalNavigationItemDataException.class, () ->
            useCase.execute(
                UpdatePortalNavigationItemUseCase.Input.builder()
                    .organizationId(ORG_ID)
                    .environmentId(ENV_ID)
                    .navigationItemId(existing.getId().toString())
                    .updatePortalNavigationItem(toUpdate)
                    .build()
            )
        );
    }

    @Test
    void should_move_api_product_between_regular_folders() {
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            "00000000-0000-0000-0000-000000000203",
            "Product",
            PortalNavigationItemId.of(APIS_ID),
            "00000000-0000-0000-0000-000000000204"
        );
        queryService.storage().add(apiProduct);
        var targetParentId = PortalNavigationItemId.of(CATEGORY1_ID);

        var output = useCase.execute(updateApiProductInput(apiProduct, targetParentId));

        assertThat(output.updatedItem().getParentId()).isEqualTo(targetParentId);
    }

    @Test
    void should_reject_moving_api_product_to_root() {
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            "00000000-0000-0000-0000-000000000205",
            "Product",
            PortalNavigationItemId.of(APIS_ID),
            "00000000-0000-0000-0000-000000000206"
        );
        queryService.storage().add(apiProduct);

        var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () ->
            useCase.execute(updateApiProductInput(apiProduct, null))
        );

        assertThat(exception.getMessage()).isEqualTo("The parentId field is required and cannot be blank.");
    }

    @Test
    void should_reject_moving_api_product_into_another_api_product_subtree() {
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            "00000000-0000-0000-0000-000000000207",
            "Product",
            PortalNavigationItemId.of(APIS_ID),
            "00000000-0000-0000-0000-000000000208"
        );
        var parentApiProduct = PortalNavigationItemFixtures.anApiProduct(
            "00000000-0000-0000-0000-000000000209",
            "Parent product",
            PortalNavigationItemId.of(APIS_ID),
            "00000000-0000-0000-0000-000000000210"
        );
        var nestedFolder = PortalNavigationItemFixtures.aFolder(
            "00000000-0000-0000-0000-000000000211",
            "Nested folder",
            parentApiProduct.getId()
        );
        queryService.storage().addAll(List.of(apiProduct, parentApiProduct, nestedFolder));

        var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () ->
            useCase.execute(updateApiProductInput(apiProduct, nestedFolder.getId()))
        );

        assertThat(exception.getMessage()).isEqualTo("Parent hierarchy cannot include API Product items.");
    }

    @Test
    void should_reject_moving_api_product_into_api_subtree() {
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            "00000000-0000-0000-0000-000000000212",
            "Product",
            PortalNavigationItemId.of(APIS_ID),
            "00000000-0000-0000-0000-000000000213"
        );
        queryService.storage().add(apiProduct);

        var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () ->
            useCase.execute(updateApiProductInput(apiProduct, PortalNavigationItemId.of(API1_FOLDER_ID)))
        );

        assertThat(exception.getMessage()).isEqualTo("Parent hierarchy cannot include API items.");
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

        var toUpdate = UpdatePortalNavigationItem.builder().type(PortalNavigationItemType.LINK).title("New Title").order(-1).build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(existing.getId().toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        // When / Then
        assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

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
    void should_publish_only_selected_folder_when_propagation_is_omitted() {
        var parentFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000010", "Parent")
            .toBuilder()
            .published(false)
            .build();
        var childFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000011", "Child", parentFolder.getId())
            .toBuilder()
            .published(false)
            .build();
        var grandChildPage = PortalNavigationItemFixtures.aPage("20000000-0000-4000-8000-000000000012", "Grand Child", childFolder.getId())
            .toBuilder()
            .published(false)
            .build();
        crudService.initWith(List.of(parentFolder, childFolder, grandChildPage));
        queryService.initWith(List.copyOf(crudService.storage()));

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(parentFolder.getId().toString())
            .updatePortalNavigationItem(updateFolderPublished(parentFolder, true))
            .build();

        var output = useCase.execute(input);

        assertThat(output.updatedItem().getPublished()).isTrue();
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, parentFolder.getId()).getPublished()).isTrue();
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, childFolder.getId()).getPublished()).isFalse();
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, grandChildPage.getId()).getPublished()).isFalse();
    }

    @Test
    void should_publish_folder_descendants_when_propagation_is_enabled() {
        var parentFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000013", "Parent")
            .toBuilder()
            .published(false)
            .build();
        var childFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000014", "Child", parentFolder.getId())
            .toBuilder()
            .published(false)
            .build();
        var grandChildPage = PortalNavigationItemFixtures.aPage("20000000-0000-4000-8000-000000000015", "Grand Child", childFolder.getId())
            .toBuilder()
            .published(false)
            .build();
        crudService.initWith(List.of(parentFolder, childFolder, grandChildPage));
        queryService.initWith(List.copyOf(crudService.storage()));

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(parentFolder.getId().toString())
            .updatePortalNavigationItem(updateFolderPublished(parentFolder, true))
            .propagatePublishToChildren(true)
            .build();

        var output = useCase.execute(input);

        assertThat(output.updatedItem().getPublished()).isTrue();
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, parentFolder.getId()).getPublished()).isTrue();
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, childFolder.getId()).getPublished()).isTrue();
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, grandChildPage.getId()).getPublished()).isTrue();
    }

    @Test
    void should_unpublish_folder_descendants_when_propagation_is_omitted() {
        var parentFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000016", "Parent")
            .toBuilder()
            .published(true)
            .build();
        var childFolder = PortalNavigationItemFixtures.aFolder("20000000-0000-4000-8000-000000000017", "Child", parentFolder.getId())
            .toBuilder()
            .published(true)
            .build();
        var grandChildPage = PortalNavigationItemFixtures.aPage("20000000-0000-4000-8000-000000000018", "Grand Child", childFolder.getId())
            .toBuilder()
            .published(true)
            .build();
        crudService.initWith(List.of(parentFolder, childFolder, grandChildPage));
        queryService.initWith(List.copyOf(crudService.storage()));

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(parentFolder.getId().toString())
            .updatePortalNavigationItem(updateFolderPublished(parentFolder, false))
            .build();

        var output = useCase.execute(input);

        assertThat(output.updatedItem().getPublished()).isFalse();
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, parentFolder.getId()).getPublished()).isFalse();
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, childFolder.getId()).getPublished()).isFalse();
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, grandChildPage.getId()).getPublished()).isFalse();
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

        var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));
        assertThat(exception.getMessage()).isEqualTo("The parentId field is required and cannot be blank.");
    }

    @Test
    void should_add_parent_api_to_page_item() {
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(PAGE11_ID));
        assertThat(existing).isNotNull();

        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.PAGE)
            .title("Title")
            .parentId(PortalNavigationItemId.of(API1_ID))
            .published(existing.getPublished())
            .visibility(existing.getVisibility())
            .build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(existing.getId().toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        var result = useCase.execute(input);
        assertThat(result).isNotNull();
        assertThat(result.updatedItem()).isNotNull();
        assertThat(result.updatedItem().getParentId()).isEqualTo(PortalNavigationItemId.of(API1_ID));

        var updated = queryService.findByIdAndEnvironmentId(ENV_ID, existing.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getParentId()).isEqualTo(PortalNavigationItemId.of(API1_ID));
    }

    @Test
    void should_add_parent_api_to_folder_item() {
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_FOLDER_ID));
        assertThat(existing).isNotNull();

        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.FOLDER)
            .title("Title")
            .parentId(PortalNavigationItemId.of(API1_ID))
            .published(existing.getPublished())
            .visibility(existing.getVisibility())
            .build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(existing.getId().toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        var result = useCase.execute(input);
        assertThat(result).isNotNull();
        assertThat(result.updatedItem()).isNotNull();
        assertThat(result.updatedItem().getParentId()).isEqualTo(PortalNavigationItemId.of(API1_ID));

        var updated = queryService.findByIdAndEnvironmentId(ENV_ID, existing.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getParentId()).isEqualTo(PortalNavigationItemId.of(API1_ID));
    }

    @Test
    void should_add_parent_api_to_link_item() {
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(LINK1_ID));
        assertThat(existing).isNotNull();

        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.LINK)
            .title("Title")
            .parentId(PortalNavigationItemId.of(API1_ID))
            .published(existing.getPublished())
            .visibility(existing.getVisibility())
            .url("https://gravitee.io")
            .build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(existing.getId().toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        var result = useCase.execute(input);
        assertThat(result).isNotNull();
        assertThat(result.updatedItem()).isNotNull();
        assertThat(result.updatedItem().getParentId()).isEqualTo(PortalNavigationItemId.of(API1_ID));
        // LINK1 was a root item (rootId = LINK1_ID), moving under API1 (rootId = APIS_ID) changes rootId
        assertThat(result.updatedItem().getRootId()).isEqualTo(PortalNavigationItemId.of(APIS_ID));

        var updated = queryService.findByIdAndEnvironmentId(ENV_ID, existing.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getParentId()).isEqualTo(PortalNavigationItemId.of(API1_ID));
        assertThat(updated.getRootId()).isEqualTo(PortalNavigationItemId.of(APIS_ID));
    }

    @Nested
    class ParentChange {

        @Test
        void should_reject_moving_container_below_its_descendant() {
            var parentFolder = PortalNavigationItemFixtures.aFolder("00000000-0000-0000-0000-000000000214", "Parent folder");
            var childFolder = PortalNavigationItemFixtures.aFolder(
                "00000000-0000-0000-0000-000000000215",
                "Child folder",
                parentFolder.getId()
            );
            crudService.initWith(List.of(parentFolder, childFolder));
            queryService.initWith(List.copyOf(crudService.storage()));
            var toUpdate = UpdatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title(parentFolder.getTitle())
                .parentId(childFolder.getId())
                .published(parentFolder.getPublished())
                .visibility(parentFolder.getVisibility())
                .build();
            var input = UpdatePortalNavigationItemUseCase.Input.builder()
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .navigationItemId(parentFolder.getId().toString())
                .updatePortalNavigationItem(toUpdate)
                .build();

            var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

            assertThat(exception.getMessage()).isEqualTo("Cyclic dependency detected in parent hierarchy.");
        }

        @Test
        void should_update_rootId_and_propagate_to_children_when_non_root_item_moves_to_different_non_root_parent() {
            // Given — sampleNavigationItems: APIS (root) → CATEGORY1 (child, rootId=APIS_ID, has children incl. PAGE11)
            // Find the second root folder (guides) which has a different rootId
            var targetParent = queryService
                .findTopLevelItemsByEnvironmentIdAndPortalArea(ENV_ID, PortalArea.TOP_NAVBAR)
                .stream()
                .filter(item -> item instanceof PortalNavigationFolder && !item.getId().equals(PortalNavigationItemId.of(APIS_ID)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected a second root folder in sample data"));

            var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(CATEGORY1_ID));

            var toUpdate = UpdatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title(existing.getTitle())
                .parentId(targetParent.getId()) // move CATEGORY1 under the other root folder
                .published(existing.getPublished())
                .visibility(existing.getVisibility())
                .build();

            var input = UpdatePortalNavigationItemUseCase.Input.builder()
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .navigationItemId(CATEGORY1_ID)
                .updatePortalNavigationItem(toUpdate)
                .build();

            // When
            var result = useCase.execute(input);

            // Then — CATEGORY1's rootId changes to the target root folder's rootId
            assertThat(result.updatedItem().getRootId()).isEqualTo(targetParent.getId());

            // And its child PAGE11 has rootId propagated to the same value
            var page11 = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(PAGE11_ID));
            assertThat(page11.getRootId()).isEqualTo(targetParent.getId());
        }

        @Test
        void should_update_rootId_to_self_and_propagate_to_children_when_non_root_item_moves_to_root_level() {
            // Given — CATEGORY1 is a non-root folder (rootId=APIS_ID) with children incl. PAGE11
            var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(CATEGORY1_ID));
            assertThat(existing.getParentId()).isNotNull(); // confirm it's currently non-root

            var toUpdate = UpdatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title(existing.getTitle())
                .parentId(null) // move to root level
                .published(existing.getPublished())
                .visibility(existing.getVisibility())
                .build();

            var input = UpdatePortalNavigationItemUseCase.Input.builder()
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .navigationItemId(CATEGORY1_ID)
                .updatePortalNavigationItem(toUpdate)
                .build();

            // When
            var result = useCase.execute(input);

            // Then — CATEGORY1's rootId equals its own id (it is now a root)
            assertThat(result.updatedItem().getParentId()).isNull();
            assertThat(result.updatedItem().getRootId()).isEqualTo(result.updatedItem().getId());

            // And its child PAGE11 has rootId propagated to CATEGORY1's id
            var page11 = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(PAGE11_ID));
            assertThat(page11.getRootId()).isEqualTo(result.updatedItem().getId());
        }

        @Test
        void should_update_rootId_when_root_item_moves_to_non_root_parent() {
            // Given — find the second root folder (guides, rootId=guides.id) to move under CATEGORY1 (rootId=APIS_ID)
            var rootItem = queryService
                .findTopLevelItemsByEnvironmentIdAndPortalArea(ENV_ID, PortalArea.TOP_NAVBAR)
                .stream()
                .filter(item -> item instanceof PortalNavigationFolder && !item.getId().equals(PortalNavigationItemId.of(APIS_ID)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected a second root folder in sample data"));

            assertThat(rootItem.getParentId()).isNull(); // confirm it's currently a root

            var toUpdate = UpdatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title(rootItem.getTitle())
                .parentId(PortalNavigationItemId.of(CATEGORY1_ID)) // move under non-root folder
                .published(rootItem.getPublished())
                .visibility(rootItem.getVisibility())
                .build();

            var input = UpdatePortalNavigationItemUseCase.Input.builder()
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .navigationItemId(rootItem.getId().toString())
                .updatePortalNavigationItem(toUpdate)
                .build();

            // When
            var result = useCase.execute(input);

            // Then — former root item now has rootId = APIS_ID (inherited from CATEGORY1's rootId)
            assertThat(result.updatedItem().getParentId()).isEqualTo(PortalNavigationItemId.of(CATEGORY1_ID));
            assertThat(result.updatedItem().getRootId()).isEqualTo(PortalNavigationItemId.of(APIS_ID));
        }
    }

    @Test
    void should_not_add_api_parent_to_api_item() {
        var existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID));
        assertThat(existing).isNotNull();

        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.API)
            .title("Title")
            .parentId(PortalNavigationItemId.of(API2_ID)) // parent cannot be API
            .published(existing.getPublished())
            .visibility(existing.getVisibility())
            .build();

        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(existing.getId().toString())
            .updatePortalNavigationItem(toUpdate)
            .build();

        var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));
        assertThat(exception.getMessage()).isEqualTo("Parent hierarchy cannot include API items.");
    }

    private UpdatePortalNavigationItem updateFolderPublished(PortalNavigationFolder folder, boolean published) {
        return UpdatePortalNavigationItem.builder()
            .type(folder.getType())
            .title(folder.getTitle())
            .order(folder.getOrder())
            .parentId(folder.getParentId())
            .published(published)
            .visibility(folder.getVisibility())
            .build();
    }

    private UpdatePortalNavigationItemUseCase.Input updateApiProductInput(
        PortalNavigationApiProduct apiProduct,
        PortalNavigationItemId parentId
    ) {
        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.API_PRODUCT)
            .title(apiProduct.getTitle())
            .order(apiProduct.getOrder())
            .parentId(parentId)
            .published(apiProduct.getPublished())
            .visibility(apiProduct.getVisibility())
            .build();
        return UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(apiProduct.getId().toString())
            .updatePortalNavigationItem(toUpdate)
            .build();
    }

    @Nested
    class SourcedItems {

        private static final String SOURCED_FOLDER_ID = "00000000-0000-0000-0000-00000000f001";
        private static final String CHILD_PAGE_ID = "00000000-0000-0000-0000-00000000f002";
        private static final String SOURCED_PAGE_ID = "00000000-0000-0000-0000-00000000f003";

        private PortalNavigationItemSource aSource() {
            return PortalNavigationItemSource.builder()
                .sourceType("http-fetcher")
                .sourceConfiguration("{\"url\":\"https://example.com/doc.md\"}")
                .build();
        }

        private PortalNavigationItemSource aSourceWithSecret(String token) {
            return PortalNavigationItemSource.builder()
                .sourceType("http-fetcher")
                .sourceConfiguration("{\"token\":\"" + token + "\"}")
                .build();
        }

        /** What the client sends back after a read: the secret replaced by its placeholder. */
        private PortalNavigationItemSource aMaskedSource() {
            return aSourceWithSecret(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA_REPLACEMENT);
        }

        private PortalNavigationItem givenASourcedPageWithSecret() {
            var page = PortalNavigationItemFixtures.aPage(SOURCED_PAGE_ID, "Sourced Page", null)
                .toBuilder()
                .source(aSourceWithSecret(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA))
                .build();
            page.markAsRoot();
            queryService.storage().add(page);
            return page;
        }

        private PortalNavigationItem givenASourcedPage() {
            var page = PortalNavigationItemFixtures.aPage(SOURCED_PAGE_ID, "Sourced Page", null).toBuilder().source(aSource()).build();
            page.markAsRoot();
            queryService.storage().add(page);
            return page;
        }

        private PortalNavigationItem givenAChildOfSourcedFolder() {
            var folder = PortalNavigationItemFixtures.aFolder(SOURCED_FOLDER_ID, "Sourced Folder").toBuilder().source(aSource()).build();
            folder.markAsRoot();
            var child = PortalNavigationItemFixtures.aPage(CHILD_PAGE_ID, "Child Page", folder.getId());
            queryService.storage().add(folder);
            queryService.storage().add(child);
            return child;
        }

        private UpdatePortalNavigationItemUseCase.Input anUpdateInput(PortalNavigationItem item, UpdatePortalNavigationItem toUpdate) {
            return UpdatePortalNavigationItemUseCase.Input.builder()
                .organizationId(ORG_ID)
                .environmentId(ENV_ID)
                .navigationItemId(item.getId().toString())
                .updatePortalNavigationItem(toUpdate)
                .build();
        }

        private UpdatePortalNavigationItem.UpdatePortalNavigationItemBuilder anUpdateKeeping(PortalNavigationItem item) {
            return UpdatePortalNavigationItem.builder()
                .type(item.getType())
                .title(item.getTitle())
                .order(item.getOrder())
                .parentId(item.getParentId())
                .published(item.getPublished())
                .visibility(item.getVisibility());
        }

        @Test
        void should_reject_rename_of_sourced_item() {
            var page = givenASourcedPage();
            var input = anUpdateInput(page, anUpdateKeeping(page).title("Renamed").source(aSource()).build());

            var error = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

            assertThat(error).hasMessageContaining("cannot be renamed or moved");
        }

        @Test
        void should_reject_move_of_sourced_item() {
            var page = givenASourcedPage();
            var input = anUpdateInput(
                page,
                anUpdateKeeping(page).parentId(PortalNavigationItemId.of(CATEGORY1_ID)).source(aSource()).build()
            );

            var error = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

            assertThat(error).hasMessageContaining("cannot be renamed or moved");
        }

        @Test
        void should_allow_rename_when_source_is_removed_in_same_update() {
            var page = givenASourcedPage();
            var input = anUpdateInput(page, anUpdateKeeping(page).title("Renamed").segment("renamed").build());

            var output = useCase.execute(input);

            assertThat(output.updatedItem().getTitle()).isEqualTo("Renamed");
            assertThat(output.updatedItem().getSource()).isNull();
        }

        @Test
        void should_restore_the_masked_secret_before_the_configuration_is_validated() {
            var page = givenASourcedPageWithSecret();
            var input = anUpdateInput(page, anUpdateKeeping(page).source(aMaskedSource()).build());

            useCase.execute(input);

            assertThat(sourceDomainService.lastValidatedConfiguration())
                .contains(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA)
                .doesNotContain(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA_REPLACEMENT);
        }

        @Test
        void should_mask_the_secret_again_in_the_response() {
            var page = givenASourcedPageWithSecret();
            var input = anUpdateInput(page, anUpdateKeeping(page).source(aMaskedSource()).build());

            var output = useCase.execute(input);

            assertThat(output.updatedItem().getSource().getSourceConfiguration())
                .contains(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA_REPLACEMENT)
                .doesNotContain(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA);
        }

        @Test
        void should_reject_moving_an_item_below_a_sourced_folder() {
            var folder = PortalNavigationItemFixtures.aFolder(SOURCED_FOLDER_ID, "Sourced Folder").toBuilder().source(aSource()).build();
            folder.markAsRoot();
            var rootPage = PortalNavigationItemFixtures.aPage(CHILD_PAGE_ID, "Root Page", null);
            rootPage.markAsRoot();
            queryService.storage().addAll(List.of(folder, rootPage));

            var input = anUpdateInput(rootPage, anUpdateKeeping(rootPage).parentId(folder.getId()).build());

            var error = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

            assertThat(error).hasMessageContaining("cannot be moved below");
        }

        @Test
        void should_allow_moving_an_item_below_a_folder_without_source() {
            var folder = PortalNavigationItemFixtures.aFolder(SOURCED_FOLDER_ID, "Plain Folder");
            folder.markAsRoot();
            var rootPage = PortalNavigationItemFixtures.aPage(CHILD_PAGE_ID, "Root Page", null);
            rootPage.markAsRoot();
            queryService.storage().addAll(List.of(folder, rootPage));

            var input = anUpdateInput(rootPage, anUpdateKeeping(rootPage).parentId(folder.getId()).build());

            var output = useCase.execute(input);

            assertThat(output.updatedItem().getParentId()).isEqualTo(folder.getId());
        }

        @Test
        void should_reject_update_of_child_of_sourced_folder() {
            var child = givenAChildOfSourcedFolder();
            var input = anUpdateInput(child, anUpdateKeeping(child).title("Renamed child").build());

            var error = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

            assertThat(error).hasMessageContaining("read-only");
        }

        private GraviteeMarkdownPageContent givenAnAutomationManagedContent() {
            var content = new GraviteeMarkdownPageContent(
                PortalPageContentId.random(),
                ORG_ID,
                ENV_ID,
                GraviteeMarkdown.of("# automation content"),
                new AutomationMetadata(AutomationMetadata.ReferenceType.PORTAL, "portal-id", "page", Optional.empty(), Optional.empty())
            );
            pageContentCrudService.create(content);
            return content;
        }

        @Test
        void should_reject_adding_source_on_automation_managed_page() {
            var page = PortalNavigationItemFixtures.aPage(
                SOURCED_PAGE_ID,
                "Automation Page",
                null,
                givenAnAutomationManagedContent().getId()
            );
            page.markAsRoot();
            queryService.storage().add(page);

            var input = anUpdateInput(page, anUpdateKeeping(page).source(aSource()).build());

            var error = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

            assertThat(error).hasMessageContaining("Automation API");
        }

        @Test
        void should_reject_adding_source_on_a_folder_whose_subtree_contains_an_automation_managed_page() {
            var folder = PortalNavigationItemFixtures.aFolder(SOURCED_FOLDER_ID, "Folder");
            folder.markAsRoot();
            var intermediate = PortalNavigationItemFixtures.aFolder("00000000-0000-0000-0000-00000000f010", "Intermediate", folder.getId());
            var automationPage = PortalNavigationItemFixtures.aPage(
                "00000000-0000-0000-0000-00000000f011",
                "Automation Page",
                intermediate.getId(),
                givenAnAutomationManagedContent().getId()
            );
            queryService.storage().addAll(List.of(folder, intermediate, automationPage));

            var input = anUpdateInput(folder, anUpdateKeeping(folder).source(aSource()).build());

            var error = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

            assertThat(error).hasMessageContaining("Automation API");
        }

        @Test
        void should_accept_adding_source_when_the_automation_managed_page_is_outside_the_subtree() {
            var folder = PortalNavigationItemFixtures.aFolder(SOURCED_FOLDER_ID, "Folder");
            folder.markAsRoot();
            var sibling = PortalNavigationItemFixtures.aPage(
                "00000000-0000-0000-0000-00000000f012",
                "Automation Page",
                null,
                givenAnAutomationManagedContent().getId()
            );
            sibling.markAsRoot();
            queryService.storage().addAll(List.of(folder, sibling));

            var input = anUpdateInput(folder, anUpdateKeeping(folder).source(aSource()).build());

            assertDoesNotThrow(() -> useCase.execute(input));
        }

        @Test
        void should_reject_invalid_cron_expression_on_update() {
            var page = givenASourcedPage();
            var invalidSource = PortalNavigationItemSource.builder()
                .sourceType("http-fetcher")
                .sourceConfiguration("{}")
                .useAutoFetch(true)
                .fetchCron("not-a-cron")
                .build();
            var input = anUpdateInput(page, anUpdateKeeping(page).source(invalidSource).build());

            var error = assertThrows(io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemSourceException.class, () ->
                useCase.execute(input)
            );

            assertThat(error).hasMessageContaining("not-a-cron");
        }
    }
}
