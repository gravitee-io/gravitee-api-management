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

import io.gravitee.apim.core.exception.ValidationDomainException;

/**
 * Raised when a required scope dimension on the trace explorer endpoints — currently {@code module}
 * and {@code apiId} — is missing or blank. Both bound the query so no unbounded scan ever hits the
 * tracing backend; see {@code TracingResource}'s class javadoc for the full rationale. The apim
 * {@code ValidationDomainExceptionMapper} translates this to HTTP 400.
 *
 * <p>Name intentionally describes the semantic ({@code missing scope}) rather than the transport —
 * a scope dimension can arrive on the wire as a query param ({@code GET /{traceId}}), a JSON body
 * field ({@code POST /search}), or any future endpoint variant. The {@code technicalCode}
 * {@code tracing.scope.missing} stays stable across those shapes.
 *
 * @author GraviteeSource Team
 */
public class MissingTracingScopeException extends ValidationDomainException {

    public MissingTracingScopeException(String dimensionName) {
        super("Required scope dimension '" + dimensionName + "' is missing", "tracing.scope.missing");
    }
}
