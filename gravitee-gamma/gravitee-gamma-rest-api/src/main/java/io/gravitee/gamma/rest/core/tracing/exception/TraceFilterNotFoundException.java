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
 * Raised when the values endpoint addresses a filter name that isn't in the registry for the given
 * module scope. Distinct from {@link UnsupportedFilterException#unknownName(String)}: that one
 * fires from the search endpoint's translator when the body carries an unknown name (validation
 * error → HTTP 400), this one fires from a URL path lookup ("the addressed sub-resource doesn't
 * exist" → HTTP 404 via the apim {@code NotFoundDomainExceptionMapper}).
 *
 * @author GraviteeSource Team
 */
public class TraceFilterNotFoundException extends NotFoundDomainException {

    public TraceFilterNotFoundException(String filterName) {
        super("Trace filter '" + filterName + "' not found", filterName);
    }
}
