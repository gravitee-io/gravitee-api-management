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

import io.gravitee.gateway.report.guard.LogGuardStrategy;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Basic strategy that check the memory pressure and activate the guard for
 * a defined period if the threshold is reached before checking the pressure again.
 * This strategy prevents an oscillation effect if the memory is near the threshold.
 *
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class CoolDownLogGuardStrategy implements LogGuardStrategy {

    private final NodeHealthCheckService nodeHealthCheckService;
    private final Duration cooldownDuration;

    private Instant cooldownStartTime;
    private final AtomicBoolean guardState = new AtomicBoolean(false);

    @Override
    public String getName() {
        return "cooldown";
    }

    @Override
    public boolean execute() {
        // Check if pressure is too high and guard is not activated
        if (nodeHealthCheckService.isGcPressureTooHigh() && !guardState.get()) {
            //Initialize cooldownStartTime
            cooldownStartTime = Instant.now();
            guardState.set(true);
            log.debug("Cooldown started for {}seconds", cooldownDuration.toSeconds());
        } else if (guardState.get()) {
            //Check if cooldown period is finished
            if (Instant.now().isAfter(cooldownStartTime.plus(cooldownDuration))) {
                if (nodeHealthCheckService.isGcPressureTooHigh()) {
                    // Reset cooldown start time if memory pressure is still too high
                    cooldownStartTime = Instant.now();
                    guardState.set(true);
                    log.debug("Cooldown restarted");
                } else {
                    guardState.set(false);
                    log.debug("Cooldown finished");
                }
            }
        }

        return guardState.get();
    }
}
