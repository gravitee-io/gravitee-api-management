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
package io.gravitee.apim.core.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class ValidationDomainException extends AbstractDomainException {

    private final Map<String, String> parameters = new HashMap<>();
    private final String technicalCode;

    public ValidationDomainException(String message) {
        this(message, (String) null);
    }

    public ValidationDomainException(String message, String technicalCode) {
        super(message);
        this.technicalCode = technicalCode;
    }

    public ValidationDomainException(String message, Map<String, String> parameters) {
        this(message, parameters, null);
    }

    public ValidationDomainException(String message, Map<String, String> parameters, String technicalCode) {
        super(message);
        this.parameters.putAll(parameters);
        this.technicalCode = technicalCode;
    }

    public ValidationDomainException(String message, Throwable cause) {
        super(message, cause);
        this.technicalCode = null;
    }
}
