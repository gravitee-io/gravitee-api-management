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
package io.gravitee.rest.api.service.impl;

import static java.util.stream.Collectors.toMap;

import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiIdsCalculatorService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.imports.ImportApiJsonNode;
import io.gravitee.rest.api.service.imports.ImportJsonNodeWithIds;
import io.gravitee.rest.api.service.imports.ImportPlanJsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
@Component
public class ApiIdsCalculatorServiceImpl implements ApiIdsCalculatorService {

    private ApiService apiService;
    private PageService pageService;
    private PlanService planService;

    @Override
    public ImportApiJsonNode recalculateApiDefinitionIds(ExecutionContext executionContext, ImportApiJsonNode apiJsonNode) {
        return recalculateApiDefinitionIds(executionContext, apiJsonNode, null);
    }

    @Override
    public ImportApiJsonNode recalculateApiDefinitionIds(
        ExecutionContext executionContext,
        ImportApiJsonNode apiJsonNode,
        String urlApiId
    ) {
        /*
         * In case of an update, if the API definition ID is the same as the resource ID targeted by the update,
         * we don't apply any kind of ID transformation so that we don't break previous exports that don't hold
         * a cross ID
         */
        if (!apiJsonNode.hasId() || !apiJsonNode.getId().equals(urlApiId)) {
            findApiByEnvironmentAndCrossId(executionContext.getEnvironmentId(), apiJsonNode.getCrossId()).ifPresentOrElse(
                api -> recalculateIdsFromCrossId(executionContext, apiJsonNode, api),
                () -> recalculateIdsFromDefinitionIds(executionContext.getEnvironmentId(), apiJsonNode, urlApiId)
            );
        }
        return generateEmptyIds(apiJsonNode);
    }

    private Optional<ApiEntity> findApiByEnvironmentAndCrossId(String environmentId, String crossId) {
        return crossId == null ? Optional.empty() : apiService.findByEnvironmentIdAndCrossId(environmentId, crossId);
    }

    private void recalculateIdsFromCrossId(ExecutionContext executionContext, ImportApiJsonNode apiJsonNode, ApiEntity api) {
        apiJsonNode.setId(api.getId());
        Map<String, String> pagesIdsMap = recalculatePageIdsFromCrossIds(executionContext.getEnvironmentId(), api, apiJsonNode);
        recalculatePlanIdsFromCrossIds(executionContext, api, apiJsonNode.getPlans(), pagesIdsMap);
    }

    private void recalculatePlanIdsFromCrossIds(
        ExecutionContext executionContext,
        ApiEntity api,
        List<ImportPlanJsonNode> plansNodes,
        Map<String, String> pagesIdsMap
    ) {
        Map<String, PlanEntity> plansByCrossId = planService
            .findByApi(executionContext, api.getId())
            .stream()
            .filter(plan -> plan.getCrossId() != null)
            .collect(toMap(PlanEntity::getCrossId, Function.identity()));

        plansNodes
            .stream()
            .filter(ImportJsonNodeWithIds::hasCrossId)
            .forEach(plan -> {
                PlanEntity matchingPlan = plansByCrossId.get(plan.getCrossId());
                plan.setApi(api.getId());
                plan.setId(matchingPlan != null ? matchingPlan.getId() : UuidString.generateRandom());
                recalculateGeneralConditionsPageId(plan, pagesIdsMap);
            });
    }

    /**
     * Recalculate imported API pages ID, using crossID.
     *
     * @param environmentId
     * @param api
     * @param apiJsonNode
     * @return the map of old ID - new ID
     */
    private Map<String, String> recalculatePageIdsFromCrossIds(String environmentId, ApiEntity api, ImportApiJsonNode apiJsonNode) {
        var pagesNodes = apiJsonNode.getPages();
        Map<String, String> idsMap = new HashMap<>();

        Map<String, PageEntity> pagesByCrossId = pageService
            .findByApi(environmentId, api.getId())
            .stream()
            .filter(page -> page.getCrossId() != null)
            .collect(toMap(PageEntity::getCrossId, Function.identity()));

        pagesNodes
            .stream()
            .filter(ImportJsonNodeWithIds::hasCrossId)
            .forEach(page -> {
                String pageId = page.hasId() ? page.getId() : null;
                PageEntity matchingPage = pagesByCrossId.get(page.getCrossId());
                page.setApi(api.getId());
                if (matchingPage != null) {
                    idsMap.put(pageId, matchingPage.getId());
                    page.setId(matchingPage.getId());
                    updatePagesHierarchy(pagesNodes, pageId, matchingPage.getId());
                } else {
                    String newPageId = canRegenerateId(apiJsonNode) ? UuidString.generateRandom() : pageId;
                    idsMap.put(pageId, newPageId);
                    page.setId(newPageId);
                    updatePagesHierarchy(pagesNodes, pageId, newPageId);
                }
            });

        return idsMap;
    }

