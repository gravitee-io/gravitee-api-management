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
package io.gravitee.gateway.reactive.core.context.interruption;

import io.gravitee.gateway.reactive.api.ExecutionFailure;

/**
 * This exception is thrown to indicate that the current execution context has been interrupted with error
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InterruptionFailureException extends RuntimeException {

    private final ExecutionFailure executionFailure;

    public InterruptionFailureException(final ExecutionFailure executionFailure) {
        this.executionFailure = executionFailure;
    }

    public ExecutionFailure getExecutionFailure() {
        return executionFailure;
    }
}
