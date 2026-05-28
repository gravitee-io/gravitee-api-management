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
package io.gravitee.gamma.rest.core.tracing.exception;

import io.gravitee.apim.core.exception.NotFoundDomainException;

/**
 * Raised when the trace explorer is asked for a trace id that doesn't exist in the configured backing
 * store — or where the env / module / api resource-attribute filter rejects every matching span,
 * which collapses to the same "not found" outcome from the caller's perspective. The apim
 * {@code NotFoundDomainExceptionMapper} translates this to HTTP 404.
 *
 * @author GraviteeSource Team
 */
public class TraceNotFoundException extends NotFoundDomainException {

    public TraceNotFoundException(String traceId, String apiId) {
        super("Trace " + traceId + " not found for API " + apiId, traceId);
    }
}
