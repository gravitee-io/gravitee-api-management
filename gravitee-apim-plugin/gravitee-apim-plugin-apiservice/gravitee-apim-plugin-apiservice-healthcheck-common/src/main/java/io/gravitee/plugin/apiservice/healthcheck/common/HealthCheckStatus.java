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
package io.gravitee.plugin.apiservice.healthcheck.common;

import static io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint.Status.DOWN;
import static io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint.Status.TRANSITIONALLY_DOWN;
import static io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint.Status.TRANSITIONALLY_UP;
import static io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint.Status.UP;

import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class HealthCheckStatus {

    @Getter
    @Setter
    private ManagedEndpoint.Status currentStatus;

    private final int successThreshold;
    private final int failureThreshold;
    private int successCount;
    private int failureCount;

    public HealthCheckStatus(final ManagedEndpoint.Status currentStatus, int successThreshold, int failureThreshold) {
        this.currentStatus = currentStatus;
        this.successThreshold = successThreshold;
        this.failureThreshold = failureThreshold;
    }

    /**
     * Report an health check success and adapt the current status consequently depending on the success threshold.
     * The following transitions can occur:
     * <ul>
     *     <li><code>UP</code> -> <code>UP</code>: the current status is already <code>UP</code>, no change</li>
     *     <li><code>TRANSITIONALLY_UP | TRANSITIONALLY_DOWN</code> -> <code>UP</code>: the current status was in a transition and the success threshold has been reached, move to <code>UP</code></li>
     *     <li><code>DOWN</code> -> <code>TRANSITIONALLY_UP</code>: the current status was <code>DOWN</code> and a success is reported, move to <code>TRANSITIONALLY_UP</code></li>
     * </ul>
     *
     * @return the new status after a success has been reported.
     */
    public ManagedEndpoint.Status reportSuccess() {
        if (currentStatus != UP) {
            successCount++;

            if (currentStatus == DOWN && successCount < successThreshold) {
                currentStatus = TRANSITIONALLY_UP;
            } else if (successCount == successThreshold) {
                currentStatus = UP;
                successCount = 0;
            }
        }

        return currentStatus;
    }

    /**
     * Report an health check failure and adapt the current status consequently depending on the success and failure thresholds.
     * The following transitions can occur:
     * <ul>
     *     <li><code>DOWN</code> -> <code>DOWN</code>: the current status is already <code>DOWN</code>, no change</li>
     *     <li><code>TRANSITIONALLY_UP | TRANSITIONALLY_DOWN</code> -> <code>DOWN</code>: the current status was in a transition and the failure threshold has been reached, back to <code>DOWN</code></li>
     *     <li><code>UP</code> -> <code>TRANSITIONALLY_DOWN</code>: the current status was <code>UP</code> and a failure is reported, move to <code>TRANSITIONALLY_DOWN</code></li>
     * </ul>
     *
     * @return the new status after a failure has been reported.
     */
    public ManagedEndpoint.Status reportFailure() {
        if (currentStatus != DOWN) {
            failureCount++;

            if (currentStatus == UP && failureCount < failureThreshold) {
                currentStatus = TRANSITIONALLY_DOWN;
            } else if (failureCount == failureThreshold) {
                currentStatus = DOWN;
                failureCount = 0;
            }
        }

        return currentStatus;
    }
}
