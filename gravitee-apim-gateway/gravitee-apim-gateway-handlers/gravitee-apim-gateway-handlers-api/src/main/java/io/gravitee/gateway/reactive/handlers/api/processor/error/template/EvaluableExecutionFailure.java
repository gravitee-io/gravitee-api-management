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
package io.gravitee.gateway.reactive.handlers.api.processor.error.template;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EvaluableExecutionFailure {

    private final ExecutionFailure executionFailure;
    private final String cause;

    public EvaluableExecutionFailure(final ExecutionFailure executionFailure) {
        this(executionFailure, null);
    }

    /**
     * @param cause the cleaned-up failure detail (the {@code Diagnostic} message stored on the metrics and indexed in
     *   analytics), exposed to templates as {@code {#error.cause}}. May be {@code null}, in which case
     *   {@link #getCause()} falls back to the {@link ExecutionFailure#message()}.
     */
    public EvaluableExecutionFailure(final ExecutionFailure executionFailure, final String cause) {
        this.executionFailure = executionFailure;
        this.cause = cause;
    }

    public int getStatusCode() {
        return executionFailure.statusCode();
    }

    public String getKey() {
        return executionFailure.key();
    }

    public String getMessage() {
        return executionFailure.message();
    }

    public String getCause() {
        return cause != null ? cause : executionFailure.message();
    }

    public Map<String, Object> getParameters() {
        return executionFailure.parameters();
    }
}
