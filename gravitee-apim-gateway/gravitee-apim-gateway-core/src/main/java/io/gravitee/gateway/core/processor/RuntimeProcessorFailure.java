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
package io.gravitee.gateway.core.processor;

import io.gravitee.gateway.api.processor.ProcessorFailure;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RuntimeProcessorFailure implements ProcessorFailure {

    static final String GATEWAY_PROCESSOR_INTERNAL_ERROR_KEY = "GATEWAY_PROCESSOR_INTERNAL_ERROR_KEY";

    private final String message;

    public RuntimeProcessorFailure(final String message) {
        this.message = message;
    }

    @Override
    public int statusCode() {
        return 500;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public String key() {
        return GATEWAY_PROCESSOR_INTERNAL_ERROR_KEY;
    }

    @Override
    public Map<String, Object> parameters() {
        return null;
    }

    @Override
    public String contentType() {
        return null;
    }
}
