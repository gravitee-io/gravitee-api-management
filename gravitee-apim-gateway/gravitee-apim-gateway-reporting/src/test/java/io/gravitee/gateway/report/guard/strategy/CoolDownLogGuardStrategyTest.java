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

package io.gravitee.gateway.report.guard.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class CoolDownLogGuardStrategyTest {

    @Mock
    private NodeHealthCheckService nodeHealthCheckService;

    private CoolDownLogGuardStrategy cut;

    @BeforeEach
    void setUp() {
        cut = new CoolDownLogGuardStrategy(nodeHealthCheckService, Duration.ofSeconds(3));
    }

    @AfterEach
    void tearDown() {}

    @Test
    void should_return_true_when_gc_pressure_too_high() {
        when(nodeHealthCheckService.isGcPressureTooHigh()).thenReturn(true);

        assertThat(cut.execute()).isTrue();
    }

    @Test
    void should_return_false_when_gc_pressure_not_too_high() {
        when(nodeHealthCheckService.isGcPressureTooHigh()).thenReturn(false);

        assertThat(cut.execute()).isFalse();
    }

    @Test
    void should_return_true_during_cooldown_period_once_started_and_false_after_if_gc_pressure_dropped() {
        when(nodeHealthCheckService.isGcPressureTooHigh()).thenReturn(true).thenReturn(false);

        assertThat(cut.execute()).isTrue();

        //Check if result is true during cooldown period and false once cooldown period has ended
        await().atLeast(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(4)).untilAsserted(() -> assertThat(cut.execute()).isFalse());
    }
}
