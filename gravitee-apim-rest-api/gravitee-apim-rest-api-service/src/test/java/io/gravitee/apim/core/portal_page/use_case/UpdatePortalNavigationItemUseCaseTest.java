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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.ParentAreaMismatchException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference.ApiReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

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
            sourceDomainService,
            new ApiOwnedNavigationDomainService(queryService, crudService)
        );
        useCase = new UpdatePortalNavigationItemUseCase(queryService, validatorService, domainService, sourceDomainService);

        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
    }

    /**
     * The API-scoped endpoint returns stored parents, so its callers send them back as they are, unlike
     * the portal editor, which sends the parent the item is displayed under.
     */
    @Nested
    class ApiOwnedDocumentationSentWithItsStoredParent {

        private static final NavigationItemReference API_REFERENCE = new NavigationItemReference.ApiReference("api-id");

        private PortalNavigationPage anOwnedPage(String title, int order) {
            var page = PortalNavigationItemFixtures.aPage(title, null).toBuilder().reference(API_REFERENCE).order(order).build();
            page.markAsRoot();
            crudService.create(page);
            return page;
        }

        private PortalNavigationItem update(PortalNavigationItem existing, UpdatePortalNavigationItem toUpdate) {
            return useCase
                .execute(
                    UpdatePortalNavigationItemUseCase.Input.builder()
                        .organizationId(ORG_ID)
                        .environmentId(ENV_ID)
                        .navigationItemId(existing.getId().json())
                        .updatePortalNavigationItem(toUpdate)
                        .build()
                )
                .updatedItem();
        }

        private UpdatePortalNavigationItem.UpdatePortalNavigationItemBuilder anUpdateOf(PortalNavigationItem existing) {
            return UpdatePortalNavigationItem.builder()
                .type(existing.getType())
                .title(existing.getTitle())
                .order(existing.getOrder())
                .parentId(existing.getParentId())
                .published(existing.getPublished())
                .visibility(existing.getVisibility());
        }

        @Test
        void should_keep_the_owner_and_empty_stored_parent_of_a_top_level_api_owned_item_on_rename() {
            var page = anOwnedPage("Overview", 0);

            update(page, anUpdateOf(page).title("Introduction").build());

            var stored = queryService.findByIdAndEnvironmentId(ENV_ID, page.getId());
            assertThat(stored.getTitle()).isEqualTo("Introduction");
            assertThat(stored.getReference()).isEqualTo(API_REFERENCE);
            assertThat(stored.getParentId()).isNull();
        }

        @Test
        void should_flip_only_the_published_flag_of_the_updated_item() {
            var page = anOwnedPage("Overview", 0);
            var sibling = anOwnedPage("Guide", 1);

            update(page, anUpdateOf(page).published(false).build());

            assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, page.getId()).getPublished()).isFalse();
            assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, sibling.getId()).getPublished()).isTrue();
        }

        @Test
        void should_reorder_among_the_api_owned_siblings_only() {
            var first = anOwnedPage("Overview", 0);
            var second = anOwnedPage("Guide", 1);
            var portalRootOrders = topLevelPortalOrders();

            update(second, anUpdateOf(second).order(0).build());

            assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, second.getId()).getOrder()).isZero();
            assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, first.getId()).getOrder()).isEqualTo(1);
            assertThat(topLevelPortalOrders()).isEqualTo(portalRootOrders);
        }

        private Map<PortalNavigationItemId, Integer> topLevelPortalOrders() {
            return queryService
                .findTopLevelItemsByEnvironmentIdAndPortalArea(ENV_ID, PortalArea.TOP_NAVBAR)
                .stream()
                .filter(item -> !(item.getReference() instanceof NavigationItemReference.ApiReference))
                .collect(Collectors.toMap(PortalNavigationItem::getId, PortalNavigationItem::getOrder));
        }
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

    @ParameterizedTest
    @CsvSource({ "API, 0", "API, 2", "API_PRODUCT, 0", "API_PRODUCT, 2" })
    void should_reject_moving_api_or_api_product_under_persisted_api_owned_folder(PortalNavigationItemType type, int nestedFolderCount) {
        var parent = PortalNavigationItemFixtures.aFolder("API documentation").toBuilder().reference(new ApiReference("api-1")).build();
        parent.markAsRoot();
        crudService.create(parent);
        for (int depth = 0; depth < nestedFolderCount; depth++) {
            var nestedFolder = PortalNavigationItemFixtures.aFolder("Section " + depth)
                .toBuilder()
                .reference(parent.getReference())
                .build();
            nestedFolder.updateParent(parent);
            crudService.create(nestedFolder);
            parent = nestedFolder;
        }
        PortalNavigationItem existing;
        if (type == PortalNavigationItemType.API) {
            existing = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API2_ID));
        } else {
            existing = PortalNavigationItemFixtures.anApiProduct(
                API_PRODUCT_ID,
                "Product",
                PortalNavigationItemId.of(APIS_ID),
                "product-id"
            );
            crudService.create(existing);
        }
        var originalParentId = existing.getParentId();
        var originalRootId = existing.getRootId();
        var toUpdate = UpdatePortalNavigationItem.builder()
            .type(type)
            .title(existing.getTitle())
            .order(existing.getOrder())
            .parentId(parent.getId())
            .published(existing.getPublished())
            .visibility(existing.getVisibility())
            .build();
        var input = UpdatePortalNavigationItemUseCase.Input.builder()
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .navigationItemId(existing.getId().json())
            .updatePortalNavigationItem(toUpdate)
            .build();

        var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

        assertThat(exception.getMessage()).isEqualTo(InvalidPortalNavigationItemDataException.parentHierarchyContainsApi().getMessage());
        assertThat(queryService.findByIdAndEnvironmentId(ENV_ID, existing.getId())).satisfies(item -> {
            assertThat(item.getParentId()).isEqualTo(originalParentId);
            assertThat(item.getRootId()).isEqualTo(originalRootId);
        });
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
    void should_hand_a_page_to_the_api_when_moved_under_its_listing() {
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

        var updated = queryService.findByIdAndEnvironmentId(ENV_ID, existing.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getParentId()).isNull();
        assertThat(updated.getReference()).isEqualTo(new ApiReference("api-1"));
    }

    @Test
    void should_keep_a_folder_stored_under_a_listing_when_saved_in_place() {
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
    void should_hand_a_link_to_the_api_when_moved_under_its_listing() {
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

        // LINK1 was a root item of the portal and is now a root of the API's documentation: no stored parent
        var updated = queryService.findByIdAndEnvironmentId(ENV_ID, existing.getId());
        assertThat(updated).isNotNull();
        assertThat(updated.getParentId()).isNull();
        assertThat(updated.getRootId()).isEqualTo(existing.getId());
        assertThat(updated.getReference()).isEqualTo(new ApiReference("api-1"));
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
    class ApiOwnedDocumentation {

        private static final String DOCUMENT_ID = "00000000-0000-0000-0000-00000000d001";
        private static final String FOLDER_ID = "00000000-0000-0000-0000-00000000d002";
        private static final String OTHER_DOCUMENT_ID = "00000000-0000-0000-0000-00000000d003";
        private final ApiReference owner = new ApiReference("api-1");

        @ParameterizedTest
        @EnumSource(value = PortalNavigationItemType.class, names = { "PAGE", "FOLDER", "LINK" })
        void should_keep_api_owned_root_when_renamed_from_navigation(PortalNavigationItemType type) {
            var document = givenRoot(type);
            var command = updateUnder(document, PortalNavigationItemId.of(API1_ID)).title("Renamed").build();

            var saved = execute(document, command);

            assertThat(saved.getTitle()).isEqualTo("Renamed");
            assertRoot(document);
            assertThat(command.getParentId()).isEqualTo(PortalNavigationItemId.of(API1_ID));
        }

        @ParameterizedTest
        @CsvSource({ "false, PUBLIC", "true, PRIVATE" })
        void should_keep_api_owned_root_when_publication_or_visibility_changes(boolean published, PortalVisibility visibility) {
            var document = givenRoot(PortalNavigationItemType.PAGE);

            var saved = execute(
                document,
                updateUnder(document, PortalNavigationItemId.of(API1_ID)).published(published).visibility(visibility).build()
            );

            assertThat(saved.getPublished()).isEqualTo(published);
            assertThat(saved.getVisibility()).isEqualTo(visibility);
            assertRoot(document);
        }

        @Test
        void should_reorder_only_roots_of_the_same_api() {
            var document = givenRoot(PortalNavigationItemType.PAGE);
            var sibling = PortalNavigationItemFixtures.aFolder(FOLDER_ID, "Sibling").toBuilder().reference(owner).order(1).build();
            sibling.markAsRoot();
            var otherApiRoot = PortalNavigationItemFixtures.aFolder(OTHER_DOCUMENT_ID, "Other API")
                .toBuilder()
                .reference(new ApiReference("api-2"))
                .order(0)
                .build();
            otherApiRoot.markAsRoot();
            queryService.storage().addAll(List.of(sibling, otherApiRoot));
            var physicalChild = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_FOLDER_ID));
            var physicalChildOrder = physicalChild.getOrder();
            var portalRoot = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(APIS_ID));
            var portalRootOrder = portalRoot.getOrder();

            execute(document, updateUnder(document, PortalNavigationItemId.of(API1_ID)).order(1).build());

            assertRoot(document);
            assertThat(document.getOrder()).isEqualTo(1);
            assertThat(sibling.getOrder()).isZero();
            assertThat(otherApiRoot.getOrder()).isZero();
            assertThat(physicalChild.getOrder()).isEqualTo(physicalChildOrder);
            assertThat(portalRoot.getOrder()).isEqualTo(portalRootOrder);
        }

        @Test
        void should_accept_another_listing_of_the_same_api_without_reparenting() {
            var document = givenRoot(PortalNavigationItemType.PAGE);
            var listing = PortalNavigationItemFixtures.anApi(
                OTHER_DOCUMENT_ID,
                "Another listing",
                PortalNavigationItemId.of(CATEGORY1_ID),
                owner.apiId()
            );
            queryService.storage().add(listing);

            execute(document, updateUnder(document, listing.getId()).build());

            assertRoot(document);
        }

        @Test
        void should_validate_segments_in_the_api_root_namespace() {
            var document = givenRoot(PortalNavigationItemType.FOLDER);
            var sibling = PortalNavigationItemFixtures.aFolder(FOLDER_ID, "Sibling").toBuilder().reference(owner).build();
            sibling.markAsRoot();
            queryService.storage().add(sibling);

            assertThatThrownBy(() ->
                execute(document, updateUnder(document, PortalNavigationItemId.of(API1_ID)).segment(sibling.getSegment()).build())
            ).isInstanceOf(PathConflictException.class);

            assertRoot(document);
            assertThat(document.getSegment()).isEqualTo("document");
        }

        @Test
        void should_discard_a_rendered_parent_not_resolved_by_the_server() {
            var document = givenRoot(PortalNavigationItemType.PAGE);
            var command = updateUnder(document, null)
                .renderedParentId(PortalNavigationItemId.of("00000000-0000-0000-0000-00000000ffff"))
                .build();

            execute(document, command);

            assertRoot(document);
        }

        @Test
        void should_move_nested_documentation_to_the_api_root() {
            var folder = givenRoot(PortalNavigationItemType.FOLDER);
            var document = PortalNavigationItemFixtures.aFolder(FOLDER_ID, "Nested folder").toBuilder().reference(owner).build();
            document.updateParent((PortalNavigationFolder) folder);
            var child = PortalNavigationItemFixtures.aPage(OTHER_DOCUMENT_ID, "Child", document.getId())
                .toBuilder()
                .reference(owner)
                .build();
            child.updateParent(document);
            queryService.storage().addAll(List.of(document, child));

            execute(document, updateUnder(document, PortalNavigationItemId.of(API1_ID)).order(0).build());

            assertRoot(document);
            assertThat(document.getOrder()).isZero();
            assertThat(folder.getOrder()).isEqualTo(1);
            assertThat(child.getParentId()).isEqualTo(document.getId());
            assertThat(child.getRootId()).isEqualTo(document.getId());
            assertThat(child.getReference()).isEqualTo(owner);
        }

        @Test
        void should_keep_a_real_move_into_an_api_owned_folder() {
            var document = givenRoot(PortalNavigationItemType.PAGE);
            var folder = PortalNavigationItemFixtures.aFolder(FOLDER_ID, "Target").toBuilder().reference(owner).order(1).build();
            folder.markAsRoot();
            queryService.storage().add(folder);

            var saved = execute(document, updateUnder(document, folder.getId()).build());

            assertThat(saved.getParentId()).isEqualTo(folder.getId());
            assertThat(saved.getRootId()).isEqualTo(folder.getId());
            assertThat(saved.getReference()).isEqualTo(owner);
            assertThat(folder.getOrder()).isZero();
        }

        @Test
        void should_validate_publication_against_the_displayed_parent() {
            var document = givenRoot(PortalNavigationItemType.PAGE);
            queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID)).setPublished(false);

            assertThatThrownBy(() -> execute(document, updateUnder(document, PortalNavigationItemId.of(API1_ID)).build()))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining("published");
            assertRoot(document);
        }

        @Test
        void should_validate_visibility_against_the_displayed_parent() {
            var document = givenRoot(PortalNavigationItemType.PAGE);
            queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID)).setVisibility(PortalVisibility.PRIVATE);

            assertThatThrownBy(() -> execute(document, updateUnder(document, PortalNavigationItemId.of(API1_ID)).build()))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining("public");
            assertRoot(document);
        }

        @Test
        void should_validate_area_against_the_displayed_parent() {
            var document = givenRoot(PortalNavigationItemType.PAGE);
            queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID)).setArea(PortalArea.HOMEPAGE);

            assertThatThrownBy(() -> execute(document, updateUnder(document, PortalNavigationItemId.of(API1_ID)).build())).isInstanceOf(
                ParentAreaMismatchException.class
            );
            assertRoot(document);
        }

        @Test
        void should_not_resolve_a_listing_from_another_environment() {
            var document = givenRoot(PortalNavigationItemType.PAGE);
            queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID)).setEnvironmentId("other-env");

            assertThatThrownBy(() -> execute(document, updateUnder(document, PortalNavigationItemId.of(API1_ID)).build())).isInstanceOf(
                ParentNotFoundException.class
            );
            assertRoot(document);
        }

        private PortalNavigationItem givenRoot(PortalNavigationItemType type) {
            PortalNavigationItem document = switch (type) {
                case PAGE -> PortalNavigationItemFixtures.aPage(DOCUMENT_ID, "Document", null).toBuilder().reference(owner).build();
                case FOLDER -> PortalNavigationItemFixtures.aFolder(DOCUMENT_ID, "Document").toBuilder().reference(owner).build();
                case LINK -> PortalNavigationLink.builder()
                    .id(PortalNavigationItemId.of(DOCUMENT_ID))
                    .organizationId(ORG_ID)
                    .environmentId(ENV_ID)
                    .reference(owner)
                    .title("Document")
                    .segment("document")
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .url("https://example.com")
                    .published(true)
                    .visibility(PortalVisibility.PUBLIC)
                    .build();
                default -> throw new IllegalArgumentException("Not a documentation type: " + type);
            };
            document.markAsRoot();
            queryService.storage().add(document);
            return document;
        }

        private UpdatePortalNavigationItem.UpdatePortalNavigationItemBuilder updateUnder(
            PortalNavigationItem document,
            PortalNavigationItemId parentId
        ) {
            return UpdatePortalNavigationItem.builder()
                .type(document.getType())
                .title(document.getTitle())
                .segment(document.getSegment())
                .order(document.getOrder())
                .parentId(parentId)
                .published(document.getPublished())
                .visibility(document.getVisibility())
                .source(document.getSource())
                .url(document instanceof PortalNavigationLink link ? link.getUrl() : null);
        }

        private PortalNavigationItem execute(PortalNavigationItem document, UpdatePortalNavigationItem command) {
            return useCase
                .execute(
                    UpdatePortalNavigationItemUseCase.Input.builder()
                        .organizationId(ORG_ID)
                        .environmentId(ENV_ID)
                        .navigationItemId(document.getId().json())
                        .updatePortalNavigationItem(command)
                        .build()
                )
                .updatedItem();
        }

        private void assertRoot(PortalNavigationItem document) {
            var stored = queryService.findByIdAndEnvironmentId(ENV_ID, document.getId());
            assertThat(stored.getParentId()).isNull();
            assertThat(stored.getRootId()).isEqualTo(document.getId());
            assertThat(stored.getReference()).isEqualTo(owner);
        }
    }

    /**
     * The portal editor's path: the owner of a page, folder or link follows the place it is moved to.
     */
    @Nested
    class OwnerChangeOnMove {

        private static final ApiReference API_A = new ApiReference("api-a");
        private static final ApiReference API_B = new ApiReference("api-b");
        private static final NavigationItemReference PORTAL = NavigationItemReference.defaultReference();

        private PortalNavigationFolder section;

        @BeforeEach
        void setUp() {
            section = store(PortalNavigationItemFixtures.aFolder("Documentation section"));
        }

        @Test
        void should_make_a_portal_page_api_owned_when_moved_onto_a_listing_row() {
            var listing = aListingOf(API_A);
            var page = store(PortalNavigationItemFixtures.aPage("Guide", section.getId()));

            move(page, listing.getId());

            assertOwnedRootOf(API_A, page);
        }

        @Test
        void should_make_a_top_level_portal_page_api_owned_when_moved_onto_a_listing_row() {
            var listing = aListingOf(API_A);
            var page = store(PortalNavigationItemFixtures.aPage("Guide", null));

            move(page, listing.getId());

            assertOwnedRootOf(API_A, page);
        }

        @Test
        void should_validate_the_move_against_the_listing_row_it_is_dropped_on() {
            var hiddenListing = aListingOf(API_A);
            hiddenListing.setPublished(false);
            crudService.update(hiddenListing);
            var page = store(PortalNavigationItemFixtures.aPage("Guide", null));

            assertThatThrownBy(() -> move(page, hiddenListing.getId())).isInstanceOf(InvalidPortalNavigationItemDataException.class);
            assertThat(find(page).getReference()).isEqualTo(PORTAL);
        }

        @Test
        void should_make_a_portal_page_api_owned_when_moved_into_a_folder_the_api_owns() {
            var apiFolder = store(ownedBy(API_A, PortalNavigationItemFixtures.aFolder("Api guides")));
            var page = store(PortalNavigationItemFixtures.aPage("Guide", section.getId()));

            move(page, apiFolder.getId());

            assertThat(find(page).getReference()).isEqualTo(API_A);
            assertThat(find(page).getParentId()).isEqualTo(apiFolder.getId());
        }

        @Test
        void should_carry_the_new_owner_down_a_three_level_subtree() {
            var listing = aListingOf(API_A);
            var folder = store(PortalNavigationItemFixtures.aFolder("Tutorials", section.getId()));
            var subFolder = store(PortalNavigationItemFixtures.aFolder("Advanced", folder.getId()));
            var page = store(PortalNavigationItemFixtures.aPage("Tuning", subFolder.getId()));

            move(folder, listing.getId());

            assertOwnedRootOf(API_A, folder);
            assertThat(List.of(find(subFolder), find(page))).allSatisfy(descendant -> {
                assertThat(descendant.getReference()).isEqualTo(API_A);
                assertThat(descendant.getRootId()).isEqualTo(folder.getId());
            });
            assertThat(find(subFolder).getParentId()).isEqualTo(folder.getId());
            assertThat(find(page).getParentId()).isEqualTo(subFolder.getId());
        }

        @Test
        void should_revert_the_whole_subtree_when_moved_back_to_a_portal_folder() {
            var listing = aListingOf(API_A);
            var folder = store(PortalNavigationItemFixtures.aFolder("Tutorials", section.getId()));
            var subFolder = store(PortalNavigationItemFixtures.aFolder("Advanced", folder.getId()));
            var page = store(PortalNavigationItemFixtures.aPage("Tuning", subFolder.getId()));
            move(folder, listing.getId());

            move(find(folder), section.getId());

            assertThat(List.of(find(folder), find(subFolder), find(page))).allSatisfy(item -> {
                assertThat(item.getReference()).isEqualTo(PORTAL);
                assertThat(item.getRootId()).isEqualTo(section.getId());
            });
            assertThat(find(folder).getParentId()).isEqualTo(section.getId());
        }

        @Test
        void should_take_the_other_api_as_owner_when_moved_onto_its_listing_row() {
            aListingOf(API_A);
            var listingOfB = aListingOf(API_B);
            var page = store(ownedBy(API_A, PortalNavigationItemFixtures.aPage("Guide", null)));

            move(page, listingOfB.getId());

            assertOwnedRootOf(API_B, page);
        }

        @Test
        void should_take_the_other_api_as_owner_when_moved_into_its_folder() {
            var folderOfB = store(ownedBy(API_B, PortalNavigationItemFixtures.aFolder("Guides of B")));
            var page = store(ownedBy(API_A, PortalNavigationItemFixtures.aPage("Guide", null)));

            move(page, folderOfB.getId());

            assertThat(find(page).getReference()).isEqualTo(API_B);
            assertThat(find(page).getParentId()).isEqualTo(folderOfB.getId());
        }

        @Test
        void should_leave_no_mixed_owners_in_a_moved_subtree() {
            var listing = aListingOf(API_A);
            var folder = store(PortalNavigationItemFixtures.aFolder("Tutorials", section.getId()));
            var portalPage = store(PortalNavigationItemFixtures.aPage("Portal page", folder.getId()));
            var pageOfB = store(ownedBy(API_B, PortalNavigationItemFixtures.aPage("Page of B", folder.getId())));

            move(folder, listing.getId());

            assertThat(List.of(find(folder), find(portalPage), find(pageOfB)))
                .extracting(PortalNavigationItem::getReference)
                .containsOnly(API_A);
        }

        @Test
        void should_become_portal_owned_when_moved_under_an_api_product() {
            var product = store(
                PortalNavigationItemFixtures.anApiProduct(PortalNavigationItemId.random().json(), "Product", section.getId(), "product-ref")
            );
            var productFolder = store(PortalNavigationItemFixtures.aFolder("Product guides", product.getId()));
            var page = store(ownedBy(API_A, PortalNavigationItemFixtures.aPage("Guide", null)));

            move(page, productFolder.getId());

            assertThat(find(page).getReference()).isEqualTo(PORTAL);
            assertThat(find(page).getParentId()).isEqualTo(productFolder.getId());
        }

        @Test
        void should_not_change_the_owner_of_a_listing_row() {
            var listing = aListingOf(API_A);
            var otherSection = store(PortalNavigationItemFixtures.aFolder("Other section"));

            move(listing, otherSection.getId());

            assertThat(find(listing).getReference()).isEqualTo(PORTAL);
            assertThat(find(listing).getParentId()).isEqualTo(otherSection.getId());
        }

        @Test
        void should_keep_the_owner_and_the_descendants_when_only_the_title_changes() {
            var folder = store(PortalNavigationItemFixtures.aFolder("Tutorials", section.getId()));
            var pageOfB = store(ownedBy(API_B, PortalNavigationItemFixtures.aPage("Page of B", folder.getId())));

            execute(folder, updateOf(folder).title("Handbooks").build(), false);

            assertThat(find(folder).getReference()).isEqualTo(PORTAL);
            assertThat(find(pageOfB).getReference()).isEqualTo(API_B);
        }

        @Test
        void should_keep_the_owner_when_moved_between_two_folders_of_the_same_api() {
            var from = store(ownedBy(API_A, PortalNavigationItemFixtures.aFolder("Basics")));
            var to = store(ownedBy(API_A, PortalNavigationItemFixtures.aFolder("Advanced")));
            var page = store(ownedBy(API_A, PortalNavigationItemFixtures.aPage("Guide", from.getId())));

            move(page, to.getId());

            assertThat(find(page).getReference()).isEqualTo(API_A);
            assertThat(find(page).getParentId()).isEqualTo(to.getId());
        }

        @Test
        void should_not_treat_a_move_between_two_portal_references_as_a_change_of_owner() {
            var otherPortal = new NavigationItemReference.PortalReference(PortalId.of("00000000-0000-0000-0000-0000000000aa"));
            var managed = store(
                automationManaged(PortalNavigationItemFixtures.aPage("Managed page", null)).toBuilder().reference(otherPortal).build()
            );

            move(managed, section.getId());

            assertThat(find(managed).getParentId()).isEqualTo(section.getId());
            assertThat(find(managed).getReference()).isEqualTo(otherPortal);
        }

        @Test
        void should_keep_a_portal_owned_item_stored_under_a_listing_row_when_saved_in_place() {
            var listing = aListingOf(API_A);
            var legacyPage = store(PortalNavigationItemFixtures.aPage("Legacy page", listing.getId()));

            execute(legacyPage, updateOf(legacyPage).title("Renamed legacy page").build(), false);

            assertThat(find(legacyPage).getReference()).isEqualTo(PORTAL);
            assertThat(find(legacyPage).getParentId()).isEqualTo(listing.getId());
        }

        @Test
        void should_make_a_nested_api_owned_item_portal_owned_when_moved_to_the_portal_top_level() {
            var apiRoot = store(ownedBy(API_A, PortalNavigationItemFixtures.aFolder("Tutorials")));
            var folder = store(ownedBy(API_A, PortalNavigationItemFixtures.aFolder("Advanced", apiRoot.getId())));
            var page = store(ownedBy(API_A, PortalNavigationItemFixtures.aPage("Guide", folder.getId())));

            move(folder, null);

            assertThat(List.of(find(folder), find(page))).extracting(PortalNavigationItem::getReference).containsOnly(PORTAL);
            assertThat(find(folder).getParentId()).isNull();
            assertThat(find(apiRoot).getReference()).isEqualTo(API_A);
        }

        /**
         * A root of an API's documentation is stored with no parent, and that is what a read of it returns:
         * sent back as it is, it has not moved.
         */
        @Test
        void should_keep_an_api_owned_root_and_its_descendants_when_saved_with_no_parent() {
            var folder = store(ownedBy(API_A, PortalNavigationItemFixtures.aFolder("Tutorials")));
            var page = store(ownedBy(API_A, PortalNavigationItemFixtures.aPage("Guide", folder.getId())));

            execute(folder, updateOf(folder).title("Handbooks").build(), false);

            assertThat(find(folder).getTitle()).isEqualTo("Handbooks");
            assertThat(List.of(find(folder), find(page))).extracting(PortalNavigationItem::getReference).containsOnly(API_A);
        }

        @Test
        void should_still_rename_an_automation_managed_api_root_saved_with_no_parent() {
            var managed = store(
                ownedBy(API_A, PortalNavigationItemFixtures.aPage("Managed page", null))
                    .toBuilder()
                    .automationMetadata(
                        new AutomationMetadata(
                            AutomationMetadata.ReferenceType.API,
                            API_A.apiId(),
                            "Managed page",
                            Optional.empty(),
                            Optional.empty()
                        )
                    )
                    .build()
            );

            execute(managed, updateOf(managed).title("Renamed managed page").build(), false);

            assertThat(find(managed).getTitle()).isEqualTo("Renamed managed page");
            assertThat(find(managed).getReference()).isEqualTo(API_A);
        }

        @Test
        void should_keep_the_owner_at_the_api_root_when_the_owner_is_fixed() {
            var folder = store(ownedBy(API_A, PortalNavigationItemFixtures.aFolder("Tutorials")));
            var page = store(ownedBy(API_A, PortalNavigationItemFixtures.aPage("Guide", folder.getId())));

            execute(page, updateOf(page).parentId(null).build(), true);

            assertOwnedRootOf(API_A, page);
        }

        @Test
        void should_refuse_moving_a_folder_holding_a_listing_row_onto_an_api() {
            var listingOfA = aListingOf(API_A);
            var folder = store(PortalNavigationItemFixtures.aFolder("Partner APIs", section.getId()));
            var nestedListing = store(
                PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "Api B", folder.getId(), API_B.apiId())
            );

            assertThatThrownBy(() -> move(folder, listingOfA.getId()))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining("cannot be moved into the documentation of an API");
            assertThat(find(folder).getReference()).isEqualTo(PORTAL);
            assertThat(find(nestedListing).getParentId()).isEqualTo(folder.getId());
        }

        @Test
        void should_refuse_moving_a_folder_holding_an_api_product_into_a_folder_an_api_owns() {
            var apiFolder = store(ownedBy(API_A, PortalNavigationItemFixtures.aFolder("Api guides")));
            var folder = store(PortalNavigationItemFixtures.aFolder("Products", section.getId()));
            store(
                PortalNavigationItemFixtures.anApiProduct(PortalNavigationItemId.random().json(), "Product", folder.getId(), "product-ref")
            );

            assertThatThrownBy(() -> move(folder, apiFolder.getId()))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining("cannot be moved into the documentation of an API");
            assertThat(find(folder).getReference()).isEqualTo(PORTAL);
            assertThat(find(folder).getParentId()).isEqualTo(section.getId());
        }

        @Test
        void should_refuse_changing_the_owner_of_an_automation_managed_item() {
            var listing = aListingOf(API_A);
            var managed = store(automationManaged(PortalNavigationItemFixtures.aPage("Managed page", section.getId())));

            assertThatThrownBy(() -> move(managed, listing.getId()))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining("automation");
            assertThat(find(managed).getReference()).isEqualTo(PORTAL);
        }

        @Test
        void should_refuse_changing_the_owner_of_a_folder_holding_an_automation_managed_item() {
            var listing = aListingOf(API_A);
            var folder = store(PortalNavigationItemFixtures.aFolder("Tutorials", section.getId()));
            store(automationManaged(PortalNavigationItemFixtures.aPage("Managed page", folder.getId())));

            assertThatThrownBy(() -> move(folder, listing.getId()))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining("automation");
            assertThat(find(folder).getReference()).isEqualTo(PORTAL);
        }

        @Test
        void should_still_move_an_automation_managed_item_when_the_owner_does_not_change() {
            var otherSection = store(PortalNavigationItemFixtures.aFolder("Other section"));
            var managed = store(automationManaged(PortalNavigationItemFixtures.aPage("Managed page", section.getId())));

            move(managed, otherSection.getId());

            assertThat(find(managed).getParentId()).isEqualTo(otherSection.getId());
            assertThat(find(managed).getReference()).isEqualTo(PORTAL);
        }

        private PortalNavigationApi aListingOf(ApiReference api) {
            return store(
                PortalNavigationItemFixtures.anApi(
                    PortalNavigationItemId.random().json(),
                    "Listing of " + api.apiId(),
                    section.getId(),
                    api.apiId()
                )
            );
        }

        private <T extends PortalNavigationItem> T store(T item) {
            if (item.getParentId() == null) {
                item.markAsRoot();
            } else {
                item.updateParent((PortalNavigationItemContainer) queryService.findByIdAndEnvironmentId(ENV_ID, item.getParentId()));
            }
            crudService.create(item);
            return item;
        }

        private static PortalNavigationFolder ownedBy(ApiReference api, PortalNavigationFolder folder) {
            return folder.toBuilder().reference(api).build();
        }

        private static PortalNavigationPage ownedBy(ApiReference api, PortalNavigationPage page) {
            return page.toBuilder().reference(api).build();
        }

        private static PortalNavigationPage automationManaged(PortalNavigationPage page) {
            return page
                .toBuilder()
                .automationMetadata(
                    new AutomationMetadata(
                        AutomationMetadata.ReferenceType.PORTAL,
                        "portal-id",
                        page.getTitle(),
                        Optional.empty(),
                        Optional.empty()
                    )
                )
                .build();
        }

        private PortalNavigationItem find(PortalNavigationItem item) {
            return queryService.findByIdAndEnvironmentId(ENV_ID, item.getId());
        }

        private void move(PortalNavigationItem item, PortalNavigationItemId parentId) {
            execute(item, updateOf(item).parentId(parentId).build(), false);
        }

        private UpdatePortalNavigationItem.UpdatePortalNavigationItemBuilder updateOf(PortalNavigationItem item) {
            return UpdatePortalNavigationItem.builder()
                .type(item.getType())
                .title(item.getTitle())
                .segment(item.getSegment())
                .order(item.getOrder())
                .parentId(item.getParentId())
                .published(item.getPublished())
                .visibility(item.getVisibility());
        }

        private void execute(PortalNavigationItem item, UpdatePortalNavigationItem command, boolean ownerFixed) {
            useCase.execute(
                UpdatePortalNavigationItemUseCase.Input.builder()
                    .organizationId(ORG_ID)
                    .environmentId(ENV_ID)
                    .navigationItemId(item.getId().json())
                    .updatePortalNavigationItem(command)
                    .ownerFixed(ownerFixed)
                    .build()
            );
        }

        private void assertOwnedRootOf(ApiReference api, PortalNavigationItem item) {
            var stored = find(item);
            assertThat(stored.getReference()).isEqualTo(api);
            assertThat(stored.getParentId()).isNull();
            assertThat(stored.getRootId()).isEqualTo(item.getId());
        }
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
        void should_update_publication_of_a_sourced_api_root_without_moving_it() {
            var source = aSourceWithSecret(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA);
            source.setSubtreeImport(true);
            var page = PortalNavigationItemFixtures.aPage(SOURCED_PAGE_ID, "Sourced API page", null)
                .toBuilder()
                .reference(new ApiReference("api-1"))
                .source(source)
                .build();
            page.markAsRoot();
            queryService.storage().add(page);

            var output = useCase.execute(
                anUpdateInput(
                    page,
                    anUpdateKeeping(page).parentId(PortalNavigationItemId.of(API1_ID)).published(false).source(aMaskedSource()).build()
                )
            );

            assertThat(output.updatedItem().getParentId()).isNull();
            assertThat(output.updatedItem().getRootId()).isEqualTo(page.getId());
            assertThat(output.updatedItem().getReference()).isEqualTo(new ApiReference("api-1"));
            assertThat(output.updatedItem().getPublished()).isFalse();
            assertThat(sourceDomainService.lastValidatedConfiguration()).contains(
                PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA
            );
            assertThat(output.updatedItem().getSource().getSourceConfiguration())
                .contains(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA_REPLACEMENT)
                .doesNotContain(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA);
            assertThat(output.updatedItem().getSource().isSubtreeImport()).isTrue();
        }

        @Test
        void should_still_reject_renaming_a_sourced_api_root() {
            var page = PortalNavigationItemFixtures.aPage(SOURCED_PAGE_ID, "Sourced API page", null)
                .toBuilder()
                .reference(new ApiReference("api-1"))
                .source(aSource())
                .build();
            page.markAsRoot();
            queryService.storage().add(page);
            var input = anUpdateInput(
                page,
                anUpdateKeeping(page).parentId(PortalNavigationItemId.of(API1_ID)).title("Renamed").source(aSource()).build()
            );

            assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining("cannot be renamed or moved");

            assertThat(page.getParentId()).isNull();
            assertThat(page.getTitle()).isEqualTo("Sourced API page");
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
        void should_reject_attaching_a_file_listing_source_to_a_folder() {
            // Fetching such a folder re-imports the subtree; only the import endpoint may create one
            sourceDomainService.givenRemoteFile("/docs/guide.md", "# Guide");
            var folder = PortalNavigationItemFixtures.aFolder(SOURCED_FOLDER_ID, "Folder");
            folder.markAsRoot();
            var child = PortalNavigationItemFixtures.aPage(CHILD_PAGE_ID, "Child Page", folder.getId());
            queryService.storage().addAll(List.of(folder, child));

            var input = anUpdateInput(folder, anUpdateKeeping(folder).source(aSource()).build());

            var error = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

            assertThat(error).hasMessageContaining("file-listing source");
        }

        @Test
        void should_accept_attaching_a_source_that_cannot_list_files_to_a_folder_with_children() {
            // The in-memory source is not files-capable by default: such a source cannot trigger a re-import
            var folder = PortalNavigationItemFixtures.aFolder(SOURCED_FOLDER_ID, "Folder");
            folder.markAsRoot();
            var child = PortalNavigationItemFixtures.aPage(CHILD_PAGE_ID, "Child Page", folder.getId());
            queryService.storage().addAll(List.of(folder, child));

            var input = anUpdateInput(folder, anUpdateKeeping(folder).source(aSource()).build());

            assertDoesNotThrow(() -> useCase.execute(input));
        }

        @Test
        void should_accept_updating_the_source_of_an_import_managed_folder_and_keep_its_marker() {
            sourceDomainService.givenRemoteFile("/docs/guide.md", "# Guide");
            var folder = PortalNavigationItemFixtures.aFolder(SOURCED_FOLDER_ID, "Imported Docs")
                .toBuilder()
                .source(aSource().toBuilder().subtreeImport(true).build())
                .build();
            folder.markAsRoot();
            var child = PortalNavigationItemFixtures.aPage(CHILD_PAGE_ID, "Imported Page", folder.getId());
            queryService.storage().addAll(List.of(folder, child));

            var input = anUpdateInput(folder, anUpdateKeeping(folder).source(aSource()).build());

            var output = useCase.execute(input);

            // The payload carries no marker: it must be carried over from the stored source
            assertThat(output.updatedItem().getSource().isSubtreeImport()).isTrue();
        }

        @Test
        void should_reject_updating_an_import_managed_folder_to_a_source_that_cannot_list_files() {
            // The in-memory source is not files-capable by default: such a source cannot re-run the import
            var folder = PortalNavigationItemFixtures.aFolder(SOURCED_FOLDER_ID, "Imported Docs")
                .toBuilder()
                .source(aSource().toBuilder().subtreeImport(true).build())
                .build();
            folder.markAsRoot();
            var child = PortalNavigationItemFixtures.aPage(CHILD_PAGE_ID, "Imported Page", folder.getId());
            queryService.storage().addAll(List.of(folder, child));

            var input = anUpdateInput(folder, anUpdateKeeping(folder).source(aSource()).build());

            var error = assertThrows(InvalidPortalNavigationItemDataException.class, () -> useCase.execute(input));

            assertThat(error).hasMessageContaining("must be able to list files");
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
