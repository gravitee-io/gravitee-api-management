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
package io.gravitee.gamma.authorization.service.exception;

import java.util.Objects;

public class AuthzInvalidEntityIdException extends AuthzApiException {

    private final AuthzEntityIdValidationCode code;

    public AuthzInvalidEntityIdException(AuthzEntityIdValidationCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    public AuthzEntityIdValidationCode code() {
        return code;
    }

    @Override
    public int httpStatus() {
        return 400;
    }

    @Override
    public String errorCode() {
        return code.name();
    }
}
