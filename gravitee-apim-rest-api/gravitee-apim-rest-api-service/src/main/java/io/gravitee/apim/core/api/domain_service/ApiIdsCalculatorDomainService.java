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

import static java.util.stream.Collectors.toMap;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DomainService
public class ApiIdsCalculatorDomainService {

    private final ApiQueryService apiQueryService;
    private final PageQueryService pageQueryService;
    private final PlanQueryService planQueryService;

    public ApiIdsCalculatorDomainService(
        ApiQueryService apiQueryService,
        PageQueryService pageQueryService,
        PlanQueryService planQueryService
    ) {
        this.apiQueryService = apiQueryService;
        this.pageQueryService = pageQueryService;
        this.planQueryService = planQueryService;
    }

    public ImportDefinition recalculateApiDefinitionIds(String environmentId, ImportDefinition toRecalculate) {
        Objects.requireNonNull(toRecalculate.getApiExport(), "Api is mandatory");
        if (toRecalculate.getApiExport().getId() == null || toRecalculate.getApiExport().getId().isEmpty()) {
            findApiByEnvironmentAndCrossId(environmentId, toRecalculate.getApiExport().getCrossId())
                .ifPresentOrElse(
                    api -> recalculateIdsFromCrossId(environmentId, toRecalculate, api),
                    () -> recalculateIdsFromDefinitionIds(environmentId, toRecalculate)
                );
        }
        return generateEmptyIdsForPlansAndPages(toRecalculate);
    }

    private void recalculateIdsFromCrossId(String environmentId, ImportDefinition toRecalculate, Api api) {
        log.debug("Recalculating page and plans ids from cross id {} for api {}", api.getCrossId(), api.getId());
        toRecalculate.getApiExport().setId(api.getId());
        Map<String, String> newPageIdsByOldPageIds = recalculatePageIdsFromCrossIds(api, toRecalculate.getPages());
        recalculatePlanIdsFromCrossIds(api, toRecalculate.getPlans(), newPageIdsByOldPageIds);
    }

    /**
     * Recalculate imported API pages ID, using crossID.
     */
    private Map<String, String> recalculatePageIdsFromCrossIds(Api api, List<Page> pagesToRecalculate) {
        Map<String, String> idsMap = new HashMap<>();

        Map<String, Page> pagesByCrossId = pageQueryService
            .searchByApiId(api.getId())
            .stream()
            .filter(page -> page.getCrossId() != null)
            .collect(toMap(Page::getCrossId, Function.identity()));

        pagesToRecalculate
            .stream()
            .filter(page -> page.getCrossId() != null && !page.getCrossId().isEmpty())
            .forEach(page -> {
                String pageId = page.getId() != null && !page.getId().isEmpty() ? page.getId() : null;
                Page matchingPage = pagesByCrossId.get(page.getCrossId());
                String newPageId = matchingPage != null ? matchingPage.getId() : UuidString.generateRandom();
                page.setId(newPageId);
                idsMap.put(pageId, newPageId);
                updatePagesHierarchy(pagesToRecalculate, pageId, newPageId);
            });

        return idsMap;
    }

    private void recalculatePlanIdsFromCrossIds(
        Api api,
        Set<PlanWithFlows> plansToRecalculate,
        Map<String, String> newPageIdsByOldPageIds
    ) {
        Map<String, Plan> plansByCrossId = planQueryService
            .findAllByApiId(api.getId())
            .stream()
            .filter(plan -> plan.getCrossId() != null)
            .collect(toMap(Plan::getCrossId, Function.identity()));

        plansToRecalculate
            .stream()
            .map(plan -> recalculateGeneralConditionsPageId(plan, newPageIdsByOldPageIds))
            .filter(plan -> plan.getCrossId() != null && !plan.getCrossId().isEmpty())
            .forEach(plan -> {
                Plan matchingPlan = plansByCrossId.get(plan.getCrossId());
                plan.setId(matchingPlan != null ? matchingPlan.getId() : UuidString.generateRandom());
            });
    }

