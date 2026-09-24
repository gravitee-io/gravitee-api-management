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
package io.gravitee.rest.api.service.impl.configuration.dictionary;

import static java.util.Collections.unmodifiableMap;

import io.gravitee.rest.api.service.exceptions.AbstractValidationException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author GraviteeSource Team
 */
public class InvalidDictionaryPropertyOptionsException extends AbstractValidationException {

    private final String propertyKey;
    private final String detail;

    public InvalidDictionaryPropertyOptionsException(String propertyKey, String detail) {
        this.propertyKey = propertyKey;
        this.detail = detail;
    }

    @Override
    public String getMessage() {
        return "Options for dictionary property [" + propertyKey + "] are invalid: " + detail + ".";
    }

    @Override
    public String getTechnicalCode() {
        return "dictionary.propertyOptions.invalid";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("property", propertyKey);
        parameters.put("detail", detail);
        return unmodifiableMap(parameters);
    }

    @Override
    public Map<String, String> getConstraints() {
        return Map.of();
    }
}