    private void recalculateIdsFromDefinitionIds(String environmentId, ImportApiJsonNode apiJsonNode, String urlApiId) {
        if (canRegenerateId(apiJsonNode)) {
            String targetApiId = urlApiId == null ? UuidString.generateForEnvironment(environmentId, apiJsonNode.getId()) : urlApiId;
            apiJsonNode.setId(targetApiId);
            Map<String, String> pagesIdsMap = recalculatePageIdsFromDefinitionIds(apiJsonNode.getPages(), environmentId, targetApiId);
            recalculatePlanIdsFromDefinitionIds(apiJsonNode.getPlans(), environmentId, targetApiId, pagesIdsMap);
        }
    }

    private void recalculatePlanIdsFromDefinitionIds(
        List<ImportPlanJsonNode> plansNodes,
        String environmentId,
        String apiId,
        Map<String, String> pagesIdsMap
    ) {
        plansNodes
            .stream()
            .filter(ImportJsonNodeWithIds::hasId)
            .forEach(plan -> {
                plan.setId(UuidString.generateForEnvironment(environmentId, apiId, plan.getId()));
                plan.setApi(apiId);
                recalculateGeneralConditionsPageId(plan, pagesIdsMap);
            });
    }

    private static void recalculateGeneralConditionsPageId(ImportPlanJsonNode plan, Map<String, String> pagesIdsMap) {
        if (plan.hasGeneralConditions()) {
            plan.setGeneralConditions(pagesIdsMap.get(plan.getGeneralConditions()));
        }
    }

    /**
     * Recalculate imported API pages ID, using definition ID.
     *
     * @param pagesNodes
     * @param environmentId
     * @param apiId
     * @return the map of old ID - new ID
     */
    private Map<String, String> recalculatePageIdsFromDefinitionIds(
        List<ImportJsonNodeWithIds> pagesNodes,
        String environmentId,
        String apiId
    ) {
        Map<String, String> idsMap = new HashMap<>();
        pagesNodes
            .stream()
            .filter(ImportJsonNodeWithIds::hasId)
            .forEach(page -> {
                String oldPageId = page.getId();
                String newPageId = UuidString.generateForEnvironment(environmentId, apiId, oldPageId);
                idsMap.put(oldPageId, newPageId);
                page.setId(newPageId);
                page.setApi(apiId);
                updatePagesHierarchy(pagesNodes, oldPageId, newPageId);
            });
        return idsMap;
    }

    private boolean canRegenerateId(ImportApiJsonNode apiJsonNode) {
        // If the definition is managed by kubernetes, do not try to recalculate ids because k8s is the source of truth.
        return !apiJsonNode.isKubernetesOrigin();
    }

    private void updatePagesHierarchy(List<ImportJsonNodeWithIds> pagesNodes, String parentId, String newParentId) {
        pagesNodes
            .stream()
            .filter(child -> isChildPageOf(child, parentId))
            .forEach(child -> child.setParentId(newParentId));
    }

    private boolean isChildPageOf(ImportJsonNodeWithIds pageNode, String parentPageId) {
        return pageNode.hasParentId() && pageNode.getParentId().equals(parentPageId);
    }

    private ImportApiJsonNode generateEmptyIds(ImportApiJsonNode apiJsonNode) {
        Stream.concat(apiJsonNode.getPlans().stream(), apiJsonNode.getPages().stream())
            .filter(node -> !node.hasId())
            .forEach(node -> node.setId(UuidString.generateRandom()));
        return apiJsonNode;
    }
}
