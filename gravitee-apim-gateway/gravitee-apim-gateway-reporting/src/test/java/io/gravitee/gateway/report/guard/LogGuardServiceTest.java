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

package io.gravitee.gateway.report.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
class LogGuardServiceTest {

    @Mock
    private NodeHealthCheckService nodeHealthCheckService;

    private LogGuardService cut;

    @BeforeEach
    void setUp() throws Exception {
        cut = new LogGuardService(true, "cooldown", 30, nodeHealthCheckService);

        cut.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        cut.stop();
    }

    @Test
    void should_be_active_when_gc_pressure_too_high() {
        when(nodeHealthCheckService.isGcPressureTooHigh()).thenReturn(true);

        assertThat(cut.isLogGuardActive()).isTrue();
    }

    @Test
    void should_be_inactive_when_gc_pressure_not_too_high() {
        when(nodeHealthCheckService.isGcPressureTooHigh()).thenReturn(false);

        assertThat(cut.isLogGuardActive()).isFalse();
    }
}
