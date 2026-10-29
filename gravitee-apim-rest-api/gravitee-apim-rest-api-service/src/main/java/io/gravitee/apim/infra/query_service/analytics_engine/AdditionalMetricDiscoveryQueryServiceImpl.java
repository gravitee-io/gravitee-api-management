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

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricDiscovery;
import io.gravitee.apim.core.analytics_engine.model.DiscoveredAdditionalMetric;
import io.gravitee.apim.core.analytics_engine.query_service.AdditionalMetricDiscoveryQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricDiscoveryQuery;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricDiscoveryResult;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@CustomLog
@Service
public class AdditionalMetricDiscoveryQueryServiceImpl implements AdditionalMetricDiscoveryQueryService {

    static final Duration WINDOW = Duration.ofDays(30);
    static final Duration TIMEOUT = Duration.ofSeconds(5);
    static final Duration REFRESH_AFTER = Duration.ofMinutes(5);

    private static final ThreadFactory REFRESH_THREADS = runnable -> {
        var thread = new Thread(runnable, "additional-metric-discovery");
        thread.setDaemon(true);
        return thread;
    };

    private final AnalyticsRepository analyticsRepository;
    private final LoadingCache<QueryContext, AdditionalMetricDiscovery> passes;

    @Autowired
    public AdditionalMetricDiscoveryQueryServiceImpl(@Lazy AnalyticsRepository analyticsRepository) {
        this(analyticsRepository, Ticker.systemTicker(), Executors.newSingleThreadExecutor(REFRESH_THREADS));
    }

    AdditionalMetricDiscoveryQueryServiceImpl(AnalyticsRepository analyticsRepository, Ticker ticker, Executor refreshExecutor) {
        this.analyticsRepository = analyticsRepository;
        this.passes = CacheBuilder.newBuilder()
            .refreshAfterWrite(REFRESH_AFTER)
            .ticker(ticker)
            .build(CacheLoader.asyncReloading(CacheLoader.from(this::pass), refreshExecutor));
    }

    @Override
    public AdditionalMetricDiscovery discover(String organizationId, String environmentId) {
        if (organizationId == null || organizationId.isBlank() || environmentId == null || environmentId.isBlank()) {
            throw new IllegalArgumentException("An additional metric discovery needs an organization and an environment");
        }
        try {
            return passes.getUnchecked(new QueryContext(organizationId, environmentId));
        } catch (UncheckedExecutionException e) {
            // Not kept: the next request tries again.
            return AdditionalMetricDiscovery.unavailable(TimeProvider.now().toInstant());
        }
    }

    /** Throws when the pass fails, so the cache keeps nothing on a first load and the previous pass on a refresh. */
    private AdditionalMetricDiscovery pass(QueryContext queryContext) {
        var now = TimeProvider.now().toInstant();
        AdditionalMetricDiscoveryResult result;
        try {
            result = analyticsRepository.discoverAdditionalMetrics(
                queryContext,
                new AdditionalMetricDiscoveryQuery(now.minus(WINDOW), now, TIMEOUT)
            );
        } catch (RuntimeException e) {
            log.warn("Unable to discover the additional metrics of environment [{}]", queryContext.getEnvId(), e);
            throw e;
        }
        var metrics = result
            .metrics()
            .stream()
            .map(metric -> new DiscoveredAdditionalMetric(metric.rawKey(), metric.lastSeen()))
            .toList();
        return new AdditionalMetricDiscovery(metrics, now, result.truncated());
    }
}
