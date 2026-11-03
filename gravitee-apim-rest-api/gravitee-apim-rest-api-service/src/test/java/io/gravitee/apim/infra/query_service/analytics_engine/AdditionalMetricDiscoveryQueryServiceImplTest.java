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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.base.Ticker;
import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricDiscovery;
import io.gravitee.apim.core.analytics_engine.model.DiscoveredAdditionalMetric;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricDiscoveryQuery;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricDiscoveryResult;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricLastSeen;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class AdditionalMetricDiscoveryQueryServiceImplTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");
    private static final Instant SEEN = Instant.parse("2026-10-28T08:05:00Z");

    @Mock
    private AnalyticsRepository analyticsRepository;

    private final AtomicLong nanos = new AtomicLong();
    private final Queue<Runnable> refreshes = new ArrayDeque<>();
    private AdditionalMetricDiscoveryQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
        Ticker ticker = new Ticker() {
            @Override
            public long read() {
                return nanos.get();
            }
        };
        service = new AdditionalMetricDiscoveryQueryServiceImpl(analyticsRepository, ticker, refreshes::add);
    }

    @AfterEach
    void tearDown() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_discover_the_last_30_days_of_the_environment_within_the_timeout() {
        when(analyticsRepository.discoverAdditionalMetrics(any(), any())).thenReturn(seen("long_failover_count", true));

        var discovery = service.discover(ORG_ID, ENV_ID);

        assertThat(discovery).isEqualTo(
            new AdditionalMetricDiscovery(List.of(new DiscoveredAdditionalMetric("long_failover_count", SEEN)), NOW, true)
        );
        verify(analyticsRepository).discoverAdditionalMetrics(new QueryContext(ORG_ID, ENV_ID), query());
    }

    @Test
    void should_reuse_a_pass_per_environment_until_it_is_due_for_refresh() {
        when(analyticsRepository.discoverAdditionalMetrics(any(), any())).thenReturn(AdditionalMetricDiscoveryResult.NONE);

        service.discover(ORG_ID, ENV_ID);
        nanos.addAndGet(AdditionalMetricDiscoveryQueryServiceImpl.REFRESH_AFTER.minusSeconds(1).toNanos());
        service.discover(ORG_ID, ENV_ID);
        service.discover(ORG_ID, "other-env");

        verify(analyticsRepository, times(1)).discoverAdditionalMetrics(new QueryContext(ORG_ID, ENV_ID), query());
        verify(analyticsRepository, times(1)).discoverAdditionalMetrics(new QueryContext(ORG_ID, "other-env"), query());
        assertThat(refreshes).isEmpty();
    }

    @Test
    void should_serve_the_previous_pass_while_the_next_one_runs() {
        when(analyticsRepository.discoverAdditionalMetrics(any(), any())).thenReturn(seen("long_before", false), seen("long_after", false));
        var before = service.discover(ORG_ID, ENV_ID);
        nanos.addAndGet(AdditionalMetricDiscoveryQueryServiceImpl.REFRESH_AFTER.plusSeconds(1).toNanos());

        assertThat(service.discover(ORG_ID, ENV_ID)).isEqualTo(before);

        runRefreshes();
        assertThat(service.discover(ORG_ID, ENV_ID).metrics()).extracting(DiscoveredAdditionalMetric::rawKey).containsExactly("long_after");
    }

    @Test
    void should_keep_the_previous_pass_and_warn_when_a_refresh_fails() {
        when(analyticsRepository.discoverAdditionalMetrics(any(), any()))
            .thenReturn(seen("long_before", false))
            .thenThrow(new IllegalStateException("No endpoint available"));
        var before = service.discover(ORG_ID, ENV_ID);
        nanos.addAndGet(AdditionalMetricDiscoveryQueryServiceImpl.REFRESH_AFTER.plusSeconds(1).toNanos());

        var warnings = captureWarnings(() -> {
            service.discover(ORG_ID, ENV_ID);
            runRefreshes();
        });

        assertThat(service.discover(ORG_ID, ENV_ID)).isEqualTo(before);
        assertThat(warnings)
            .singleElement()
            .satisfies(event -> assertThat(event.getFormattedMessage()).contains(ENV_ID));
    }

    @Test
    void should_answer_an_unavailable_discovery_and_warn_when_the_first_pass_fails() {
        when(analyticsRepository.discoverAdditionalMetrics(any(), any())).thenThrow(new IllegalStateException("No endpoint available"));

        var warnings = captureWarnings(() ->
            assertThat(service.discover(ORG_ID, ENV_ID)).isEqualTo(AdditionalMetricDiscovery.unavailable(NOW))
        );

        assertThat(warnings)
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains(ENV_ID);
                assertThat(event.getThrowableProxy().getMessage()).isEqualTo("No endpoint available");
            });
    }

    @Test
    void should_try_again_on_the_next_request_after_a_failed_first_pass() {
        when(analyticsRepository.discoverAdditionalMetrics(any(), any()))
            .thenThrow(new IllegalStateException("No endpoint available"))
            .thenReturn(AdditionalMetricDiscoveryResult.NONE);

        assertThat(service.discover(ORG_ID, ENV_ID)).isEqualTo(AdditionalMetricDiscovery.unavailable(NOW));
        assertThat(service.discover(ORG_ID, ENV_ID)).isEqualTo(new AdditionalMetricDiscovery(List.of(), NOW, false));

        verify(analyticsRepository, times(2)).discoverAdditionalMetrics(new QueryContext(ORG_ID, ENV_ID), query());
    }

    @Test
    void should_refuse_to_discover_without_an_organization_or_an_environment() {
        assertThatThrownBy(() -> service.discover(ORG_ID, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.discover(ORG_ID, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.discover(null, ENV_ID)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.discover(" ", ENV_ID)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(analyticsRepository);
    }

    private void runRefreshes() {
        while (!refreshes.isEmpty()) {
            refreshes.poll().run();
        }
    }

    private static List<ILoggingEvent> captureWarnings(Runnable action) {
        var logger = (Logger) LoggerFactory.getLogger(AdditionalMetricDiscoveryQueryServiceImpl.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.detachAppender(appender);
        }
        return appender.list
            .stream()
            .filter(event -> event.getLevel() == Level.WARN)
            .toList();
    }

    private static AdditionalMetricDiscoveryResult seen(String rawKey, boolean truncated) {
        return new AdditionalMetricDiscoveryResult(List.of(new AdditionalMetricLastSeen(rawKey, SEEN)), truncated);
    }

    private static AdditionalMetricDiscoveryQuery query() {
        return new AdditionalMetricDiscoveryQuery(NOW.minus(Duration.ofDays(30)), NOW, AdditionalMetricDiscoveryQueryServiceImpl.TIMEOUT);
    }
}
