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
package io.gravitee.apim.core.log.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.log.model.ConnectionLog;
import io.gravitee.apim.core.plan.model.Plan;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ConnectionLogMetadataDomainService {

    public static final String UNKNOWN_SERVICE = "1";

    private static final String UNKNOWN_SERVICE_MAPPED = "?";
    private static final String METADATA_NAME = "name";
    private static final String METADATA_DELETED = "deleted";
    private static final String METADATA_UNKNOWN = "unknown";
    private static final String METADATA_VERSION = "version";
    private static final String METADATA_UNKNOWN_API_NAME = "Unknown API (not found)";
    private static final String METADATA_UNKNOWN_APPLICATION_NAME = "Unknown application (keyless)";
    private static final String METADATA_UNKNOWN_PLAN_NAME = "Unknown plan";
    private static final String METADATA_DELETED_API_NAME = "Deleted API";
    private static final String METADATA_DELETED_APPLICATION_NAME = "Deleted application";
    private static final String METADATA_DELETED_PLAN_NAME = "Deleted plan";

    public Map<String, Map<String, String>> getMetadataForApplicationConnectionLog(@NotNull List<ConnectionLog> applicationConnectionLogs) {
        Map<String, Map<String, String>> metadata = new HashMap<>();

        applicationConnectionLogs.forEach(applicationConnectionLog -> {
            var apiId = applicationConnectionLog.getApiId();
            var planId = applicationConnectionLog.getPlanId();

            if (apiId != null) {
                metadata.computeIfAbsent(apiId, mapApiToMetadata(apiId, applicationConnectionLog.getApi()));
            }

            if (planId != null) {
                metadata.computeIfAbsent(planId, mapPlanToMetadata(planId, applicationConnectionLog.getPlan()));
            }
        });
        return metadata;
    }

    private Function<String, Map<String, String>> mapApiToMetadata(@NotNull String apiId, Api api) {
        return s -> {
            var metadata = new HashMap<String, String>();

            if (isAnUnknownService(apiId)) {
                metadata.put(METADATA_NAME, METADATA_UNKNOWN_API_NAME);
                metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            } else if (api == null) {
                metadata.put(METADATA_NAME, METADATA_DELETED_API_NAME);
                metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
            } else if (isAnUnknownService(api.getId())) {
                metadata.put(METADATA_NAME, METADATA_UNKNOWN_API_NAME);
                metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            } else {
                metadata.put(METADATA_NAME, api.getName());
                metadata.put(METADATA_VERSION, api.getVersion());
                if (Api.ApiLifecycleState.ARCHIVED.equals(api.getApiLifecycleState())) {
                    metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                }
            }

            return metadata;
        };
    }

    private Function<String, Map<String, String>> mapPlanToMetadata(@NotNull String planId, Plan plan) {
        return s -> {
            var metadata = new HashMap<String, String>();

            if (isAnUnknownService(planId)) {
                metadata.put(METADATA_NAME, METADATA_UNKNOWN_PLAN_NAME);
                metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            } else if (plan == null) {
                metadata.put(METADATA_NAME, METADATA_DELETED_PLAN_NAME);
                metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
            } else if (isAnUnknownService(plan.getId())) {
                metadata.put(METADATA_NAME, METADATA_UNKNOWN_PLAN_NAME);
                metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
            } else {
                metadata.put(METADATA_NAME, plan.getName());
            }

            return metadata;
        };
    }

    private static boolean isAnUnknownService(String id) {
        return Objects.equals(id, UNKNOWN_SERVICE) || Objects.equals(id, UNKNOWN_SERVICE_MAPPED);
    }
}
