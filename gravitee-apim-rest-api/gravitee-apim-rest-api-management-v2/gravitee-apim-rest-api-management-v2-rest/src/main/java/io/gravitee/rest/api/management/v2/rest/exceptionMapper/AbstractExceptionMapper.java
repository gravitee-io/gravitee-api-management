/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.exceptionMapper;

import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.util.Map;

public abstract class AbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    protected Error convert(AbstractManagementException e) {
        return convert(e, e.getHttpStatusCode(), e.getTechnicalCode(), e.getParameters());
    }

    protected Error convert(final Throwable t, final int status) {
        return convert(t, status, null, null);
    }

    protected Error convert(final Throwable t, final int status, final String technicalCode, final Map<String, String> parameters) {
        return Error.builder().httpStatus(status).message(t.getMessage()).parameters(parameters).technicalCode(technicalCode).build();
    }
}
