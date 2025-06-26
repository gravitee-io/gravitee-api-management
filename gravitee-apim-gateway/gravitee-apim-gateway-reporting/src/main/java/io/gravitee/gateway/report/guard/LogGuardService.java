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

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.report.guard.strategy.CoolDownLogGuardStrategy;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class LogGuardService extends AbstractService<LogGuardService> {

    private final boolean isLogGuardEnabled;

    private final String strategy;

    private final int cooldownDurationInSeconds;

    private final NodeHealthCheckService nodeHealthCheckService;

    private LogGuardStrategy logGuardStrategy;

    @Override
    public void doStart() throws Exception {
        if (isLogGuardEnabled) {
            if (!strategy.isEmpty() && strategy.equals("cooldown")) {
                logGuardStrategy = new CoolDownLogGuardStrategy(nodeHealthCheckService, Duration.ofSeconds(cooldownDurationInSeconds));
            } else {
                throw new IllegalStateException("Log guard strategy unknown: " + strategy);
            }
        }
    }

    /**
     *
     * @return the current state of the log guard
     */
    public boolean isLogGuardActive() {
        if (logGuardStrategy != null) {
            return logGuardStrategy.execute();
        }
        return false;
    }
}
