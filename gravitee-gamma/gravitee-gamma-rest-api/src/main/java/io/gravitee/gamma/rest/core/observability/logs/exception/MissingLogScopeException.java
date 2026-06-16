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
package io.gravitee.gamma.rest.core.observability.logs.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;

/**
 * Raised when a required scope dimension on the log detail endpoint — currently {@code apiId} — is
 * missing or blank. The apim {@code ValidationDomainExceptionMapper} translates this to HTTP 400.
 *
 * @author GraviteeSource Team
 */
public class MissingLogScopeException extends ValidationDomainException {

    public MissingLogScopeException(String dimensionName) {
        super("Required scope dimension '" + dimensionName + "' is missing", "observability.log.scope.missing");
    }
}
