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
package io.gravitee.apim.infra.query_service.analytics_engine;

import io.gravitee.apim.core.analytics_engine.query_service.BuiltInAdditionalMetricsQueryService;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Union of what the analytics repository resolves (its field resolvers and logs-path adapters) and of the fields
 * {@link FilterValuesQueryServiceImpl} lists values from, which this module maps itself.
 *
 * @author GraviteeSource Team
 */
@Service
public class BuiltInAdditionalMetricsQueryServiceImpl implements BuiltInAdditionalMetricsQueryService {

    private final AnalyticsRepository analyticsRepository;

    public BuiltInAdditionalMetricsQueryServiceImpl(@Lazy AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public Map<String, Set<String>> namesByRawKey() {
        var namesByRawKey = new HashMap<String, Set<String>>();
        analyticsRepository.builtInAdditionalMetricKeys().forEach((key, names) -> merge(namesByRawKey, key, names));
        FilterValuesQueryServiceImpl.additionalMetricKeys().forEach((key, names) -> merge(namesByRawKey, key, names));
        return namesByRawKey
            .entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    private static void merge(Map<String, Set<String>> namesByRawKey, String key, Set<String> names) {
        namesByRawKey.computeIfAbsent(key, k -> new HashSet<>()).addAll(names);
    }
}
