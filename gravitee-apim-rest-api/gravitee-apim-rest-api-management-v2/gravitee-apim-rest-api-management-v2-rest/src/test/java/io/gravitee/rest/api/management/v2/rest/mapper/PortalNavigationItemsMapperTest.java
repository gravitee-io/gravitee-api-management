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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.PortalNavigationItemsFixtures;
import fixtures.core.model.PortalNavigationItemFixtures;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemType;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageContentType;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationSubscriptionForm;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsMapperTest {

    private PortalNavigationItemsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = PortalNavigationItemsMapper.INSTANCE;
    }

    @Nested
    class DomainToResource {

        @Test
        void should_map_portal_navigation_page() {
            var page = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE_ID, "My Page", null);
            page.setOrder(1);

            var result = mapper.map(page);

            assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage.class);
            assertThat(result.getId()).isEqualTo(UUID.fromString(PortalNavigationItemFixtures.PAGE_ID));
            assertThat(result.getOrganizationId()).isEqualTo("org-id");
            assertThat(result.getEnvironmentId()).isEqualTo("env-id");
            assertThat(result.getTitle()).isEqualTo("My Page");
            assertThat(result.getType()).isEqualTo(PortalNavigationItemType.PAGE);
            assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(1);
            assertThat(result.getParentId()).isNull();
            assertThat(result.getPortalPageContentId()).isEqualTo(page.getPortalPageContentId().id());
            assertThat(result.getRootId()).isEqualTo(page.getRootId().id());
        }

        @Test
        void should_map_portal_navigation_folder() {
            var folder = PortalNavigationItemFixtures.aFolder(PortalNavigationItemFixtures.FOLDER_ID, "My Folder");
            folder.setOrder(2);

            var result = mapper.map(folder);

            assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder.class);
            assertThat(result.getId()).isEqualTo(UUID.fromString(PortalNavigationItemFixtures.FOLDER_ID));
            assertThat(result.getOrganizationId()).isEqualTo("org-id");
            assertThat(result.getEnvironmentId()).isEqualTo("env-id");
            assertThat(result.getTitle()).isEqualTo("My Folder");
            assertThat(result.getType()).isEqualTo(PortalNavigationItemType.FOLDER);
            assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(2);
            assertThat(result.getParentId()).isNull();
            assertThat(result.getRootId()).isEqualTo(folder.getRootId().id());
        }

        @Test
        void should_map_portal_navigation_subscription_form() {
            var subscriptionForm = PortalNavigationItemFixtures.aSubscriptionForm(
                PortalNavigationItemFixtures.PAGE_ID,
                io.gravitee.apim.core.portal_page.model.PortalPageContentId.random()
            );

            var result = mapper.map(subscriptionForm);

            assertThat(result.getId()).isEqualTo(UUID.fromString(PortalNavigationItemFixtures.PAGE_ID));
            assertThat(result.getType()).isEqualTo(PortalNavigationItemType.SUBSCRIPTION_FORM);
            assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.SUBSCRIPTION_FORM);
            assertThat(result.getPortalPageContentId()).isEqualTo(subscriptionForm.getPortalPageContentId().id());
            assertThat(result.getRootId()).isEqualTo(subscriptionForm.getRootId().id());
        }

        @Test
        void should_map_portal_navigation_link() {
            var link = PortalNavigationItemFixtures.aLink();

            var result = mapper.map(link);

            assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLink.class);
            assertThat(result.getId()).isEqualTo(UUID.fromString(PortalNavigationItemFixtures.LINK_ID));
            assertThat(result.getOrganizationId()).isEqualTo("org-id");
            assertThat(result.getEnvironmentId()).isEqualTo("env-id");
            assertThat(result.getTitle()).isEqualTo("My Link");
            assertThat(result.getType()).isEqualTo(PortalNavigationItemType.LINK);
            assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(3);
            assertThat(result.getParentId()).isNull();
            assertThat(result.getUrl()).isEqualTo("https://example.com");
            assertThat(result.getRootId()).isEqualTo(link.getRootId().id());
        }

        @Test
        void should_map_portal_navigation_api() {
            var api = PortalNavigationItemFixtures.anApi();

            var result = mapper.map(api);

            assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationApi.class);
            assertThat(result.getId()).isEqualTo(UUID.fromString(PortalNavigationItemFixtures.API_ID));
            assertThat(result.getOrganizationId()).isEqualTo("org-id");
            assertThat(result.getEnvironmentId()).isEqualTo("env-id");
            assertThat(result.getTitle()).isEqualTo("My Api");
            assertThat(result.getType()).isEqualTo(PortalNavigationItemType.API);
            assertThat(result.getArea()).isEqualTo(io.gravitee.rest.api.management.v2.rest.model.PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(3);
            assertThat(result.getParentId()).isNull();
            assertThat(result.getApiId()).isEqualTo("apiId");
            assertThat(result.getRootId()).isEqualTo(api.getRootId().id());
            assertThat(result.getCategoryIds()).isEmpty();
        }

        @Test
        void should_map_portal_navigation_api_category_ids() {
            var api = PortalNavigationItemFixtures.anApi();
            var category1 = PortalCategoryId.random();
            var category2 = PortalCategoryId.random();
            api.update(PortalNavigationItemFixtures.anUpdatePortalNavigationApi(List.of(category1, category2)));

            var result = mapper.map(api);

            assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationApi.class);
            assertThat(result.getCategoryIds()).containsExactly(category1.id(), category2.id());
        }

        @Test
        void should_map_portal_navigation_api_product() {
            var category1 = PortalCategoryId.random();
            var category2 = PortalCategoryId.random();
            var apiProduct = PortalNavigationItemFixtures.anApiProduct().toBuilder().categoryIds(List.of(category1, category2)).build();

            var result = mapper.map(apiProduct);

            assertThat(result).isInstanceOf(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationApiProduct.class);
            assertThat(result.getId()).isEqualTo(UUID.fromString(PortalNavigationItemFixtures.API_PRODUCT_ID));
            assertThat(result.getType()).isEqualTo(PortalNavigationItemType.API_PRODUCT);
            assertThat(result.getApiProductId()).isEqualTo(UUID.fromString(apiProduct.getApiProductId()));
            assertThat(result.getCategoryIds()).containsExactly(category1.id(), category2.id());
            assertThat(result.getRootId()).isEqualTo(apiProduct.getRootId().id());
        }

        @Test
        void should_map_list_of_portal_navigation_items() {
            var items = PortalNavigationItemFixtures.sampleNavigationItems();

            var result = mapper.map(items);

            assertThat(result).hasSize(13);
            // Check that all items are mapped correctly
            assertThat(
                result
                    .stream()
                    .map(i -> (BasePortalNavigationItem) i.getActualInstance())
                    .map(BasePortalNavigationItem::getId)
            ).containsExactlyInAnyOrder(
                PortalNavigationItemFixtures.SAMPLE_NAVIGATION_ITEMS_IDS.stream().map(UUID::fromString).toArray(UUID[]::new)
            );
        }

        @Test
        void should_map_portal_navigation_page_source() {
            var page = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE_ID, "My Page", null)
                .toBuilder()
                .source(
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource.builder()
                        .sourceType("github-fetcher")
                        .sourceConfiguration("{\"repository\":\"docs\"}")
                        .useAutoFetch(true)
                        .fetchCron("0 */10 * * * *")
                        .lastFetchedAt(Instant.parse("2026-07-17T10:00:00Z"))
                        .lastFetchAttemptAt(Instant.parse("2026-07-17T11:00:00Z"))
                        .lastFetchError("boom")
                        .build()
                )
                .build();

            var result = mapper.map(page);

            var source = result.getSource();
            assertThat(source).isNotNull();
            assertThat(source.getType()).isEqualTo("github-fetcher");
            assertThat(source.getConfiguration()).isEqualTo(Map.of("repository", "docs"));
            assertThat(source.getUseAutoFetch()).isTrue();
            assertThat(source.getFetchCron()).isEqualTo("0 */10 * * * *");
            assertThat(source.getLastFetchedAt()).isEqualTo(OffsetDateTime.parse("2026-07-17T10:00:00Z"));
            assertThat(source.getLastFetchAttemptAt()).isEqualTo(OffsetDateTime.parse("2026-07-17T11:00:00Z"));
            assertThat(source.getLastFetchError()).isEqualTo("boom");
        }

        @Test
        void should_map_portal_navigation_page_without_source() {
            var page = PortalNavigationItemFixtures.aPage(PortalNavigationItemFixtures.PAGE_ID, "My Page", null);

            var result = mapper.map(page);

            assertThat(result.getSource()).isNull();
        }
    }

    @Nested
    class ResourceToDomain {

        @Test
        void should_map_create_portal_navigation_page() {
            final var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage();

            var result = mapper.map(page);

            assertThat(result).isInstanceOf(CreatePortalNavigationItem.class);
            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.PAGE);
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo(page.getTitle());
            assertThat(result.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(1);
            assertThat(result.getParentId().id()).isEqualTo(page.getParentId());
            assertThat(result.getPortalPageContentId().id()).isEqualTo(((CreatePortalNavigationPage) page).getPortalPageContentId());
        }

        @Test
        void should_map_content_type_from_create_portal_navigation_page() {
            final var page = (CreatePortalNavigationPage) PortalNavigationItemsFixtures.aCreatePortalNavigationPage();
            page.setContentType(PortalPageContentType.OPENAPI);

            var result = mapper.map(page);

            assertThat(result).isInstanceOf(CreatePortalNavigationItem.class);
            assertThat(result.getContentType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalPageContentType.OPENAPI);
        }

        @Test
        void should_map_create_portal_navigation_folder() {
            final var folder = PortalNavigationItemsFixtures.aCreatePortalNavigationFolder();

            var result = mapper.map(folder);

            assertThat(result).isInstanceOf(CreatePortalNavigationItem.class);
            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.FOLDER);
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo(folder.getTitle());
            assertThat(result.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(2);
            assertThat(result.getParentId().id()).isEqualTo(folder.getParentId());
        }

        @Test
        void should_map_create_portal_navigation_link() {
            final var link = PortalNavigationItemsFixtures.aCreatePortalNavigationLink();

            var result = mapper.map(link);

            assertThat(result).isInstanceOf(CreatePortalNavigationItem.class);
            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.LINK);
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTitle()).isEqualTo(link.getTitle());
            assertThat(result.getArea()).isEqualTo(PortalArea.TOP_NAVBAR);
            assertThat(result.getOrder()).isEqualTo(3);
            assertThat(result.getParentId().id()).isEqualTo(link.getParentId());
            assertThat(result.getUrl()).isEqualTo(((CreatePortalNavigationLink) link).getUrl());
        }

        @Test
        void should_map_create_portal_navigation_api_category_ids() {
            final var api = (CreatePortalNavigationApi) PortalNavigationItemsFixtures.aCreatePortalNavigationApi();
            var category1 = UUID.randomUUID();
            var category2 = UUID.randomUUID();
            api.setCategoryIds(List.of(category1, category2));

            var result = mapper.map(api);

            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.API);
            assertThat(result.getCategoryIds()).containsExactly(new PortalCategoryId(category1), new PortalCategoryId(category2));
        }

        @Test
        void should_map_update_portal_navigation_api_category_ids() {
            final var api = (UpdatePortalNavigationApi) PortalNavigationItemsFixtures.anUpdatePortalNavigationApi();
            var category1 = UUID.randomUUID();
            api.setCategoryIds(List.of(category1));

            var result = mapper.map(api);

            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.API);
            assertThat(result.getCategoryIds()).containsExactly(new PortalCategoryId(category1));
        }

        @Test
        void should_map_update_portal_navigation_subscription_form() {
            var subscriptionForm = new UpdatePortalNavigationSubscriptionForm();
            subscriptionForm.type(io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemType.SUBSCRIPTION_FORM);
            subscriptionForm.title("Subscription Form");
            subscriptionForm.order(0);
            subscriptionForm.published(true);
            subscriptionForm.visibility(io.gravitee.rest.api.management.v2.rest.model.PortalVisibility.PUBLIC);

            var result = mapper.map(subscriptionForm);

            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.SUBSCRIPTION_FORM);
            assertThat(result.getTitle()).isEqualTo("Subscription Form");
            assertThat(result.getOrder()).isEqualTo(0);
            assertThat(result.getPublished()).isTrue();
            assertThat(result.getVisibility()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalVisibility.PUBLIC);
        }

        @Test
        void should_map_create_portal_navigation_api_product() {
            var category1 = UUID.randomUUID();
            var category2 = UUID.randomUUID();
            final var apiProduct = PortalNavigationItemsFixtures.aCreatePortalNavigationApiProduct(List.of(category1, category2));

            var result = mapper.map(apiProduct);

            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.API_PRODUCT);
            assertThat(result.getApiProductId()).isEqualTo(apiProduct.getApiProductId().toString());
            assertThat(result.getCategoryIds()).containsExactly(new PortalCategoryId(category1), new PortalCategoryId(category2));
        }

        @Test
        void should_map_update_portal_navigation_api_product_without_product_relinking_field() {
            var categoryId = UUID.randomUUID();
            final var apiProduct = PortalNavigationItemsFixtures.anUpdatePortalNavigationApiProduct(List.of(categoryId));

            var result = mapper.map(apiProduct);

            assertThat(result.getType()).isEqualTo(io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.API_PRODUCT);
            assertThat(result.getTitle()).isEqualTo("Updated API Product");
            assertThat(result.getPublished()).isFalse();
            assertThat(result.getCategoryIds()).containsExactly(new PortalCategoryId(categoryId));
        }

        @Test
        void should_map_bulk_create_portal_navigation_items() {
            final var page = PortalNavigationItemsFixtures.aCreatePortalNavigationPage();
            final var folder = PortalNavigationItemsFixtures.aCreatePortalNavigationFolder();
            final var link = PortalNavigationItemsFixtures.aCreatePortalNavigationLink();
            final var api = PortalNavigationItemsFixtures.aCreatePortalNavigationApi();
            var apiProductCategoryId = UUID.randomUUID();
            final var apiProduct = PortalNavigationItemsFixtures.aCreatePortalNavigationApiProduct(List.of(apiProductCategoryId));

            final var requestItems = java.util.List.of(page, folder, link, api, apiProduct);

            final var result = mapper.mapCreatePortalNavigationItems(requestItems);

            assertThat(result).hasSize(5);
            assertThat(result)
                .extracting(io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem::getType)
                .containsExactly(
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.PAGE,
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.FOLDER,
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.LINK,
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.API,
                    io.gravitee.apim.core.portal_page.model.PortalNavigationItemType.API_PRODUCT
                );
            assertThat(result.get(4).getCategoryIds()).containsExactly(new PortalCategoryId(apiProductCategoryId));
        }

        @Test
        void should_map_source_from_create_portal_navigation_page() {
            final var page = (CreatePortalNavigationPage) PortalNavigationItemsFixtures.aCreatePortalNavigationPage();
            page.setSource(
                new io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemSource()
                    .type("github-fetcher")
                    .configuration(new java.util.LinkedHashMap<>(Map.of("repository", "docs")))
                    .useAutoFetch(true)
                    .fetchCron("0 */10 * * * *")
            );

            var result = mapper.map(page);

            var source = result.getSource();
            assertThat(source).isNotNull();
            assertThat(source.getSourceType()).isEqualTo("github-fetcher");
            // pin the exact stored format: sameOrigin comparisons rely on it, a format change must fail here
            assertThat(source.getSourceConfiguration()).isEqualTo(
                """
                {
                  "repository" : "docs"
                }"""
            );
            assertThat(source.isUseAutoFetch()).isTrue();
            assertThat(source.getFetchCron()).isEqualTo("0 */10 * * * *");
            assertThat(source.getLastFetchedAt()).isNull();
            assertThat(source.getLastFetchError()).isNull();
        }

        @Test
        void should_drop_client_provided_server_managed_fetch_state() {
            final var page = (CreatePortalNavigationPage) PortalNavigationItemsFixtures.aCreatePortalNavigationPage();
            // the readOnly fields are only reachable through the @JsonCreator constructor, which is
            // exactly how Jackson would materialize them from a crafted request body
            page.setSource(
                new io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemSource(
                    OffsetDateTime.parse("2026-07-17T10:00:00Z"),
                    OffsetDateTime.parse("2026-07-17T11:00:00Z"),
                    "injected error",
                    true
                )
                    .type("github-fetcher")
                    .configuration(new java.util.LinkedHashMap<>(Map.of("repository", "docs")))
            );

            var result = mapper.map(page);

            var source = result.getSource();
            assertThat(source).isNotNull();
            assertThat(source.getLastFetchedAt()).isNull();
            // a forged attempt date would let a client push its page's next auto-fetch arbitrarily far out
            assertThat(source.getLastFetchAttemptAt()).isNull();
            assertThat(source.getLastFetchError()).isNull();
            // a forged marker would turn the folder's next fetch into a subtree-replacing re-import
            assertThat(source.isSubtreeImport()).isFalse();
        }

        @Test
        void should_map_source_from_update_portal_navigation_page() {
            final var update = new io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationPage()
                .source(
                    new io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItemSource()
                        .type("http-fetcher")
                        .configuration(new java.util.LinkedHashMap<>(Map.of("url", "https://example.com/doc.md")))
                )
                .type(PortalNavigationItemType.PAGE)
                .title("My Page")
                .order(1)
                .published(true)
                .visibility(io.gravitee.rest.api.management.v2.rest.model.PortalVisibility.PUBLIC);

            var result = mapper.map(update);

            var source = result.getSource();
            assertThat(source).isNotNull();
            assertThat(source.getSourceType()).isEqualTo("http-fetcher");
            // pin the exact stored format: sameOrigin comparisons rely on it, a format change must fail here
            assertThat(source.getSourceConfiguration()).isEqualTo(
                """
                {
                  "url" : "https://example.com/doc.md"
                }"""
            );
            assertThat(source.isUseAutoFetch()).isFalse();
        }
    }

    @Nested
    class ApisMetadataMapping {

        @Test
        void should_include_the_port_in_a_native_kafka_api_context_path() {
            var api = fixtures.core.model.ApiFixtures.aNativeApi();

            var summary = mapper.mapApiSummary(api);

            assertThat(summary.getContextPath()).isEqualTo("native.kafka:1000");
        }
    }
}
