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
package io.gravitee.gateway.handlers.api.processor.error.templates;

import io.gravitee.gateway.api.processor.ProcessorFailure;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EvaluableProcessorFailure {

    private final ProcessorFailure failure;

    public EvaluableProcessorFailure(final ProcessorFailure failure) {
        this.failure = failure;
    }

    public int getStatusCode() {
        return failure.statusCode();
    }

    public String getKey() {
        return failure.key();
    }

    public String getMessage() {
        return failure.message();
    }

    public Map<String, Object> getParameters() {
        return failure.parameters();
    }
}
