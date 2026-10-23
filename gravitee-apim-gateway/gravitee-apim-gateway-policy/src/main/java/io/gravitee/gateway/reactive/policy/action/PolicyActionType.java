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
package io.gravitee.gateway.reactive.policy.action;

import io.gravitee.gateway.reactive.api.ExecutionPhase;

/**
 * The moments a policy can register an action for on the execution context, apart from its own execution.
 */
public enum PolicyActionType {
    /** Right before the endpoint is invoked. */
    ENDPOINT_REQUEST("endpoint-request", ExecutionPhase.REQUEST),
    /** Right before the entrypoint sends the response. */
    ENTRYPOINT_RESPONSE("entrypoint-response", ExecutionPhase.RESPONSE),
    /** Once the response has been written, whichever way the request ended. */
    TERMINATE("terminate", ExecutionPhase.RESPONSE);

    private final String label;
    private final ExecutionPhase phase;

    PolicyActionType(final String label, final ExecutionPhase phase) {
        this.label = label;
        this.phase = phase;
    }

    public String label() {
        return label;
    }

    public ExecutionPhase phase() {
        return phase;
    }
}
