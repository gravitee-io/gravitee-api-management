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
import java.util.HashMap;
import java.util.Map;

/**
 * @author GraviteeSource Team
 */
public class InvalidUserException extends AbstractManagementException {

    private final String message;
    private final String source;
    private final String userId;
    private final String organizationId;

    public InvalidUserException(String message, String source, String userId, String organizationId) {
        this.message = message;
        this.source = source;
        this.userId = userId;
        this.organizationId = organizationId;
    }

    public static InvalidUserException cannotBeCreated(String source, String userId, String organizationId) {
        return new InvalidUserException("User cannot be created.", source, userId, organizationId);
    }

    public static InvalidUserException cannotBeUpdated(String source, String userId, String organizationId) {
        return new InvalidUserException("User cannot be updated.", source, userId, organizationId);
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getTechnicalCode() {
        return "user.invalid";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("user", userId);
        parameters.put("organizationId", organizationId);
        parameters.put("source", source);
        return parameters;
    }
}
