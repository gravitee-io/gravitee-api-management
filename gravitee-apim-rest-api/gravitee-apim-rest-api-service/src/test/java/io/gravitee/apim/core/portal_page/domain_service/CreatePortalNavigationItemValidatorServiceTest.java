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

import static fixtures.core.model.PortalNavigationItemFixtures.API1_FOLDER_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.APIS_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PAGE11_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.exception.HomepageAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.InvalidUrlFormatException;
import io.gravitee.apim.core.portal_page.exception.ItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentAreaMismatchException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentTypeMismatchException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.ArrayList;
import java.util.List;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreatePortalNavigationItemValidatorServiceTest {

    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private PortalPageContentQueryServiceInMemory pageContentQueryService;
    private PortalNavigationItemValidatorService validatorService;

    @BeforeEach
    void setUp() {
        final var storage = new ArrayList<PortalNavigationItem>();

        pageContentQueryService = new PortalPageContentQueryServiceInMemory();
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        validatorService = new PortalNavigationItemValidatorService(navigationItemsQueryService, pageContentQueryService);
        navigationItemsQueryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
        pageContentQueryService.initWith(PortalPageContentFixtures.samplePortalPageContents());
    }

    @Nested
    class ValidateItem {

        @Test
        void should_fail_when_item_with_provided_id_already_exists() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.of(APIS_ID))
                .type(PortalNavigationItemType.FOLDER)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(ItemAlreadyExistsException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Item with provided id already exists");
        }

        @Test
        void should_fail_when_homepage_already_exists() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .title("title")
                .area(PortalArea.HOMEPAGE)
                .order(0)
                .build();
            navigationItemsQueryService.storage().add(PortalNavigationItem.from(createPortalNavigationItem, ORG_ID, ENV_ID));

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(HomepageAlreadyExistsException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Homepage already exists");
        }

        @Test
        void should_fail_when_page_content_is_not_found() {
            // Given
            final var NON_EXISTENT_ID = PortalPageContentId.random();
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .portalPageContentId(NON_EXISTENT_ID)
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.PAGE)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(PageContentNotFoundException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Page content not found");
        }

        @Test
        void should_fail_if_url_is_invalid() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.LINK)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .url("invalid-url")
                .build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(InvalidUrlFormatException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Provided url is invalid");
        }

        @Test
        void should_fail_when_api_item_has_null_api_id() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .parentId(PortalNavigationItemId.of(APIS_ID))
                .apiId(null)
                .build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(InvalidPortalNavigationItemDataException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("The apiId field is required and cannot be blank.");
        }

        @Test
        void should_fail_when_api_item_has_empty_api_id() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .parentId(PortalNavigationItemId.of(APIS_ID))
                .apiId("")
                .build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(InvalidPortalNavigationItemDataException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("The apiId field is required and cannot be blank.");
        }

        @Test
        void should_fail_when_api_item_has_null_parent_id() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .apiId("api-id")
                .build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(InvalidPortalNavigationItemDataException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("The parentId field is required and cannot be blank.");
        }

        @Test
        void should_fail_when_api_item_is_not_in_top_navbar_area() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("title")
                .area(PortalArea.HOMEPAGE)
                .order(0)
                .parentId(PortalNavigationItemId.of(APIS_ID))
                .apiId("api-id")
                .build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(InvalidPortalNavigationItemDataException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("API items can only be added to TOP_NAVBAR area.");
        }

        @Test
        void should_fail_when_api_item_has_duplicate_api_id() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .parentId(PortalNavigationItemId.of(APIS_ID))
                .apiId("api-1")
                .build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(InvalidPortalNavigationItemDataException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("The apiId api-1 is already used by another API navigation item.");
        }

        @Test
        void should_fail_when_api_item_parent_hierarchy_contains_api() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .parentId(PortalNavigationItemId.of(API1_FOLDER_ID))
                .apiId("api-id")
                .build();

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(InvalidPortalNavigationItemDataException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Parent hierarchy cannot include API items.");
        }

        @Test
        void should_validate_api_item_when_payload_is_valid() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("API 2")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .parentId(PortalNavigationItemId.of(APIS_ID))
                .apiId("api-2")
                .build();

            // Then
            assertDoesNotThrow(() -> validatorService.validateOne(createPortalNavigationItem, ENV_ID));
        }

        @Test
        void should_validate_all_when_payload_is_valid() {
            // Given
            final var firstCreateApiPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("API 2")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .parentId(PortalNavigationItemId.of(APIS_ID))
                .apiId("api-2")
                .build();

            final var createLinkPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.LINK)
                .title("Documentation")
                .area(PortalArea.TOP_NAVBAR)
                .order(1)
                .url("https://example.org/docs")
                .build();

            final var secondCreateApiPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("API 3")
                .area(PortalArea.TOP_NAVBAR)
                .order(2)
                .parentId(PortalNavigationItemId.of(APIS_ID))
                .apiId("api-3")
                .build();

            // Then
            assertDoesNotThrow(() ->
                validatorService.validateAll(
                    List.of(firstCreateApiPortalNavigationItem, createLinkPortalNavigationItem, secondCreateApiPortalNavigationItem),
                    ENV_ID
                )
            );
        }

        @Test
        void should_fail_validate_all_when_api_item_has_null_parent_id() {
            // Given
            final var validCreateLinkPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.LINK)
                .title("Documentation")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .url("https://example.org/docs")
                .build();

            final var invalidCreateApiPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("API 2")
                .area(PortalArea.TOP_NAVBAR)
                .order(1)
                .apiId("api-2")
                .build();

            // When
            final ThrowingRunnable throwing = () ->
                validatorService.validateAll(List.of(validCreateLinkPortalNavigationItem, invalidCreateApiPortalNavigationItem), ENV_ID);

            // Then
            Exception exception = assertThrows(InvalidPortalNavigationItemDataException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("The parentId field is required and cannot be blank.");
        }

        @Test
        void should_fail_validate_all_when_api_items_have_duplicate_api_id() {
            // Given
            final var firstCreateApiPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("API 2")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .parentId(PortalNavigationItemId.of(APIS_ID))
                .apiId("shared-api-id")
                .build();

            final var secondCreateApiPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.API)
                .title("API 3")
                .area(PortalArea.TOP_NAVBAR)
                .order(1)
                .parentId(PortalNavigationItemId.of(APIS_ID))
                .apiId("shared-api-id")
                .build();

            // When
            final ThrowingRunnable throwing = () ->
                validatorService.validateAll(List.of(firstCreateApiPortalNavigationItem, secondCreateApiPortalNavigationItem), ENV_ID);

            // Then
            Exception exception = assertThrows(InvalidPortalNavigationItemDataException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("The apiId shared-api-id is already used by another API navigation item.");
        }
    }

    @Nested
    class ValidateParent {

        @Test
        void should_fail_when_parent_is_not_found() {
            // Given
            final String NON_EXISTENT_ID = "6c2c004d-c4f2-4a2b-b2c3-857a4dfcc842";
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.FOLDER)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .build();
            createPortalNavigationItem.setParentId(PortalNavigationItemId.of(NON_EXISTENT_ID));

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(ParentNotFoundException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Parent item with provided id does not exist");
        }

        @Test
        void should_fail_when_parent_is_not_folder() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.FOLDER)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .build();
            createPortalNavigationItem.setParentId(PortalNavigationItemId.of(PAGE11_ID));

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(ParentTypeMismatchException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Parent item with id %s is not a folder", PAGE11_ID);
        }

        @Test
        void should_fail_when_parent_is_in_different_area() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.FOLDER)
                .title("title")
                .area(PortalArea.HOMEPAGE)
                .order(0)
                .build();
            createPortalNavigationItem.setParentId(PortalNavigationItemId.of(APIS_ID));

            // When
            final ThrowingRunnable throwing = () -> validatorService.validateOne(createPortalNavigationItem, ENV_ID);

            // Then
            Exception exception = assertThrows(ParentAreaMismatchException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Parent item with id %s belongs to a different area than the child item", APIS_ID);
        }
    }
}
