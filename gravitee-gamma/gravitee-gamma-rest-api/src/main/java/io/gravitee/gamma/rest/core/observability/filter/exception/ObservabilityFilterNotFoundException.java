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
package io.gravitee.gamma.rest.core.observability.filter.exception;

import io.gravitee.apim.core.exception.NotFoundDomainException;

/**
 * Raised when the values endpoint addresses a filter name that isn't in the catalog. The filter is
 * addressed via the URL path, so a missing path segment is a natural 404 (mapped by the apim
 * {@code NotFoundDomainExceptionMapper}) — distinct from
 * {@link UnsupportedObservabilityFilterException#unknownName(String)}, which is a request-body
 * validation error (400) on the query endpoints.
 *
 * @author GraviteeSource Team
 */
public class ObservabilityFilterNotFoundException extends NotFoundDomainException {

    public ObservabilityFilterNotFoundException(String filterName) {
        super("Observability filter '" + filterName + "' not found", filterName);
    }
}
