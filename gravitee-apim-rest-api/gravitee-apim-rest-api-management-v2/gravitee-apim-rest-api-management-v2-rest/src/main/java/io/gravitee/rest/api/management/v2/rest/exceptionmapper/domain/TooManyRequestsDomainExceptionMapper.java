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
package io.gravitee.rest.api.management.v2.rest.exceptionmapper.domain;

import io.gravitee.apim.core.exception.TooManyRequestsDomainException;
import io.gravitee.common.http.HttpStatusCode;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class TooManyRequestsDomainExceptionMapper extends AbstractDomainExceptionMapper<TooManyRequestsDomainException> {

    @Override
    public Response toResponse(TooManyRequestsDomainException exception) {
        var response = Response.status(HttpStatusCode.TOO_MANY_REQUESTS_429)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(convert(exception, HttpStatusCode.TOO_MANY_REQUESTS_429));
        if (exception.getRetryAfter() != null) {
            response.header(HttpHeaders.RETRY_AFTER, retryAfterSeconds(exception));
        }
        return response.build();
    }

    /**
     * Retry-After is expressed in whole seconds; round up so that a client honoring it never retries too early.
     */
    private static long retryAfterSeconds(TooManyRequestsDomainException exception) {
        return Math.max(1, (long) Math.ceil(exception.getRetryAfter().toMillis() / 1000.0));
    }
}
