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
package io.gravitee.apim.core.api.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.Map;

public class MaskedApiPropertyValueException extends ValidationDomainException {

    public MaskedApiPropertyValueException(String propertyKey) {
        super(
            "Property [" +
                propertyKey +
                "] holds the placeholder that stands in for an encrypted value, but no encrypted value is " +
                "stored under that key. Send the real value instead.",
            Map.of("key", propertyKey),
            "api.property.maskedValue"
        );
    }
}
