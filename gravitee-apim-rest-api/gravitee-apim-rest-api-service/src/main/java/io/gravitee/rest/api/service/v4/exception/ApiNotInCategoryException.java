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
package io.gravitee.rest.api.service.v4.exception;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import java.util.Map;

/**
 * @author GraviteeSource Team
 */
public class ApiNotInCategoryException extends AbstractManagementException {

    private final Map<String, String> parameters;

    public ApiNotInCategoryException(final Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return "The API is not in the Category requested.";
    }

    @Override
    public String getTechnicalCode() {
        return "apiCategory.invalid";
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }
}
