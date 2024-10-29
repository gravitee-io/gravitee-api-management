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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import fixtures.core.model.PlanFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.rest.api.model.context.OriginContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiIdsCalculatorDomainServiceTest {

    private static final String API_DB_ID = "api-db-id";
    private static final String ENVIRONMENT_ID = "default";
    public static final String API_CROSS_ID = "api-cross-id";
    private static final Page PAGE_WITH_CHANGING_CROSS_ID = buildPage("another-page-id", "changing-cross-id");
    private ApiIdsCalculatorDomainService cut;

    ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    PageQueryServiceInMemory pageQueryServiceInMemory = new PageQueryServiceInMemory();
    PlanQueryServiceInMemory planQueryServiceInMemory = new PlanQueryServiceInMemory();

    @BeforeEach
    void setUp() {
        cut = new ApiIdsCalculatorDomainService(apiQueryService, pageQueryServiceInMemory, planQueryServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiQueryService.reset();
        pageQueryServiceInMemory.reset();
        planQueryServiceInMemory.reset();
    }

    @Test
    void should_fail_if_no_api_entity() {
        assertThatThrownBy(() -> cut.recalculateApiDefinitionIds(ENVIRONMENT_ID, ImportDefinition.builder().build()))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Api is mandatory");
    }

    @Nested
    class PlansAndPagesEmptyIds {

        @ParameterizedTest
        @MethodSource("provideEmptyOrNullPlansAndPage")
        void should_not_generate_ids_for_plans_and_pages_when_absent(Set<PlanWithFlows> plans, List<Page> pages) {
            final ImportDefinition toRecalculate = buildImportDefinition("api-id", plans, pages);
            final ImportDefinition result = cut.recalculateApiDefinitionIds(ENVIRONMENT_ID, toRecalculate);
            assertThat(result.getPlans()).isNullOrEmpty();
            assertThat(result.getPages()).isNullOrEmpty();
        }

        private static Stream<Arguments> provideEmptyOrNullPlansAndPage() {
            return Stream.of(arguments(null, null), arguments(Set.of(), null), arguments(null, List.of()), arguments(Set.of(), List.of()));
        }

        @ParameterizedTest
        @MethodSource("providePlansAndPagesWithoutId")
        void should_generate_ids_for_plans_and_pages_when_absent(Set<PlanWithFlows> plans, List<Page> pages) {
            final ImportDefinition toRecalculate = buildImportDefinition("api-id", plans, pages);
            final ImportDefinition result = cut.recalculateApiDefinitionIds(ENVIRONMENT_ID, toRecalculate);
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
                arguments(Set.of(buildPlanWithFlows("", ""), buildPlanWithFlows(null, "")), List.of()),
                arguments(Set.of(), List.of(buildPage("", ""), buildPage(null, ""))),
                arguments(Set.of(buildPlanWithFlows("", ""), buildPlanWithFlows(null, "")), List.of(buildPage("", ""), buildPage(null, "")))
            );
        }

        @ParameterizedTest
        @MethodSource("provideMixedPlansAndPagesWithAndWithoutIds")
        void should_generate_ids_only_for_plans_and_pages_without_one(Set<PlanWithFlows> plans, List<Page> pages) {
            final ImportDefinition toRecalculate = buildImportDefinition("api-id", plans, pages);
            // Save original id by entity name
            final Map<String, String> plansIdByName = plans
                .stream()
                .filter(plan -> plan.getId() != null && !plan.getId().isEmpty())
                .collect(Collectors.toMap(PlanWithFlows::getName, PlanWithFlows::getId));
            final Map<String, String> pagesIdByName = pages
                .stream()
                .filter(page -> page.getId() != null && !page.getId().isEmpty())
                .collect(Collectors.toMap(Page::getName, Page::getId));

            final ImportDefinition result = cut.recalculateApiDefinitionIds(ENVIRONMENT_ID, toRecalculate);
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
                arguments(Set.of(buildPlanWithFlows("keyless-plan-id", ""), buildPlanWithFlows(null, "")), List.of()),
                arguments(Set.of(buildPlanWithFlows("keyless-plan-id", ""), buildPlanWithFlows("apikey-plan-id", "")), List.of()),
                arguments(Set.of(), List.of(buildPage("a-page-id", ""), buildPage(null, ""))),
                arguments(Set.of(), List.of(buildPage("a-page-id", ""), buildPage("another-page-id", ""))),
                arguments(
                    Set.of(buildPlanWithFlows("", ""), buildPlanWithFlows(null, "")),
                    List.of(buildPage("", ""), buildPage(null, ""))
                ),
                arguments(
                    Set.of(buildPlanWithFlows("keyless-plan-id", ""), buildPlanWithFlows(null, "")),
                    List.of(buildPage("a-page-id", ""), buildPage("", ""))
                )
            );
        }
    }

    @Nested
    class ExistingApiForCrossId {

        @BeforeEach
        void setUp() {
            apiQueryService.initWith(List.of(Api.builder().id(API_DB_ID).crossId(API_CROSS_ID).environmentId(ENVIRONMENT_ID).build()));
            pageQueryServiceInMemory.initWith(
                List.of(
                    buildPage("a-page-id", "a-page-cross-id").toBuilder().referenceId(API_DB_ID).build(),
                    PAGE_WITH_CHANGING_CROSS_ID.toBuilder().referenceId(API_DB_ID).build(),
                    buildPage("a-child-id", "a-child-cross-id", "a-page-id").toBuilder().referenceId(API_DB_ID).build()
                )
            );
            planQueryServiceInMemory.initWith(
                List.of(
                    buildPlanWithFlows("keyless-plan-id", "keyless-plan-cross-id").toBuilder().apiId(API_DB_ID).build(),
                    buildPlanWithFlows("a-plan-id", "a-plan-cross-id").toBuilder().apiId(API_DB_ID).build()
                )
            );
        }

        @Test
        void should_recalculate_ids_from_cross_id() {
            final PlanWithFlows newPlan = buildPlanWithFlows("new-plan-id", "new-plan-cross-id");
            newPlan.setGeneralConditions("another-page-id");
            final Set<PlanWithFlows> plans = Set.of(
                buildPlanWithFlows("keyless-plan-id", "keyless-plan-cross-id"),
                buildPlanWithFlows("a-plan-id", "a-plan-cross-id"),
                newPlan
            );
            final List<Page> pages = List.of(
                buildPage("a-page-id", "a-page-cross-id"),
                buildPage("another-page-id", "another-cross-id"),
                buildPage("a-child-id", "a-child-cross-id", "a-page-id")
            );
            final ImportDefinition toRecalculate = buildImportDefinition("", plans, pages);
            toRecalculate.getApiExport().setCrossId(API_CROSS_ID);
            // Save original id by entity name
            final Map<String, String> plansIdByName = plans
                .stream()
                .filter(plan -> plan.getId() != null && !plan.getId().isEmpty())
                .collect(Collectors.toMap(PlanWithFlows::getName, PlanWithFlows::getId));
            final Map<String, String> pagesIdByName = pages
                .stream()
                .filter(page -> page.getId() != null && !page.getId().isEmpty())
                .collect(Collectors.toMap(Page::getName, Page::getId));

            final ImportDefinition result = cut.recalculateApiDefinitionIds(ENVIRONMENT_ID, toRecalculate);

            assertThat(result.getApiExport().getId()).isEqualTo(API_DB_ID);

            for (Page page : result.getPages()) {
                assertThat(page.getId()).isNotEmpty();
                if (pagesIdByName.containsKey(page.getName()) && page.getName().equals(PAGE_WITH_CHANGING_CROSS_ID.getName())) {
                    // If map contains page's name, then id must not have been recalculated
                    assertThat(page.getId()).isEqualTo(pagesIdByName.get(page.getName()));
                } else if (page.getName().equals(PAGE_WITH_CHANGING_CROSS_ID.getName())) {
                    // else if the page is the one with another cross id, then page id should differ
                    assertThat(page.getId()).isNotEqualTo(pagesIdByName.get(PAGE_WITH_CHANGING_CROSS_ID.getName()));
                }
                // one page of the data set has a parent id, verify the matching
                if (page.getParentId() != null) {
                    pagesIdByName.forEach((key, value) -> {
                        if (value.equals("a-page-id")) {
                            String pageName = key;
                            final Optional<Page> parentPage = result
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
            for (PlanWithFlows plan : result.getPlans()) {
                assertThat(plan.getId()).isNotEmpty();
                if (plansIdByName.containsKey(plan.getName()) && plan != newPlan) {
                    // plan id should remain the same
                    assertThat(plan.getId()).isEqualTo(plansIdByName.get(plan.getName()));
                }
                // new plan entity should use the right page id for general conditions. Also, its id should be recalculated
                if (plan.getName().equals(newPlan.getName())) {
                    assertIsUuid(plan.getId());
                    final Optional<Page> pageCondition = result
                        .getPages()
                        .stream()
                        .filter(p -> p.getCrossId().equals("another-cross-id"))
                        .findFirst();
                    assertThat(pageCondition).isPresent();
                    assertThat(plan.getGeneralConditions()).isEqualTo(pageCondition.get().getId());
                }
            }
        }
    }

    @Nested
    class NoApiForCrossId {

        @Test
        void should_recalculate_ids_from_definition() {
            final Set<PlanWithFlows> plans = Set.of(buildPlanWithFlows("keyless-plan-id", ""), buildPlanWithFlows("an-id", ""));
            final List<Page> pages = List.of(buildPage("a-page-id", ""), buildPage("", ""), buildPage("a-child-id", "", "a-page-id"));
            final ImportDefinition toRecalculate = buildImportDefinition("", plans, pages);
            // Save original id by entity name
            final Map<String, String> plansIdByName = plans
                .stream()
                .filter(plan -> plan.getId() != null && !plan.getId().isEmpty())
                .collect(Collectors.toMap(PlanWithFlows::getName, PlanWithFlows::getId));
            final Map<String, String> pagesIdByName = pages
                .stream()
                .filter(page -> page.getId() != null && !page.getId().isEmpty())
                .collect(Collectors.toMap(Page::getName, Page::getId));

            final ImportDefinition result = cut.recalculateApiDefinitionIds(ENVIRONMENT_ID, toRecalculate);

            assertIsUuid(result.getApiExport().getId());

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
                                final Optional<Page> parentPage = result
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
        }

        @Test
        void should_not_recalculate_ids_from_definition_if_k8s_origin() {
            final Set<PlanWithFlows> plans = Set.of(buildPlanWithFlows("keyless-plan-id", ""), buildPlanWithFlows("an-id", ""));
            final List<Page> pages = List.of(buildPage("a-page-id", ""), buildPage("", ""));
            final ImportDefinition toRecalculate = buildImportDefinition("", plans, pages);
            // Save original id by entity name
            final Map<String, String> plansIdByName = plans
                .stream()
                .filter(plan -> plan.getId() != null && !plan.getId().isEmpty())
                .collect(Collectors.toMap(PlanWithFlows::getName, PlanWithFlows::getId));
            final Map<String, String> pagesIdByName = pages
                .stream()
                .filter(page -> page.getId() != null && !page.getId().isEmpty())
                .collect(Collectors.toMap(Page::getName, Page::getId));

            toRecalculate.getApiExport().setOriginContext(new OriginContext.Kubernetes(OriginContext.Kubernetes.Mode.FULLY_MANAGED));
            final ImportDefinition result = cut.recalculateApiDefinitionIds(ENVIRONMENT_ID, toRecalculate);

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
        }
    }

    private static ImportDefinition buildImportDefinition(String apiId, Set<PlanWithFlows> plans, List<Page> pages) {
        return ImportDefinition.builder().apiExport(ApiExport.builder().id(apiId).build()).plans(plans).pages(pages).build();
    }

    private static PlanWithFlows buildPlanWithFlows(String id, String crossId) {
        return new PlanWithFlows(
            PlanFixtures.aPlanHttpV4().setPlanId(id).toBuilder().crossId(crossId).name(generateRandomName()).build(),
            List.of()
        );
    }

    private static Page buildPage(String id, String crossId) {
        return Page.builder().id(id).crossId(crossId).name(generateRandomName()).build();
    }

    private static Page buildPage(String id, String crossId, String parentId) {
        return Page.builder().id(id).crossId(crossId).parentId(parentId).name(generateRandomName()).build();
    }

    private static void assertIsUuid(String id) {
        assertDoesNotThrow(() -> UUID.fromString(id));
    }

    private static String generateRandomName() {
        return RandomStringUtils.random(32, true, true);
    }
}
