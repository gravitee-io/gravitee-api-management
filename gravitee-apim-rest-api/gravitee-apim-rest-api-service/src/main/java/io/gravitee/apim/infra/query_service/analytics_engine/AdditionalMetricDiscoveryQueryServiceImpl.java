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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricDiscovery;
import io.gravitee.apim.core.analytics_engine.model.DiscoveredAdditionalMetric;
import io.gravitee.apim.core.analytics_engine.query_service.AdditionalMetricDiscoveryQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricDiscoveryQuery;
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
    static final Duration RETRY_UNAVAILABLE_AFTER = Duration.ofMinutes(1);

    private static final ThreadFactory REFRESH_THREADS = runnable -> {
        var thread = new Thread(runnable, "additional-metric-discovery");
        thread.setDaemon(true);
        return thread;
    };

    private final AnalyticsRepository analyticsRepository;
    private final Ticker ticker;
    private final LoadingCache<QueryContext, Pass> passes;

    /** A pass and when it ran, by the cache's ticker. */
    private record Pass(AdditionalMetricDiscovery discovery, long ranAt, boolean failed) {}

    @Autowired
    public AdditionalMetricDiscoveryQueryServiceImpl(@Lazy AnalyticsRepository analyticsRepository) {
        this(analyticsRepository, Ticker.systemTicker(), Executors.newSingleThreadExecutor(REFRESH_THREADS));
    }

    /**
     * Entries are looked at every {@link #RETRY_UNAVAILABLE_AFTER}: a failed pass is run again then, a successful one only
     * once it is {@link #REFRESH_AFTER} old. Both run in the background, the previous answer served meanwhile.
     */
    AdditionalMetricDiscoveryQueryServiceImpl(AnalyticsRepository analyticsRepository, Ticker ticker, Executor refreshExecutor) {
        this.analyticsRepository = analyticsRepository;
        this.ticker = ticker;
        this.passes = CacheBuilder.newBuilder()
            .refreshAfterWrite(RETRY_UNAVAILABLE_AFTER)
            .ticker(ticker)
            .build(
                new CacheLoader<>() {
                    @Override
                    public Pass load(QueryContext queryContext) {
                        return pass(queryContext, null);
                    }

                    @Override
                    public ListenableFuture<Pass> reload(QueryContext queryContext, Pass previous) {
                        if (!previous.failed() && ticker.read() - previous.ranAt() < REFRESH_AFTER.toNanos()) {
                            return Futures.immediateFuture(previous);
                        }
                        var next = ListenableFutureTask.create(() -> pass(queryContext, previous));
                        refreshExecutor.execute(next);
                        return next;
                    }
                }
            );
    }

    @Override
    public AdditionalMetricDiscovery discover(String organizationId, String environmentId) {
        if (organizationId == null || organizationId.isBlank() || environmentId == null || environmentId.isBlank()) {
            throw new IllegalArgumentException("An additional metric discovery needs an organization and an environment");
        }
        return passes.getUnchecked(new QueryContext(organizationId, environmentId)).discovery();
    }

    /**
     * Never throws. A failed pass keeps the previous successful one, or answers {@link AdditionalMetricDiscovery#unavailable}
     * when there is none: kept {@link #RETRY_UNAVAILABLE_AFTER}, so a failing store costs one call a minute, not one per
     * request.
     */
    private Pass pass(QueryContext queryContext, Pass previous) {
        var now = TimeProvider.now().toInstant();
        try {
            var result = analyticsRepository.discoverAdditionalMetrics(
                queryContext,
                new AdditionalMetricDiscoveryQuery(now.minus(WINDOW), now, TIMEOUT)
            );
            var metrics = result
                .metrics()
                .stream()
                .map(metric -> new DiscoveredAdditionalMetric(metric.rawKey(), metric.lastSeen()))
                .toList();
            return new Pass(new AdditionalMetricDiscovery(metrics, now, result.truncated()), ticker.read(), false);
        } catch (RuntimeException e) {
            log.warn("Unable to discover the additional metrics of environment [{}]", queryContext.getEnvId(), e);
            return previous != null && !previous.failed()
                ? previous
                : new Pass(AdditionalMetricDiscovery.unavailable(now), ticker.read(), true);
        }
    }
}
