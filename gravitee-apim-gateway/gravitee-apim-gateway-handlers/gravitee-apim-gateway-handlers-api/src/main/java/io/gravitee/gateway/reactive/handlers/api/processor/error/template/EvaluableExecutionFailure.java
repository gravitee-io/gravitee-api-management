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
package io.gravitee.gateway.reactive.handlers.api.processor.error.template;

import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EvaluableExecutionFailure {

    private final ExecutionFailure executionFailure;

    public EvaluableExecutionFailure(final ExecutionFailure executionFailure) {
        this.executionFailure = executionFailure;
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

    public Map<String, Object> getParameters() {
        return executionFailure.parameters();
    }
}
