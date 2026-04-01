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
package io.gravitee.apim.core.analytics.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics.model.AnalyticsGroupByResponse;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@UseCase
public class SearchAnalyticsGroupByUseCase {

    static final Set<String> SUPPORTED_FIELDS = Set.of("status", "mapped-status", "application", "plan", "host", "uri");

    private final AnalyticsQueryService analyticsQueryService;
    private final ApiCrudService apiCrudService;
    private final ApplicationCrudService applicationCrudService;
    private final PlanCrudService planCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateApiRequirements(input);
        validateField(input.field());

        var values = analyticsQueryService.searchGroupBy(
            executionContext,
            input.apiId(),
            input.field(),
            input.size(),
            input.from().orElse(null),
            input.to().orElse(null)
        );

        var metadata = buildMetadata(executionContext, input.field(), input.environmentId(), values);

        return new Output(new AnalyticsGroupByResponse(values, metadata));
    }

    private Map<String, Map<String, String>> buildMetadata(
        ExecutionContext executionContext,
        String field,
        String environmentId,
        Map<String, Long> values
    ) {
        return switch (field) {
            case "application" -> resolveApplicationMetadata(environmentId, values);
            case "plan" -> resolvePlanMetadata(values);
            default -> Map.of();
        };
    }

    private Map<String, Map<String, String>> resolveApplicationMetadata(String environmentId, Map<String, Long> values) {
        var metadata = new HashMap<String, Map<String, String>>();
        for (var id : values.keySet()) {
            try {
                var app = applicationCrudService.findById(id, environmentId);
                metadata.put(id, Map.of("name", app.getName()));
            } catch (ApplicationNotFoundException e) {
                metadata.put(id, Map.of("name", id, "deleted", "true", "unknown", "true"));
            }
        }
        return metadata;
    }

    private Map<String, Map<String, String>> resolvePlanMetadata(Map<String, Long> values) {
        var metadata = new HashMap<String, Map<String, String>>();
        for (var id : values.keySet()) {
            planCrudService
                .findById(id)
                .ifPresentOrElse(
                    plan -> metadata.put(id, Map.of("name", plan.getName())),
                    () -> metadata.put(id, Map.of("name", id, "deleted", "true", "unknown", "true"))
                );
        }
        return metadata;
    }

    private void validateApiRequirements(Input input) {
        final Api api = apiCrudService.get(input.apiId());
        validateApiDefinitionVersion(api.getDefinitionVersion(), input.apiId());
        validateApiIsNotTcp(api.getApiDefinitionHttpV4());
        validateApiMultiTenancyAccess(api, input.environmentId());
    }

    private static void validateField(String field) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field is required for GROUP_BY analytics");
        }
        if (!SUPPORTED_FIELDS.contains(field)) {
            throw new IllegalArgumentException(
                "Unsupported field for GROUP_BY analytics: " + field + ". Supported fields: " + SUPPORTED_FIELDS
            );
        }
    }

    private static void validateApiMultiTenancyAccess(Api api, String environmentId) {
        if (!api.belongsToEnvironment(environmentId)) {
            throw new ApiNotFoundException(api.getId());
        }
    }

    private static void validateApiDefinitionVersion(DefinitionVersion definitionVersion, String apiId) {
        if (!DefinitionVersion.V4.equals(definitionVersion)) {
            throw new ApiInvalidDefinitionVersionException(apiId);
        }
    }

    private static void validateApiIsNotTcp(io.gravitee.definition.model.v4.Api apiDefinitionV4) {
        if (apiDefinitionV4.isTcpProxy()) {
            throw new IllegalArgumentException("Analytics are not supported for TCP Proxy APIs");
        }
    }

    public record Input(String apiId, String environmentId, String field, int size, Optional<Instant> from, Optional<Instant> to) {}

    public record Output(AnalyticsGroupByResponse groupBy) {}
}
