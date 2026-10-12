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
package io.gravitee.gateway.reactive.policy;

import io.gravitee.gateway.reactive.api.ExecutionPhase;

/**
 * Thrown when a policy is installed on the gateway but no instance could be created from its configuration, so that
 * the flow it belongs to is never executed without it.
 *
 * @author GraviteeSource Team
 */
public class PolicyInstantiationException extends RuntimeException {

    public PolicyInstantiationException(final String policyId, final ExecutionPhase phase) {
        super(String.format("Unable to instantiate policy [%s] for phase [%s]", policyId, phase));
    }
}
