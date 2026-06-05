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
package io.gravitee.rest.api.service.exceptions;

import io.gravitee.common.http.HttpStatusCode;
import java.util.Map;

/**
 * Thrown when the API definition cannot be fetched from a remote URL (the host is unreachable, the
 * remote responds with a non-success status, etc.).
 *
 * <p>The cause is preserved for server-side logging but never surfaced to the caller: the message is a
 * generic, user-facing string so we don't leak the remote response body, the failing host, or the
 * underlying exception class through the API error.
 */
public class ApiDefinitionFetchException extends AbstractManagementException {

    public ApiDefinitionFetchException(Throwable cause) {
        super(cause);
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getTechnicalCode() {
        return "api.definition.fetch.failed";
    }

    @Override
    public Map<String, String> getParameters() {
        return null;
    }

    @Override
    public String getMessage() {
        return "Unable to fetch the API definition from the provided URL.";
    }
}
