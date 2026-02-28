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
package io.gravitee.apim.core.analytics_engine.model;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Map;
import java.util.Set;

public record AnalyticsQueryContext(
    AuditInfo auditInfo,
    ExecutionContext executionContext,
    Set<String> authorizedApiIds,
    Map<String, String> apiNamesById,
    Map<String, String> applicationNamesById,
    Map<ApiType, Set<String>> apiIdsByType
) {
    public AnalyticsQueryContext {
        if (authorizedApiIds == null) authorizedApiIds = Set.of();
        if (apiNamesById == null) apiNamesById = Map.of();
        if (applicationNamesById == null) applicationNamesById = Map.of();
        if (apiIdsByType == null) apiIdsByType = Map.of();
    }

    public AnalyticsQueryContext withApiNamesById(Map<String, String> apiNamesById) {
        return new AnalyticsQueryContext(auditInfo, executionContext, authorizedApiIds, apiNamesById, applicationNamesById, apiIdsByType);
    }

    public AnalyticsQueryContext withApplicationNamesById(Map<String, String> applicationNamesById) {
        return new AnalyticsQueryContext(auditInfo, executionContext, authorizedApiIds, apiNamesById, applicationNamesById, apiIdsByType);
    }
}
