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
package io.gravitee.rest.api.portal.rest.provider.domain;

import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.util.Map;

public abstract class AbstractDomainExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    protected ErrorResponse convert(final Throwable t, final int status) {
        return convert(t, status, null, null);
    }

    protected ErrorResponse convert(final Throwable t, final int status, final String technicalCode, final Map<String, String> parameters) {
        final Error error = new Error();
        error.setStatus(String.valueOf(status));
        error.message(t.getMessage());
        error.code(technicalCode != null ? "errors." + technicalCode : "errors.unexpected");
        error.parameters(parameters);

        ErrorResponse response = new ErrorResponse();
        response.addErrorsItem(error);
        return response;
    }
}
