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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @param restrictedToApplicationIds the only applications whose calls the caller may read, or empty when the calls of
 *     the authorized APIs are readable whatever application made them — keyless ones included. A present empty set
 *     matches no call.
 */
public record AnalyticsQueryContext(
    AuditInfo auditInfo,
    ExecutionContext executionContext,
    Set<String> authorizedApiIds,
    Map<String, String> apiNamesById,
    Map<String, String> applicationNamesById,
    Map<ApiType, Set<String>> apiIdsByType,
    Optional<Set<String>> restrictedToApplicationIds
) {
    public AnalyticsQueryContext {
        if (authorizedApiIds == null) authorizedApiIds = Set.of();
        if (apiNamesById == null) apiNamesById = Map.of();
        if (applicationNamesById == null) applicationNamesById = Map.of();
        if (apiIdsByType == null) apiIdsByType = Map.of();
        restrictedToApplicationIds = Objects.requireNonNull(restrictedToApplicationIds, "restrictedToApplicationIds").map(Set::copyOf);
    }

    /**
     * A context for APIs the caller already resolved. Nothing is preloaded: bucket names are looked up for the
     * returned ids only, and an {@code API_TYPE} condition matches no API, so callers narrow by type beforehand. Their
     * calls are not restricted to any application.
     */
    public static AnalyticsQueryContext forApis(AuditInfo auditInfo, Set<String> authorizedApiIds) {
        return new AnalyticsQueryContext(
            auditInfo,
            new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId()),
            authorizedApiIds,
            Map.of(),
            Map.of(),
            Map.of(),
            Optional.empty()
        );
    }
}
