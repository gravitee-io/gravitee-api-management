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
package io.gravitee.apim.core.analytics.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@DomainService
public class PlanMetadataProvider implements AnalyticsMetadataProvider {

    private static final String METADATA_NAME = "name";
    private static final String METADATA_DELETED = "deleted";
    private static final String METADATA_UNKNOWN = "unknown";
    private static final String METADATA_UNKNOWN_PLAN_NAME = "Unknown plan (keyless)";
    private static final String METADATA_DELETED_PLAN_NAME = "Deleted plan";
    private static final String UNKNOWN_SERVICE = "1";
    private static final String UNKNOWN_SERVICE_MAPPED = "?";

    private final PlanCrudService planCrudService;

    public PlanMetadataProvider(PlanCrudService planCrudService) {
        this.planCrudService = planCrudService;
    }

    @Override
    public boolean appliesTo(Field field) {
        return field == Field.PLAN;
    }

    @Override
    public Map<String, String> provide(String key, String environmentId) {
        return provide(List.of(key), environmentId).getOrDefault(key, Map.of());
    }

    record PlanMetadata(String name, boolean unknown, boolean deleted) {
        Map<String, String> toMap() {
            var result = new HashMap<String, String>();
            result.put(METADATA_NAME, name);
            if (unknown) {
                result.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            }
            if (deleted) {
                result.put(METADATA_DELETED, Boolean.TRUE.toString());
            }
            return result;
        }
    }

    private static final PlanMetadata UNKNOWN_PLAN = new PlanMetadata(METADATA_UNKNOWN_PLAN_NAME, true, false);
    private static final PlanMetadata NOT_FOUND = new PlanMetadata(METADATA_DELETED_PLAN_NAME, false, true);

    private static PlanMetadata ofPlan(Plan plan) {
        return new PlanMetadata(plan.getName(), false, false);
    }

    @Override
    public Map<String, Map<String, String>> provide(List<String> keys, String environmentId) {
        // Build Map<String, PlanMetadata>
        Map<String, PlanMetadata> metaMap = new HashMap<>();

        // Unknown plans
        keys
            .stream()
            .filter(key -> UNKNOWN_SERVICE.equals(key) || UNKNOWN_SERVICE_MAPPED.equals(key))
            .forEach(key -> metaMap.put(key, UNKNOWN_PLAN));

        // Batch for real plan ids
        List<String> planIds = keys
            .stream()
            .filter(key -> !UNKNOWN_SERVICE.equals(key) && !UNKNOWN_SERVICE_MAPPED.equals(key))
            .toList();

        if (!planIds.isEmpty()) {
            var plans = planCrudService.findByIds(planIds).stream().collect(Collectors.toMap(Plan::getId, Function.identity()));

            // Found plans
            plans.forEach((id, plan) -> metaMap.put(id, ofPlan(plan)));

            // Not found plans
            planIds
                .stream()
                .filter(id -> !plans.containsKey(id))
                .forEach(id -> metaMap.put(id, NOT_FOUND));
        }

        // Build output Map<String, Map<String, String>>
        return metaMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toMap()));
    }
}
