package io.gravitee.rest.api.management.v2.rest.model;

import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import lombok.Data;
import java.util.Map;

@Data
public class ApiAnalyticsTopHitsByEntrypointResponse {
    private Map<String, TopHitsAnalytics> statusCodesByEntrypoint;

    public static ApiAnalyticsTopHitsByEntrypointResponse ofMap(Map<String, TopHitsAnalytics> stringTopHitsAnalyticsMap) {
        final var result = new ApiAnalyticsTopHitsByEntrypointResponse();
        result.statusCodesByEntrypoint = stringTopHitsAnalyticsMap;
        return result;
    }
}
