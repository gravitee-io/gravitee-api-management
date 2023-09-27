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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiIdsCalculatorService;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiIdsCalculatorServiceImplTest {

    public static final String API_DB_ID = "api-db-id";
    private ApiIdsCalculatorService cut;

    @Mock
    private ApiService apiService;

    @Mock
    private PageService pageService;

    @Mock
    private PlanService planService;

    @BeforeEach
    void setUp() {
        cut = new ApiIdsCalculatorServiceImpl(apiService, pageService, planService);
    }

    @Test
    void should_fail_if_no_api_entity() {
        assertThatThrownBy(() -> cut.recalculateApiDefinitionIds(new ExecutionContext("default", "default"), new ExportApiEntity()))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("ApiEntity is mandatory");
    }

    @Nested
    class PlansAndPagesEmptyIds {

        @ParameterizedTest
        @MethodSource("provideEmptyOrNullPlansAndPage")
        void should_not_generate_ids_for_plans_and_pages_when_absent(Set<PlanEntity> plans, List<PageEntity> pages) {
            final ExportApiEntity toRecalculate = buildExportApiEntity("api-id", plans, pages);
            final ExportApiEntity result = cut.recalculateApiDefinitionIds(new ExecutionContext("default", "default"), toRecalculate);
            assertThat(result.getPlans()).isNullOrEmpty();
            assertThat(result.getPages()).isNullOrEmpty();
        }

        private static Stream<Arguments> provideEmptyOrNullPlansAndPage() {
            return Stream.of(arguments(null, null), arguments(Set.of(), null), arguments(null, List.of()), arguments(Set.of(), List.of()));
        }

        @ParameterizedTest
        @MethodSource("providePlansAndPagesWithoutId")
        void should_generate_ids_for_plans_and_pages_when_absent(Set<PlanEntity> plans, List<PageEntity> pages) {
            final ExportApiEntity toRecalculate = buildExportApiEntity("api-id", plans, pages);
            final ExportApiEntity result = cut.recalculateApiDefinitionIds(new ExecutionContext("default", "default"), toRecalculate);
            result
                .getPlans()
                .forEach(plan -> {
                    assertThat(plan.getId()).isNotEmpty();
                    assertIsUuid(plan.getId());
                });
            result
                .getPages()
                .forEach(page -> {
                    assertThat(page.getId()).isNotEmpty();
                    assertIsUuid(page.getId());
                });
        }

        private static Stream<Arguments> providePlansAndPagesWithoutId() {
            return Stream.of(
                arguments(Set.of(buildPlanEntity("", ""), buildPlanEntity(null, "")), List.of()),
                arguments(Set.of(), List.of(buildPageEntity("", ""), buildPageEntity(null, ""))),
                arguments(
                    Set.of(buildPlanEntity("", ""), buildPlanEntity(null, "")),
                    List.of(buildPageEntity("", ""), buildPageEntity(null, ""))
                )
            );
        }

        @ParameterizedTest
        @MethodSource("provideMixedPlansAndPagesWithAndWithoutIds")
        void should_generate_ids_only_for_plans_and_pages_whitout_one(Set<PlanEntity> plans, List<PageEntity> pages) {
            final ExportApiEntity toRecalculate = buildExportApiEntity("api-id", plans, pages);
            // Save original id by entity name
            final Map<String, String> plansIdByName = plans
                .stream()
                .filter(plan -> plan.getId() != null && !plan.getId().isEmpty())
                .collect(Collectors.toMap(PlanEntity::getName, PlanEntity::getId));
            final Map<String, String> pagesIdByName = pages
                .stream()
                .filter(page -> page.getId() != null && !page.getId().isEmpty())
                .collect(Collectors.toMap(PageEntity::getName, PageEntity::getId));

            final ExportApiEntity result = cut.recalculateApiDefinitionIds(new ExecutionContext("default", "default"), toRecalculate);
            result
                .getPlans()
                .forEach(plan -> {
                    assertThat(plan.getId()).isNotEmpty();
                    if (plansIdByName.containsKey(plan.getName())) {
                        // If map contains plan's name, then keep the original id
                        assertThat(plan.getId()).isEqualTo(plansIdByName.get(plan.getName()));
                    } else {
                        // else generate one
                        assertIsUuid(plan.getId());
                    }
                });
            result
                .getPages()
                .forEach(page -> {
                    assertThat(page.getId()).isNotEmpty();
                    if (pagesIdByName.containsKey(page.getName())) {
                        // If map contains page's name, then keep the original id
                        assertThat(page.getId()).isEqualTo(pagesIdByName.get(page.getName()));
                    } else {
                        // else generate one
                        assertIsUuid(page.getId());
                    }
                });
        }

        private static Stream<Arguments> provideMixedPlansAndPagesWithAndWithoutIds() {
            return Stream.of(
                arguments(Set.of(buildPlanEntity("keyless-plan-id", ""), buildPlanEntity(null, "")), List.of()),
                arguments(Set.of(buildPlanEntity("keyless-plan-id", ""), buildPlanEntity("apikey-plan-id", "")), List.of()),
                arguments(Set.of(), List.of(buildPageEntity("a-page-id", ""), buildPageEntity(null, ""))),
                arguments(Set.of(), List.of(buildPageEntity("a-page-id", ""), buildPageEntity("another-page-id", ""))),
                arguments(
                    Set.of(buildPlanEntity("", ""), buildPlanEntity(null, "")),
                    List.of(buildPageEntity("", ""), buildPageEntity(null, ""))
                ),
                arguments(
                    Set.of(buildPlanEntity("keyless-plan-id", ""), buildPlanEntity(null, "")),
                    List.of(buildPageEntity("a-page-id", ""), buildPageEntity("", ""))
                )
            );
        }
    }

    @Nested
    class ExistingApiForCrossId {

        @Test
        void should_recalculate_ids_from_cross_id() {
            final PlanEntity newPlanEntity = buildPlanEntity("new-plan-id", "new-plan-cross-id");
            newPlanEntity.setGeneralConditions("another-page-id");
            final Set<PlanEntity> plans = Set.of(
                buildPlanEntity("keyless-plan-id", "keyless-plan-cross-id"),
                buildPlanEntity("a-plan-id", "a-plan-cross-id"),
                newPlanEntity
            );
            final List<PageEntity> pages = List.of(
                buildPageEntity("a-page-id", "a-page-cross-id"),
                buildPageEntity("another-page-id", "another-cross-id"),
                buildPageEntity("a-child-id", "a-child-cross-id", "a-page-id")
            );
            final ExportApiEntity toRecalculate = buildExportApiEntity("", plans, pages);
            toRecalculate.getApiEntity().setCrossId("api-cross-id");
            // Save original id by entity name
            final Map<String, String> plansIdByName = plans
                .stream()
                .filter(plan -> plan.getId() != null && !plan.getId().isEmpty())
                .collect(Collectors.toMap(PlanEntity::getName, PlanEntity::getId));
            final Map<String, String> pagesIdByName = pages
                .stream()
                .filter(page -> page.getId() != null && !page.getId().isEmpty())
                .collect(Collectors.toMap(PageEntity::getName, PageEntity::getId));

            final ExecutionContext executionContext = new ExecutionContext("default", "default");
            when(apiService.findByEnvironmentIdAndCrossId(executionContext.getEnvironmentId(), toRecalculate.getApiEntity().getCrossId()))
                .thenReturn(buildApiEntityDbResult());
            final PageEntity pageWithChangingCrossId = buildPageEntity("another-page-id", "changing-cross-id");
            when(pageService.findByApi(executionContext.getEnvironmentId(), API_DB_ID))
                .thenReturn(
                    List.of(
                        buildPageEntity("a-page-id", "a-page-cross-id"),
                        pageWithChangingCrossId,
                        buildPageEntity("a-child-id", "a-child-cross-id", "a-page-id")
                    )
                );
            when(planService.findByApi(API_DB_ID))
                .thenReturn(
                    Set.of(buildPlanEntity("keyless-plan-id", "keyless-plan-cross-id"), buildPlanEntity("a-plan-id", "a-plan-cross-id"))
                );

            final ExportApiEntity result = cut.recalculateApiDefinitionIds(executionContext, toRecalculate);

            assertThat(result.getApiEntity().getId()).isEqualTo(API_DB_ID);

            for (PageEntity page : result.getPages()) {
                assertThat(page.getId()).isNotEmpty();
                if (pagesIdByName.containsKey(page.getName()) && page.getName().equals(pageWithChangingCrossId.getName())) {
                    // If map contains page's name, then id must not have been recalculated
                    assertThat(page.getId()).isEqualTo(pagesIdByName.get(page.getName()));
                } else if (page.getName().equals(pageWithChangingCrossId.getName())) {
                    // else if the page is the one with another cross id, then page id should differ
                    assertThat(page.getId()).isNotEqualTo(pagesIdByName.get(pageWithChangingCrossId.getName()));
                }
                // one page of the data set has a parent id, verify the matching
                if (page.getParentId() != null) {
                    pagesIdByName.forEach((key, value) -> {
                        if (value.equals("a-page-id")) {
                            String pageName = key;
                            final Optional<PageEntity> parentPage = result
                                .getPages()
                                .stream()
                                .filter(p -> p.getName().equals(pageName))
                                .findFirst();
                            assertThat(parentPage).isNotEmpty();
                            assertThat(page.getParentId()).isEqualTo(parentPage.get().getId());
                        }
                    });
                }
            }

            // Verify ids has properly been recalculated when empty
            for (PlanEntity plan : result.getPlans()) {
                assertThat(plan.getId()).isNotEmpty();
                if (plansIdByName.containsKey(plan.getName()) && plan != newPlanEntity) {
                    // plan id should remain the same
                    assertThat(plan.getId()).isEqualTo(plansIdByName.get(plan.getName()));
                }
                // new plan entity should use the right page id for general conditions. Also, its id should be recalculated
                if (plan.getName().equals(newPlanEntity.getName())) {
                    assertIsUuid(plan.getId());
                    final Optional<PageEntity> pageCondition = result
                        .getPages()
                        .stream()
                        .filter(p -> p.getCrossId().equals("another-cross-id"))
                        .findFirst();
                    assertThat(pageCondition).isPresent();
                    assertThat(plan.getGeneralConditions()).isEqualTo(pageCondition.get().getId());
                }
            }

            verify(apiService).findByEnvironmentIdAndCrossId(any(), any());
            verify(pageService).findByApi(executionContext.getEnvironmentId(), API_DB_ID);
            verify(planService).findByApi(API_DB_ID);
        }

        private Optional<ApiEntity> buildApiEntityDbResult() {
            final ApiEntity apiEntity = new ApiEntity();
            apiEntity.setId(API_DB_ID);
            return Optional.of(apiEntity);
        }
    }

    @Nested
    class NoApiForCrossId {

        @Test
        void should_recalculate_ids_from_definition() {
            final Set<PlanEntity> plans = Set.of(buildPlanEntity("keyless-plan-id", ""), buildPlanEntity("an-id", ""));
            final List<PageEntity> pages = List.of(
                buildPageEntity("a-page-id", ""),
                buildPageEntity("", ""),
                buildPageEntity("a-child-id", "", "a-page-id")
            );
            final ExportApiEntity toRecalculate = buildExportApiEntity("", plans, pages);
            // Save original id by entity name
            final Map<String, String> plansIdByName = plans
                .stream()
                .filter(plan -> plan.getId() != null && !plan.getId().isEmpty())
                .collect(Collectors.toMap(PlanEntity::getName, PlanEntity::getId));
            final Map<String, String> pagesIdByName = pages
                .stream()
                .filter(page -> page.getId() != null && !page.getId().isEmpty())
                .collect(Collectors.toMap(PageEntity::getName, PageEntity::getId));

            final ExecutionContext executionContext = new ExecutionContext("default", "default");
            final ExportApiEntity result = cut.recalculateApiDefinitionIds(executionContext, toRecalculate);

            // When no crossId, do not search for the api
            verify(apiService, never()).findByEnvironmentIdAndCrossId(any(), any());

            assertIsUuid(result.getApiEntity().getId());

            // Verify ids has properly been recalculated when empty
            result
                .getPlans()
                .forEach(plan -> {
                    assertThat(plan.getId()).isNotEmpty();
                    if (plansIdByName.containsKey(plan.getName())) {
                        // If map contains plan's name, then id must have been recalculated
                        assertThat(plan.getId()).isNotEqualTo(plansIdByName.get(plan.getName()));
                    }
                    assertIsUuid(plan.getId());
                });
            result
                .getPages()
                .forEach(page -> {
                    assertThat(page.getId()).isNotEmpty();
                    if (pagesIdByName.containsKey(page.getName())) {
                        // If map contains page's name, then id must have been recalculated
                        assertThat(page.getId()).isNotEqualTo(pagesIdByName.get(page.getName()));
                    }
                    // one page of the data set has a parent id, verify the matching
                    if (page.getParentId() != null) {
                        pagesIdByName.forEach((key, value) -> {
                            if (value.equals("a-page-id")) {
                                String pageName = key;
                                final Optional<PageEntity> parentPage = result
                                    .getPages()
                                    .stream()
                                    .filter(p -> p.getName().equals(pageName))
                                    .findFirst();
                                assertThat(parentPage).isNotEmpty();
                                assertThat(page.getParentId()).isEqualTo(parentPage.get().getId());
                            }
                        });
                    }
                    assertIsUuid(page.getId());
                });

            // When recalculated from definition ids, no need to get pages and plans for the api
            verify(pageService, never()).findByApi(any(), any());
            verify(planService, never()).findByApi(any());
        }

        @Test
        void should_not_recalculate_ids_from_definition_if_k8s_origin() {
            final Set<PlanEntity> plans = Set.of(buildPlanEntity("keyless-plan-id", ""), buildPlanEntity("an-id", ""));
            final List<PageEntity> pages = List.of(buildPageEntity("a-page-id", ""), buildPageEntity("", ""));
            final ExportApiEntity toRecalculate = buildExportApiEntity("", plans, pages);
            // Save original id by entity name
            final Map<String, String> plansIdByName = plans
                .stream()
                .filter(plan -> plan.getId() != null && !plan.getId().isEmpty())
                .collect(Collectors.toMap(PlanEntity::getName, PlanEntity::getId));
            final Map<String, String> pagesIdByName = pages
                .stream()
                .filter(page -> page.getId() != null && !page.getId().isEmpty())
                .collect(Collectors.toMap(PageEntity::getName, PageEntity::getId));

            toRecalculate
                .getApiEntity()
                .setDefinitionContext(
                    new DefinitionContext(DefinitionContext.ORIGIN_KUBERNETES, DefinitionContext.MODE_API_DEFINITION_ONLY)
                );
            final ExecutionContext executionContext = new ExecutionContext("default", "default");
            final ExportApiEntity result = cut.recalculateApiDefinitionIds(executionContext, toRecalculate);

            // When no crossId, do not search for the api
            verify(apiService, never()).findByEnvironmentIdAndCrossId(any(), any());

            // Verify ids has properly been recalculated when empty
            result
                .getPlans()
                .forEach(plan -> {
                    assertThat(plan.getId()).isNotEmpty();
                    if (plansIdByName.containsKey(plan.getName())) {
                        // If map contains plan's name, then keep the original id
                        assertThat(plan.getId()).isEqualTo(plansIdByName.get(plan.getName()));
                    } else {
                        // else generate one
                        assertIsUuid(plan.getId());
                    }
                });
            result
                .getPages()
                .forEach(page -> {
                    assertThat(page.getId()).isNotEmpty();
                    if (pagesIdByName.containsKey(page.getName())) {
                        // If map contains page's name, then keep the original id
                        assertThat(page.getId()).isEqualTo(pagesIdByName.get(page.getName()));
                    } else {
                        // else generate one
                        assertIsUuid(page.getId());
                    }
                });

            // When recalculated from definition ids, no need to get pages and plans for the api
            verify(pageService, never()).findByApi(any(), any());
            verify(planService, never()).findByApi(any());
        }
    }

    private static ExportApiEntity buildExportApiEntity(String apiId, Set<PlanEntity> plans, List<PageEntity> pages) {
        final ExportApiEntity exportApiEntity = new ExportApiEntity();
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(apiId);
        exportApiEntity.setApiEntity(apiEntity);
        exportApiEntity.setPlans(plans);
        exportApiEntity.setPages(pages);
        return exportApiEntity;
    }

    private static PlanEntity buildPlanEntity(String id, String crossId) {
        final PlanEntity planEntity = new PlanEntity();
        planEntity.setId(id);
        planEntity.setName(generateRandomName());
        planEntity.setCrossId(crossId);
        return planEntity;
    }

    private static PageEntity buildPageEntity(String id, String crossId) {
        final PageEntity pageEntity = new PageEntity();
        pageEntity.setId(id);
        pageEntity.setName(generateRandomName());
        pageEntity.setCrossId(crossId);
        return pageEntity;
    }

    private static PageEntity buildPageEntity(String id, String crossId, String parentId) {
        final PageEntity pageEntity = buildPageEntity(id, crossId);
        pageEntity.setParentId(parentId);
        return pageEntity;
    }

    private static void assertIsUuid(String id) {
        assertDoesNotThrow(() -> UUID.fromString(id));
    }

    private static String generateRandomName() {
        return RandomStringUtils.random(32, true, true);
    }
}
