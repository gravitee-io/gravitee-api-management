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
public class DictionaryPropertyEncryptedToPlainException extends AbstractValidationException {

    private final String dictionaryId;
    private final String propertyKey;

    public DictionaryPropertyEncryptedToPlainException(String dictionaryId, String propertyKey) {
        this.dictionaryId = dictionaryId;
        this.propertyKey = propertyKey;
    }

    @Override
    public String getMessage() {
        return (
            "Dictionary property [" +
            propertyKey +
            "] on dictionary [" +
            dictionaryId +
            "] is encrypted. Its value cannot be replaced with plaintext, and it cannot be declared plain; " +
            "supply the already-encrypted value with 'encrypted' set to true, or leave the value unchanged."
        );
    }

    @Override
    public String getTechnicalCode() {
        return "dictionary.property.encryptedToPlain";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("dictionary", dictionaryId);
        parameters.put("property", propertyKey);
        return unmodifiableMap(parameters);
    }

    @Override
    public Map<String, String> getConstraints() {
        return Map.of();
    }
}
