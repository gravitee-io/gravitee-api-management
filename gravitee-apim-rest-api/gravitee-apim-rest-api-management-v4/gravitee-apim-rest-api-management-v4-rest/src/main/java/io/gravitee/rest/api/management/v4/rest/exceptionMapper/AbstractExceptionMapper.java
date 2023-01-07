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
package io.gravitee.rest.api.management.v4.rest.exceptionMapper;

import io.gravitee.rest.api.management.v4.rest.model.ErrorEntity;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import java.util.Map;
import javax.ws.rs.ext.ExceptionMapper;

public abstract class AbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    protected ErrorEntity convert(AbstractManagementException e) {
        return convert(e, e.getHttpStatusCode(), e.getTechnicalCode(), e.getParameters());
    }

    protected ErrorEntity convert(final Throwable t, final int status) {
        return convert(t, status, null, null);
    }

    protected ErrorEntity convert(final Throwable t, final int status, final String technicalCode, final Map<String, String> parameters) {
        final ErrorEntity errorEntity = new ErrorEntity();

        errorEntity.setHttpStatus(status);
        errorEntity.setMessage(t.getMessage());
        errorEntity.setParameters(parameters);
        errorEntity.setTechnicalCode(technicalCode);
        return errorEntity;
    }
}
