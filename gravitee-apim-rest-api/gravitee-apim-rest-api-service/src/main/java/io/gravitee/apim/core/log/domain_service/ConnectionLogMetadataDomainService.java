package io.gravitee.apim.core.log.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.log.model.ConnectionLog;
import io.gravitee.apim.core.plan.model.Plan;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@DomainService
public class ConnectionLogMetadataDomainService {

    private static final String UNKNOWN_SERVICE = "1";
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

    public Map<String, Map<String, String>> getApplicationMetadata(List<ConnectionLog> data) {
        Map<String, Map<String, String>> metadata = new HashMap<>();

        data
                .forEach(logItem -> {
                    var apiId = logItem.getApiId();
                    var planId = logItem.getPlanId();

                    if (apiId != null) {
                        metadata.computeIfAbsent(apiId, mapApiToMetadata(apiId, logItem.getApi()));
                    }

                    if (planId != null) {
                        metadata.computeIfAbsent(planId, mapPlanToMetadata(planId, logItem.getPlan()));
                    }

                });
        return metadata;
    }

    private Function<String, Map<String, String>> mapApiToMetadata(String apiId, Api api) {
        return s -> {
            var metadata = new HashMap<String, String>();

            if (api == null) {
                if (Objects.equals(apiId, UNKNOWN_SERVICE) || Objects.equals(apiId, UNKNOWN_SERVICE_MAPPED)) {
                    metadata.put(METADATA_NAME, METADATA_UNKNOWN_API_NAME);
                    metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
                } else {
                    metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                    metadata.put(METADATA_NAME, METADATA_DELETED_API_NAME);
                }
            } else if (Objects.equals(api.getId(), UNKNOWN_SERVICE) || Objects.equals(api.getId(), UNKNOWN_SERVICE_MAPPED)) {
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


    private Function<String, Map<String, String>> mapPlanToMetadata(String planId, Plan plan) {
        return s -> {
            var metadata = new HashMap<String, String>();

            if (plan == null) {
                if (Objects.equals(planId, UNKNOWN_SERVICE) || Objects.equals(planId, UNKNOWN_SERVICE_MAPPED)) {
                    metadata.put(METADATA_NAME, METADATA_UNKNOWN_PLAN_NAME);
                    metadata.put(METADATA_UNKNOWN, Boolean.TRUE.toString());
                } else {
                    metadata.put(METADATA_DELETED, Boolean.TRUE.toString());
                    metadata.put(METADATA_NAME, METADATA_DELETED_PLAN_NAME);
                }
            } else {
                metadata.put(METADATA_NAME, plan.getName());
            }

            return metadata;
        };
    }

}
