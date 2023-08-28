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

import static java.util.stream.Collectors.toMap;

import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.v4.ApiIdsCalculatorService;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Slf4j
@Component("ApiIdsCalculatorServiceImplV4")
public class ApiIdsCalculatorServiceImpl implements ApiIdsCalculatorService {

    private ApiService apiService;
    private PageService pageService;
    private PlanService planService;

    @Override
    public ExportApiEntity recalculateApiDefinitionIds(ExecutionContext executionContext, ExportApiEntity toRecalculate) {
        Objects.requireNonNull(toRecalculate.getApiEntity(), "ApiEntity is mandatory");
        if (toRecalculate.getApiEntity().getId() == null || toRecalculate.getApiEntity().getId().isEmpty()) {
            findApiByEnvironmentAndCrossId(executionContext.getEnvironmentId(), toRecalculate.getApiEntity().getCrossId())
                .ifPresentOrElse(
                    api -> recalculateIdsFromCrossId(executionContext, toRecalculate, api),
                    () -> recalculateIdsFromDefinitionIds(executionContext, toRecalculate)
                );
        }
        return generateEmptyIdsForPlansAndPages(toRecalculate);
    }

    private void recalculateIdsFromCrossId(ExecutionContext executionContext, ExportApiEntity toRecalculate, ApiEntity api) {
        log.debug("Recalculating page and plans ids from cross id {} for api {}", api.getCrossId(), api.getId());
        toRecalculate.getApiEntity().setId(api.getId());
        Map<String, String> newPageIdsByOldPageIds = recalculatePageIdsFromCrossIds(
            executionContext.getEnvironmentId(),
            api,
            toRecalculate.getPages()
        );
        recalculatePlanIdsFromCrossIds(executionContext, api, toRecalculate.getPlans(), newPageIdsByOldPageIds);
    }

    /**
     * Recalculate imported API pages ID, using crossID.
     */
    private Map<String, String> recalculatePageIdsFromCrossIds(String environmentId, ApiEntity api, List<PageEntity> pagesToRecalculate) {
        Map<String, String> idsMap = new HashMap<>();

        Map<String, PageEntity> pagesByCrossId = pageService
            .findByApi(environmentId, api.getId())
            .stream()
            .filter(page -> page.getCrossId() != null)
            .collect(toMap(PageEntity::getCrossId, Function.identity()));

        pagesToRecalculate
            .stream()
            .filter(page -> page.getCrossId() != null && !page.getCrossId().isEmpty())
            .forEach(page -> {
                String pageId = page.getId() != null && !page.getId().isEmpty() ? page.getId() : null;
                PageEntity matchingPage = pagesByCrossId.get(page.getCrossId());
                String newPageId = matchingPage != null ? matchingPage.getId() : UuidString.generateRandom();
                page.setId(newPageId);
                idsMap.put(pageId, newPageId);
                updatePagesHierarchy(pagesToRecalculate, pageId, newPageId);
            });

        return idsMap;
    }

    private void recalculatePlanIdsFromCrossIds(
        ExecutionContext executionContext,
        ApiEntity api,
        Set<PlanEntity> plansToRecalculate,
        Map<String, String> newPageIdsByOldPageIds
    ) {
        Map<String, PlanEntity> plansByCrossId = planService
            .findByApi(executionContext, api.getId())
            .stream()
            .filter(plan -> plan.getCrossId() != null)
            .collect(toMap(PlanEntity::getCrossId, Function.identity()));

        plansToRecalculate
            .stream()
            .filter(plan -> plan.getCrossId() != null && !plan.getCrossId().isEmpty())
            .forEach(plan -> {
                PlanEntity matchingPlan = plansByCrossId.get(plan.getCrossId());
                plan.setId(matchingPlan != null ? matchingPlan.getId() : UuidString.generateRandom());
                recalculateGeneralConditionsPageId(plan, newPageIdsByOldPageIds);
            });
    }

    private void updatePagesHierarchy(List<PageEntity> pages, String parentId, String newParentId) {
        pages.stream().filter(page -> isChildPageOf(page, parentId)).forEach(child -> child.setParentId(newParentId));
    }

    private boolean isChildPageOf(PageEntity page, String parentPageId) {
        return page.getParentId() != null && !page.getParentId().isEmpty() && page.getParentId().equals(parentPageId);
    }

    private void recalculateIdsFromDefinitionIds(ExecutionContext executionContext, ExportApiEntity toRecalculate) {
        if (canRecalculateIds(toRecalculate)) {
            log.debug("Recalculating page and plans ids from definition");
            String newApiId = UuidString.generateForEnvironment(executionContext.getEnvironmentId(), toRecalculate.getApiEntity().getId());
            toRecalculate.getApiEntity().setId(newApiId);
            Map<String, String> pagesIdsMap = recalculatePageIdsFromDefinitionIds(
                toRecalculate.getPages(),
                executionContext.getEnvironmentId(),
                newApiId
            );
            recalculatePlanIdsFromDefinitionIds(toRecalculate.getPlans(), executionContext.getEnvironmentId(), newApiId, pagesIdsMap);
        }
    }

    private Map<String, String> recalculatePageIdsFromDefinitionIds(List<PageEntity> pages, String environmentId, String apiId) {
        Map<String, String> idsMap = new HashMap<>();
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
        return idsMap;
    }

    private void recalculatePlanIdsFromDefinitionIds(
        Set<PlanEntity> plans,
        String environmentId,
        String apiId,
        Map<String, String> pagesIdsMap
    ) {
        plans
            .stream()
            .filter(plan -> plan.getId() != null && !plan.getId().isEmpty())
            .forEach(plan -> {
                plan.setId(UuidString.generateForEnvironment(environmentId, apiId, plan.getId()));
                recalculateGeneralConditionsPageId(plan, pagesIdsMap);
            });
    }

    private Optional<ApiEntity> findApiByEnvironmentAndCrossId(String environmentId, String apiCrossId) {
        return apiCrossId == null ? Optional.empty() : apiService.findByEnvironmentIdAndCrossId(environmentId, apiCrossId);
    }

    private static void recalculateGeneralConditionsPageId(PlanEntity plan, Map<String, String> newPageIdsByOldPageIds) {
        if (plan.getGeneralConditions() != null && !plan.getGeneralConditions().isEmpty()) {
            plan.setGeneralConditions(newPageIdsByOldPageIds.get(plan.getGeneralConditions()));
        }
    }

    private boolean canRecalculateIds(ExportApiEntity api) {
        // If the definition is managed by kubernetes, do not try to recalculate ids because k8s is the source of truth.
        final DefinitionContext definitionContext = api.getApiEntity().getDefinitionContext();
        return !DefinitionContext.ORIGIN_KUBERNETES.equalsIgnoreCase(definitionContext != null ? definitionContext.getOrigin() : null);
    }

    private ExportApiEntity generateEmptyIdsForPlansAndPages(ExportApiEntity toRecalculate) {
        Stream
            .concat(
                toRecalculate.getPlans() != null ? toRecalculate.getPlans().stream() : Stream.empty(),
                toRecalculate.getPages() != null ? toRecalculate.getPages().stream() : Stream.empty()
            )
            .filter(identifiable -> identifiable.getId() == null || identifiable.getId().isEmpty())
            .forEach(identifiable -> identifiable.setId(UuidString.generateRandom()));
        return toRecalculate;
    }
}
