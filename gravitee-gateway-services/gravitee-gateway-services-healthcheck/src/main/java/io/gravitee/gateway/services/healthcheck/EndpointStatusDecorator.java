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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointStatusDecorator {

    /**
     * Number of consecutive valid health checks before considering the server as UP
     */
    private static final int RISE_LIMIT = 2;

    /**
     * Number of consecutive invalid health checks before considering the server as DOWN
     */
    private static final int FALL_LIMIT = 3;

    private final Counter counter = new Counter();

    private final Endpoint endpoint;

    public EndpointStatusDecorator(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void updateStatus(boolean success) {
        // Calculate the endpoint status
        if (success) {
            counter.rise();
        } else {
            counter.fall();
        }

        // Set status
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
