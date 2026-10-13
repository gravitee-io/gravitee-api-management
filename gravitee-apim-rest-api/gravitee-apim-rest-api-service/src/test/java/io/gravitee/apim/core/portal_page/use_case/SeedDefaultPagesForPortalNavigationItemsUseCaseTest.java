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

import static fixtures.core.model.PortalNavigationItemFixtures.API1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.API2_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.APIS_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.API_PRODUCT_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationDefaultPageDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemCreationExpansionDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.definition.model.v4.ApiType;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SeedDefaultPagesForPortalNavigationItemsUseCaseTest {

    private SeedDefaultPagesForPortalNavigationItemsUseCase useCase;
    private ApiCrudServiceInMemory apiCrudService;
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemsCrudService;
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;
    private PortalPageContentCrudServiceInMemory portalPageContentCrudService;

    @BeforeEach
    void setUp() {
        var storage = new ArrayList<PortalNavigationItem>();
        portalNavigationItemsCrudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        portalNavigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        portalPageContentCrudService = new PortalPageContentCrudServiceInMemory();

        apiCrudService = new ApiCrudServiceInMemory();
        apiCrudService.initWith(
            List.of(Api.builder().id("api-1").environmentId(ENV_ID).name("API 1").version("1.0.0").type(ApiType.PROXY).build())
        );

        portalNavigationItemsQueryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
        var apiProductQueryService = new ApiProductQueryServiceInMemory();
        var sourceDomainService = new PortalNavigationItemSourceDomainServiceInMemory();
        var pageContentQueryService = PortalPageContentQueryServiceInMemory.sharing(portalPageContentCrudService.storage());
        var apiOwnedNavigationDomainService = new ApiOwnedNavigationDomainService(
            portalNavigationItemsQueryService,
            portalNavigationItemsCrudService
        );

        useCase = new SeedDefaultPagesForPortalNavigationItemsUseCase(
            new PortalNavigationDefaultPageDomainService(
                portalNavigationItemsQueryService,
                new PortalNavigationItemDomainService(
                    portalNavigationItemsCrudService,
                    portalNavigationItemsQueryService,
                    portalPageContentCrudService,
                    pageContentQueryService,
                    apiCrudService,
                    sourceDomainService,
                    apiOwnedNavigationDomainService
                ),
                portalPageContentCrudService,
                apiCrudService,
                new PortalNavigationItemCreationExpansionDomainService(
                    apiProductQueryService,
                    apiCrudService,
                    portalNavigationItemsQueryService
                ),
                new PortalNavigationItemValidatorService(
                    portalNavigationItemsQueryService,
                    pageContentQueryService,
                    apiProductQueryService,
                    sourceDomainService,
                    apiCrudService
                ),
                apiOwnedNavigationDomainService
            )
        );
    }

    @Test
    void should_seed_default_overview_page_for_api() {
        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(PortalNavigationItemId.of(API1_ID)))
        );

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalNavigationItemsCrudService.storage())
            .filteredOn(PortalNavigationPage.class::isInstance)
            .filteredOn(item -> item.getReference().equals(new NavigationItemReference.ApiReference("api-1")))
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getTitle()).isEqualTo("Overview");
                assertThat(item.getPublished()).isFalse();
                assertThat(item.getParentId()).isNull();
                assertThat(item.getRootId()).isEqualTo(item.getId());
                assertThat(item.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
                assertThat(item.getOrder()).isZero();
            });
        assertThat(portalPageContentCrudService.storage())
            .singleElement()
            .isInstanceOfSatisfying(GraviteeMarkdownPageContent.class, content ->
                assertThat(content.getContent().value()).isEqualTo(loadTemplate("api-overview-page-content.md"))
            );
    }

    @ParameterizedTest
    @CsvSource(
        {
            "PAGE, false, true, PUBLIC",
            "LINK, false, true, PUBLIC",
            "PAGE, true, true, PUBLIC",
            "LINK, true, true, PUBLIC",
            "PAGE, false, false, PUBLIC",
            "LINK, true, false, PUBLIC",
            "PAGE, true, true, PRIVATE",
            "LINK, false, true, PRIVATE",
        }
    )
    void should_skip_seeding_when_api_owns_documentation(
        PortalNavigationItemType type,
        boolean nested,
        boolean published,
        PortalVisibility visibility
    ) {
        var owner = new NavigationItemReference.ApiReference("api-1");
        var folder = nested ? createFolder(null, owner) : null;
        var document = createDocument(type, folder, owner);
        document.setPublished(published);
        document.setVisibility(visibility);
        var previousItems = List.copyOf(portalNavigationItemsCrudService.storage());

        var output = seed(PortalNavigationItemId.of(API1_ID));

        assertThat(output.seededNavigationItemIds()).isEmpty();
        assertThat(portalNavigationItemsCrudService.storage()).containsExactlyElementsOf(previousItems);
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_seed_overview_when_api_owns_only_empty_folders() {
        var owner = new NavigationItemReference.ApiReference("api-1");
        var folder = createFolder(null, owner);
        folder.setTitle("Overview");
        folder.setSegment("overview");
        createFolder(folder, owner);

        var output = seed(PortalNavigationItemId.of(API1_ID));

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
        assertThat(portalNavigationItemsCrudService.storage())
            .filteredOn(PortalNavigationPage.class::isInstance)
            .filteredOn(item -> item.getReference().equals(owner))
            .singleElement()
            .satisfies(page -> {
                assertThat(page.getParentId()).isNull();
                assertThat(page.getOrder()).isZero();
            });
        assertThat(folder.getOrder()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({ "PAGE, false", "LINK, false", "PAGE, true", "LINK, true" })
    void should_skip_seeding_when_another_standalone_listing_has_legacy_documentation(PortalNavigationItemType type, boolean nested) {
        var listing = createApiListing(null);
        listing.changeOwner(new NavigationItemReference.PortalReference(PortalId.random()));
        var parent = nested ? createFolder(listing, NavigationItemReference.defaultReference()) : listing;
        createDocument(type, parent, NavigationItemReference.defaultReference());

        var output = seed(PortalNavigationItemId.of(API1_ID));

        assertThat(output.seededNavigationItemIds()).isEmpty();
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "api-2, env-id", "api-1, another-environment" })
    void should_ignore_documentation_owned_by_another_api_or_environment(String apiId, String environmentId) {
        var document = createDocument(PortalNavigationItemType.PAGE, null, new NavigationItemReference.ApiReference(apiId));
        document.setEnvironmentId(environmentId);

        var output = seed(PortalNavigationItemId.of(API1_ID));

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_not_follow_foreign_owned_folders_when_looking_for_documentation(boolean apiOwned) {
        var owner = apiOwned ? new NavigationItemReference.ApiReference("api-1") : NavigationItemReference.defaultReference();
        var parent = apiOwned ? createFolder(null, owner) : apiListing();
        var foreignFolder = createFolder(parent, new NavigationItemReference.ApiReference("api-2"));
        createDocument(PortalNavigationItemType.PAGE, foreignFolder, owner);

        var output = seed(PortalNavigationItemId.of(API1_ID));

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
    }

    @Test
    void should_seed_only_once_for_repeated_ids_requests_and_standalone_listings() {
        var firstListing = PortalNavigationItemId.of(API1_ID);
        var secondListing = createApiListing(null).getId();

        var first = seed(firstListing, firstListing, secondListing);
        var repeated = seed(secondListing, firstListing);

        assertThat(first.seededNavigationItemIds()).containsExactly(firstListing);
        assertThat(repeated.seededNavigationItemIds()).isEmpty();
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
    }

    @Test
    void should_preserve_private_listing_visibility_on_api_owned_overview() {
        apiListing().setVisibility(PortalVisibility.PRIVATE);

        var output = seed(PortalNavigationItemId.of(API1_ID));

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalNavigationItemsCrudService.storage())
            .filteredOn(PortalNavigationPage.class::isInstance)
            .filteredOn(item -> item.getReference().equals(new NavigationItemReference.ApiReference("api-1")))
            .singleElement()
            .satisfies(page -> {
                assertThat(page.getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
                assertThat(page.getPublished()).isFalse();
            });
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void should_seed_product_local_api_overview_independently_of_standalone_documentation(boolean nested) {
        createDocument(PortalNavigationItemType.PAGE, null, new NavigationItemReference.ApiReference("api-1"));
        var product = createProduct(null);
        var parent = nested ? createFolder(product, NavigationItemReference.defaultReference()) : product;
        var listing = createApiListing(parent);

        var output = seed(listing.getId());

        assertThat(output.seededNavigationItemIds()).containsExactly(listing.getId());
        assertThat(portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(ENV_ID, listing.getId()))
            .singleElement()
            .satisfies(page -> {
                assertThat(page).isInstanceOf(PortalNavigationPage.class);
                assertThat(page.getReference()).isEqualTo(NavigationItemReference.defaultReference());
                assertThat(page.getParentId()).isEqualTo(listing.getId());
                assertThat(page.getRootId()).isEqualTo(product.getId());
                assertThat(page.getPublished()).isFalse();
            });
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(value = PortalNavigationItemType.class, names = { "PAGE", "LINK" })
    void should_skip_seeding_when_api_in_product_has_nested_local_documentation(PortalNavigationItemType type) {
        var product = createProduct(null);
        var listing = createApiListing(product);
        var folder = createFolder(listing, NavigationItemReference.defaultReference());
        createDocument(type, folder, NavigationItemReference.defaultReference());

        var output = seed(listing.getId());

        assertThat(output.seededNavigationItemIds()).isEmpty();
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_not_use_product_local_documentation_for_a_standalone_listing_or_another_product() {
        var firstProduct = createProduct(null);
        var firstListing = createApiListing(firstProduct);
        createDocument(PortalNavigationItemType.PAGE, firstListing, NavigationItemReference.defaultReference());
        var secondProduct = createProduct(null);
        var secondListing = createApiListing(secondProduct);

        var output = seed(PortalNavigationItemId.of(API1_ID), secondListing.getId());

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID), secondListing.getId());
        assertThat(portalPageContentCrudService.storage()).hasSize(2);
        assertThat(portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(ENV_ID, secondListing.getId()))
            .singleElement()
            .satisfies(page -> assertThat(page.getReference()).isEqualTo(NavigationItemReference.defaultReference()));
    }

    @ParameterizedTest
    @EnumSource(value = PortalNavigationItemType.class, names = { "API", "API_PRODUCT" })
    void should_not_treat_nested_api_or_product_documentation_as_its_ancestors_documentation(PortalNavigationItemType type) {
        var product = createProduct(null);
        var folder = createFolder(product, NavigationItemReference.defaultReference());
        var nestedContainer = type == PortalNavigationItemType.API ? createApiListing(folder) : createProduct(folder);
        createDocument(PortalNavigationItemType.PAGE, nestedContainer, NavigationItemReference.defaultReference());

        var output = seed(product.getId());

        assertThat(output.seededNavigationItemIds()).containsExactly(product.getId());
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
    }

    @Test
    void should_validate_before_persisting_content_and_continue_with_other_items() {
        apiCrudService.initWith(
            List.of(
                Api.builder().id("api-1").environmentId("another-environment").type(ApiType.PROXY).build(),
                Api.builder().id("api-2-id").environmentId(ENV_ID).type(ApiType.PROXY).build()
            )
        );
        var previousSize = portalNavigationItemsCrudService.storage().size();

        var output = seed(PortalNavigationItemId.of(API1_ID), PortalNavigationItemId.of(API2_ID));

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API2_ID));
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
        assertThat(portalNavigationItemsCrudService.storage()).hasSize(previousSize + 1);
        assertThat(portalNavigationItemsCrudService.storage())
            .filteredOn(PortalNavigationPage.class::isInstance)
            .filteredOn(item -> item.getReference().equals(new NavigationItemReference.ApiReference("api-1")))
            .isEmpty();
    }

    @Test
    void should_seed_default_overview_page_before_generated_apis_for_api_product() {
        var parent = (PortalNavigationFolder) portalNavigationItemsQueryService.findByIdAndEnvironmentId(
            ENV_ID,
            PortalNavigationItemId.of(APIS_ID)
        );
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            API_PRODUCT_ID,
            "My API Product",
            parent.getId(),
            "00000000-0000-0000-0000-000000000019"
        )
            .toBuilder()
            .visibility(PortalVisibility.PRIVATE)
            .build();
        apiProduct.updateParent(parent);
        var generatedApi = PortalNavigationItemFixtures.anApi(
            "00000000-0000-0000-0000-000000000020",
            "Generated API",
            apiProduct.getId(),
            "api-1"
        );
        generatedApi.setVisibility(PortalVisibility.PRIVATE);
        generatedApi.setOrder(0);
        generatedApi.updateParent(apiProduct);
        portalNavigationItemsCrudService.create(apiProduct);
        portalNavigationItemsCrudService.create(generatedApi);

        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(apiProduct.getId()))
        );

        assertThat(output.seededNavigationItemIds()).containsExactly(apiProduct.getId());
        assertThat(
            portalNavigationItemsQueryService
                .findByParentIdAndEnvironmentId(ENV_ID, apiProduct.getId())
                .stream()
                .sorted(Comparator.comparing(PortalNavigationItem::getOrder))
                .toList()
        )
            .extracting(
                PortalNavigationItem::getTitle,
                PortalNavigationItem::getOrder,
                PortalNavigationItem::getPublished,
                PortalNavigationItem::getVisibility
            )
            .containsExactly(
                tuple("Overview", 0, false, PortalVisibility.PRIVATE),
                tuple("Generated API", 1, true, PortalVisibility.PRIVATE)
            );
        assertThat(portalPageContentCrudService.storage())
            .singleElement()
            .isInstanceOfSatisfying(GraviteeMarkdownPageContent.class, content ->
                assertThat(content.getContent().value()).isEqualTo(loadTemplate("api-product-overview-page-content.md"))
            );
    }

    @Test
    void should_skip_seeding_when_api_navigation_item_already_has_a_child_page() {
        var apiNavigationItem = (PortalNavigationApi) portalNavigationItemsQueryService.findByIdAndEnvironmentId(
            ENV_ID,
            PortalNavigationItemId.of(API1_ID)
        );
        var existingPage = PortalNavigationItemFixtures.aPage(
            "00000000-0000-0000-0000-000000000111",
            "Existing page",
            apiNavigationItem.getId(),
            PortalPageContentId.random()
        );
        existingPage.updateParent(apiNavigationItem);
        portalNavigationItemsCrudService.create(existingPage);

        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(PortalNavigationItemId.of(API1_ID)))
        );

        assertThat(output.seededNavigationItemIds()).isEmpty();
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_skip_seeding_when_api_product_already_has_a_child_page() {
        var parent = (PortalNavigationFolder) portalNavigationItemsQueryService.findByIdAndEnvironmentId(
            ENV_ID,
            PortalNavigationItemId.of(APIS_ID)
        );
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            API_PRODUCT_ID,
            "My API Product",
            parent.getId(),
            "00000000-0000-0000-0000-000000000019"
        );
        apiProduct.updateParent(parent);
        var existingPage = PortalNavigationItemFixtures.aPage(
            "00000000-0000-0000-0000-000000000020",
            "Existing page",
            apiProduct.getId(),
            PortalPageContentId.random()
        );
        existingPage.updateParent(apiProduct);
        portalNavigationItemsCrudService.create(apiProduct);
        portalNavigationItemsCrudService.create(existingPage);

        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(apiProduct.getId()))
        );

        assertThat(output.seededNavigationItemIds()).isEmpty();
        assertThat(portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(ENV_ID, apiProduct.getId())).containsExactly(
            existingPage
        );
        assertThat(portalPageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_seed_mcp_proxy_overview_page_for_mcp_proxy_api() {
        apiCrudService.initWith(
            List.of(Api.builder().id("api-1").environmentId(ENV_ID).name("MCP API").version("1.0.0").type(ApiType.MCP_PROXY).build())
        );

        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(PortalNavigationItemId.of(API1_ID)))
        );

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalPageContentCrudService.storage())
            .singleElement()
            .isInstanceOfSatisfying(GraviteeMarkdownPageContent.class, content ->
                assertThat(content.getContent().value()).isEqualTo(loadTemplate("api-overview-mcp-proxy-page-content.md"))
            );
    }

    @Test
    void should_skip_navigation_items_that_do_not_support_default_pages() {
        var output = useCase.execute(
            new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(
                ORG_ID,
                ENV_ID,
                List.of(PortalNavigationItemId.of(APIS_ID), PortalNavigationItemId.of(API1_ID))
            )
        );

        assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
        assertThat(portalPageContentCrudService.storage()).hasSize(1);
    }

    @Test
    void should_load_templates_regardless_of_thread_context_classloader() {
        // Given: a context classloader that cannot see this module's classpath resources at all,
        // reproducing the production failure mode (a Vert.x/plugin-owned thread whose context
        // classloader isn't the one that loaded gravitee-apim-rest-api-service).
        final var originalClassLoader = Thread.currentThread().getContextClassLoader();
        final var isolatedClassLoader = new URLClassLoader(new URL[0], null);
        Thread.currentThread().setContextClassLoader(isolatedClassLoader);

        try {
            // When
            var output = useCase.execute(
                new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(PortalNavigationItemId.of(API1_ID)))
            );

            // Then
            assertThat(output.seededNavigationItemIds()).containsExactly(PortalNavigationItemId.of(API1_ID));
            assertThat(portalPageContentCrudService.storage()).hasSize(1);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private SeedDefaultPagesForPortalNavigationItemsUseCase.Output seed(PortalNavigationItemId... ids) {
        return useCase.execute(new SeedDefaultPagesForPortalNavigationItemsUseCase.Input(ORG_ID, ENV_ID, List.of(ids)));
    }

    private PortalNavigationApi apiListing() {
        return (PortalNavigationApi) portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID));
    }

    private PortalNavigationApi createApiListing(PortalNavigationItemContainer parent) {
        var item = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "API listing", null, "api-1");
        item.attachTo(parent);
        return (PortalNavigationApi) portalNavigationItemsCrudService.create(item);
    }

    private PortalNavigationApiProduct createProduct(PortalNavigationItemContainer parent) {
        var item = PortalNavigationItemFixtures.anApiProduct(PortalNavigationItemId.random().json(), "API product", null, "product-id");
        item.attachTo(parent);
        return (PortalNavigationApiProduct) portalNavigationItemsCrudService.create(item);
    }

    private PortalNavigationFolder createFolder(PortalNavigationItemContainer parent, NavigationItemReference owner) {
        var item = PortalNavigationItemFixtures.aFolder("Documentation");
        item.attachTo(parent);
        item.changeOwner(owner);
        return (PortalNavigationFolder) portalNavigationItemsCrudService.create(item);
    }

    private PortalNavigationItem createDocument(
        PortalNavigationItemType type,
        PortalNavigationItemContainer parent,
        NavigationItemReference owner
    ) {
        var id = PortalNavigationItemId.random().json();
        var item = type == PortalNavigationItemType.PAGE
            ? PortalNavigationItemFixtures.aPage(id, "Existing documentation", null)
            : PortalNavigationItemFixtures.aLink(id, "Existing documentation", null);
        item.attachTo(parent);
        item.changeOwner(owner);
        return portalNavigationItemsCrudService.create(item);
    }

    private String loadTemplate(String templatePath) {
        try (
            var inputStream = SeedDefaultPagesForPortalNavigationItemsUseCaseTest.class.getClassLoader().getResourceAsStream(
                String.format("templates/%s", templatePath)
            )
        ) {
            assertThat(inputStream).isNotNull();
            return new String(inputStream.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load template " + templatePath, e);
        }
    }
}
