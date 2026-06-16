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

import io.gravitee.apim.core.exception.NotFoundDomainException;

/**
 * Raised when the log detail endpoint cannot find the requested log — either because the id does
 * not exist, or because the caller's {@code apiId} scope does not match (404 collapse to prevent
 * cross-API existence probing). The apim {@code NotFoundDomainExceptionMapper} translates this to
 * HTTP 404.
 *
 * @author GraviteeSource Team
 */
public class LogDetailNotFoundException extends NotFoundDomainException {

    public LogDetailNotFoundException(String requestId, String apiId) {
        super("Log " + requestId + " not found for API " + apiId, requestId);
    }
}
