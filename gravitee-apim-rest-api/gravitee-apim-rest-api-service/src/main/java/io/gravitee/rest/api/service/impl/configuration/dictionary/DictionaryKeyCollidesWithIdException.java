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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import java.util.Map;

public class DictionaryKeyCollidesWithIdException extends AbstractManagementException {

    private final String key;
    private final String existingDictionaryId;
    private final String existingDictionaryName;

    public DictionaryKeyCollidesWithIdException(String key, String existingDictionaryId, String existingDictionaryName) {
        this.key = key;
        this.existingDictionaryId = existingDictionaryId;
        this.existingDictionaryName = existingDictionaryName;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return (
            "A dictionary with key [" +
            key +
            "] cannot be created because dictionary [" +
            existingDictionaryName +
            "] already uses that value as its id in this environment."
        );
    }

    @Override
    public String getTechnicalCode() {
        return "dictionary.keyCollidesWithId";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of(
            "key",
            key,
            "existingDictionaryId",
            existingDictionaryId,
            "existingDictionaryName",
            String.valueOf(existingDictionaryName)
        );
    }
}
