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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.PortalCategoryCrudServiceInMemory;
import inmemory.PortalCategoryQueryServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_category.crud_service.PortalCategoryCrudService;
import io.gravitee.apim.core.portal_category.domain_service.PortalCategoryDomainService;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_category.use_case.CreatePortalCategoryUseCase;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Category;
import io.gravitee.repository.management.model.Environment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClassicCategoriesMigrationUpgraderTest {

    private static final Environment ANOTHER_ENVIRONMENT = Environment.builder()
        .id("ANOTHER_ENVIRONMENT")
        .hrids(List.of("another environment"))
        .name("another environment")
        .organizationId("ANOTHER_ORG")
        .build();

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    ApiRepository apiRepository;

    final PortalCategoryQueryServiceInMemory portalCategoryQueryService = new PortalCategoryQueryServiceInMemory();
    final PortalCategoryCrudServiceInMemory portalCategoryCrudService = new PortalCategoryCrudServiceInMemory();

    final List<PortalNavigationItem> portalNavigationItemsStorage = new ArrayList<>();
    final PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory(
        portalNavigationItemsStorage
    );
    final PortalNavigationItemsCrudServiceInMemory portalNavigationItemCrudService = new PortalNavigationItemsCrudServiceInMemory(
        portalNavigationItemsStorage
    );

    private ClassicCategoriesMigrationUpgrader upgrader;

    @BeforeEach
    void setUp() {
        var portalCategoryDomainService = new PortalCategoryDomainService(portalCategoryCrudService, portalCategoryQueryService);
        var createPortalCategoryUseCase = new CreatePortalCategoryUseCase(portalCategoryDomainService, portalCategoryCrudService);
        upgrader = new ClassicCategoriesMigrationUpgrader(
            environmentRepository,
            categoryRepository,
            apiRepository,
            portalCategoryQueryService,
            createPortalCategoryUseCase,
            portalNavigationItemsQueryService,
            portalNavigationItemCrudService
        );
        lenient().when(apiRepository.search(any(), any(ApiFieldFilter.class))).thenReturn(List.of());
    }

    @Test
    @SneakyThrows
    void should_do_nothing_when_there_is_no_environment() {
        when(environmentRepository.findAll()).thenReturn(Collections.emptySet());

        assertThat(upgrader.upgrade()).isTrue();

        verifyNoInteractions(categoryRepository);
    }

    @Test
    @SneakyThrows
    void should_return_false_when_something_wrong_happens() {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("this is a test exception"));

        final Executable throwing = () -> upgrader.upgrade();

        Exception exception = assertThrows(UpgraderException.class, throwing);
        assertThat(exception.getMessage()).contains("this is a test exception");
    }

    @Test
    @SneakyThrows
    void should_migrate_classic_categories_mapping_name_to_title_and_forcing_visible_true() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(categoryRepository.findAllByEnvironment(Environment.DEFAULT.getId())).thenReturn(
            Set.of(
                Category.builder()
                    .id("classic-category-id")
                    .environmentId(Environment.DEFAULT.getId())
                    .name("News")
                    .description("News category")
                    .hidden(true)
                    .picture("base64-picture")
                    .page("documentation-page-id")
                    .build()
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalCategoryCrudService.storage())
            .hasSize(1)
            .first()
            .satisfies(created -> {
                assertThat(created.getEnvironmentId()).isEqualTo(Environment.DEFAULT.getId());
                assertThat(created.getTitle()).isEqualTo("News");
                assertThat(created.getDescription()).isEqualTo("News category");
                assertThat(created.isVisible()).isTrue();
            });
    }

    @Test
    @SneakyThrows
    void should_reuse_existing_portal_category_by_title_instead_of_creating_a_duplicate() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT, ANOTHER_ENVIRONMENT));
        when(categoryRepository.findAllByEnvironment(Environment.DEFAULT.getId())).thenReturn(
            Set.of(Category.builder().id("classic-category-id").name("News").description("News category").build())
        );
        when(categoryRepository.findAllByEnvironment(ANOTHER_ENVIRONMENT.getId())).thenReturn(
            Set.of(Category.builder().id("another-classic-category-id").name("Finance").description("Finance category").build())
        );
        // A Portal Next category titled "News" already exists for DEFAULT - either this upgrader
        // already migrated it on a prior (partially failed) run, or an admin created it by hand.
        var existingPortalCategory = PortalCategory.create(Environment.DEFAULT.getId(), "News", "Pre-existing description", true);
        portalCategoryQueryService.initWith(List.of(existingPortalCategory));
        portalCategoryCrudService.initWith(List.of(existingPortalCategory));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalCategoryCrudService.storage()).hasSize(2);
        assertThat(portalCategoryCrudService.storage()).extracting(PortalCategory::getId).contains(existingPortalCategory.getId());
        assertThat(portalCategoryCrudService.storage()).extracting(PortalCategory::getTitle).containsExactlyInAnyOrder("News", "Finance");
    }

    @Test
    void test_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.CLASSIC_CATEGORIES_MIGRATION_UPGRADER);
    }

    @Nested
    class ApiCategoryAssignment {

        @Test
        @SneakyThrows
        void should_assign_new_portal_category_to_navigation_item_of_api_matched_by_classic_category_id() {
            when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
            var classicCategory = Category.builder()
                .id("classic-category-id")
                .key("classic-key")
                .environmentId(Environment.DEFAULT.getId())
                .name("News")
                .description("News category")
                .build();
            when(categoryRepository.findAllByEnvironment(Environment.DEFAULT.getId())).thenReturn(Set.of(classicCategory));
            when(apiRepository.search(any(), any(ApiFieldFilter.class))).thenReturn(
                List.of(
                    Api.builder().id("api-1").environmentId(Environment.DEFAULT.getId()).categories(Set.of("classic-category-id")).build()
                )
            );
            var existingCategoryId = PortalCategoryId.random();
            var navApi = PortalNavigationItemFixtures.anApi("api-1", PortalVisibility.PUBLIC, new ArrayList<>(List.of(existingCategoryId)));
            navApi.setEnvironmentId(Environment.DEFAULT.getId());
            portalNavigationItemsStorage.add(navApi);

            assertThat(upgrader.upgrade()).isTrue();

            var createdCategoryId = portalCategoryCrudService.storage().get(0).getId();
            assertThat(navApi.getCategoryIds()).containsExactlyInAnyOrder(existingCategoryId, createdCategoryId);
        }

        @Test
        @SneakyThrows
        void should_assign_new_portal_category_to_navigation_item_of_api_matched_by_classic_category_key() {
            when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
            var classicCategory = Category.builder()
                .id("classic-category-id")
                .key("classic-key")
                .environmentId(Environment.DEFAULT.getId())
                .name("News")
                .description("News category")
                .build();
            when(categoryRepository.findAllByEnvironment(Environment.DEFAULT.getId())).thenReturn(Set.of(classicCategory));
            when(apiRepository.search(any(), any(ApiFieldFilter.class))).thenReturn(
                List.of(Api.builder().id("api-1").environmentId(Environment.DEFAULT.getId()).categories(Set.of("classic-key")).build())
            );
            var navApi = PortalNavigationItemFixtures.anApi("api-1", PortalVisibility.PUBLIC, new ArrayList<>());
            navApi.setEnvironmentId(Environment.DEFAULT.getId());
            portalNavigationItemsStorage.add(navApi);

            assertThat(upgrader.upgrade()).isTrue();

            var createdCategoryId = portalCategoryCrudService.storage().get(0).getId();
            assertThat(navApi.getCategoryIds()).containsExactly(createdCategoryId);
        }

        @Test
        @SneakyThrows
        void should_not_fail_when_no_navigation_item_exists_for_an_api_in_the_classic_category() {
            when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
            var classicCategory = Category.builder()
                .id("classic-category-id")
                .environmentId(Environment.DEFAULT.getId())
                .name("News")
                .description("News category")
                .build();
            when(categoryRepository.findAllByEnvironment(Environment.DEFAULT.getId())).thenReturn(Set.of(classicCategory));
            when(apiRepository.search(any(), any(ApiFieldFilter.class))).thenReturn(
                List.of(
                    Api.builder()
                        .id("api-without-nav-item")
                        .environmentId(Environment.DEFAULT.getId())
                        .categories(Set.of("classic-category-id"))
                        .build()
                )
            );

            assertThat(upgrader.upgrade()).isTrue();

            assertThat(portalCategoryCrudService.storage()).hasSize(1);
            assertThat(portalNavigationItemsStorage).isEmpty();
        }
    }

    @Nested
    class PartialFailureRecovery {

        @Test
        @SneakyThrows
        void should_return_false_and_let_a_retry_reconcile_without_recreating_already_migrated_categories() {
            when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
            var newsCategory = Category.builder()
                .id("cat-news")
                .environmentId(Environment.DEFAULT.getId())
                .name("News")
                .description("News category")
                .build();
            var financeCategory = Category.builder()
                .id("cat-finance")
                .environmentId(Environment.DEFAULT.getId())
                .name("Finance")
                .description("Finance category")
                .build();
            when(categoryRepository.findAllByEnvironment(Environment.DEFAULT.getId())).thenReturn(Set.of(newsCategory, financeCategory));

            // Simulates a technical failure creating exactly one of the two categories, regardless of
            // iteration order over the Set.
            var financeCreationShouldFail = new AtomicBoolean(true);
            PortalCategoryCrudService flakyCrudService = new PortalCategoryCrudService() {
                @Override
                public PortalCategory create(PortalCategory portalCategory) {
                    if (financeCreationShouldFail.get() && portalCategory.getTitle().equals("Finance")) {
                        throw new IllegalStateException("simulated technical failure");
                    }
                    return portalCategoryCrudService.create(portalCategory);
                }

                @Override
                public PortalCategory update(PortalCategory portalCategory) {
                    return portalCategoryCrudService.update(portalCategory);
                }

                @Override
                public void delete(PortalCategoryId id) {
                    portalCategoryCrudService.delete(id);
                }

                @Override
                public Optional<PortalCategory> get(PortalCategoryId id) {
                    return portalCategoryCrudService.get(id);
                }
            };
            var flakyDomainService = new PortalCategoryDomainService(flakyCrudService, portalCategoryQueryService);
            var flakyCreateUseCase = new CreatePortalCategoryUseCase(flakyDomainService, flakyCrudService);
            var flakyUpgrader = new ClassicCategoriesMigrationUpgrader(
                environmentRepository,
                categoryRepository,
                apiRepository,
                portalCategoryQueryService,
                flakyCreateUseCase,
                portalNavigationItemsQueryService,
                portalNavigationItemCrudService
            );

            assertThat(flakyUpgrader.upgrade()).isFalse();
            assertThat(portalCategoryCrudService.storage()).hasSize(1);
            assertThat(portalCategoryCrudService.storage().get(0).getTitle()).isEqualTo("News");

            // Simulates production: PortalCategoryQueryServiceImpl and PortalCategoryCrudServiceImpl
            // read/write the same repository, so the query side already reflects what got persisted
            // by the time a retry (e.g. the next node startup) runs.
            portalCategoryQueryService.initWith(portalCategoryCrudService.storage());
            financeCreationShouldFail.set(false);

            assertThat(flakyUpgrader.upgrade()).isTrue();

            assertThat(portalCategoryCrudService.storage()).hasSize(2);
            assertThat(portalCategoryCrudService.storage())
                .extracting(PortalCategory::getTitle)
                .containsExactlyInAnyOrder("News", "Finance");
        }
    }

    @Nested
    class Idempotency {

        @Test
        @SneakyThrows
        void should_not_create_duplicate_portal_categories_when_upgrade_runs_twice() {
            when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
            when(categoryRepository.findAllByEnvironment(Environment.DEFAULT.getId())).thenReturn(
                Set.of(Category.builder().id("classic-category-id").name("News").description("News category").build())
            );

            assertThat(upgrader.upgrade()).isTrue();
            assertThat(portalCategoryCrudService.storage()).hasSize(1);

            // Both fakes are independent in-memory stores, unlike PortalCategoryQueryServiceImpl and
            // PortalCategoryCrudServiceImpl in production, which read/write the same
            // PortalCategoryRepository. Sync them to accurately simulate that shared persistence
            // before the second run.
            portalCategoryQueryService.initWith(portalCategoryCrudService.storage());

            assertThat(upgrader.upgrade()).isTrue();

            assertThat(portalCategoryCrudService.storage()).hasSize(1);
        }

        @Test
        @SneakyThrows
        void should_not_duplicate_category_assignment_on_navigation_items_when_upgrade_runs_twice() {
            when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
            var classicCategory = Category.builder()
                .id("classic-category-id")
                .environmentId(Environment.DEFAULT.getId())
                .name("News")
                .description("News category")
                .build();
            when(categoryRepository.findAllByEnvironment(Environment.DEFAULT.getId())).thenReturn(Set.of(classicCategory));
            when(apiRepository.search(any(), any(ApiFieldFilter.class))).thenReturn(
                List.of(
                    Api.builder().id("api-1").environmentId(Environment.DEFAULT.getId()).categories(Set.of("classic-category-id")).build()
                )
            );
            var navApi = PortalNavigationItemFixtures.anApi("api-1", PortalVisibility.PUBLIC, new ArrayList<>());
            navApi.setEnvironmentId(Environment.DEFAULT.getId());
            portalNavigationItemsStorage.add(navApi);

            assertThat(upgrader.upgrade()).isTrue();
            assertThat(navApi.getCategoryIds()).hasSize(1);

            portalCategoryQueryService.initWith(portalCategoryCrudService.storage());

            assertThat(upgrader.upgrade()).isTrue();

            assertThat(navApi.getCategoryIds()).hasSize(1);
        }
    }
}
