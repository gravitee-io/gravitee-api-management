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
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TechnicalManagementException extends AbstractManagementException {

    public TechnicalManagementException() {}

    public TechnicalManagementException(Throwable cause) {
        super(cause);
    }

    public TechnicalManagementException(String message) {
        super(message);
    }

    public TechnicalManagementException(String message, Throwable cause) {
        super(message, cause);
    }

    public static TechnicalManagementException ofTryingToFindById(Class<?> clazz, String id, Throwable cause) {
        String message = String.format("An error occurred while trying to find a %s with id %s", clazz.getSimpleName(), id);
        return new TechnicalManagementException(message, cause);
    }

    public static TechnicalManagementException ofTryingToCreateWithId(Class<?> clazz, String id, Throwable cause) {
        String message = String.format("An error occurred while trying to create a %s with id %s", clazz.getSimpleName(), id);
        return new TechnicalManagementException(message, cause);
    }

    public static TechnicalManagementException ofTryingToUpdateWithId(Class<?> clazz, String id, Throwable cause) {
        String message = String.format("An error occurred while trying to update the %s with id %s", clazz.getSimpleName(), id);
        return new TechnicalManagementException(message, cause);
    }

    public static TechnicalManagementException ofTryingToDeleteWithId(Class<?> clazz, String id, Throwable cause) {
        String message = String.format("An error occurred while trying to delete the %s with id %s", clazz.getSimpleName(), id);
        return new TechnicalManagementException(message, cause);
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.INTERNAL_SERVER_ERROR_500;
    }

    @Override
    public String getTechnicalCode() {
        return "unexpected";
    }

    @Override
    public Map<String, String> getParameters() {
        return null;
    }
}
