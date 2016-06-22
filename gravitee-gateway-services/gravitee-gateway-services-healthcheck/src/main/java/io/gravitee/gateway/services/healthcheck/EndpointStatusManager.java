/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.healthcheck;

import io.gravitee.definition.model.Endpoint;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
class EndpointStatusManager {

    /**
     * Number of consecutive valid health checks before considering the server as UP
     */
    private static final int RISE_LIMIT = 2;

    /**
     * Number of consecutive invalid health checks before considering the server as DOWN
     */
    private static final int FALL_LIMIT = 3;

    private Map<Endpoint, Counter> statuses = new HashMap<>();

    public void update(Endpoint endpoint, boolean success) {
        Counter counter = statuses.computeIfAbsent(endpoint, edpt -> new Counter());

        if (success) {
            counter.rise();
        } else {
            counter.fall();
        }

        // Calculate the endpoint status
        endpoint.setStatus(counter.status());
    }

    private class Counter {
        private int rise = RISE_LIMIT;

        private int fall;

        private boolean success;
        private boolean previous;

        void rise() {
            rise = Math.min(++rise, RISE_LIMIT);
            fall = Math.max(--fall, 0);
            previous = success;
            success = true;
        }

        void fall() {
            fall = Math.min(++fall, FALL_LIMIT);
            rise = Math.max(--rise, 0);
            previous = success;
            success = false;
        }

        Endpoint.Status status() {
            if (rise == RISE_LIMIT) {
                return Endpoint.Status.UP;
            } else if(fall == FALL_LIMIT) {
                return Endpoint.Status.DOWN;
            } else if(success && !previous) {
                return Endpoint.Status.TRANSITIONALLY_UP;
            } else {
                return Endpoint.Status.TRANSITIONALLY_DOWN;
            }
        }
    }
}