    private void updatePagesHierarchy(List<Page> pages, String parentId, String newParentId) {
        pages.stream().filter(page -> isChildPageOf(page, parentId)).forEach(child -> child.setParentId(newParentId));
    }

    private boolean isChildPageOf(Page page, String parentPageId) {
        return page.getParentId() != null && !page.getParentId().isEmpty() && page.getParentId().equals(parentPageId);
    }

    private void recalculateIdsFromDefinitionIds(String environmentId, ImportDefinition toRecalculate) {
        if (canRecalculateIds(toRecalculate)) {
            log.debug("Recalculating page and plans ids from definition");
            String newApiId = UuidString.generateForEnvironment(environmentId, toRecalculate.getApiExport().getId());
            toRecalculate.getApiExport().setId(newApiId);
            Map<String, String> pagesIdsMap = recalculatePageIdsFromDefinitionIds(toRecalculate.getPages(), environmentId, newApiId);
            recalculatePlanIdsFromDefinitionIds(toRecalculate.getPlans(), environmentId, newApiId, pagesIdsMap);
        }
    }

    private Map<String, String> recalculatePageIdsFromDefinitionIds(List<Page> pages, String environmentId, String apiId) {
        Map<String, String> idsMap = new HashMap<>();
        if (pages != null) {
            pages
                .stream()
                .filter(page -> page.getId() != null && !page.getId().isEmpty())
                .forEach(page -> {
                    String oldPageId = page.getId();
                    String newPageId = UuidString.generateForEnvironment(environmentId, apiId, oldPageId);
                    page.setId(newPageId);
                    idsMap.put(oldPageId, newPageId);
                    updatePagesHierarchy(pages, oldPageId, newPageId);
                });
        }
        return idsMap;
    }

    private void recalculatePlanIdsFromDefinitionIds(
        Set<PlanWithFlows> plans,
        String environmentId,
        String apiId,
        Map<String, String> pagesIdsMap
    ) {
        if (plans != null) {
            plans
                .stream()
                .map(plan -> recalculateGeneralConditionsPageId(plan, pagesIdsMap))
                .filter(plan -> plan.getId() != null && !plan.getId().isEmpty())
                .forEach(plan -> {
                    plan.setId(UuidString.generateForEnvironment(environmentId, apiId, plan.getId()));
                });
        }
    }

    private Optional<Api> findApiByEnvironmentAndCrossId(String environmentId, String apiCrossId) {
        return apiCrossId == null ? Optional.empty() : apiQueryService.findByEnvironmentIdAndCrossId(environmentId, apiCrossId);
    }

    private static PlanWithFlows recalculateGeneralConditionsPageId(PlanWithFlows plan, Map<String, String> newPageIdsByOldPageIds) {
        if (plan.getGeneralConditions() != null && !plan.getGeneralConditions().isEmpty()) {
            plan.setGeneralConditions(newPageIdsByOldPageIds.get(plan.getGeneralConditions()));
        }
        return plan;
    }

    private boolean canRecalculateIds(ImportDefinition api) {
        // If the definition is managed by kubernetes, do not try to recalculate ids because k8s is the source of truth.
        return !(api.getApiExport().getOriginContext() instanceof OriginContext.Kubernetes);
    }

    private ImportDefinition generateEmptyIdsForPlansAndPages(ImportDefinition toRecalculate) {
        if (toRecalculate.getPages() != null) {
            toRecalculate
                .getPages()
                .stream()
                .filter(page -> page.getId() == null || page.getId().isEmpty())
                .forEach(page -> page.setId(UuidString.generateRandom()));
        }

        if (toRecalculate.getPlans() != null) {
            toRecalculate
                .getPlans()
                .stream()
                .filter(plan -> plan.getId() == null || plan.getId().isEmpty())
                .forEach(plan -> plan.setId(UuidString.generateRandom()));
        }

        return toRecalculate;
    }
}
