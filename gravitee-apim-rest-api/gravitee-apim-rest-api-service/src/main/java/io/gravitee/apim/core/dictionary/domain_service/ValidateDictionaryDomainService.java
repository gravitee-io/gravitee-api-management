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
package io.gravitee.apim.core.dictionary.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.dictionary.model.Dictionary;
import io.gravitee.apim.core.dictionary.model.DictionaryType;
import io.gravitee.apim.core.exception.ValidationDomainException;

@DomainService
public class ValidateDictionaryDomainService {

    public void validate(Dictionary dictionary) {
        if (dictionary.getType() == DictionaryType.MANUAL) {
            if (dictionary.getProperties() == null || dictionary.getProperties().isEmpty()) {
                throw new ValidationDomainException("Manual dictionary must have at least one property.");
            }
            if (dictionary.getProvider() != null || dictionary.getTrigger() != null) {
                throw new ValidationDomainException(
                    "Manual dictionary must not have 'dynamic' properties (provider, trigger). Set type to 'DYNAMIC' or remove them."
                );
            }
        } else if (dictionary.getType() == DictionaryType.DYNAMIC) {
            if (dictionary.getProvider() == null || dictionary.getTrigger() == null) {
                throw new ValidationDomainException("Dynamic dictionary must have a provider and a trigger.");
            }
            if (dictionary.getProperties() != null && !dictionary.getProperties().isEmpty()) {
                throw new ValidationDomainException(
                    "Dynamic dictionary must not have 'manual' properties. Set type to 'MANUAL' or remove them."
                );
            }
        }
    }
}
